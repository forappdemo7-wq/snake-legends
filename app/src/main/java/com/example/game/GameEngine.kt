package com.example.game

import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.ui.graphics.Color
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

class GameEngine {
    // Arena Boundaries (Upgraded map size to a spacious 4000f x 4000f)
    var arenaWidth = 4000f
    var arenaHeight = 4000f

    // Match configurations
    var gameMode: String = "Casual" // Casual, Ranked, Battle Royale, Private Room
    var arenaTheme: ArenaTheme = ArenaTheme.CYBER_CITY
    var maxMatchSnakes: Int = 16

    // Lists of objects
    val snakes = mutableListOf<Snake>()
    val orbs = mutableListOf<Orb>()
    val particles = mutableListOf<Particle>()
    val floatingTexts = mutableListOf<FloatingText>()
    val powerUps = mutableListOf<PowerUp>()
    val hazards = mutableListOf<Hazard>()
    val killEvents = mutableListOf<KillEvent>()

    // Sentinel Anti-Cheat Security Core
    val antiCheat = AntiCheatManager()

    // Player instance
    var playerSnake: Snake? = null

    // Battle Royale safe zone
    var safeZoneRadius = 2500f
    var safeZoneCenter = Vector2D(2000f, 2000f)
    var isSafeZoneShrinking = false

    // Leaderboard tracking
    var alivePlayersCount = 0
    val rankingList = mutableListOf<Pair<String, Int>>()

    // Game states
    var isGameOver = false
    var isVictory = false
    var rankingPlacement = 0
    var totalCoinsEarned = 0
    var totalXpEarned = 0
    var totalKills = 0

    // Premium Juice, Camera Shake & Map Events
    var cameraShake = 0f
    var activeWeather = "NORMAL" // "NORMAL", "ENERGY_STORM", "ERUPTION", "ICE_BLIZZARD", "ANCIENT_RITUAL", "GRAVITY_SHIFT", "OVERCHARGE_PULSE"
    var weatherTimer = 180 // frames until next weather shift/event
    var activeEventName = "CALM"
    var eventDuration = 120
    var selectedPlayerAbility = "SHIELD" // "SHIELD", "FREEZE_PULSE", "EMP_BLAST", "SPEED_BURST", "GHOST_PHASE"

    // Callback on game updates for sound/haptics
    var onHapticTrigger: ((hapticType: String) -> Unit)? = null

    init {
        resetEngine()
    }

    fun resetEngine() {
        snakes.clear()
        orbs.clear()
        particles.clear()
        floatingTexts.clear()
        powerUps.clear()
        hazards.clear()
        killEvents.clear()
        isGameOver = false
        isVictory = false
        rankingPlacement = 0
        totalCoinsEarned = 0
        totalXpEarned = 0
        totalKills = 0
        antiCheat.resetTracker()

        // Determine arena dimensions depending on user selected maxMatchSnakes
        when (maxMatchSnakes) {
            16 -> {
                arenaWidth = 4000f
                arenaHeight = 4000f
            }
            50 -> {
                arenaWidth = 6000f
                arenaHeight = 6000f
            }
            100 -> {
                arenaWidth = 8500f
                arenaHeight = 8500f
            }
            else -> {
                arenaWidth = 4000f
                arenaHeight = 4000f
            }
        }

        safeZoneRadius = if (gameMode == "Battle Royale") {
            when (maxMatchSnakes) {
                16 -> 2500f
                50 -> 3800f
                100 -> 5400f
                else -> 2500f
            }
        } else {
            when (maxMatchSnakes) {
                16 -> 4500f
                50 -> 6500f
                100 -> 9000f
                else -> 4500f
            }
        }
        safeZoneCenter = Vector2D(arenaWidth / 2f, arenaHeight / 2f)
        isSafeZoneShrinking = gameMode == "Battle Royale"

        val random = Random(System.currentTimeMillis())
        val hazardMultiplier = when (maxMatchSnakes) {
            16 -> 1.0f
            50 -> 2.2f
            100 -> 4.5f
            else -> 1.0f
        }

        when (arenaTheme) {
            ArenaTheme.CYBER_CITY -> {
                for (i in 0 until (12 * hazardMultiplier).toInt()) {
                    val pos = Vector2D(random.nextFloat() * (arenaWidth - 400f) + 200f, random.nextFloat() * (arenaHeight - 400f) + 200f)
                    hazards.add(Hazard(id = "electro_$i", type = "electro_gate", position = pos, size = 50f))
                }
            }
            ArenaTheme.LAVA_WORLD -> {
                for (i in 0 until (14 * hazardMultiplier).toInt()) {
                    val pos = Vector2D(random.nextFloat() * (arenaWidth - 400f) + 200f, random.nextFloat() * (arenaHeight - 400f) + 200f)
                    hazards.add(Hazard(id = "lava_$i", type = "lava_pit", position = pos, size = 65f))
                }
            }
            ArenaTheme.FROZEN_ARENA -> {
                for (i in 0 until (16 * hazardMultiplier).toInt()) {
                    val pos = Vector2D(random.nextFloat() * (arenaWidth - 400f) + 200f, random.nextFloat() * (arenaHeight - 400f) + 200f)
                    hazards.add(Hazard(id = "ice_$i", type = "ice_spike", position = pos, size = 45f))
                }
            }
            ArenaTheme.JUNGLE_TEMPLE -> {
                for (i in 0 until (10 * hazardMultiplier).toInt()) {
                    val pos = Vector2D(random.nextFloat() * (arenaWidth - 400f) + 200f, random.nextFloat() * (arenaHeight - 400f) + 200f)
                    hazards.add(Hazard(id = "totem_$i", type = "totem", position = pos, size = 55f))
                }
            }
            ArenaTheme.SPACE_STATION -> {
                for (i in 0 until (8 * hazardMultiplier).toInt()) {
                    val pos = Vector2D(random.nextFloat() * (arenaWidth - 500f) + 250f, random.nextFloat() * (arenaHeight - 500f) + 250f)
                    hazards.add(Hazard(id = "vortex_$i", type = "quantum_vortex", position = pos, size = 80f))
                }
            }
            ArenaTheme.NEON_GRID -> {
                for (i in 0 until (12 * hazardMultiplier).toInt()) {
                    val pos = Vector2D(random.nextFloat() * (arenaWidth - 400f) + 200f, random.nextFloat() * (arenaHeight - 400f) + 200f)
                    hazards.add(Hazard(id = "laser_$i", type = "neon_gate", position = pos, size = 40f))
                }
            }
        }

        // Reset Screen effects state
        cameraShake = 0f
        activeWeather = "NORMAL"
        weatherTimer = 400

        // Setup Player Snake
        val player = Snake(
            id = "player",
            name = "You (Legend)",
            isPlayer = true,
            position = Vector2D(arenaWidth / 2f, arenaHeight / 2f),
            primaryColor = Color(0xFF00FFCC),
            secondaryColor = Color(0xFF0099FF),
            activeAbilityType = selectedPlayerAbility
        )
        // Set body segment history
        for (i in 0 until player.length) {
            player.body.add(Vector2D(player.position.x, player.position.y + i * 15f))
        }
        playerSnake = player
        snakes.add(player)

        // Spawn Bot Snakes
        val botNames = listOf(
            "ViperX", "ShadowCrawl", "NeonVenom", "ApexViper", "SlitherKing",
            "SlinkyMaster", "Wraith", "CobaltFangs", "CrimsonOuroboros", "GlitchSnake",
            "StealthBoa", "HydraMax", "PhantomStrider", "AbyssGlow", "VenomGod"
        )
        val totalBots = maxMatchSnakes - 1
        val prefixes = listOf("Giga", "Shadow", "Neon", "Apex", "Viper", "Slither", "Wrath", "Phantom", "Abyss", "Venom", "Cyber", "Mega", "Alpha", "Zenith", "Turbo", "Cosmic", "Rogue", "Solar", "Glitch", "Aero")
        val suffixes = listOf("King", "Master", "Fangs", "Strider", "Glow", "God", "Boa", "Glidr", "X", "Alpha", "Slayer", "Onyx", "Rider", "Wraith", "Specter", "Nova", "Pulse", "Crawl", "Strike", "Titan")

        for (i in 0 until totalBots) {
            val botAngle = Random.nextFloat() * 6.28f
            val botPos = Vector2D(
                Random.nextFloat() * (arenaWidth - 400f) + 200f,
                Random.nextFloat() * (arenaHeight - 400f) + 200f
            )
            val colors = getRandomColorsForBot()
            val name = if (i < botNames.size) {
                botNames[i]
            } else {
                "${prefixes.random(random)}_${suffixes.random(random)}${Random.nextInt(5, 99)}"
            }
            val bot = Snake(
                id = "bot_$i",
                name = name,
                isPlayer = false,
                position = botPos,
                angle = botAngle,
                primaryColor = colors.first,
                secondaryColor = colors.second,
                skinName = getRandomSkinName()
            )
            // Assign random active ability to bots
            bot.activeAbilityType = listOf("SHIELD", "FREEZE_PULSE", "EMP_BLAST", "SPEED_BURST", "GHOST_PHASE").random()
            for (j in 0 until bot.length) {
                bot.body.add(Vector2D(botPos.x, botPos.y + j * 15f))
            }
            snakes.add(bot)
        }

        // Spawn initial energy orbs dynamically scaled to Arena scale
        val orbAmount = when (maxMatchSnakes) {
            16 -> 450
            50 -> 1000
            100 -> 2200
            else -> 450
        }
        val powerUpAmount = when (maxMatchSnakes) {
            16 -> 15
            50 -> 35
            100 -> 80
            else -> 15
        }
        spawnOrbs(orbAmount)
        spawnPowerUps(powerUpAmount)
        updateLeaderboard()
    }

