package com.example.game

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*          // your Room entities and DAO (MatchRecord, UserProfile, etc.)
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * ViewModel for the game – manages the game engine, multiplayer, database, and UI state.
 * All data classes (Snake, Orb, ArenaTheme, etc.) come from GameModels.kt in this package.
 */
class GameViewModel(application: Application) : AndroidViewModel(application) {

    // ---------- Database & Repository ----------
    private val db = GameDatabase.getDatabase(application, viewModelScope)
    private val repository = GameRepository(db.gameDao())

    // ---------- Exposed UI State ----------
    val userProfile: StateFlow<UserProfile?> = repository.userProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val unlockedCosmetics: StateFlow<List<UnlockedCosmetic>> = repository.unlockedCosmetics
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val matchRecords: StateFlow<List<MatchRecord>> = repository.matchRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val clans: StateFlow<List<Clan>> = repository.clans
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val achievements: StateFlow<List<Achievement>> = repository.achievements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ---------- Game Engine & Multiplayer ----------
    val gameEngine = GameEngine()
    val multiplayerManager = MultiplayerManager()   // Must be defined in com.example.game

    private val _isGameActive = MutableStateFlow(false)
    val isGameActive: StateFlow<Boolean> = _isGameActive.asStateFlow()

    // ---------- Haptics ----------
    private val vibrator = application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    var hapticsEnabled: Boolean = true

    // ---------- UI Settings ----------
    var privateRoomCode: String = ""
    val selectedAbility = MutableStateFlow("SHIELD")

    // ---------- Initialization ----------
    init {
        // Wire up haptic callbacks from the engine to actual device vibration
        gameEngine.onHapticTrigger = { type ->
            triggerDeviceHaptic(type)
        }
    }

    // ---------- Public Actions ----------

    /**
     * Starts a new game with the given mode, theme, and optional room code.
     */
    fun startNewGame(mode: String, theme: ArenaTheme, roomCode: String = "") {
        this.privateRoomCode = roomCode
        viewModelScope.launch {
            val user = repository.userProfile.first()  // suspends until profile is loaded
            val skin = user?.currentSkin ?: "Neon Cyber"

            // Configure engine
            gameEngine.selectedPlayerAbility = selectedAbility.value
            gameEngine.gameMode = mode
            gameEngine.arenaTheme = theme
            gameEngine.resetEngine()

            // Apply player skin
            gameEngine.playerSnake?.let { player ->
                player.skinName = skin
                player.primaryColor = getPrimaryColorForSkin(skin)
                player.secondaryColor = getSecondaryColorForSkin(skin)
            }

            _isGameActive.value = true
        }
    }

    /**
     * Cancels the current game without saving.
     */
    fun cancelActiveGame() {
        _isGameActive.value = false
    }

    /**
     * Finishes the game, saves the match record, and checks achievements.
     */
    fun finishActiveGameAndSave() {
        viewModelScope.launch {
            val score = gameEngine.playerSnake?.score ?: 0
            val placement = gameEngine.rankingPlacement
            val coins = gameEngine.totalCoinsEarned
            val xp = gameEngine.totalXpEarned
            val mode = gameEngine.gameMode

            // Save match record
            val record = MatchRecord(
                mode = mode,
                score = score,
                placement = placement,
                xpEarned = xp,
                coinsEarned = coins
            )
            repository.saveMatchRecord(record)

            // Check achievements
            checkAchievementsState(score, placement, mode)

            _isGameActive.value = false
        }
    }

    /**
     * Checks and updates achievements based on the game outcome.
     */
    private suspend fun checkAchievementsState(score: Int, placement: Int, mode: String) {
        val achievementsList = repository.achievements.first()
        val user = repository.userProfile.first() ?: return

        achievementsList.forEach { ach ->
            if (ach.completed) return@forEach

            var completedNow = false
            var currentVal = ach.currentValue

            when (ach.id) {
                "first_kill" -> {
                    if (gameEngine.totalKills >= 1) {
                        completedNow = true
                        currentVal = 1
                    }
                }
                "score_500" -> {
                    if (score >= 500) {
                        completedNow = true
                        currentVal = maxOf(ach.currentValue, score)
                    }
                }
                "level_5" -> {
                    currentVal = user.level
                    if (user.level >= 5) {
                        completedNow = true
                    }
                }
                "br_win" -> {
                    if (mode == "Battle Royale" && placement == 1) {
                        completedNow = true
                        currentVal = 1
                    }
                }
            }

            if (completedNow || currentVal != ach.currentValue) {
                // Update achievement progress
                repository.updateAchievementProgress(ach.id, currentVal, completedNow)
                if (completedNow) {
                    // Claim reward coins if achievement completed
                    repository.claimAchievementReward(ach.id, ach.rewardCoins)
                }
            }
        }
    }

