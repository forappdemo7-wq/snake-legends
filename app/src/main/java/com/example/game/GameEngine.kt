// GameEngine.kt
package com.example.game

import androidx.compose.ui.graphics.Color
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import java.util.UUID

/**
 * The core game engine that manages all logic, AI, physics, and events.
 * Uses data classes from GameModels.kt.
 */
class GameEngine {

    // ---------- Arena & Game Configuration ----------
    var arenaWidth = 2000f
    var arenaHeight = 2000f
    var gameMode: String = "Casual" // Casual, Ranked, Battle Royale, Private Room
    var arenaTheme: ArenaTheme = ArenaTheme.CYBER_CITY
    var selectedPlayerAbility = "SHIELD"

    // ---------- Thread‑safe Collections ----------
    private val _snakes = mutableListOf<Snake>()
    private val _orbs = mutableListOf<Orb>()
    private val _particles = mutableListOf<Particle>()
    private val _floatingTexts = mutableListOf<FloatingText>()
    private val _powerUps = mutableListOf<PowerUp>()
    private val _hazards = mutableListOf<Hazard>()
    private val _killEvents = mutableListOf<KillEvent>()

    // Public read‑only copies (safe for UI iteration)
    val snakes: List<Snake> get() = synchronized(_snakes) { _snakes.toList() }
    val orbs: List<Orb> get() = synchronized(_orbs) { _orbs.toList() }
    val particles: List<Particle> get() = synchronized(_particles) { _particles.toList() }
    val floatingTexts: List<FloatingText> get() = synchronized(_floatingTexts) { _floatingTexts.toList() }
    val powerUps: List<PowerUp> get() = synchronized(_powerUps) { _powerUps.toList() }
    val hazards: List<Hazard> get() = synchronized(_hazards) { _hazards.toList() }
    val killEvents: List<KillEvent> get() = synchronized(_killEvents) { _killEvents.toList() }

    // ---------- Player & Game State ----------
    var playerSnake: Snake? = null
        private set

    // Battle Royale safe zone
    var safeZoneRadius = 1200f
    var safeZoneCenter = Vector2D(1000f, 1000f)
    var isSafeZoneShrinking = false

    // Leaderboard
    var alivePlayersCount = 0
        private set
    val rankingList = mutableListOf<Pair<String, Int>>()

    // Game outcomes
    var isGameOver = false  // now public
    var isVictory = false
        private set
    var rankingPlacement = 0
        private set
    var totalCoinsEarned = 0
        private set
    var totalXpEarned = 0
        private set
    var totalKills = 0
        private set

    // Camera shake & weather
    var cameraShake = 0f
        private set
    var activeWeather = "NORMAL"
        private set
    var activeEventName = "CALM"
        private set

    // Haptic callback (set by UI)
    var onHapticTrigger: ((hapticType: String) -> Unit)? = null

    // ---------- Constants (all in frames at 60 fps) ----------
    companion object {
    private const val FRAME_RATE = 60
    private fun secondsToFrames(seconds: Int) = seconds * FRAME_RATE
    private fun secondsToFramesFloat(seconds: Float) = (seconds * FRAME_RATE).toInt()

    // All these are now normal `val` (not `const`) because they are runtime-computed.
    private val ABILITY_SHIELD_DURATION = secondsToFrames(3)
    private val ABILITY_FREEZE_DURATION = secondsToFrames(3)
    private val ABILITY_EMP_DURATION = secondsToFrames(3)
    private val ABILITY_SPEED_BURST_DURATION = secondsToFramesFloat(1.6f)
    private val ABILITY_GHOST_DURATION = secondsToFrames(3)

    private val COOLDOWN_SHIELD = secondsToFrames(9)
    private val COOLDOWN_FREEZE = secondsToFrames(10)
    private val COOLDOWN_EMP = secondsToFrames(11)
    private val COOLDOWN_SPEED = secondsToFrames(7)
    private val COOLDOWN_GHOST = secondsToFrames(9)

    private val POWERUP_DURATION = secondsToFrames(5)
    private val WEATHER_DURATION = secondsToFrames(3)

    private const val ORB_SPAWN_COUNT = 30
    private const val MAX_ORBS = 100
    private const val MIN_SAFE_ZONE_RADIUS = 300f
    private const val SHRINK_SPEED = 0.25f
    private const val TURN_SPEED_BASE = 0.15f
    private const val TURN_SPEED_SLIP = 0.06f
    private const val TURN_SPEED_BOOST = 0.22f
    private const val SPEED_BASE = 4.0f
    private const val SPEED_BOOST = 7.5f
    private const val SPEED_SLOW_FROZEN = 1.5f
    private const val SPEED_SLOW_EMPED = 2.8f
    private const val SPEED_BURST_MAX = 13.0f

    private const val MAGNET_RANGE = 185f
    private const val MAGNET_PULL_FACTOR = 6.5f

    private const val BOT_SPEED_BASE = 3.5f
    private const val BOT_SPEED_BOOST = 6.5f
    private const val BOT_TURN_SPEED = 0.08f
    private const val BOT_TURN_SLIP = 0.03f
    private const val BOT_TURN_ESCAPE = 0.25f

    private const val BODY_SEGMENT_GAP = 6
    private const val EAT_RADIUS = 32f
    private const val HAZARD_ACTIVE_PHASE = 84
    private const val HAZARD_CYCLE = 120

    private const val CELESTIAL_ORB_POINTS = 100
    private const val SUPER_ORB_POINTS = 25
    private const val NORMAL_ORB_POINTS = 5

    private const val COINS_PER_SCORE = 1 // placeholder
    private const val XP_PER_SCORE = 1
    private const val KILL_COINS = 100
    private const val KILL_XP = 400
    private const val WIN_COINS = 500
    private const val WIN_XP = 1000
}
    // Internal timers
    private var weatherTimer = 180
    private var eventDuration = 0

    // ---------- Initialisation ----------
    init {
        resetEngine()
    }

    // ---------- Public API ----------