    private fun spawnPowerUps(amount: Int) {
        for (i in 0 until amount) {
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
            powerUps.add(PowerUp(id, pos, type, color))
        }
    }

    private fun getRandomColorsForBot(): Pair<Color, Color> {
        val skins = listOf(
            Pair(Color(0xFFFF3366), Color(0xFFFF9900)), // Hot Lava
            Pair(Color(0xFF9933FF), Color(0xFFFF00D6)), // Twilight Neon
            Pair(Color(0xFF00FF33), Color(0xFF33FFCC)), // Cyber Mint
            Pair(Color(0xFFFFFF33), Color(0xFFFFCC00)), // Gold Standard
            Pair(Color(0xFFFFFFFF), Color(0xFF999999))  // Ghost Veil
        )
        return skins.random()
    }

    private fun getRandomSkinName(): String {
        return listOf("Volcanic Lava", "Twilight Neon", "Cyber Mint", "Gold Standard", "Ghost Veil").random()
    }

    private fun spawnOrbs(amount: Int) {
        for (i in 0 until amount) {
            val id = UUID.randomUUID().toString()
            val pos = Vector2D(
                Random.nextFloat() * (arenaWidth - 60f) + 30f,
                Random.nextFloat() * (arenaHeight - 60f) + 30f
            )
            val roll = Random.nextLong(100)
            val isCelestial = roll < 2
            val isSuper = !isCelestial && roll < 10
            val size = if (isCelestial) 22f else if (isSuper) 13f else 6f
            val points = if (isCelestial) 100 else if (isSuper) 25 else 5
            val colors = listOf(
                Color(0xFF00FFCC), Color(0xFFFF3366), Color(0xFFFFFF33),
                Color(0xFF9933FF), Color(0xFF33FFCC), Color(0xFFFF9900)
            )
            val color = if (isCelestial) Color(0xFFFF00FF) else colors.random()
            orbs.add(Orb(id, pos, size, color, points, isSuper, isCelestial))
        }
    }

    fun syncMultiplayerSnakes(peers: Map<String, PeerSnakeData>) {
        synchronized(snakes) {
            peers.forEach { (username, peerData) ->
                val peerId = "mp_$username"
                var existingSnake = snakes.firstOrNull { it.id == peerId }
                
                val primaryColorVal = try {
                    Color(android.graphics.Color.parseColor(peerData.primaryColorHex))
                } catch (e: Exception) {
                    Color(0xFFFF3366)
                }
                val secondaryColorVal = try {
                    Color(android.graphics.Color.parseColor(peerData.secondaryColorHex))
                } catch (e: Exception) {
                    Color(0xFFFF9900)
                }

                if (existingSnake == null) {
                    val snake = Snake(
                        id = peerId,
                        name = peerData.name,
                        isPlayer = false,
                        position = peerData.position,
                        angle = peerData.angle,
                        speed = peerData.speed,
                        length = peerData.length,
                        score = peerData.score,
                        primaryColor = primaryColorVal,
                        secondaryColor = secondaryColorVal,
                        isBoosting = peerData.isBoosting,
                        isAlive = peerData.isAlive
                    )
                    peerData.body.forEach { segment ->
                        snake.body.add(segment)
                    }
                    snakes.add(snake)
                } else {
                    existingSnake.position = peerData.position
                    existingSnake.angle = peerData.angle
                    existingSnake.speed = peerData.speed
                    existingSnake.length = peerData.length
                    existingSnake.score = peerData.score
                    existingSnake.isBoosting = peerData.isBoosting
                    existingSnake.isAlive = peerData.isAlive
                    
                    existingSnake.body.clear()
                    peerData.body.forEach { segment ->
                        existingSnake.body.add(segment)
                    }
                }
            }
            
            val mpIdsToRemove = mutableListOf<String>()
            snakes.forEach { snake ->
                if (snake.id.startsWith("mp_") && !peers.containsKey(snake.name)) {
                    mpIdsToRemove.add(snake.id)
                }
            }
            snakes.removeAll { mpIdsToRemove.contains(it.id) }
        }
    }

