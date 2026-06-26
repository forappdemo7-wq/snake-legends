package com.example.server

import android.util.Log
import com.example.game.Vector2D
import com.example.game.PowerUpType
import com.example.game.Hazard
import kotlinx.coroutines.*
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.cos
import kotlin.math.sin

class GameServer {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var serverJob: Job? = null
    
    // Core Modules
    val antiCheat = ServerAntiCheatEngine()
    val collision = CollisionManager()
    val food = FoodManager()
    val social = FriendsClansManager()
    val ranking = RankingManager()
    val optimizer = NetworkOptimizer()
    val replay = ReplaySystem()
    val observability = ObservabilityManager()
    val sessions = PlayerSessionManager()
    val matches = MatchManager(social, ranking)

    // Match Constants
    val arenaWidth = 3000f
    val arenaHeight = 3000f
    private val TICK_RATE_HZ = 30
    private val MS_PER_TICK = 1000L / TICK_RATE_HZ

    // Simulation states
    private var serverTick = 0L
    private val activeSnakes = ConcurrentHashMap<String, SnakeState>()
    private val activeHazards = CopyOnWriteArrayList<Hazard>()
    private val playerInputs = ConcurrentHashMap<String, ClientInputPacket>()
    
    // Event buffers
    private val tickEvents = CopyOnWriteArrayList<String>()
    private var activeEvent = "CALM"
    private var safeZoneRadius = 3000f
    private val safeZoneCenter = Vector2D(1500f, 1500f)

    // Registered local snapshot receivers
    private val localSnapshotListeners = CopyOnWriteArrayList<(ServerStateSnapshot) -> Unit>()

    init {
        // Initialize hazards authoritatively
        initializeHazards()
        // Food spawn
        food.spawnInitialFood(arenaWidth, arenaHeight, targetOrbCount = 200)
    }

    fun startServer() {
        if (serverJob != null) return
        Log.i("GameServer", "LAUNCHING AUTHORITATIVE DEDICATED GAME SERVER (30Hz TICK RATE)")
        observability.incrementMatches()
        
        serverJob = scope.launch {
            var lastTime = System.currentTimeMillis()
            while (isActive) {
                val startTime = System.currentTimeMillis()
                
                // 1. Core Physics Simulation Tick
                simulateTick()
                
                // 2. Performance Tracking
                val duration = System.currentTimeMillis() - startTime
                observability.recordTick(duration)

                // 3. Sync Sleep
                val delayTime = MS_PER_TICK - duration
                if (delayTime > 0) {
                    delay(delayTime)
                } else {
                    Log.w("GameServer", "SERVER TICK OVERRUN! Frame took $duration ms (Target max: $MS_PER_TICK ms)")
                }
            }
        }
    }

    fun stopServer() {
        serverJob?.cancel()
        serverJob = null
        activeSnakes.clear()
        playerInputs.clear()
        Log.i("GameServer", "AUTHORITATIVE GAME SERVER SHUTDOWN COMPLETED.")
    }

    private fun initializeHazards() {
        activeHazards.clear()
        activeHazards.add(Hazard("hz_lava_1", "lava_pit", Vector2D(600f, 600f), size = 80f))
        activeHazards.add(Hazard("hz_electro_1", "electro_gate", Vector2D(1800f, 1200f), size = 45f))
        activeHazards.add(Hazard("hz_ice_1", "ice_spike", Vector2D(1000f, 2000f), size = 50f))
        activeHazards.add(Hazard("hz_vortex_1", "quantum_vortex", Vector2D(2200f, 2200f), size = 90f))
    }

    // ---------- External Player API ----------

    fun joinPlayerSession(playerId: String, username: String) {
        sessions.authenticateGuest(playerId, username)
        
        val startPos = Vector2D(1000f + (Math.random() * 1000f).toFloat(), 1000f + (Math.random() * 1000f).toFloat())
        val defaultColor = "#00FFCC"
        val secColor = "#0099FF"
        
        val initialBody = listOf(
            startPos,
            Vector2D(startPos.x, startPos.y + 15f),
            Vector2D(startPos.x, startPos.y + 30f),
            Vector2D(startPos.x, startPos.y + 45f)
        )

        val snake = SnakeState(
            id = playerId,
            name = username,
            position = startPos,
            angle = 0f,
            speed = 4.0f,
            length = 4,
            score = 0,
            isBoosting = false,
            body = initialBody,
            isAlive = true,
            activePowerUpType = null,
            powerUpTimer = 0,
            activeAbilityType = "SHIELD",
            abilityCooldownRemaining = 0,
            abilityActiveDuration = 0,
            isFrozen = false,
            isEmped = false,
            primaryColorHex = defaultColor,
            secondaryColorHex = secColor
        )
        
        activeSnakes[playerId] = snake
        observability.incrementActivePlayers()
        social.setPlayerOnline(playerId, true, "Active Battle")
    }