    fun resetEngine() {
        // Clear all collections
        synchronized(_snakes) { _snakes.clear() }
        synchronized(_orbs) { _orbs.clear() }
        synchronized(_particles) { _particles.clear() }
        synchronized(_floatingTexts) { _floatingTexts.clear() }
        synchronized(_powerUps) { _powerUps.clear() }
        synchronized(_hazards) { _hazards.clear() }
        synchronized(_killEvents) { _killEvents.clear() }

        // Reset state
        isGameOver = false
        isVictory = false
        rankingPlacement = 0
        totalCoinsEarned = 0
        totalXpEarned = 0
        totalKills = 0
        safeZoneRadius = if (gameMode == "Battle Royale") 1200f else 2200f
        isSafeZoneShrinking = gameMode == "Battle Royale"
        cameraShake = 0f
        activeWeather = "NORMAL"
        activeEventName = "CALM"
        weatherTimer = 400
        eventDuration = 0

        val random = Random(System.currentTimeMillis())

        // Spawn theme hazards
        val hazardConfig = when (arenaTheme) {
            ArenaTheme.CYBER_CITY -> listOf("electro_gate" to 5 to 50f)
            ArenaTheme.LAVA_WORLD -> listOf("lava_pit" to 6 to 65f)
            ArenaTheme.FROZEN_ARENA -> listOf("ice_spike" to 7 to 45f)
            ArenaTheme.JUNGLE_TEMPLE -> listOf("totem" to 4 to 55f)
            ArenaTheme.SPACE_STATION -> listOf("quantum_vortex" to 3 to 80f)
            ArenaTheme.NEON_GRID -> listOf("neon_gate" to 5 to 40f)
        }
        hazardConfig.forEach { (typeCount, size) ->
            val (type, count) = typeCount
            repeat(count) { i ->
                val pos = Vector2D(
                    random.nextFloat() * (arenaWidth - 400f) + 200f,
                    random.nextFloat() * (arenaHeight - 400f) + 200f
                )
                synchronized(_hazards) {
                    _hazards.add(Hazard("${type}_$i", type, pos, size))
                }
            }
        }

        // Player snake
        val player = Snake(
            id = "player",
            name = "You (Legend)",
            isPlayer = true,
            position = Vector2D(arenaWidth / 2f, arenaHeight / 2f),
            primaryColor = Color(0xFF00FFCC),
            secondaryColor = Color(0xFF0099FF),
            activeAbilityType = selectedPlayerAbility
        )
        repeat(player.length) { i ->
            player.body.add(Vector2D(player.position.x, player.position.y + i * 15f))
        }
        playerSnake = player
        synchronized(_snakes) { _snakes.add(player) }

        // Bot snakes
        val botNames = listOf(
            "ViperX", "ShadowCrawl", "NeonVenom", "ApexViper", "SlitherKing",
            "SlinkyMaster", "Wraith", "CobaltFangs", "CrimsonOuroboros", "GlitchSnake",
            "StealthBoa", "HydraMax", "PhantomStrider", "AbyssGlow", "VenomGod"
        )
        val abilities = listOf("SHIELD", "FREEZE_PULSE", "EMP_BLAST", "SPEED_BURST", "GHOST_PHASE")
        botNames.forEachIndexed { i, name ->
            val angle = random.nextFloat() * 6.28f
            val pos = Vector2D(
                random.nextFloat() * (arenaWidth - 400f) + 200f,
                random.nextFloat() * (arenaHeight - 400f) + 200f
            )
            val (primary, secondary) = getRandomColorsForBot()
            val bot = Snake(
                id = "bot_$i",
                name = name,
                isPlayer = false,
                position = pos,
                angle = angle,
                primaryColor = primary,
                secondaryColor = secondary,
                skinName = getRandomSkinName(),
                activeAbilityType = abilities.random()
            )
            repeat(bot.length) { j ->
                bot.body.add(Vector2D(pos.x, pos.y + j * 15f))
            }
            synchronized(_snakes) { _snakes.add(bot) }
        }

        spawnOrbs(120)
        spawnPowerUps(6)
        updateLeaderboard()
    }

    /**
     * Syncs multiplayer snake data from a peer map.
     * Requires that `PeerSnakeData` is defined elsewhere (e.g. in MultiplayerManager.kt).
     */
    fun syncMultiplayerSnakes(peers: Map<String, PeerSnakeData>) {
        synchronized(_snakes) {
            val mpIds = peers.keys.map { "mp_$it" }.toSet()
            peers.forEach { (username, peerData) ->
                val peerId = "mp_$username"
                val existing = _snakes.firstOrNull { it.id == peerId }
                val primaryColor = try {
                    Color(android.graphics.Color.parseColor(peerData.primaryColorHex))
                } catch (_: Exception) {
                    Color(0xFFFF3366)
                }
                val secondaryColor = try {
                    Color(android.graphics.Color.parseColor(peerData.secondaryColorHex))
                } catch (_: Exception) {
                    Color(0xFFFF9900)
                }
                if (existing == null) {
                    val snake = Snake(
                        id = peerId,
                        name = peerData.name,
                        isPlayer = false,
                        position = peerData.position,
                        angle = peerData.angle,
                        speed = peerData.speed,
                        length = peerData.length,
                        score = peerData.score,
                        primaryColor = primaryColor,
                        secondaryColor = secondaryColor,
                        isBoosting = peerData.isBoosting,
                        isAlive = peerData.isAlive
                    )
                    snake.body.addAll(peerData.body)
                    _snakes.add(snake)
                } else {
                    existing.position = peerData.position
                    existing.angle = peerData.angle
                    existing.speed = peerData.speed
                    existing.length = peerData.length
                    existing.score = peerData.score
                    existing.isBoosting = peerData.isBoosting
                    existing.isAlive = peerData.isAlive
                    existing.body.clear()
                    existing.body.addAll(peerData.body)
                }
            }
            _snakes.removeAll { it.id.startsWith("mp_") && it.id !in mpIds }
        }
    }