    fun onTick(
        joystickAngle: Float?,
        isBoosting: Boolean,
        abilityTriggered: Boolean
    ) {
        if (isGameOver) return

        // 1. Decay camera shake
        cameraShake = (cameraShake - 0.45f).coerceAtLeast(0f)

        // 2. Weather & Arena Map Event Simulation Tick
        weatherTimer--
        if (weatherTimer <= 0) {
            weatherTimer = Random.nextInt(320, 550) // Switch weather/events state every 8-12 seconds
            
            val potentialEvents = when (arenaTheme) {
                ArenaTheme.CYBER_CITY -> listOf("NORMAL", "ENERGY_STORM")
                ArenaTheme.LAVA_WORLD -> listOf("NORMAL", "ERUPTION")
                ArenaTheme.FROZEN_ARENA -> listOf("NORMAL", "ICE_BLIZZARD")
                ArenaTheme.JUNGLE_TEMPLE -> listOf("NORMAL", "ANCIENT_RITUAL")
                ArenaTheme.SPACE_STATION -> listOf("NORMAL", "GRAVITY_SHIFT")
                ArenaTheme.NEON_GRID -> listOf("NORMAL", "OVERCHARGE_PULSE")
            }
            activeWeather = if (activeWeather == "NORMAL") potentialEvents.random() else "NORMAL"

            if (activeWeather != "NORMAL") {
                cameraShake = 16f
                triggerHaptic("heavy")
                eventDuration = 180 // lasts 3 seconds (180 frames)
                activeEventName = when (activeWeather) {
                    "ENERGY_STORM" -> "ELECTRON LIGHTNING HURRICANE!"
                    "ERUPTION" -> "LAVA VOLCANO ERUPTION!"
                    "ICE_BLIZZARD" -> "SUB-ZERO COLD ICE BLIZZARD!"
                    "ANCIENT_RITUAL" -> "ANCIENT TOTEM RUNES ACTIVE!"
                    "GRAVITY_SHIFT" -> "AIRLOCK GRAVITY ANOMALY WELL!"
                    "OVERCHARGE_PULSE" -> "OVERCHARGED GRID ENERGY OVERLOAD!"
                    else -> "ARENA CRITICAL OUTBREAK EVENT!"
                }
                playerSnake?.let { p ->
                    floatingTexts.add(FloatingText(p.position, "MAP WARNING: $activeEventName", Color(0xFFFF3366)))
                }
            } else {
                activeEventName = "CALM"
                playerSnake?.let { p ->
                    floatingTexts.add(FloatingText(p.position, "ARENA STABILIZED", Color(0xFF00FFCC)))
                }
            }
        }

        if (eventDuration > 0) {
            eventDuration--
            // Global weather behaviors
            when (activeWeather) {
                "ENERGY_STORM" -> {
                    cameraShake = maxOf(cameraShake, 1.3f)
                    if (Random.nextInt(50) == 0) {
                        snakes.filter { it.isAlive }.randomOrNull()?.let { victim ->
                            victim.isEmped = true
                            victim.empTimer = 120
                            floatingTexts.add(FloatingText(victim.position, "STRUCK BY LIGHTNING STUN!", Color(0xFFFFFF33)))
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
                        orbs.add(Orb(UUID.randomUUID().toString(), lavaBall, 14f, Color(0xFFFF5722), 40, true))
                        for (i in 0..4) {
                            particles.add(Particle(lavaBall, Vector2D(Random.nextFloat() * 6f - 3f, Random.nextFloat() * 6f - 3f), Color(0xFFFF4500), fadeSpeed = 0.04f, size = 12f))
                        }
                    }
                }
                "GRAVITY_SHIFT" -> {
                    // Pull snakes slowly towards center
                    val center = Vector2D(arenaWidth / 2f, arenaHeight / 2f)
                    for (snake in snakes) {
                        if (!snake.isAlive) continue
                        val pullDir = (center - snake.position).normalized()
                        snake.position = snake.position + pullDir * 1.6f
                    }
                }
                "OVERCHARGE_PULSE" -> {
                    if (Random.nextInt(12) == 0) {
                        val sparkPos = Vector2D(Random.nextFloat() * arenaWidth, Random.nextFloat() * arenaHeight)
                        particles.add(Particle(sparkPos, Vector2D(0f, 0f), Color(0xFF00FFCC), fadeSpeed = 0.03f, size = 14f))
                    }
                }
            }
        }

        // 3. Shrink Safe Zone overlay (Battle Royale Mode)
        if (gameMode == "Battle Royale" && isSafeZoneShrinking) {
            safeZoneRadius = (safeZoneRadius - 0.25f).coerceAtLeast(300f)
        }

        val player = playerSnake ?: return

        // 4. Update Power-Up Active Timers, Magnet Pull states, Debuffs & Cool downs for all snakes
        for (snake in snakes) {
            if (!snake.isAlive) continue

            // 4.1 Decrement Ability cooldowns
            if (snake.abilityCooldownRemaining > 0) {
                snake.abilityCooldownRemaining--
            }

            // 4.2 Decrement Active Ability durations
            if (snake.abilityActiveDuration > 0) {
                snake.abilityActiveDuration--
                if (snake.abilityActiveDuration <= 0) {
                    snake.specialAbilityActive = false
                    if (snake.activeAbilityType == "GHOST_PHASE") {
                        snake.activePowerUpType = null
                    }
                }
            }

            // 4.3 Decrement status effects
            if (snake.isFrozen) {
                snake.freezeTimer--
                if (snake.freezeTimer <= 0) {
                    snake.isFrozen = false
                }
            }
            if (snake.isEmped) {
                snake.empTimer--
                if (snake.empTimer <= 0) {
                    snake.isEmped = false
                }
            }

            // Decrement active power-up timer
            if (snake.activePowerUpType != null && snake.activeAbilityType != "GHOST_PHASE") {
                snake.powerUpTimer--
                if (snake.powerUpTimer <= 0) {
                    snake.activePowerUpType = null
                }
            }

            // Magnetic Pull of surrounding orbs towards head
            if (snake.activePowerUpType == PowerUpType.MAGNET) {
                for (orb in orbs) {
                    val dist = snake.position.distance(orb.position)
                    if (dist < 185f) {
                        val pullForce = ((185f - dist) / 185f) * 6.5f + 1.5f
                        val pullDir = (snake.position - orb.position).normalized()
                        orb.position = orb.position + pullDir * pullForce
                    }
                }
            }
        }

        // 5. Power-Up Pick up Collision Detection
        val gatheredPowerUpIds = mutableListOf<String>()
        for (powerUp in powerUps) {
            for (snake in snakes) {
                if (!snake.isAlive) continue
                if (snake.position.distance(powerUp.position) < 38f) {
                    gatheredPowerUpIds.add(powerUp.id)
                    
                    if (powerUp.type == PowerUpType.GROWTH) {
                        snake.score += 75
                        snake.length = 4 + (snake.score / 25)
                        if (snake.isPlayer) {
                            totalCoinsEarned += 25
                            totalXpEarned += 100
                            floatingTexts.add(FloatingText(snake.position, "GROWTH POTION +3 SEGMENTS!", Color(0xFF66BB6A)))
                            triggerHaptic("heavy")
                            cameraShake = 6f
                        }
                    } else {
                        snake.activePowerUpType = powerUp.type
                        snake.powerUpTimer = powerUp.durationFrames
                        if (snake.isPlayer) {
                            val activeText = when (powerUp.type) {
                                PowerUpType.MAGNET -> "MAGNET FORCEFIELD ACTIVE!"
                                PowerUpType.DOUBLE_POINTS -> "DOUBLE POINTS MULTIPLIER!"
                                PowerUpType.SHIELD -> "FORCEFIELD SHIELD ACTIVE!"
                                PowerUpType.GHOST -> "GHOST MODE: PHASE THROUGH WALLS!"
                                PowerUpType.SPEED_BOOST -> "SPEED BOOST VELOCITY ACTIVATED!"
                                else -> "UPGRADE ACTIVE!"
                            }
                            floatingTexts.add(FloatingText(snake.position, activeText, powerUp.color))
                            triggerHaptic("medium")
                            cameraShake = 4f
                        }
                    }
                    break
                }
            }
        }
        if (gatheredPowerUpIds.isNotEmpty()) {
            powerUps.removeAll { gatheredPowerUpIds.contains(it.id) }
            spawnPowerUps(gatheredPowerUpIds.size)
        }

        // 6. Arena Hazards Collision Mechanics (with Special Ability mitigations)
        for (hazard in hazards) {
            hazard.state = (hazard.state + 1) % 360
            for (snake in snakes) {
                if (!snake.isAlive) continue
                val dist = snake.position.distance(hazard.position)
                when (hazard.type) {
                    "lava_pit" -> {
                        if (dist < hazard.size + 15f) {
                            if (snake.activePowerUpType != PowerUpType.SHIELD && snake.activePowerUpType != PowerUpType.GHOST && !snake.specialAbilityActive) {
                                if (Random.nextInt(15) == 0) {
                                    snake.length = maxOf(3, snake.length - 1)
                                    if (snake.isPlayer) {
                                        floatingTexts.add(FloatingText(snake.position, "THERMAL LAVA BURN! -1 RING", Color(0xFFFF4500)))
                                        triggerHaptic("medium")
                                        cameraShake = 7f
                                    }
                                    particles.add(Particle(snake.position, Vector2D(Random.nextFloat() * 2f - 1f, -2f), Color(0xFFFF9900), alpha = 0.9f))
                                }
                            }
                        }
                    }
                    "electro_gate" -> {
                        val isActive = (hazard.state % 120) < 84
                        if (isActive && dist < hazard.size + 10f) {
                            if (snake.activePowerUpType != PowerUpType.SHIELD && snake.activePowerUpType != PowerUpType.GHOST && !snake.specialAbilityActive) {
                                snake.isAlive = false
                                if (snake.isPlayer) {
                                    triggerHaptic("heavy")
                                    cameraShake = 22f
                                }
                                publishKill(null, snake, "electro_gate")
                                eliminateSnake(snake)
                                break
                            }
                        }
                    }
                    "ice_spike" -> {
                        if (dist < hazard.size + 12f) {
                            if (snake.activePowerUpType != PowerUpType.SHIELD && !snake.specialAbilityActive) {
                                snake.isAlive = false
                                if (snake.isPlayer) {
                                    triggerHaptic("heavy")
                                    cameraShake = 24f
                                }
                                publishKill(null, snake, "ice_spike")
                                eliminateSnake(snake)
                                break
                            } else {
                                val pushDir = (snake.position - hazard.position).normalized()
                                snake.position = hazard.position + pushDir * (hazard.size + 20f)
                                snake.angle = atan2(pushDir.y.toDouble(), pushDir.x.toDouble()).toFloat()
                            }
                        }
                    }
                    "totem" -> {
                        if (dist < hazard.size + 40f) {
                            if (Random.nextInt(20) == 0) {
                                snake.score += 15
                                snake.length = 4 + (snake.score / 25)
                                if (snake.isPlayer) {
                                    floatingTexts.add(FloatingText(snake.position, "ANCIENT COGNITION +15", Color(0xFF81C784)))
                                    triggerHaptic("light")
                                }
                            }
                        }
                    }
                    "quantum_vortex" -> {
                        if (dist < hazard.size + 150f) {
                            val suctionPower = ((hazard.size + 150f) - dist) / (hazard.size + 150f) * 3f
                            val pullDir = (hazard.position - snake.position).normalized()
                            snake.position = snake.position + pullDir * suctionPower
                            if (dist < hazard.size * 0.45f) {
                                if (snake.activePowerUpType != PowerUpType.SHIELD && snake.activePowerUpType != PowerUpType.GHOST && !snake.specialAbilityActive) {
                                    snake.isAlive = false
                                    if (snake.isPlayer) {
                                        triggerHaptic("heavy")
                                        cameraShake = 25f
                                    }
                                    publishKill(null, snake, "quantum_vortex")
                                    eliminateSnake(snake)
                                    break
                                }
                            }
                        }
                    }
                    "neon_gate" -> {
                        val isBlocked = (hazard.state % 180) > 90
                        if (isBlocked && dist < hazard.size + 12f) {
                            if (snake.activePowerUpType != PowerUpType.SHIELD && snake.activePowerUpType != PowerUpType.GHOST && !snake.specialAbilityActive) {
                                if (Random.nextInt(8) == 0) {
                                    snake.length = maxOf(3, snake.length - 1)
                                    if (snake.isPlayer) {
                                        floatingTexts.add(FloatingText(snake.position, "LASER GATE GLITCH! -1 RING", Color(0xFF00E5FF)))
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

        // 7. Core Active Ability trigger hooks for player
        if (abilityTriggered && player.abilityCooldownRemaining <= 0 && !player.specialAbilityActive && player.isAlive) {
            triggerAbility(player)
        }

        // 8. Move Player Snake with physics-based smooth turning and speed interpolation
        if (player.isAlive) {
            if (joystickAngle != null) {
                val targetAngle = joystickAngle
                var diff = targetAngle - player.angle
                // Wrap angular limits correctly
                while (diff < -Math.PI) diff += (2 * Math.PI).toFloat()
                while (diff > Math.PI) diff -= (2 * Math.PI).toFloat()
                
                // Rotational agility: slower in ice blizzards, faster on speed bursts
                var turnSpeed = 0.15f
                if (activeWeather == "ICE_BLIZZARD") {
                    turnSpeed = 0.06f // Slippery!
                } else if (player.abilityActiveDuration > 0 && player.activeAbilityType == "SPEED_BURST") {
                    turnSpeed = 0.22f // Extra sharp steering when boosting!
                }
                player.angle += (diff * turnSpeed)
            }
            player.isBoosting = isBoosting && player.length >= 4 && !player.isEmped && !player.isFrozen

            // Speed calculation - augmented by boosts and debuffs
            var targetSpeed = if (player.isBoosting) 7.5f else 4.0f
            if (player.activePowerUpType == PowerUpType.SPEED_BOOST) {
                targetSpeed *= 1.6f
            }
            if (player.isFrozen) {
                targetSpeed = 1.5f // high freeze slow
            }
            if (player.isEmped) {
                targetSpeed = 2.8f // speed penalty
            }
            if (player.abilityActiveDuration > 0 && player.activeAbilityType == "SPEED_BURST") {
                targetSpeed = 13.0f // extreme drive speed
            }

            // Juicy interpolation for acceleration & deceleration
            player.speed = player.speed + (targetSpeed - player.speed) * 0.14f

            // Tiny shake on extreme speeds
            if (player.speed > 8f) {
                cameraShake = maxOf(cameraShake, 1.1f)
            }

            val dirX = cos(player.angle.toDouble()).toFloat()
            val dirY = sin(player.angle.toDouble()).toFloat()
            player.position = player.position + Vector2D(dirX * player.speed, dirY * player.speed)

            // Length management on boosting (Score-based decay like slither.io/snake.io)
            if (player.isBoosting && Random.nextInt(10) == 0 && player.score > 15) {
                player.score = maxOf(0, player.score - 3)
                player.length = 4 + (player.score / 25)
                val lastSeg = player.body.lastOrNull() ?: player.position
                orbs.add(Orb(UUID.randomUUID().toString(), lastSeg, 6f, player.primaryColor, 4, false))
            }

            // Save history with spacing
            updateBodySegments(player)

            // Spawn fine-grain particle sparks on boost trails with Cosmic Dust glitters and gaseous exhausts
            if (player.isBoosting && Random.nextInt(2) == 0) {
                val randVal = Random.nextInt(3)
                val isNebulaPuff = randVal == 0
                val isStarGlitter = randVal == 1
                particles.add(
                    Particle(
                        position = player.position,
                        velocity = Vector2D(-dirX * 2.5f + (Random.nextFloat() * 1.5f - 0.75f), -dirY * 2.5f + (Random.nextFloat() * 1.5f - 0.75f)),
                        color = if (isStarGlitter) Color.White else player.primaryColor,
                        alpha = 1.0f,
                        fadeSpeed = if (isNebulaPuff) 0.04f else 0.08f,
                        size = if (isNebulaPuff) 8f else (if (isStarGlitter) 5f else 6f),
                        isStar = isStarGlitter,
                        isNebula = isNebulaPuff
                    )
                )
            }

            // Bounds boundary check
            if (player.activePowerUpType == PowerUpType.GHOST) {
                if (player.position.x < 0) {
                    player.position = Vector2D(arenaWidth, player.position.y)
                    player.body.clear()
                } else if (player.position.x > arenaWidth) {
                    player.position = Vector2D(0f, player.position.y)
                    player.body.clear()
                }
                if (player.position.y < 0) {
                    player.position = Vector2D(player.position.x, arenaHeight)
                    player.body.clear()
                } else if (player.position.y > arenaHeight) {
                    player.position = Vector2D(player.position.x, 0f)
                    player.body.clear()
                }
            } else {
                if (player.position.x < 0 || player.position.x > arenaWidth ||
                    player.position.y < 0 || player.position.y > arenaHeight
                ) {
                    player.isAlive = false
                    triggerHaptic("heavy")
                    cameraShake = 25f
                    publishKill(null, player, "border")
                    eliminateSnake(player)
                }
            }

            // Safe zone damages
            if (gameMode == "Battle Royale") {
                val distFromCenter = player.position.distance(safeZoneCenter)
                if (distFromCenter > safeZoneRadius) {
                    if (Random.nextInt(15) == 0) {
                        player.length--
                        floatingTexts.add(FloatingText(player.position, "-1 RING BURNING", Color(0xFFFF3333)))
                        cameraShake = 5f
                        if (player.length < 3) {
                            player.isAlive = false
                            triggerHaptic("heavy")
                            cameraShake = 22f
                            publishKill(null, player, "safe_zone")
                            eliminateSnake(player)
                        }
                    }
                }
            }

            // Sentinel Anti-Cheat Security Audit
            antiCheat.performValidation(player, floatingTexts) {
                onHapticTrigger?.invoke("heavy")
            }
        }

        // 9. Move Bot snakes with AI heuristics, turning rates, and active triggers
        for (snake in snakes) {
            if (snake.isPlayer || !snake.isAlive || snake.id.startsWith("mp_")) continue

            // 9.1 AI trigger choice: if close to opponents, use tactical abilities!
            if (snake.abilityCooldownRemaining <= 0) {
                val nearbyPlayer = snakes.firstOrNull { it.id != snake.id && it.isAlive && it.position.distance(snake.position) < 220f }
                if (nearbyPlayer != null) {
                    triggerAbility(snake)
                }
            }

            // 9.2 Target decision logic
            snake.botTargetTimer--
            if (snake.botTarget == null || snake.botTargetTimer <= 0) {
                snake.botTargetTimer = Random.nextInt(60, 150)
                val choice = Random.nextLong(100)
                if (choice < 60 && orbs.isNotEmpty()) {
                    var nearestOrb = orbs[0]
                    var minDistance = Float.MAX_VALUE
                    for (i in 0 until minOf(20, orbs.size)) {
                        val distance = snake.position.distance(orbs[i].position)
                        if (distance < minDistance) {
                            minDistance = distance
                            nearestOrb = orbs[i]
                        }
                    }
                    snake.botTarget = nearestOrb.position
                } else if (choice < 90 && snakes.size > 1) {
                    val targetSnake = snakes.first { it.id != snake.id && it.isAlive }
                    snake.botTarget = targetSnake.position
                } else {
                    snake.botTarget = Vector2D(Random.nextFloat() * arenaWidth, Random.nextFloat() * arenaHeight)
                }
            }

            // 9.3 Turn toward AI target smoothly
            val target = snake.botTarget
            if (target != null) {
                val dx = target.x - snake.position.x
                val dy = target.y - snake.position.y
                val targetAngle = atan2(dy.toDouble(), dx.toDouble()).toFloat()
                
                val diff = targetAngle - snake.angle
                val s1 = sin(diff.toDouble()).toFloat()
                
                var turnSpeed = 0.08f
                if (activeWeather == "ICE_BLIZZARD") {
                    turnSpeed = 0.03f // slip
                }
                if (snake.position.x < 150f || snake.position.x > arenaWidth - 150f ||
                    snake.position.y < 150f || snake.position.y > arenaHeight - 150f
                ) {
                    turnSpeed = 0.25f // steep steer
                }
                snake.angle += if (s1 > 0) turnSpeed else -turnSpeed
            }

            // Randomly enable boosting
            if (Random.nextInt(50) == 0 && snake.length > 5 && !snake.isFrozen && !snake.isEmped) {
                snake.isBoosting = !snake.isBoosting
            }

            // Speed computations
            var botSpeed = if (snake.isBoosting) 6.5f else 3.5f
            if (snake.activePowerUpType == PowerUpType.SPEED_BOOST) {
                botSpeed *= 1.6f
            }
            if (snake.isFrozen) {
                botSpeed = 1.4f
            }
            if (snake.isEmped) {
                botSpeed = 2.5f
            }
            if (snake.abilityActiveDuration > 0 && snake.activeAbilityType == "SPEED_BURST") {
                botSpeed = 11.5f
            }

            // Eased bot speeds
            snake.speed = snake.speed + (botSpeed - snake.speed) * 0.14f

            val bDirX = cos(snake.angle.toDouble()).toFloat()
            val bDirY = sin(snake.angle.toDouble()).toFloat()
            snake.position = snake.position + Vector2D(bDirX * snake.speed, bDirY * snake.speed)

            // Length management on boosting (Score-based decay for bot trails)
            if (snake.isBoosting && Random.nextInt(12) == 0 && snake.score > 20) {
                snake.score = maxOf(0, snake.score - 3)
                snake.length = 4 + (snake.score / 25)
                val lastSeg = snake.body.lastOrNull() ?: snake.position
                orbs.add(Orb(UUID.randomUUID().toString(), lastSeg, 5f, snake.primaryColor, 4, false))
            }

            updateBodySegments(snake)

            // Spawn fine-grain particle sparks on bot boost trails
            if (snake.isBoosting && Random.nextInt(2) == 0) {
                val randVal = Random.nextInt(3)
                val isNebulaPuff = randVal == 0
                val isStarGlitter = randVal == 1
                particles.add(
                    Particle(
                        position = snake.position,
                        velocity = Vector2D(-bDirX * 2.5f + (Random.nextFloat() * 1.5f - 0.75f), -bDirY * 2.5f + (Random.nextFloat() * 1.5f - 0.75f)),
                        color = if (isStarGlitter) Color.White else snake.primaryColor,
                        alpha = 1.0f,
                        fadeSpeed = if (isNebulaPuff) 0.04f else 0.08f,
                        size = if (isNebulaPuff) 8f else (if (isStarGlitter) 5f else 6f),
                        isStar = isStarGlitter,
                        isNebula = isNebulaPuff
                    )
                )
            }

            if (snake.activePowerUpType == PowerUpType.GHOST) {
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
                if (snake.position.x < 30f || snake.position.x > arenaWidth - 30f ||
                    snake.position.y < 30f || snake.position.y > arenaHeight - 30f
                ) {
                    snake.botTarget = Vector2D(arenaWidth / 2f, arenaHeight / 2f)
                    snake.botTargetTimer = 60
                }
            }
        }

        // 10. Head-to-Body Collision Detection
        for (snake in snakes) {
            if (!snake.isAlive) continue

            for (other in snakes) {
                if (!other.isAlive) continue
                // Don't collide with self. Ignore if shielded, phasing ghost, or utilizing active invents.
                if (snake.id == other.id || snake.specialAbilityActive || snake.activePowerUpType == PowerUpType.SHIELD || snake.activePowerUpType == PowerUpType.GHOST) continue

                val segmentsToCheck = if (other.body.size > 2) other.body.subList(2, other.body.size) else other.body
                for (seg in segmentsToCheck) {
                    val collisionThreshold = (11f * snake.thicknessFactor) + (11f * other.thicknessFactor)
                    if (snake.position.distance(seg) < collisionThreshold) {
                        snake.isAlive = false
                        
                        if (snake.isPlayer) {
                            triggerHaptic("heavy")
                            cameraShake = 25f
                        } else {
                            if (other.isPlayer) {
                                totalKills++
                                totalCoinsEarned += 100
                                totalXpEarned += 400
                                floatingTexts.add(FloatingText(snake.position, "KILL +400 XP", Color(0xFFFF5252)))
                                triggerHaptic("heavy")
                                cameraShake = 20f
                            }
                        }
                        publishKill(other, snake, "collision")
                        eliminateSnake(snake)
                        break
                    }
                }
            }
        }

        // 11. Food Eating Check
        val eatenOrbIds = mutableListOf<String>()
        for (orb in orbs) {
            val pEatingDist = (15f * player.thicknessFactor) + 12f
            if (player.isAlive && player.position.distance(orb.position) < pEatingDist) {
                val multiplier = if (player.activePowerUpType == PowerUpType.DOUBLE_POINTS) 2 else 1
                player.score += orb.points * multiplier
                player.length = 4 + (player.score / 25)
                eatenOrbIds.add(orb.id)
                
                val coinsToAdd = (if (orb.isCelestialOrb) 50 else if (orb.isSuperOrb) 5 else 1) * multiplier
                val xpToAdd = (if (orb.isCelestialOrb) 150 else if (orb.isSuperOrb) 20 else 5) * multiplier
                totalCoinsEarned += coinsToAdd
                totalXpEarned += xpToAdd
                
                val particleCount = if (orb.isCelestialOrb) 20 else 5
                for (i in 0 until particleCount) {
                    val pColor = if (orb.isCelestialOrb) {
                        listOf(Color(0xFFFF00FF), Color(0xFF00FFCC), Color(0xFFFFFF33), Color(0xFFE040FB)).random()
                    } else {
                        orb.color
                    }
                    val isCelestial = orb.isCelestialOrb
                    val isNebulaPuff = isCelestial && (i % 3 == 0)
                    val isStarGlitter = (isCelestial && !isNebulaPuff) || (!isCelestial && Random.nextInt(4) == 0)
                    particles.add(
                        Particle(
                            position = orb.position,
                            velocity = Vector2D(
                                Random.nextFloat() * (if (isCelestial) 10f else 5f) - (if (isCelestial) 5f else 2.5f),
                                Random.nextFloat() * (if (isCelestial) 10f else 5f) - (if (isCelestial) 5f else 2.5f)
                            ),
                            color = pColor,
                            alpha = 1.0f,
                            fadeSpeed = if (isNebulaPuff) 0.02f else (if (isCelestial) 0.04f else 0.07f),
                            size = if (isNebulaPuff) 14f else (if (isCelestial) 8f else 5f),
                            isStar = isStarGlitter,
                            isNebula = isNebulaPuff
                        )
                    )
                }

                if (orb.isCelestialOrb) {
                    floatingTexts.add(FloatingText(orb.position, "CELESTIAL ANOMALY! +${orb.points * multiplier}", Color(0xFFFF00FF)))
                    triggerHaptic("heavy")
                    cameraShake = 12f
                } else if (orb.isSuperOrb) {
                    floatingTexts.add(FloatingText(orb.position, "+${orb.points * multiplier} length", Color(0xFFFFFF33)))
                    triggerHaptic("medium")
                } else {
                    triggerHaptic("light")
                }
            }

            // Check if bots eat
            for (snake in snakes) {
                if (snake.isPlayer || !snake.isAlive) continue
                val sEatingDist = (15f * snake.thicknessFactor) + 12f
                if (snake.position.distance(orb.position) < sEatingDist) {
                    val multiplier = if (snake.activePowerUpType == PowerUpType.DOUBLE_POINTS) 2 else 1
                    snake.score += orb.points * multiplier
                    snake.length = 4 + (snake.score / 25)
                    eatenOrbIds.add(orb.id)
                    
                    if (orb.isCelestialOrb) {
                        for (i in 0..5) {
                            val isNebulaPuff = i == 0
                            val isStarGlitter = i == 1
                            particles.add(
                                Particle(
                                    position = orb.position,
                                    velocity = Vector2D(Random.nextFloat() * 6f - 3f, Random.nextFloat() * 6f - 3f),
                                    color = orb.color,
                                    alpha = 0.8f,
                                    fadeSpeed = if (isNebulaPuff) 0.03f else 0.06f,
                                    size = if (isNebulaPuff) 12f else 6f,
                                    isStar = isStarGlitter,
                                    isNebula = isNebulaPuff
                                )
                            )
                        }
                    }
                }
            }
        }
        orbs.removeAll { eatenOrbIds.contains(it.id) }

        if (orbs.size < 350) {
            spawnOrbs(80)
        }

        // 12. Tick particles
        val iterator = particles.iterator()
        while (iterator.hasNext()) {
            val p = iterator.next()
            p.position += p.velocity
            p.alpha -= p.fadeSpeed
            if (p.alpha <= 0) {
                iterator.remove()
            }
        }

        // 13. Tick texts
        val textIterator = floatingTexts.iterator()
        while (textIterator.hasNext()) {
            val txt = textIterator.next()
            txt.position = txt.position + Vector2D(0f, txt.speedY)
            txt.life--
            txt.alpha = (txt.life / 40f).coerceIn(0f, 1f)
            if (txt.life <= 0) {
                textIterator.remove()
            }
        }

        // Clean lists
        snakes.removeAll { !it.isAlive && !it.isPlayer }

        // Players death respawn/game-over conditions
        if (!player.isAlive) {
            isGameOver = true
            if (gameMode != "Casual") {
                determinePlacement()
            }
        }

        if ((gameMode == "Battle Royale" || gameMode == "Ranked") && !isGameOver) {
            val survivors = snakes.filter { it.isAlive }
            if (survivors.size == 1 && survivors.first().isPlayer) {
                isVictory = true
                rankingPlacement = 1
                isGameOver = true
                totalCoinsEarned += 500
                totalXpEarned += 1000
                triggerHaptic("heavy")
                cameraShake = 20f
            }
        }

        updateLeaderboard()
    }

    fun triggerAbility(snake: Snake) {
        when (snake.activeAbilityType) {
            "SHIELD" -> {
                snake.specialAbilityActive = true
                snake.shieldDuration = 180 // lasts 3 seconds (180 frames)
                snake.abilityCooldownRemaining = 540 // 9 second cooldown
                snake.abilityActiveDuration = 180
                if (snake.isPlayer) {
                    floatingTexts.add(FloatingText(snake.position, "FORCEFIELD SHIELD ACTIVE!", Color(0xFF00E5FF)))
                    triggerHaptic("medium")
                    cameraShake = 6f
                }
                spawnRingParticles(snake.position, Color(0xFF00E5FF), 25, 8f)
            }
            "FREEZE_PULSE" -> {
                snake.abilityCooldownRemaining = 600 // 10 second cooldown
                if (snake.isPlayer) {
                    floatingTexts.add(FloatingText(snake.position, "FREEZE PULSE WAVE!", Color(0xFF80D8FF)))
                    triggerHaptic("heavy")
                    cameraShake = 16f
                }
                spawnRingParticles(snake.position, Color(0xFF33B5E5), 40, 11f)
                
                for (other in snakes) {
                    if (other.id == snake.id || !other.isAlive) continue
                    if (other.position.distance(snake.position) < 300f) {
                        if (other.activePowerUpType != PowerUpType.SHIELD && !other.specialAbilityActive) {
                            other.isFrozen = true
                            other.freezeTimer = 180 // freeze 3 seconds
                            if (other.isPlayer) {
                                floatingTexts.add(FloatingText(other.position, "FROZEN SLOW DOWN!", Color(0xFF80D8FF)))
                                triggerHaptic("heavy")
                            }
                        }
                    }
                }
            }
            "EMP_BLAST" -> {
                snake.abilityCooldownRemaining = 660 // 11 second cooldown
                if (snake.isPlayer) {
                    floatingTexts.add(FloatingText(snake.position, "EMP RUNE BLAST!", Color(0xFFFFCC00)))
                    triggerHaptic("heavy")
                    cameraShake = 22f
                }
                spawnRingParticles(snake.position, Color(0xFFFFEE55), 40, 10f)
                
                for (other in snakes) {
                    if (other.id == snake.id || !other.isAlive) continue
                    if (other.position.distance(snake.position) < 320f) {
                        if (other.activePowerUpType != PowerUpType.SHIELD && !other.specialAbilityActive) {
                            other.isEmped = true
                            other.empTimer = 180
                            other.isBoosting = false
                            if (other.isPlayer) {
                                floatingTexts.add(FloatingText(other.position, "EMP: BOOST BLOCKED!", Color(0xFFFFA000)))
                                triggerHaptic("heavy")
                            }
                        }
                    }
                }
                
                // Mutate nearby food into celestial food!
                var mutatedCount = 0
                val iterator = orbs.iterator()
                val newlyAdded = mutableListOf<Orb>()
                while (iterator.hasNext()) {
                    val orb = iterator.next()
                    if (orb.position.distance(snake.position) < 320f && !orb.isCelestialOrb && mutatedCount < 4) {
                        iterator.remove()
                        newlyAdded.add(Orb(UUID.randomUUID().toString(), orb.position, 22f, Color(0xFFFF00FF), 120, isSuperOrb = false, isCelestialOrb = true))
                        mutatedCount++
                    }
                }
                orbs.addAll(newlyAdded)
            }
            "SPEED_BURST" -> {
                snake.abilityActiveDuration = 100 // lasts 1.6 seconds
                snake.abilityCooldownRemaining = 420 // 7 seconds cooldown
                snake.specialAbilityActive = true
                if (snake.isPlayer) {
                    floatingTexts.add(FloatingText(snake.position, "HYPERSONIC BURST ACTIVE!", Color(0xFFFF5722)))
                    triggerHaptic("medium")
                    cameraShake = 5f
                }
                spawnRingParticles(snake.position, Color(0xFFFF5722), 20, 7f)
            }
            "GHOST_PHASE" -> {
                snake.abilityActiveDuration = 180 // lasts 3 seconds
                snake.abilityCooldownRemaining = 540 // 9 second cooldown
                snake.specialAbilityActive = true
                snake.activePowerUpType = PowerUpType.GHOST
                snake.powerUpTimer = 180
                if (snake.isPlayer) {
                    floatingTexts.add(FloatingText(snake.position, "GHOST INDUCTION ACTIVE!", Color(0xFFB0BEC5)))
                    triggerHaptic("light")
                }
                spawnRingParticles(snake.position, Color(0xFFCFD8DC), 15, 6f)
            }
        }
    }

    private fun spawnRingParticles(center: Vector2D, color: Color, count: Int, speed: Float) {
        for (i in 0 until count) {
            val angle = (i * (2 * Math.PI / count)).toFloat()
            val vel = Vector2D(cos(angle.toDouble()).toFloat() * speed, sin(angle.toDouble()).toFloat() * speed)
            particles.add(
                Particle(
                    center,
                    vel,
                    color,
                    alpha = 1.0f,
                    fadeSpeed = 0.04f,
                    size = 9f
                )
            )
        }
    }

    private fun updateBodySegments(snake: Snake) {
        // Snake moves. Head positions inserted at index 0.
        // We crop body size up to current tracked length * 4.
        // Node spline interpolation spacing. Segment is spawned every 4-5 ticks/frames.
        snake.body.add(0, Vector2D(snake.position.x, snake.position.y))
        
        val gap = 6 // step spacing between body rendering sections
        val targetSize = snake.length * gap
        while (snake.body.size > targetSize) {
            snake.body.removeAt(snake.body.lastIndex)
        }
    }

    private fun eliminateSnake(snake: Snake) {
        // Spawn premium food orbs in a beautiful spline configuration where they died
        val step = maxOf(1, snake.body.size / 10)
        for (i in 0 until snake.body.size step step) {
            val segmentPos = snake.body[i]
            val bigOrb = Orb(
                id = UUID.randomUUID().toString(),
                position = segmentPos,
                size = 12f,
                color = snake.primaryColor,
                points = 15,
                isSuperOrb = true
            )
            orbs.add(bigOrb)

            // Spawn explosive cosmic stargaze nebula graphic particles
            for (j in 0..3) {
                val vel = Vector2D(
                    (Random.nextFloat() * 10f - 5f),
                    (Random.nextFloat() * 10f - 5f)
                )
                val isNebulaPuff = j == 0
                val isStarGlitter = j == 1
                particles.add(
                    Particle(
                        position = segmentPos,
                        velocity = vel,
                        color = if (isStarGlitter) Color.White else (if (isNebulaPuff) snake.primaryColor else snake.secondaryColor),
                        alpha = 1.0f,
                        fadeSpeed = if (isNebulaPuff) 0.02f else 0.05f,
                        size = if (isNebulaPuff) 16f else (if (isStarGlitter) 7f else 10f),
                        isStar = isStarGlitter,
                        isNebula = isNebulaPuff
                    )
                )
            }
        }
    }

    fun publishKill(killer: Snake?, victim: Snake, cause: String) {
        val weaponOrCause = when (cause) {
            "collision" -> {
                if (killer != null) {
                    when {
                        killer.specialAbilityActive && killer.activeAbilityType == "SPEED_BURST" -> "Overdrive Strike ⚡"
                        killer.specialAbilityActive && killer.activeAbilityType == "SHIELD" -> "Forcefield Smash 🛡️"
                        killer.specialAbilityActive && killer.activeAbilityType == "FREEZE_PULSE" -> "Sub-Zero Slam ❄️"
                        killer.specialAbilityActive && killer.activeAbilityType == "EMP_BLAST" -> "EMP Disruptor 💥"
                        killer.specialAbilityActive && killer.activeAbilityType == "GHOST_PHASE" -> "Phased Ambush 👻"
                        else -> "Grid Collision 💥"
                    }
                } else {
                    "Grid Collision 💥"
                }
            }
            "electro_gate" -> "Electro-Gate Overload ⚡"
            "lava_pit" -> "Thermal Lava Melt 🔥"
            "ice_spike" -> "Glacial Spear Pierced ❄️"
            "quantum_vortex" -> "Singularity Collapse 🌌"
            "border" -> "Vector Out-of-Bounds 🚫"
            "safe_zone" -> "System Zone Dissolution ☠️"
            else -> cause
        }
        val event = KillEvent(
            id = java.util.UUID.randomUUID().toString(),
            killerName = killer?.name,
            victimName = victim.name,
            weaponOrCause = weaponOrCause
        )
        synchronized(killEvents) {
            killEvents.add(0, event)
            if (killEvents.size > 5) {
                killEvents.removeAt(killEvents.lastIndex)
            }
        }
    }

    private fun determinePlacement() {
        // Placement corresponds to order of other live snakes remaining
        val liveBotsCount = snakes.filter { !it.isPlayer && it.isAlive }.size
        rankingPlacement = liveBotsCount + 1
        // Assign coins based on performance
        val scaleCoeff = if (rankingPlacement == 1) 350 else if (rankingPlacement <= 3) 150 else 30
        totalCoinsEarned += scaleCoeff
        totalXpEarned += (400 - (rankingPlacement * 20)).coerceAtLeast(50)
    }

    private fun updateLeaderboard() {
        alivePlayersCount = snakes.filter { it.isAlive }.size
        val ranked = snakes.map { Pair(it.name, it.score) }.sortedByDescending { it.second }
        rankingList.clear()
        rankingList.addAll(ranked.take(10))
    }

    private fun triggerHaptic(type: String) {
        onHapticTrigger?.invoke(type)
    }

    fun syncWithServerSnapshot(snapshot: com.example.server.ServerStateSnapshot) {
        synchronized(snakes) {
            snakes.clear()
            snapshot.snakes.forEach { sState ->
                val primaryColorVal = try {
                    Color(android.graphics.Color.parseColor(sState.primaryColorHex))
                } catch (e: Exception) {
                    Color(0xFF00FFCC)
                }
                val secondaryColorVal = try {
                    Color(android.graphics.Color.parseColor(sState.secondaryColorHex))
                } catch (e: Exception) {
                    Color(0xFF0099FF)
                }
                
                val snake = Snake(
                    id = sState.id,
                    name = sState.name,
                    isPlayer = sState.id == "player_local",
                    isAlive = sState.isAlive,
                    position = sState.position,
                    angle = sState.angle,
                    speed = sState.speed,
                    length = sState.length,
                    score = sState.score,
                    primaryColor = primaryColorVal,
                    secondaryColor = secondaryColorVal,
                    isBoosting = sState.isBoosting,
                    activePowerUpType = sState.activePowerUpType,
                    powerUpTimer = sState.powerUpTimer
                )
                sState.body.forEach { seg ->
                    snake.body.add(seg)
                }
                snakes.add(snake)
                if (sState.id == "player_local") {
                    playerSnake = snake
                    isGameOver = !sState.isAlive
                }
            }
            
            orbs.clear()
            snapshot.orbs.forEach { oState ->
                val colorVal = try {
                    Color(android.graphics.Color.parseColor(oState.colorHex))
                } catch (e: Exception) {
                    Color(0xFF00FFCC)
                }
                orbs.add(
                    Orb(
                        id = oState.id,
                        position = oState.position,
                        size = if (oState.isCelestial) 14f else if (oState.isSuper) 10f else 6f,
                        color = colorVal,
                        points = oState.points,
                        isSuperOrb = oState.isSuper,
                        isCelestialOrb = oState.isCelestial
                    )
                )
            }
            
            powerUps.clear()
            snapshot.powerUps.forEach { pState ->
                val colorVal = try {
                    Color(android.graphics.Color.parseColor(pState.colorHex))
                } catch (e: Exception) {
                    Color(0xFF00FFCC)
                }
                powerUps.add(
                    PowerUp(
                        id = pState.id,
                        position = pState.position,
                        type = pState.type,
                        color = colorVal
                    )
                )
            }
            
            activeEventName = snapshot.activeEvent
            safeZoneRadius = snapshot.safeZoneRadius
            alivePlayersCount = snakes.filter { it.isAlive }.size
            val ranked = snakes.map { Pair(it.name, it.score) }.sortedByDescending { it.second }
            rankingList.clear()
            rankingList.addAll(ranked.take(10))
        }
    }
}