    /**
     * Purchase a cosmetic item.
     */
    fun buyCosmetic(name: String, type: String, price: Int, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val user = repository.userProfile.first()
            if (user == null) {
                onError("Profile not synced")
                return@launch
            }
            if (user.coins < price) {
                onError("Insufficient Premium Coins")
                return@launch
            }
            val success = repository.unlockCosmetic(name, type, price)
            if (success) {
                onSuccess()
            } else {
                onError("Transaction canceled")
            }
        }
    }

    /**
     * Select a cosmetic as the current skin or trail.
     */
    fun selectCosmetic(name: String, type: String) {
        viewModelScope.launch {
            val user = repository.userProfile.first() ?: return@launch
            val updated = if (type == "skin") {
                user.copy(currentSkin = name)
            } else {
                user.copy(currentTrail = name)
            }
            repository.updateProfile(updated)
        }
    }

    /**
     * Create or join a clan.
     */
    fun joinOrCreateClan(isCreate: Boolean, name: String, tag: String, onCompleted: (String) -> Unit) {
        viewModelScope.launch {
            if (isCreate) {
                val success = repository.createClan(name, tag)
                if (success) {
                    onCompleted("Successfully formed clan [$tag] $name!")
                } else {
                    onCompleted("Could not construct clan. Premium construction costs 300 coins.")
                }
            } else {
                repository.joinClan(name)
                onCompleted("Welcome to $name!")
            }
        }
    }

    fun leaveCurrentClan() {
        viewModelScope.launch {
            repository.leaveClan()
        }
    }

    /**
     * Ad‑based or daily free coins.
     */
    fun earnFreeCoins(amount: Int) {
        viewModelScope.launch {
            val user = repository.userProfile.first() ?: return@launch
            val updated = user.copy(coins = user.coins + amount)
            repository.updateProfile(updated)
        }
    }

    /**
     * Update the player's display name.
     */
    fun updateUsername(newUsername: String) {
        viewModelScope.launch {
            val user = repository.userProfile.first() ?: return@launch
            val updated = user.copy(username = newUsername)
            repository.updateProfile(updated)
        }
    }

    // ---------- Private Helpers ----------

    /**
     * Trigger device vibration based on haptic type.
     */
    private fun triggerDeviceHaptic(type: String) {
        if (!hapticsEnabled) return
        vibrator?.let { v ->
            try {
                if (v.hasVibrator()) {
                    val duration = when (type) {
                        "light" -> 15L
                        "medium" -> 40L
                        "heavy" -> 100L
                        else -> 20L
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val strength = when (type) {
                            "light" -> 60
                            "medium" -> 150
                            "heavy" -> 255
                            else -> 100
                        }
                        v.vibrate(VibrationEffect.createOneShot(duration, strength))
                    } else {
                        @Suppress("DEPRECATION")
                        v.vibrate(duration)
                    }
                }
            } catch (e: Exception) {
                // Safely ignore if permission or hardware is unavailable
                e.printStackTrace()
            }
        }
    }

    /**
     * Map skin name to primary Compose color.
     */
    private fun getPrimaryColorForSkin(skin: String): Color {
        return when (skin) {
            "Neon Cyber" -> Color(0xFF00FFCC)
            "Volcanic Lava" -> Color(0xFFFF3300)
            "Phantom Ghost" -> Color(0xFFD4E6F1)
            "Galactic Cosmic" -> Color(0xFF9933FF)
            "Stealth Cyber" -> Color(0xFF00E676)
            else -> Color(0xFF33D1FF)
        }
    }

    /**
     * Map skin name to secondary Compose color.
     */
    private fun getSecondaryColorForSkin(skin: String): Color {
        return when (skin) {
            "Neon Cyber" -> Color(0xFF0099FF)
            "Volcanic Lava" -> Color(0xFFFFBB00)
            "Phantom Ghost" -> Color(0xFF90A4AE)
            "Galactic Cosmic" -> Color(0xFFFF5252)
            "Stealth Cyber" -> Color(0xFF37474F)
            else -> Color(0xFF0055FF)
        }
    }
}