    /**
     * Called every game frame (typically 60 times per second).
     * @param joystickAngle direction of the joystick in radians, or null if not moving
     * @param isBoosting whether the player is holding the boost button
     * @param abilityTriggered whether the player just triggered an ability
     */
    fun onTick(
        joystickAngle: Float?,
        isBoosting: Boolean,
        abilityTriggered: Boolean
    ) {
        if (isGameOver) return

        // 1. Decay camera shake
        cameraShake = (cameraShake - 0.45f).coerceAtLeast(0f)

        // 2. Weather
        updateWeather()

        val player = playerSnake ?: return

        // 3. Update timers & statuses
        updateSnakeStatuses()

        // 4. Magnet pull
        processMagnetPull()

        // 5. Power‑up pickups
        processPowerUpPickups()

        // 6. Hazards
        processHazards()

        // 7. Player ability trigger
        if (abilityTriggered && player.abilityCooldownRemaining <= 0 && !player.specialAbilityActive && player.isAlive) {
            triggerAbility(player)
        }

        // 8. Move player
        moveSnake(player, joystickAngle, isBoosting, isPlayer = true)

        // 9. Move bots
        moveBotSnakes()

        // 10. Head‑to‑body collisions
        processCollisions()

        // 11. Eat orbs
        processOrbEating()

        // 12. Update particles & texts
        updateParticlesAndTexts()

        // 13. Clean dead non‑players
        synchronized(_snakes) {
            _snakes.removeAll { !it.isAlive && !it.isPlayer }
        }

        // 14. Game over / victory
        checkGameEndConditions()

        updateLeaderboard()
    }

    // ---------- Private Helpers ----------

    private fun updateWeather() {
        weatherTimer--
        if (weatherTimer <= 0) {
            weatherTimer = Random.nextInt(320, 550)
            val possible = when (arenaTheme) {
                ArenaTheme.CYBER_CITY -> listOf("NORMAL", "ENERGY_STORM")
                ArenaTheme.LAVA_WORLD -> listOf("NORMAL", "ERUPTION")
                ArenaTheme.FROZEN_ARENA -> listOf("NORMAL", "ICE_BLIZZARD")
                ArenaTheme.JUNGLE_TEMPLE -> listOf("NORMAL", "ANCIENT_RITUAL")
                ArenaTheme.SPACE_STATION -> listOf("NORMAL", "GRAVITY_SHIFT")
                ArenaTheme.NEON_GRID -> listOf("NORMAL", "OVERCHARGE_PULSE")
            }
            activeWeather = if (activeWeather == "NORMAL") possible.random() else "NORMAL"

            if (activeWeather != "NORMAL") {
                cameraShake = 16f
                triggerHaptic("heavy")
                eventDuration = WEATHER_DURATION
                activeEventName = when (activeWeather) {
                    "ENERGY_STORM" -> "ELECTRON LIGHTNING HURRICANE!"
                    "ERUPTION" -> "LAVA VOLCANO ERUPTION!"
                    "ICE_BLIZZARD" -> "SUB‑ZERO COLD ICE BLIZZARD!"
                    "ANCIENT_RITUAL" -> "ANCIENT TOTEM RUNES ACTIVE!"
                    "GRAVITY_SHIFT" -> "AIRLOCK GRAVITY ANOMALY WELL!"
                    "OVERCHARGE_PULSE" -> "OVERCHARGED GRID ENERGY OVERLOAD!"
                    else -> "ARENA CRITICAL OUTBREAK EVENT!"
                }
                playerSnake?.let { p ->
                    addFloatingText(p.position, "MAP WARNING: $activeEventName", Color(0xFFFF3366))
                }
            } else {
                activeEventName = "CALM"
                playerSnake?.let { p ->
                    addFloatingText(p.position, "ARENA STABILIZED", Color(0xFF00FFCC))
                }
            }
        }

        // Active weather effects
        if (eventDuration > 0) {
            eventDuration--
            when (activeWeather) {
                "ENERGY_STORM" -> {
                    cameraShake = maxOf(cameraShake, 1.3f)
                    if (Random.nextInt(50) == 0) {
                        val alive = getAliveSnakes()
                        if (alive.isNotEmpty()) {
                            val victim = alive.random()
                            victim.isEmped = true
                            victim.empTimer = 120
                            addFloatingText(victim.position, "STRUCK BY LIGHTNING STUN!", Color(0xFFFFFF33))
                            if (victim.isPlayer) {
                                triggerHaptic("medium")
                                cameraShake = 10f
                            }
                        }
                    }
                }
                "ERUPTION" -> {
                    cameraShake = maxOf(cameraShake, 1.8f)
                    if (Random.nextInt(25) == 0) {
                        val lavaBall = Vector2D(Random.nextFloat() * arenaWidth, Random.nextFloat() * arenaHeight)
                        synchronized(_orbs) {
                            _orbs.add(Orb(UUID.randomUUID().toString(), lavaBall, 14f, Color(0xFFFF5722), 40, true))
                        }
                        repeat(5) {
                            addParticle(
                                lavaBall,
                                Vector2D(Random.nextFloat() * 6f - 3f, Random.nextFloat() * 6f - 3f),
                                Color(0xFFFF4500),
                                fadeSpeed = 0.04f,
                                size = 12f
                            )
                        }
                    }
                }
                "GRAVITY_SHIFT" -> {
                    val center = Vector2D(arenaWidth / 2f, arenaHeight / 2f)
                    synchronized(_snakes) {
                        _snakes.filter { it.isAlive }.forEach { snake ->
                            val pullDir = (center - snake.position).normalized()
                            snake.position += pullDir * 1.6f
                        }
                    }
                }
                "OVERCHARGE_PULSE" -> {
                    if (Random.nextInt(12) == 0) {
                        val sparkPos = Vector2D(Random.nextFloat() * arenaWidth, Random.nextFloat() * arenaHeight)
                        addParticle(sparkPos, Vector2D(0f, 0f), Color(0xFF00FFCC), fadeSpeed = 0.03f, size = 14f)
                    }
                }
            }
        }

        // Shrink safe zone (Battle Royale)
        if (gameMode == "Battle Royale" && isSafeZoneShrinking) {
            safeZoneRadius = (safeZoneRadius - SHRINK_SPEED).coerceAtLeast(MIN_SAFE_ZONE_RADIUS)
        }
    }

    private fun getAliveSnakes(): List<Snake> = synchronized(_snakes) { _snakes.filter { it.isAlive } }

