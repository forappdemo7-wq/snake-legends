package com.example.server

import androidx.compose.ui.graphics.Color
import com.example.game.PowerUpType
import com.example.game.Vector2D
import java.util.UUID
import kotlin.random.Random

class FoodManager {
    private val orbs = mutableListOf<OrbState>()
    private val powerUps = mutableListOf<PowerUpState>()

    private val orbColors = listOf(
        "#00FFCC", "#FF3366", "#FFCC00", "#33FFF0",
        "#FF33FF", "#9933FF", "#33FFCC", "#FF9900"
    )

    fun spawnInitialFood(width: Float, height: Float, targetOrbCount: Int) {
        orbs.clear()
        powerUps.clear()
        replenishFood(width, height, targetOrbCount)
        replenishPowerUps(width, height, 8)
    }

    fun replenishFood(width: Float, height: Float, targetCount: Int) {
        val currentSize = orbs.size
        if (currentSize < targetCount) {
            val toSpawn = targetCount - currentSize
            for (i in 0 until toSpawn) {
                val id = UUID.randomUUID().toString()
                val pos = Vector2D(Random.nextFloat() * width, Random.nextFloat() * height)
                
                val rand = Random.nextInt(100)
                val isCelestial = rand == 0
                val isSuper = !isCelestial && rand < 10
                
                val points = if (isCelestial) 150 else if (isSuper) 25 else 4
                val colorHex = if (isCelestial) "#FF00FF" else orbColors.random()
                
                orbs.add(
                    OrbState(
                        id = id,
                        position = pos,
                        points = points,
                        isSuper = isSuper,
                        isCelestial = isCelestial,
                        colorHex = colorHex
                    )
                )
            }
        }
    }

    fun replenishPowerUps(width: Float, height: Float, targetCount: Int) {
        val currentSize = powerUps.size
        if (currentSize < targetCount) {
            val toSpawn = targetCount - currentSize
            val types = PowerUpType.values()
            for (i in 0 until toSpawn) {
                val id = UUID.randomUUID().toString()
                val pos = Vector2D(Random.nextFloat() * (width - 100f) + 50f, Random.nextFloat() * (height - 100f) + 50f)
                val type = types.random()
                val colorHex = when (type) {
                    PowerUpType.MAGNET -> "#22D3EE"
                    PowerUpType.DOUBLE_POINTS -> "#FBBF24"
                    PowerUpType.SHIELD -> "#38BDF8"
                    PowerUpType.GROWTH -> "#34D399"
                    PowerUpType.GHOST -> "#A78BFA"
                    PowerUpType.SPEED_BOOST -> "#F87171"
                }
                powerUps.add(
                    PowerUpState(
                        id = id,
                        position = pos,
                        type = type,
                        colorHex = colorHex
                    )
                )
            }
        }
    }

    fun applyMagnetPull(snakes: List<SnakeState>) {
        for (snake in snakes) {
            if (!snake.isAlive || snake.activePowerUpType != PowerUpType.MAGNET) continue
            
            for (i in orbs.indices) {
                val orb = orbs[i]
                val dist = snake.position.distance(orb.position)
                if (dist < 185f) {
                    val pullForce = ((185f - dist) / 185f) * 6.5f + 1.5f
                    val pullDir = (snake.position - orb.position).normalized()
                    orbs[i] = orb.copy(position = orb.position + pullDir * pullForce)
                }
            }
        }
    }

    data class EatResult(
        val eatenOrbIds: List<String>,
        val collectedPowerUp: PowerUpState?,
        val scoreIncrement: Int,
        val lengthIncrement: Int
    )

    fun validateCollection(
        snakeId: String,
        position: Vector2D,
        thicknessFactor: Float,
        hasDoublePoints: Boolean
    ): EatResult {
        val eatenOrbs = mutableListOf<String>()
        var scoreGained = 0
        var lengthGained = 0

        val sEatingDist = (15f * thicknessFactor) + 12f
        val iterator = orbs.iterator()
        while (iterator.hasNext()) {
            val orb = iterator.next()
            if (position.distance(orb.position) < sEatingDist) {
                eatenOrbs.add(orb.id)
                iterator.remove()
                
                val multiplier = if (hasDoublePoints) 2 else 1
                scoreGained += orb.points * multiplier
            }
        }

        var collectedPowerUp: PowerUpState? = null
        val pEatingDist = 38f
        val pIterator = powerUps.iterator()
        while (pIterator.hasNext()) {
            val pUp = pIterator.next()
            if (position.distance(pUp.position) < pEatingDist) {
                collectedPowerUp = pUp
                pIterator.remove()
                
                if (pUp.type == PowerUpType.GROWTH) {
                    scoreGained += 75
                }
                break
            }
        }

        if (scoreGained > 0) {
            lengthGained = scoreGained / 25
        }

        return EatResult(eatenOrbs, collectedPowerUp, scoreGained, lengthGained)
    }

    fun spawnSlainSnakeFood(segments: List<Vector2D>, colorHex: String) {
        segments.forEach { seg ->
            if (Random.nextInt(3) == 0) {
                orbs.add(
                    OrbState(
                        id = UUID.randomUUID().toString(),
                        position = seg,
                        points = 10,
                        isSuper = true,
                        isCelestial = false,
                        colorHex = colorHex
                    )
                )
            }
        }
    }

    fun getOrbsSnapshot(): List<OrbState> = orbs.toList()
    fun getPowerUpsSnapshot(): List<PowerUpState> = powerUps.toList()
}
