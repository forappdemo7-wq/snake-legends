package com.example.server

import com.example.game.PowerUpType
import com.example.game.Vector2D
import com.example.game.Hazard
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.atan2

class CollisionManager {
    
    data class CollisionResult(
        val isDead: Boolean,
        val killerId: String?,
        val victimId: String,
        val cause: String // "border", "safe_zone", "lava_pit", "electro_gate", "ice_spike", "quantum_vortex", "collision"
    )

    fun checkBoundaryCollision(
        snakeId: String,
        position: Vector2D,
        activePowerUp: PowerUpType?,
        arenaWidth: Float,
        arenaHeight: Float
    ): Pair<Vector2D, CollisionResult?> {
        var nextPos = position
        var collision: CollisionResult? = null

        if (activePowerUp == PowerUpType.GHOST) {
            // Phasing wrap-around
            var wrapped = false
            var rx = position.x
            var ry = position.y
            if (position.x < 0) {
                rx = arenaWidth
                wrapped = true
            } else if (position.x > arenaWidth) {
                rx = 0f
                wrapped = true
            }
            if (position.y < 0) {
                ry = arenaHeight
                wrapped = true
            } else if (position.y > arenaHeight) {
                ry = 0f
                wrapped = true
            }
            if (wrapped) {
                nextPos = Vector2D(rx, ry)
            }
        } else {
            // Strict borders
            if (position.x < 0 || position.x > arenaWidth || position.y < 0 || position.y > arenaHeight) {
                collision = CollisionResult(
                    isDead = true,
                    killerId = null,
                    victimId = snakeId,
                    cause = "border"
                )
            }
        }
        return Pair(nextPos, collision)
    }

    fun checkSafeZoneCollision(
        snakeId: String,
        position: Vector2D,
        safeZoneCenter: Vector2D,
        safeZoneRadius: Float,
        currentLength: Int
    ): CollisionResult? {
        val distFromCenter = position.distance(safeZoneCenter)
        if (distFromCenter > safeZoneRadius) {
            // Inside storm damage zone
            if (currentLength < 3) {
                return CollisionResult(
                    isDead = true,
                    killerId = null,
                    victimId = snakeId,
                    cause = "safe_zone"
                )
            }
        }
        return null
    }

    fun checkHazardCollisions(
        snakeId: String,
        position: Vector2D,
        angle: Float,
        speed: Float,
        activePowerUp: PowerUpType?,
        specialAbilityActive: Boolean,
        hazards: List<Hazard>
    ): Pair<Float, CollisionResult?> {
        var updatedAngle = angle
        var collision: CollisionResult? = null

        for (hazard in hazards) {
            val dist = position.distance(hazard.position)
            when (hazard.type) {
                "electro_gate" -> {
                    val isActive = (hazard.state % 120) < 84
                    if (isActive && dist < hazard.size + 10f) {
                        if (activePowerUp != PowerUpType.SHIELD && activePowerUp != PowerUpType.GHOST && !specialAbilityActive) {
                            collision = CollisionResult(
                                isDead = true,
                                killerId = null,
                                victimId = snakeId,
                                cause = "electro_gate"
                            )
                            break
                        }
                    }
                }
                "ice_spike" -> {
                    if (dist < hazard.size + 12f) {
                        if (activePowerUp != PowerUpType.SHIELD && !specialAbilityActive) {
                            collision = CollisionResult(
                                isDead = true,
                                killerId = null,
                                victimId = snakeId,
                                cause = "ice_spike"
                            )
                            break
                        } else {
                            // Bounce off spikes safely if shielded
                            val pushDir = (position - hazard.position).normalized()
                            updatedAngle = atan2(pushDir.y.toDouble(), pushDir.x.toDouble()).toFloat()
                        }
                    }
                }
                "quantum_vortex" -> {
                    if (dist < hazard.size * 0.45f) {
                        if (activePowerUp != PowerUpType.SHIELD && activePowerUp != PowerUpType.GHOST && !specialAbilityActive) {
                            collision = CollisionResult(
                                isDead = true,
                                killerId = null,
                                victimId = snakeId,
                                cause = "quantum_vortex"
                            )
                            break
                        }
                    }
                }
            }
        }
        return Pair(updatedAngle, collision)
    }

    fun checkSnakeCollisions(
        snakes: List<SnakeState>
    ): List<CollisionResult> {
        val results = mutableListOf<CollisionResult>()

        for (snake in snakes) {
            if (!snake.isAlive) continue

            for (other in snakes) {
                if (!other.isAlive || snake.id == other.id) continue
                // Shielded/Phasing states ignore hits
                if (snake.activePowerUpType == PowerUpType.SHIELD || snake.activePowerUpType == PowerUpType.GHOST || snake.abilityActiveDuration > 0) continue

                val segmentsToCheck = if (other.body.size > 2) other.body.subList(2, other.body.size) else other.body
                
                // Calculate thickness scaling
                val snakeThick = 1.0f + kotlin.math.sqrt(snake.score.toFloat().coerceAtLeast(0f)) * 0.085f
                val otherThick = 1.0f + kotlin.math.sqrt(other.score.toFloat().coerceAtLeast(0f)) * 0.085f
                val threshold = (11f * snakeThick) + (11f * otherThick)

                for (seg in segmentsToCheck) {
                    if (snake.position.distance(seg) < threshold) {
                        results.add(
                            CollisionResult(
                                isDead = true,
                                killerId = other.id,
                                victimId = snake.id,
                                cause = "collision"
                            )
                        )
                        break
                    }
                }
            }
        }
        return results
    }
}