    fun submitPlayerInput(playerId: String, packet: ClientInputPacket) {
        // Rate-Limit protection check
        if (!sessions.checkRateLimit(playerId)) return

        // Packet authentication signature verify
        if (!sessions.verifyPacketSignature(playerId, packet.tickNumber, packet.timestamp, packet.signature)) {
            Log.e("GameServer", "SECURITY BLOCK: Invalid packet cryptographic signature for player $playerId!")
            return
        }

        playerInputs[packet.playerId] = packet
    }

    fun leavePlayerSession(playerId: String) {
        activeSnakes.remove(playerId)
        playerInputs.remove(playerId)
        sessions.removeSession(playerId)
        antiCheat.clearPlayerSession(playerId)
        observability.decrementActivePlayers()
    }

    fun registerSnapshotListener(listener: (ServerStateSnapshot) -> Unit) {
        localSnapshotListeners.add(listener)
    }

    fun unregisterSnapshotListener(listener: (ServerStateSnapshot) -> Unit) {
        localSnapshotListeners.remove(listener)
    }

    // ---------- Core Autoritative Loop Execution ----------

    private fun simulateTick() {
        serverTick++
        tickEvents.clear()

        // 1. Process hazards ticking
        activeHazards.forEach { it.state = (it.state + 1) % 360 }

        // 2. Magnet Pull & Spawn updates
        food.applyMagnetPull(activeSnakes.values.toList())
        food.replenishFood(arenaWidth, arenaHeight, 200)

        // 3. Authoritative Movement & Input Application
        val currentSnakes = activeSnakes.entries.toList()
        for ((pId, snake) in currentSnakes) {
            if (!snake.isAlive) continue

            val input = playerInputs[pId]
            var joystickAngle = snake.angle
            var isBoosting = snake.isBoosting
            var triggerAbility = false

            if (input != null && input.tickNumber >= serverTick - 10) { // Limit window tolerance
                if (input.joystickAngle != null) {
                    joystickAngle = input.joystickAngle
                }
                isBoosting = input.isBoosting
                triggerAbility = input.triggerAbility
            }

            // Anti-cheat verification
            val antiCheatResponse = antiCheat.validatePlayerInput(
                pId,
                snake.position,
                snake.score,
                snake.length,
                triggerAbility
            )
            if (antiCheatResponse == AntiCheatAction.BAN) {
                // Permanently ban hacker
                leavePlayerSession(pId)
                continue
            }

            // Calculate Speed & Turning Physics
            var targetSpeed = if (isBoosting && snake.length >= 4) 7.5f else 4.0f
            if (snake.activePowerUpType == PowerUpType.SPEED_BOOST) targetSpeed *= 1.6f
            if (snake.isFrozen) targetSpeed = 1.5f

            val interpolatedSpeed = snake.speed + (targetSpeed - snake.speed) * 0.14f
            val dirX = cos(joystickAngle.toDouble()).toFloat()
            val dirY = sin(joystickAngle.toDouble()).toFloat()
            val nextPosition = snake.position + Vector2D(dirX * interpolatedSpeed, dirY * interpolatedSpeed)

            // Update body history trail authoritatively
            val nextBody = mutableListOf<Vector2D>()
            nextBody.add(nextPosition)
            if (snake.body.isNotEmpty()) {
                var previousSeg = nextPosition
                for (i in 0 until snake.length - 1) {
                    val currentSeg = snake.body.getOrNull(i) ?: nextPosition
                    val dist = previousSeg.distance(currentSeg)
                    if (dist > 15f) {
                        val pullDir = (previousSeg - currentSeg).normalized()
                        val newSeg = previousSeg - pullDir * 15f
                        nextBody.add(newSeg)
                        previousSeg = newSeg
                    } else {
                        nextBody.add(currentSeg)
                        previousSeg = currentSeg
                    }
                }
            }

            // Decrement powerup & ability cooldowns authoritatively
            var puTimer = maxOf(0, snake.powerUpTimer - 1)
            var activePu = if (puTimer <= 0) null else snake.activePowerUpType

            // Rebuild updated authoritative state for this tick
            val updatedSnake = snake.copy(
                position = nextPosition,
                angle = joystickAngle,
                speed = interpolatedSpeed,
                body = nextBody,
                isBoosting = isBoosting,
                activePowerUpType = activePu,
                powerUpTimer = puTimer
            )
            activeSnakes[pId] = updatedSnake
        }

        // 4. Server-Side Boundary & Hazard Collision Validations
        for ((pId, snake) in activeSnakes) {
            if (!snake.isAlive) continue

            // 4.1 Border collisions
            val (boundedPos, borderResult) = collision.checkBoundaryCollision(
                pId, snake.position, snake.activePowerUpType, arenaWidth, arenaHeight
            )
            if (borderResult != null) {
                eliminatePlayer(pId, borderResult.cause)
                continue
            }
            if (boundedPos != snake.position) {
                activeSnakes[pId] = snake.copy(position = boundedPos, body = listOf(boundedPos) + snake.body.drop(1))
            }

            // 4.2 Hazard collisions
            val (adjustedAngle, hazardResult) = collision.checkHazardCollisions(
                pId, snake.position, snake.angle, snake.speed, snake.activePowerUpType,
                snake.abilityActiveDuration > 0, activeHazards
            )
            if (hazardResult != null) {
                eliminatePlayer(pId, hazardResult.cause)
                continue
            }
            if (adjustedAngle != snake.angle) {
                activeSnakes[pId] = snake.copy(angle = adjustedAngle)
            }
        }

        // 5. Head-to-Body Snake Collision Resolution
        val collisions = collision.checkSnakeCollisions(activeSnakes.values.toList())
        collisions.forEach { col ->
            eliminatePlayer(col.victimId, col.cause, col.killerId)
        }

        // 6. Food and Powerup verified ingestion
        for ((pId, snake) in activeSnakes) {
            if (!snake.isAlive) continue

            val hasDouble = snake.activePowerUpType == PowerUpType.DOUBLE_POINTS
            val thick = 1.0f + kotlin.math.sqrt(snake.score.toFloat().coerceAtLeast(0f)) * 0.085f
            
            val collectionResult = food.validateCollection(pId, snake.position, thick, hasDouble)
            if (collectionResult.scoreIncrement > 0) {
                val nextScore = snake.score + collectionResult.scoreIncrement
                val nextLength = 4 + (nextScore / 25)
                
                val updated = snake.copy(
                    score = nextScore,
                    length = nextLength
                )
                activeSnakes[pId] = updated
                
                // Track stats profiles on social database
                social.updateProfileStats(pId, xpGained = (collectionResult.scoreIncrement * 4L), score = nextScore, isWin = false)
            }

            if (collectionResult.collectedPowerUp != null) {
                val pUp = collectionResult.collectedPowerUp
                activeSnakes[pId] = snake.copy(
                    activePowerUpType = pUp.type,
                    powerUpTimer = 450
                )
                tickEvents.add("pu_collected:${snake.name}:${pUp.type}")
            }
        }

        // 7. Replay frame recording
        if (activeSnakes.isNotEmpty()) {
            replay.recordFrame(
                matchId = "active_match",
                tick = serverTick,
                inputs = playerInputs.values.toList(),
                events = tickEvents.toList()
            )
        }

        // 8. Generate & Broadcast authoritatively validated state snapshot to local listening clients
        val stateSnapshot = generateServerSnapshot()
        
        // Dispatch to client receivers
        localSnapshotListeners.forEach { it(stateSnapshot) }
    }

