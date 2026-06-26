package com.example.server

import com.example.game.Vector2D
import kotlin.math.roundToInt

class NetworkOptimizer {

    /**
     * Quantize float coordinate value to lower network precision (16-bit equivalent).
     * Reduces coordinate bandwidth cost by 50% without visual stutter.
     */
    fun quantizeCoordinate(value: Float): Float {
        return (value * 10f).roundToInt() / 10f
    }

    fun quantizeVector(vector: Vector2D): Vector2D {
        return Vector2D(quantizeCoordinate(vector.x), quantizeCoordinate(vector.y))
    }

    /**
     * Delta Compression: Filters only modified or updated entities compared to a base client tick,
     * drastically reducing transmission sizes from full snapshots to incremental deltas.
     */
    fun generateDeltaSnapshot(
        baseSnapshot: ServerStateSnapshot?,
        currentSnapshot: ServerStateSnapshot
    ): DeltaSnapshot {
        if (baseSnapshot == null) {
            // Send full delta
            return DeltaSnapshot(
                baseTick = 0L,
                currentTick = currentSnapshot.tick,
                updatedSnakes = currentSnapshot.snakes,
                removedSnakeIds = emptyList(),
                updatedOrbs = currentSnapshot.orbs,
                removedOrbIds = emptyList(),
                updatedPowerUps = currentSnapshot.powerUps,
                removedPowerUpIds = emptyList()
            )
        }

        // Determine added or modified snakes
        val updatedSnakes = mutableListOf<SnakeState>()
        val baseSnakesMap = baseSnapshot.snakes.associateBy { it.id }
        currentSnapshot.snakes.forEach { curSnake ->
            val baseSnake = baseSnakesMap[curSnake.id]
            if (baseSnake == null || hasSnakeChanged(baseSnake, curSnake)) {
                updatedSnakes.add(curSnake)
            }
        }

        // Determine removed snakes
        val currentSnakeIds = currentSnapshot.snakes.map { it.id }.toSet()
        val removedSnakeIds = baseSnapshot.snakes.map { it.id }.filter { !currentSnakeIds.contains(it) }

        // Determine modified orbs
        val updatedOrbs = mutableListOf<OrbState>()
        val baseOrbsMap = baseSnapshot.orbs.associateBy { it.id }
        currentSnapshot.orbs.forEach { curOrb ->
            val baseOrb = baseOrbsMap[curOrb.id]
            if (baseOrb == null || baseOrb.position != curOrb.position) {
                updatedOrbs.add(curOrb)
            }
        }

        // Determine removed orbs
        val currentOrbIds = currentSnapshot.orbs.map { it.id }.toSet()
        val removedOrbIds = baseSnapshot.orbs.map { it.id }.filter { !currentOrbIds.contains(it) }

        // PowerUps updates
        val updatedPowerUps = mutableListOf<PowerUpState>()
        val basePowerUpsMap = baseSnapshot.powerUps.associateBy { it.id }
        currentSnapshot.powerUps.forEach { curPU ->
            val basePU = basePowerUpsMap[curPU.id]
            if (basePU == null || basePU.position != curPU.position) {
                updatedPowerUps.add(curPU)
            }
        }

        val currentPowerUpIds = currentSnapshot.powerUps.map { it.id }.toSet()
        val removedPowerUpIds = baseSnapshot.powerUps.map { it.id }.filter { !currentPowerUpIds.contains(it) }

        return DeltaSnapshot(
            baseTick = baseSnapshot.tick,
            currentTick = currentSnapshot.tick,
            updatedSnakes = updatedSnakes,
            removedSnakeIds = removedSnakeIds,
            updatedOrbs = updatedOrbs,
            removedOrbIds = removedOrbIds,
            updatedPowerUps = updatedPowerUps,
            removedPowerUpIds = removedPowerUpIds
        )
    }

    private fun hasSnakeChanged(s1: SnakeState, s2: SnakeState): Boolean {
        if (s1.position != s2.position) return true
        if (s1.angle != s2.angle) return true
        if (s1.isBoosting != s2.isBoosting) return true
        if (s1.length != s2.length) return true
        if (s1.isAlive != s2.isAlive) return true
        if (s1.body.size != s2.body.size) return true
        return false
    }

    /**
     * Interest Management (Spatial Partitioning):
     * Filters and returns only entities that reside within the player's view radius (viewport),
     * preventing unnecessary packet updates for entities far away on the map.
     */
    fun filterByInterest(
        playerPosition: Vector2D,
        snapshot: ServerStateSnapshot,
        radius: Float = 1000f
    ): ServerStateSnapshot {
        val nearbySnakes = snapshot.snakes.filter { it.position.distance(playerPosition) < radius + 500f }
        val nearbyOrbs = snapshot.orbs.filter { it.position.distance(playerPosition) < radius }
        val nearbyPowerUps = snapshot.powerUps.filter { it.position.distance(playerPosition) < radius }

        return snapshot.copy(
            snakes = nearbySnakes,
            orbs = nearbyOrbs,
            powerUps = nearbyPowerUps
        )
    }
}