    private fun updateSnakeStatuses() {
        synchronized(_snakes) {
            _snakes.forEach { snake ->
                if (!snake.isAlive) return@forEach

                // Ability cooldowns
                if (snake.abilityCooldownRemaining > 0) snake.abilityCooldownRemaining--
                if (snake.abilityActiveDuration > 0) {
                    snake.abilityActiveDuration--
                    if (snake.abilityActiveDuration <= 0) {
                        snake.specialAbilityActive = false
                        if (snake.activeAbilityType == "GHOST_PHASE") {
                            snake.activePowerUpType = null
                        }
                    }
                }

                // Debuffs
                if (snake.isFrozen) {
                    snake.freezeTimer--
                    if (snake.freezeTimer <= 0) snake.isFrozen = false
                }
                if (snake.isEmped) {
                    snake.empTimer--
                    if (snake.empTimer <= 0) snake.isEmped = false
                }

                // Power‑up timer (except ghost which is ability‑driven)
                if (snake.activePowerUpType != null && snake.activeAbilityType != "GHOST_PHASE") {
                    snake.powerUpTimer--
                    if (snake.powerUpTimer <= 0) {
                        snake.activePowerUpType = null
                    }
                }
            }
        }
    }

    private fun processMagnetPull() {
        synchronized(_snakes) {
            synchronized(_orbs) {
                _snakes.filter { it.isAlive && it.activePowerUpType == PowerUpType.MAGNET }.forEach { snake ->
                    _orbs.forEach { orb ->
                        val dist = snake.position.distance(orb.position)
                        if (dist < MAGNET_RANGE) {
                            val pullForce = ((MAGNET_RANGE - dist) / MAGNET_RANGE) * MAGNET_PULL_FACTOR + 1.5f
                            val pullDir = (snake.position - orb.position).normalized()
                            orb.position += pullDir * pullForce
                        }
                    }
                }
            }
        }
    }

    private fun processPowerUpPickups() {
        val toRemove = mutableListOf<String>()
        synchronized(_powerUps) {
            synchronized(_snakes) {
                _powerUps.forEach { powerUp ->
                    _snakes.filter { it.isAlive }.forEach { snake ->
                        if (snake.position.distance(powerUp.position) < 38f) {
                            toRemove.add(powerUp.id)
                            if (powerUp.type == PowerUpType.GROWTH) {
                                snake.score += 75
                                snake.length = 4 + (snake.score / 25)
                                if (snake.isPlayer) {
                                    totalCoinsEarned += 25
                                    totalXpEarned += 100
                                    addFloatingText(snake.position, "GROWTH POTION +3 SEGMENTS!", Color(0xFF66BB6A))
                                    triggerHaptic("heavy")
                                    cameraShake = 6f
                                }
                            } else {
                                snake.activePowerUpType = powerUp.type
                                snake.powerUpTimer = POWERUP_DURATION
                                if (snake.isPlayer) {
                                    val msg = when (powerUp.type) {
                                        PowerUpType.MAGNET -> "MAGNET FORCEFIELD ACTIVE!"
                                        PowerUpType.DOUBLE_POINTS -> "DOUBLE POINTS MULTIPLIER!"
                                        PowerUpType.SHIELD -> "FORCEFIELD SHIELD ACTIVE!"
                                        PowerUpType.GHOST -> "GHOST MODE: PHASE THROUGH WALLS!"
                                        PowerUpType.SPEED_BOOST -> "SPEED BOOST VELOCITY ACTIVATED!"
                                        else -> "UPGRADE ACTIVE!"
                                    }
                                    addFloatingText(snake.position, msg, powerUp.color)
                                    triggerHaptic("medium")
                                    cameraShake = 4f
                                }
                            }
                            return@forEach // one snake per power‑up
                        }
                    }
                }
            }
        }
        if (toRemove.isNotEmpty()) {
            synchronized(_powerUps) { _powerUps.removeAll { it.id in toRemove } }
            spawnPowerUps(toRemove.size)
        }
    }

