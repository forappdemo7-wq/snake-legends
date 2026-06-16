package com.example.game

import android.app.Application
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val db = GameDatabase.getDatabase(application, viewModelScope)
    private val repository = GameRepository(db.gameDao())

    // Expose UI states reactively
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

    // Active live Engine configurations
    val gameEngine = GameEngine()
    val multiplayerManager = MultiplayerManager()
    
    private val _isGameActive = MutableStateFlow(false)
    val isGameActive: StateFlow<Boolean> = _isGameActive.asStateFlow()

    private val vibrator = application.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator

    init {
        // Wire up haptic callbacks from the engine to actual Android hardware
        gameEngine.onHapticTrigger = { type ->
            triggerDeviceHaptic(type)
        }
    }

    var privateRoomCode: String = ""
    val selectedAbility = MutableStateFlow("SHIELD")

    fun startNewGame(mode: String, theme: ArenaTheme, roomCode: String = "") {
        this.privateRoomCode = roomCode
        viewModelScope.launch {
            val user = repository.userProfile.first()
            val skin = user?.currentSkin ?: "Neon Cyber"
            
            gameEngine.selectedPlayerAbility = selectedAbility.value
            gameEngine.gameMode = mode
            gameEngine.arenaTheme = theme
            gameEngine.resetEngine()
            
            // Set Player Skin properties
            gameEngine.playerSnake?.let { p ->
                p.skinName = skin
                p.primaryColor = getPrimaryColorForSkin(skin)
                p.secondaryColor = getSecondaryColorForSkin(skin)
            }
            
            _isGameActive.value = true
        }
    }

    fun cancelActiveGame() {
        _isGameActive.value = false
    }

    fun finishActiveGameAndSave() {
        viewModelScope.launch {
            val score = gameEngine.playerSnake?.score ?: 0
            val placement = gameEngine.rankingPlacement
            val coins = gameEngine.totalCoinsEarned
            val xp = gameEngine.totalXpEarned
            val mode = gameEngine.gameMode

            // Log entry
            val record = MatchRecord(
                mode = mode,
                score = score,
                placement = placement,
                xpEarned = xp,
                coinsEarned = coins
            )
            repository.saveMatchRecord(record)
            
            // Re-verify achievement progress directly on successful save
            checkAchievementsState(score, placement, mode)

            _isGameActive.value = false
        }
    }

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
                // TODO: Ensure your GameRepository has these methods:
                //   suspend fun updateAchievementProgress(id: String, currentValue: Int, completed: Boolean)
                //   suspend fun claimAchievementReward(id: String, rewardCoins: Int)
                //
                // For now, we'll call them if they exist, else log a warning.
                try {
                    repository.updateAchievementProgress(ach.id, currentVal, completedNow)
                    if (completedNow) {
                        repository.claimAchievementReward(ach.id, ach.rewardCoins)
                    }
                } catch (e: Exception) {
                    // If the methods don't exist, you can implement them in your repository.
                    // For now, just log to avoid crashing.
                    android.util.Log.w("GameViewModel", "Achievement update failed: ${e.message}")
                }
            }
        }
    }

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

    fun earnFreeCoins(amount: Int) {
        viewModelScope.launch {
            val user = repository.userProfile.first() ?: return@launch
            val updated = user.copy(coins = user.coins + amount)
            repository.updateProfile(updated)
        }
    }

    var hapticsEnabled: Boolean = true

    fun updateUsername(newUsername: String) {
        viewModelScope.launch {
            val user = repository.userProfile.first() ?: return@launch
            val updated = user.copy(username = newUsername)
            repository.updateProfile(updated)
        }
    }

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
                e.printStackTrace()
            }
        }
    }

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