    private fun eliminatePlayer(playerId: String, cause: String, killerId: String? = null) {
        val snake = activeSnakes[playerId] ?: return
        val killedState = snake.copy(isAlive = false)
        activeSnakes[playerId] = killedState
        
        val killerName = killerId?.let { activeSnakes[it]?.name } ?: "The Void"
        Log.i("GameServer", "[ELIMINATION] Player '${snake.name}' slain by '$killerName' (Cause: $cause)")
        
        tickEvents.add("kill:${snake.name}:$killerName:$cause")

        // Explode snake body segments into sweet food orbs
        food.spawnSlainSnakeFood(snake.body, snake.primaryColorHex)

        // Adjust Elo rating MMR placements
        ranking.updateCompetitiveRating(
            playerId = playerId,
            placement = activeSnakes.values.count { it.isAlive } + 1,
            totalPlayers = activeSnakes.size,
            kills = if (killerId != null) 1 else 0,
            score = snake.score
        )
    }

    private fun generateServerSnapshot(): ServerStateSnapshot {
        val rawSnapshotText = "tick:$serverTick:snakes:${activeSnakes.size}"
        val signature = sessions.signServerPayload(rawSnapshotText)

        return ServerStateSnapshot(
            tick = serverTick,
            timestamp = System.currentTimeMillis(),
            snakes = activeSnakes.values.toList(),
            orbs = food.getOrbsSnapshot(),
            powerUps = food.getPowerUpsSnapshot(),
            activeEvent = activeEvent,
            safeZoneRadius = safeZoneRadius,
            serverSignature = signature
        )
    }
}