    private fun processHazards() {
        synchronized(_hazards) {
            synchronized(_snakes) {
                _hazards.forEach { hazard ->
                    hazard.state = (hazard.state + 1) % 360
                    _snakes.filter { it.isAlive }.forEach { snake ->
                        val dist = snake.position.distance(hazard.position)
                        when (hazard.type) {
                            "lava_pit" -> {
                                if (dist < hazard.size + 15f) {
                                    if (!isInvulnerable(snake) && Random.nextInt(15) == 0) {
                                        snake.length = maxOf(3, snake.length - 1)
                                        if (snake.isPlayer) {
                                            addFloatingText(snake.position, "THERMAL LAVA BURN! -1 RING", Color(0xFFFF4500))
                                            triggerHaptic("medium")
                                            cameraShake = 7f
                                        }
                                        addParticle(snake.position, Vector2D(Random.nextFloat() * 2f - 1f, -2f), Color(0xFFFF9900), alpha = 0.9f)
                                    }
                                }
                            }
                            "electro_gate" -> {
                                val active = (hazard.state % HAZARD_CYCLE) < HAZARD_ACTIVE_PHASE
                                if (active && dist < hazard.size + 10f) {
                                    if (!isInvulnerable(snake)) {
                                        killSnake(snake, null, "electro_gate")
                                    }
                                }
                            }
                            "ice_spike" -> {
                                if (dist < hazard.size + 12f) {
                                    if (!isInvulnerable(snake)) {
                                        killSnake(snake, null, "ice_spike")
                                    } else {
                                        val pushDir = (snake.position - hazard.position).normalized()
                                        snake.position = hazard.position + pushDir * (hazard.size + 20f)
                                        snake.angle = atan2(pushDir.y.toDouble(), pushDir.x.toDouble()).toFloat()
                                    }
                                }
                            }
                            "totem" -> {
                                if (dist < hazard.size + 40f && Random.nextInt(20) == 0) {
                                    snake.score += 15
                                    snake.length = 4 + (snake.score / 25)
                                    if (snake.isPlayer) {
                                        addFloatingText(snake.position, "ANCIENT COGNITION +15", Color(0xFF81C784))
                                        triggerHaptic("light")
                                    }
                                }
                            }
                            "quantum_vortex" -> {
                                if (dist < hazard.size + 150f) {
                                    val suction = ((hazard.size + 150f) - dist) / (hazard.size + 150f) * 3f
                                    val pullDir = (hazard.position - snake.position).normalized()
                                    snake.position += pullDir * suction
                                    if (dist < hazard.size * 0.45f) {
                                        if (!isInvulnerable(snake)) {
                                            killSnake(snake, null, "quantum_vortex")
                                        }
                                    }
                                }
                            }
                            "neon_gate" -> {
                                val blocked = (hazard.state % 180) > 90
                                if (blocked && dist < hazard.size + 12f) {
                                    if (!isInvulnerable(snake) && Random.nextInt(8) == 0) {
                                        snake.length = maxOf(3, snake.length - 1)
                                        if (snake.isPlayer) {
                                            addFloatingText(snake.position, "LASER GATE GLITCH! -1 RING", Color(0xFF00E5FF))
                                            triggerHaptic("heavy")
                                            cameraShake = 8f
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun isInvulnerable(snake: Snake): Boolean =
        snake.activePowerUpType == PowerUpType.SHIELD ||
                snake.activePowerUpType == PowerUpType.GHOST ||
                snake.specialAbilityActive

    private fun killSnake(snake: Snake, killer: Snake?, cause: String) {
        if (!snake.isAlive) return
        snake.isAlive = false
        if (snake.isPlayer) {
            triggerHaptic("heavy")
            cameraShake = 25f
        } else if (killer?.isPlayer == true) {
            totalKills++
            totalCoinsEarned += KILL_COINS
            totalXpEarned += KILL_XP
            addFloatingText(snake.position, "KILL +${KILL_XP} XP", Color(0xFFFF5252))
            triggerHaptic("heavy")
            cameraShake = 20f
        }
        publishKill(killer, snake, cause)
        eliminateSnake(snake)
    }

    private fun moveSnake(snake: Snake, joystickAngle: Float?, isBoosting: Boolean, isPlayer: Boolean) {
        if (!snake.isAlive) return

        // Player controls
        if (isPlayer) {
            if (joystickAngle != null) {
                var diff = joystickAngle - snake.angle
                while (diff < -Math.PI) diff += (2 * Math.PI).toFloat()
                while (diff > Math.PI) diff -= (2 * Math.PI).toFloat()
                var turnSpeed = TURN_SPEED_BASE
                if (activeWeather == "ICE_BLIZZARD") turnSpeed = TURN_SPEED_SLIP
                else if (snake.abilityActiveDuration > 0 && snake.activeAbilityType == "SPEED_BURST") turnSpeed = TURN_SPEED_BOOST
                snake.angle += diff * turnSpeed
            }
            snake.isBoosting = isBoosting && snake.length > 5 && !snake.isEmped && !snake.isFrozen
        }

        // Speed calculation
        var targetSpeed = if (snake.isBoosting) SPEED_BOOST else SPEED_BASE
        if (snake.activePowerUpType == PowerUpType.SPEED_BOOST) targetSpeed *= 1.6f
        if (snake.isFrozen) targetSpeed = SPEED_SLOW_FROZEN
        if (snake.isEmped) targetSpeed = SPEED_SLOW_EMPED
        if (snake.abilityActiveDuration > 0 && snake.activeAbilityType == "SPEED_BURST") {
            targetSpeed = SPEED_BURST_MAX
        }
        snake.speed += (targetSpeed - snake.speed) * 0.14f

        if (snake.speed > 8f) cameraShake = maxOf(cameraShake, 1.1f)

        // Move
        val dirX = cos(snake.angle.toDouble()).toFloat()
        val dirY = sin(snake.angle.toDouble()).toFloat()
        snake.position += Vector2D(dirX * snake.speed, dirY * snake.speed)

        // Boost length loss
        if (snake.isBoosting && Random.nextInt(12) == 0 && snake.length > 5) {
            snake.length--
            val lastSeg = snake.body.lastOrNull() ?: snake.position
            synchronized(_orbs) {
                _orbs.add(Orb(UUID.randomUUID().toString(), lastSeg, 5f, snake.primaryColor, 4, false))
            }
            if (snake.body.size > snake.length) {
                snake.body.removeAt(snake.body.lastIndex)
            }
        }

        updateBodySegments(snake)

        // Boost particles
        if (snake.isBoosting && Random.nextInt(3) == 0) {
            addParticle(snake.position, Vector2D(-dirX * 3f, -dirY * 3f), snake.primaryColor, fadeSpeed = 0.08f, size = 6f)
        }

        // Boundary handling
        if (snake.activePowerUpType == PowerUpType.GHOST || (snake.specialAbilityActive && snake.activeAbilityType == "GHOST_PHASE")) {
            // Wrap around
            if (snake.position.x < 0) {
                snake.position = Vector2D(arenaWidth, snake.position.y)
                snake.body.clear()
            } else if (snake.position.x > arenaWidth) {
                snake.position = Vector2D(0f, snake.position.y)
                snake.body.clear()
            }
            if (snake.position.y < 0) {
                snake.position = Vector2D(snake.position.x, arenaHeight)
                snake.body.clear()
            } else if (snake.position.y > arenaHeight) {
                snake.position = Vector2D(snake.position.x, 0f)
                snake.body.clear()
            }
        } else {
            if (snake.position.x < 0 || snake.position.x > arenaWidth ||
                snake.position.y < 0 || snake.position.y > arenaHeight
            ) {
                if (isPlayer) {
                    killSnake(snake, null, "border")
                } else {
                    // Bots bounce back
                    snake.position = Vector2D(
                        snake.position.x.coerceIn(30f, arenaWidth - 30f),
                        snake.position.y.coerceIn(30f, arenaHeight - 30f)
                    )
                }
            }
        }

        // Safe zone damage (Battle Royale)
        if (gameMode == "Battle Royale") {
            if (snake.position.distance(safeZoneCenter) > safeZoneRadius) {
                if (Random.nextInt(15) == 0) {
                    snake.length--
                    addFloatingText(snake.position, "-1 RING BURNING", Color(0xFFFF3333))
                    cameraShake = 5f
                    if (snake.length < 3) {
                        killSnake(snake, null, "safe_zone")
                    }
                }
            }
        }
    }

    private fun moveBotSnakes() {
        synchronized(_snakes) {
            _snakes.filter { !it.isPlayer && it.isAlive && !it.id.startsWith("mp_") }.forEach { snake ->
                // AI ability trigger
                if (snake.abilityCooldownRemaining <= 0) {
                    val nearby = _snakes.firstOrNull { it.id != snake.id && it.isAlive && it.position.distance(snake.position) < 220f }
                    if (nearby != null) triggerAbility(snake)
                }

                // Target selection
                snake.botTargetTimer--
                if (snake.botTarget == null || snake.botTargetTimer <= 0) {
                    snake.botTargetTimer = Random.nextInt(60, 150)
                    val choice = Random.nextLong(100)
                    when {
                        choice < 60 -> {
                            val aliveOrbs = synchronized(_orbs) { _orbs.toList() }
                            if (aliveOrbs.isNotEmpty()) {
                                val nearest = aliveOrbs.minByOrNull { it.position.distance(snake.position) }
                                snake.botTarget = nearest?.position
                            }
                        }
                        choice < 90 -> {
                            val others = _snakes.filter { it.id != snake.id && it.isAlive }
                            if (others.isNotEmpty()) {
                                snake.botTarget = others.random().position
                            }
                        }
                        else -> {
                            snake.botTarget = Vector2D(Random.nextFloat() * arenaWidth, Random.nextFloat() * arenaHeight)
                        }
                    }
                    if (snake.botTarget == null) {
                        snake.botTarget = Vector2D(arenaWidth / 2f, arenaHeight / 2f)
                    }
                }

                // Turn toward target
                val target = snake.botTarget ?: return@forEach
                val dx = target.x - snake.position.x
                val dy = target.y - snake.position.y
                val targetAngle = atan2(dy.toDouble(), dx.toDouble()).toFloat()
                var diff = targetAngle - snake.angle
                while (diff < -Math.PI) diff += (2 * Math.PI).toFloat()
                while (diff > Math.PI) diff -= (2 * Math.PI).toFloat()

                var turnSpeed = BOT_TURN_SPEED
                if (activeWeather == "ICE_BLIZZARD") turnSpeed = BOT_TURN_SLIP
                if (snake.position.x < 150f || snake.position.x > arenaWidth - 150f ||
                    snake.position.y < 150f || snake.position.y > arenaHeight - 150f
                ) {
                    turnSpeed = BOT_TURN_ESCAPE
                }
                snake.angle += diff.coerceIn(-turnSpeed, turnSpeed)

                // Random boost toggle
                if (Random.nextInt(50) == 0 && snake.length > 8 && !snake.isFrozen && !snake.isEmped) {
                    snake.isBoosting = !snake.isBoosting
                }

                // Move the bot using the same physics
                moveSnake(snake, null, snake.isBoosting, isPlayer = false)
            }
        }
    }

    private fun processCollisions() {
        synchronized(_snakes) {
            val alive = _snakes.filter { it.isAlive }.toList()
            for (i in alive.indices) {
                val snake = alive[i]
                if (!snake.isAlive || isInvulnerable(snake)) continue
                for (j in alive.indices) {
                    if (i == j) continue
                    val other = alive[j]
                    if (!other.isAlive || isInvulnerable(other)) continue

                    val segments = if (other.body.size > 2) other.body.subList(2, other.body.size) else other.body
                    for (seg in segments) {
                        if (snake.position.distance(seg) < 22f) {
                            killSnake(snake, other, "collision")
                            break
                        }
                    }
                }
            }
        }
    }

    private fun processOrbEating() {
        val eatenIds = mutableSetOf<String>()
        val snakesCopy = getAliveSnakes()

        synchronized(_orbs) {
            _orbs.forEach { orb ->
                snakesCopy.forEach { snake ->
                    if (snake.position.distance(orb.position) < EAT_RADIUS) {
                        val multiplier = if (snake.activePowerUpType == PowerUpType.DOUBLE_POINTS) 2 else 1
                        val points = orb.points * multiplier
                        snake.score += points
                        snake.length = 4 + (snake.score / 25)
                        eatenIds.add(orb.id)

                        val coins = (if (orb.isCelestialOrb) 50 else if (orb.isSuperOrb) 5 else 1) * multiplier
                        val xp = (if (orb.isCelestialOrb) 150 else if (orb.isSuperOrb) 20 else 5) * multiplier

                        if (snake.isPlayer) {
                            totalCoinsEarned += coins
                            totalXpEarned += xp
                            val count = if (orb.isCelestialOrb) 16 else 3
                            repeat(count) {
                                val pColor = if (orb.isCelestialOrb) {
                                    listOf(Color(0xFFFF00FF), Color(0xFF00FFCC), Color(0xFFFFFF33), Color(0xFFE040FB)).random()
                                } else {
                                    orb.color
                                }
                                val speed = if (orb.isCelestialOrb) 8f else 4f
                                addParticle(
                                    orb.position,
                                    Vector2D(Random.nextFloat() * speed - speed / 2, Random.nextFloat() * speed - speed / 2),
                                    pColor,
                                    alpha = 1f,
                                    fadeSpeed = if (orb.isCelestialOrb) 0.03f else 0.08f,
                                    size = if (orb.isCelestialOrb) 8f else 5f
                                )
                            }
                            when {
                                orb.isCelestialOrb -> {
                                    addFloatingText(orb.position, "CELESTIAL ANOMALY! +${points}", Color(0xFFFF00FF))
                                    triggerHaptic("heavy")
                                    cameraShake = 12f
                                }
                                orb.isSuperOrb -> {
                                    addFloatingText(orb.position, "+${points} length", Color(0xFFFFFF33))
                                    triggerHaptic("medium")
                                }
                                else -> triggerHaptic("light")
                            }
                        } else {
                            // Bot eating – just visual for celestial
                            if (orb.isCelestialOrb) {
                                repeat(5) {
                                    addParticle(
                                        orb.position,
                                        Vector2D(Random.nextFloat() * 4f - 2f, Random.nextFloat() * 4f - 2f),
                                        orb.color,
                                        alpha = 0.8f,
                                        fadeSpeed = 0.05f,
                                        size = 6f
                                    )
                                }
                            }
                        }
                        return@forEach // orb eaten
                    }
                }
            }
            _orbs.removeAll { it.id in eatenIds }
            if (_orbs.size < MAX_ORBS) {
                spawnOrbs(ORB_SPAWN_COUNT)
            }
        }
    }

    private fun updateParticlesAndTexts() {
        synchronized(_particles) {
            val iter = _particles.iterator()
            while (iter.hasNext()) {
                val p = iter.next()
                p.position += p.velocity
                p.alpha -= p.fadeSpeed
                if (p.alpha <= 0) iter.remove()
            }
        }
        synchronized(_floatingTexts) {
            val iter = _floatingTexts.iterator()
            while (iter.hasNext()) {
                val t = iter.next()
                t.position += Vector2D(0f, t.speedY)
                t.life--
                t.alpha = (t.life / 40f).coerceIn(0f, 1f)
                if (t.life <= 0) iter.remove()
            }
        }
    }

    private fun checkGameEndConditions() {
        val player = playerSnake ?: return
        if (!player.isAlive) {
            isGameOver = true
            if (gameMode != "Casual") determinePlacement()
            return
        }
        if ((gameMode == "Battle Royale" || gameMode == "Ranked") && !isGameOver) {
            val survivors = getAliveSnakes()
            if (survivors.size == 1 && survivors.first().isPlayer) {
                isVictory = true
                rankingPlacement = 1
                isGameOver = true
                totalCoinsEarned += WIN_COINS
                totalXpEarned += WIN_XP
                triggerHaptic("heavy")
                cameraShake = 20f
            }
        }
    }

    private fun updateLeaderboard() {
        alivePlayersCount = getAliveSnakes().size
        val ranked = synchronized(_snakes) { _snakes.map { it.name to it.score } }
            .sortedByDescending { it.second }
        rankingList.clear()
        rankingList.addAll(ranked.take(10))
    }

    private fun determinePlacement() {
        val liveBots = getAliveSnakes().filter { !it.isPlayer }.size
        rankingPlacement = liveBots + 1
        val bonus = when {
            rankingPlacement == 1 -> 350
            rankingPlacement <= 3 -> 150
            else -> 30
        }
        totalCoinsEarned += bonus
        totalXpEarned += (400 - (rankingPlacement * 20)).coerceAtLeast(50)
    }

    // ---------- Spawning helpers ----------

    private fun spawnPowerUps(amount: Int) {
        repeat(amount) {
            val id = UUID.randomUUID().toString()
            val pos = Vector2D(
                Random.nextFloat() * (arenaWidth - 160f) + 80f,
                Random.nextFloat() * (arenaHeight - 160f) + 80f
            )
            val type = PowerUpType.values().random()
            val color = when (type) {
                PowerUpType.MAGNET -> Color(0xFFE040FB)
                PowerUpType.DOUBLE_POINTS -> Color(0xFFFFEB3B)
                PowerUpType.SHIELD -> Color(0xFF00E5FF)
                PowerUpType.GROWTH -> Color(0xFF66BB6A)
                PowerUpType.GHOST -> Color(0xFFB0BEC5)
                PowerUpType.SPEED_BOOST -> Color(0xFFFF5722)
            }
            synchronized(_powerUps) {
                _powerUps.add(PowerUp(id, pos, type, color, durationFrames = POWERUP_DURATION))
            }
        }
    }

    private fun getRandomColorsForBot(): Pair<Color, Color> {
        val skins = listOf(
            Color(0xFFFF3366) to Color(0xFFFF9900),
            Color(0xFF9933FF) to Color(0xFFFF00D6),
            Color(0xFF00FF33) to Color(0xFF33FFCC),
            Color(0xFFFFFF33) to Color(0xFFFFCC00),
            Color(0xFFFFFFFF) to Color(0xFF999999)
        )
        return skins.random()
    }

    private fun getRandomSkinName(): String =
        listOf("Volcanic Lava", "Twilight Neon", "Cyber Mint", "Gold Standard", "Ghost Veil").random()

    private fun spawnOrbs(amount: Int) {
        repeat(amount) {
            val id = UUID.randomUUID().toString()
            val pos = Vector2D(
                Random.nextFloat() * (arenaWidth - 60f) + 30f,
                Random.nextFloat() * (arenaHeight - 60f) + 30f
            )
            val roll = Random.nextLong(100)
            val isCelestial = roll < 2
            val isSuper = !isCelestial && roll < 10
            val size = if (isCelestial) 22f else if (isSuper) 13f else 6f
            val points = when {
                isCelestial -> CELESTIAL_ORB_POINTS
                isSuper -> SUPER_ORB_POINTS
                else -> NORMAL_ORB_POINTS
            }
            val colors = listOf(
                Color(0xFF00FFCC), Color(0xFFFF3366), Color(0xFFFFFF33),
                Color(0xFF9933FF), Color(0xFF33FFCC), Color(0xFFFF9900)
            )
            val color = if (isCelestial) Color(0xFFFF00FF) else colors.random()
            synchronized(_orbs) {
                _orbs.add(Orb(id, pos, size, color, points, isSuper, isCelestial))
            }
        }
    }

    // ---------- Ability system ----------

    fun triggerAbility(snake: Snake) {
        when (snake.activeAbilityType) {
            "SHIELD" -> {
                snake.specialAbilityActive = true
                snake.shieldDuration = ABILITY_SHIELD_DURATION
                snake.abilityCooldownRemaining = COOLDOWN_SHIELD
                snake.abilityActiveDuration = ABILITY_SHIELD_DURATION
                if (snake.isPlayer) {
                    addFloatingText(snake.position, "FORCEFIELD SHIELD ACTIVE!", Color(0xFF00E5FF))
                    triggerHaptic("medium")
                    cameraShake = 6f
                }
                spawnRingParticles(snake.position, Color(0xFF00E5FF), 25, 8f)
            }
            "FREEZE_PULSE" -> {
                snake.abilityCooldownRemaining = COOLDOWN_FREEZE
                if (snake.isPlayer) {
                    addFloatingText(snake.position, "FREEZE PULSE WAVE!", Color(0xFF80D8FF))
                    triggerHaptic("heavy")
                    cameraShake = 16f
                }
                spawnRingParticles(snake.position, Color(0xFF33B5E5), 40, 11f)
                synchronized(_snakes) {
                    _snakes.filter { it.id != snake.id && it.isAlive && it.position.distance(snake.position) < 300f }
                        .forEach { other ->
                            if (!isInvulnerable(other)) {
                                other.isFrozen = true
                                other.freezeTimer = ABILITY_FREEZE_DURATION
                                if (other.isPlayer) {
                                    addFloatingText(other.position, "FROZEN SLOW DOWN!", Color(0xFF80D8FF))
                                    triggerHaptic("heavy")
                                }
                            }
                        }
                }
            }
            "EMP_BLAST" -> {
                snake.abilityCooldownRemaining = COOLDOWN_EMP
                if (snake.isPlayer) {
                    addFloatingText(snake.position, "EMP RUNE BLAST!", Color(0xFFFFCC00))
                    triggerHaptic("heavy")
                    cameraShake = 22f
                }
                spawnRingParticles(snake.position, Color(0xFFFFEE55), 40, 10f)
                synchronized(_snakes) {
                    _snakes.filter { it.id != snake.id && it.isAlive && it.position.distance(snake.position) < 320f }
                        .forEach { other ->
                            if (!isInvulnerable(other)) {
                                other.isEmped = true
                                other.empTimer = ABILITY_EMP_DURATION
                                other.isBoosting = false
                                if (other.isPlayer) {
                                    addFloatingText(other.position, "EMP: BOOST BLOCKED!", Color(0xFFFFA000))
                                    triggerHaptic("heavy")
                                }
                            }
                        }
                }
                // Mutate nearby orbs to celestial
                synchronized(_orbs) {
                    var mutated = 0
                    val newOrbs = mutableListOf<Orb>()
                    val iter = _orbs.iterator()
                    while (iter.hasNext()) {
                        val orb = iter.next()
                        if (orb.position.distance(snake.position) < 320f && !orb.isCelestialOrb && mutated < 4) {
                            iter.remove()
                            newOrbs.add(Orb(UUID.randomUUID().toString(), orb.position, 22f, Color(0xFFFF00FF), CELESTIAL_ORB_POINTS, isCelestialOrb = true))
                            mutated++
                        }
                    }
                    _orbs.addAll(newOrbs)
                }
            }
            "SPEED_BURST" -> {
                snake.abilityActiveDuration = ABILITY_SPEED_BURST_DURATION
                snake.abilityCooldownRemaining = COOLDOWN_SPEED
                snake.specialAbilityActive = true
                if (snake.isPlayer) {
                    addFloatingText(snake.position, "HYPERSONIC BURST ACTIVE!", Color(0xFFFF5722))
                    triggerHaptic("medium")
                    cameraShake = 5f
                }
                spawnRingParticles(snake.position, Color(0xFFFF5722), 20, 7f)
            }
            "GHOST_PHASE" -> {
                snake.abilityActiveDuration = ABILITY_GHOST_DURATION
                snake.abilityCooldownRemaining = COOLDOWN_GHOST
                snake.specialAbilityActive = true
                snake.activePowerUpType = PowerUpType.GHOST
                snake.powerUpTimer = ABILITY_GHOST_DURATION
                if (snake.isPlayer) {
                    addFloatingText(snake.position, "GHOST INDUCTION ACTIVE!", Color(0xFFB0BEC5))
                    triggerHaptic("light")
                }
                spawnRingParticles(snake.position, Color(0xFFCFD8DC), 15, 6f)
            }
        }
    }

    private fun spawnRingParticles(center: Vector2D, color: Color, count: Int, speed: Float) {
        repeat(count) { i ->
            val angle = (i * (2 * Math.PI / count)).toFloat()
            val vel = Vector2D(cos(angle.toDouble()).toFloat() * speed, sin(angle.toDouble()).toFloat() * speed)
            addParticle(center, vel, color, alpha = 1f, fadeSpeed = 0.04f, size = 9f)
        }
    }

    // ---------- Body management ----------

    private fun updateBodySegments(snake: Snake) {
        snake.body.add(0, Vector2D(snake.position.x, snake.position.y))
        val target = snake.length * BODY_SEGMENT_GAP
        while (snake.body.size > target) {
            snake.body.removeAt(snake.body.lastIndex)
        }
    }

    // ---------- Elimination ----------

    private fun eliminateSnake(snake: Snake) {
        val step = maxOf(1, snake.body.size / 10)
        for (i in 0 until snake.body.size step step) {
            val seg = snake.body[i]
            synchronized(_orbs) {
                _orbs.add(Orb(UUID.randomUUID().toString(), seg, 12f, snake.primaryColor, 15, true))
            }
            repeat(3) {
                addParticle(
                    seg,
                    Vector2D(Random.nextFloat() * 8f - 4f, Random.nextFloat() * 8f - 4f),
                    snake.secondaryColor,
                    alpha = 1f,
                    fadeSpeed = 0.03f,
                    size = 10f
                )
            }
        }
    }

    // ---------- Kill events ----------

    fun publishKill(killer: Snake?, victim: Snake, cause: String) {
        val weapon = when (cause) {
            "collision" -> {
                if (killer != null) {
                    when {
                        killer.specialAbilityActive && killer.activeAbilityType == "SPEED_BURST" -> "Overdrive Strike ⚡"
                        killer.specialAbilityActive && killer.activeAbilityType == "SHIELD" -> "Forcefield Smash 🛡️"
                        killer.specialAbilityActive && killer.activeAbilityType == "FREEZE_PULSE" -> "Sub‑Zero Slam ❄️"
                        killer.specialAbilityActive && killer.activeAbilityType == "EMP_BLAST" -> "EMP Disruptor 💥"
                        killer.specialAbilityActive && killer.activeAbilityType == "GHOST_PHASE" -> "Phased Ambush 👻"
                        else -> "Grid Collision 💥"
                    }
                } else "Grid Collision 💥"
            }
            "electro_gate" -> "Electro‑Gate Overload ⚡"
            "lava_pit" -> "Thermal Lava Melt 🔥"
            "ice_spike" -> "Glacial Spear Pierced ❄️"
            "quantum_vortex" -> "Singularity Collapse 🌌"
            "border" -> "Vector Out‑of‑Bounds 🚫"
            "safe_zone" -> "System Zone Dissolution ☠️"
            else -> cause
        }
        synchronized(_killEvents) {
            _killEvents.add(0, KillEvent(UUID.randomUUID().toString(), killer?.name, victim.name, weapon))
            if (_killEvents.size > 5) _killEvents.removeAt(_killEvents.lastIndex)
        }
    }

    // ---------- Visual effect helpers ----------

    private fun addParticle(
        position: Vector2D,
        velocity: Vector2D,
        color: Color,
        alpha: Float = 1f,
        fadeSpeed: Float = 0.05f,
        size: Float = 5f
    ) {
        synchronized(_particles) {
            _particles.add(Particle(position, velocity, color, alpha, fadeSpeed, size))
        }
    }

    private fun addFloatingText(position: Vector2D, text: String, color: Color) {
        synchronized(_floatingTexts) {
            _floatingTexts.add(FloatingText(position, text, color))
        }
    }

    private fun triggerHaptic(type: String) {
        onHapticTrigger?.invoke(type)
    }
}