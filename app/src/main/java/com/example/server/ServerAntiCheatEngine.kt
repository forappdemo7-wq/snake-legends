package com.example.server

import android.util.Log
import com.example.game.Vector2D
import java.util.concurrent.ConcurrentHashMap

enum class AntiCheatAction {
    NONE,
    WARNING,
    FLAG,
    SUSPEND,
    BAN
}

data class CheatViolation(
    val playerId: String,
    val type: String,
    val severity: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val details: String
)

class ServerAntiCheatEngine {
    private val playerViolations = ConcurrentHashMap<String, MutableList<CheatViolation>>()
    private val lastPlayerPositions = ConcurrentHashMap<String, Vector2D>()
    private val lastPlayerScores = ConcurrentHashMap<String, Int>()
    private val lastPlayerLengths = ConcurrentHashMap<String, Int>()
    private val lastAbilityTimestamps = ConcurrentHashMap<String, Long>()
    
    // Limits
    private val MAX_SPEED_LIMIT = 24.0f // Absolute maximum velocity with extreme speed boost
    private val MAX_SCORE_GROWTH_PER_TICK = 500 // Absolute maximum score per single frame (e.g. eating celestial orb)
    private val MAX_LENGTH_GROWTH_PER_TICK = 20
    private val MIN_ABILITY_COOLDOWN_MS = 2500L

    fun validatePlayerInput(
        playerId: String,
        currentPos: Vector2D,
        currentScore: Int,
        currentLength: Int,
        isUsingAbility: Boolean
    ): AntiCheatAction {
        val now = System.currentTimeMillis()
        val violations = playerViolations.getOrPut(playerId) { mutableListOf() }
        
        // 1. Teleport and Speed Validation
        val lastPos = lastPlayerPositions[playerId]
        if (lastPos != null) {
            val travelDistance = currentPos.distance(lastPos)
            if (travelDistance > MAX_SPEED_LIMIT) {
                val excess = travelDistance - MAX_SPEED_LIMIT
                logViolation(
                    playerId,
                    "SPEED_HACK",
                    severity = excess / 5.0f,
                    details = "Traveled $travelDistance units in one tick (Limit: $MAX_SPEED_LIMIT). Excess: $excess"
                )
            }
        }
        lastPlayerPositions[playerId] = currentPos

        // 2. Score Validation
        val lastScore = lastPlayerScores[playerId]
        if (lastScore != null) {
            val scoreDiff = currentScore - lastScore
            if (scoreDiff > MAX_SCORE_GROWTH_PER_TICK) {
                logViolation(
                    playerId,
                    "SCORE_EXPLOIT",
                    severity = scoreDiff / 100.0f,
                    details = "Score grew by $scoreDiff in one tick (Limit: $MAX_SCORE_GROWTH_PER_TICK)"
                )
            } else if (scoreDiff < 0) {
                // Score decrease can happen from boosting, which is fine
            }
        }
        lastPlayerScores[playerId] = currentScore

        // 3. Length Validation
        val lastLength = lastPlayerLengths[playerId]
        if (lastLength != null) {
            val lengthDiff = currentLength - lastLength
            if (lengthDiff > MAX_LENGTH_GROWTH_PER_TICK) {
                logViolation(
                    playerId,
                    "LENGTH_EXPLOIT",
                    severity = lengthDiff / 5.0f,
                    details = "Length grew by $lengthDiff segments in one tick (Limit: $MAX_LENGTH_GROWTH_PER_TICK)"
                )
            }
        }
        lastPlayerLengths[playerId] = currentLength

        // 4. Ability Cooldown Validation
        if (isUsingAbility) {
            val lastAbilityTime = lastAbilityTimestamps[playerId]
            if (lastAbilityTime != null) {
                val durationSinceLast = now - lastAbilityTime
                if (durationSinceLast < MIN_ABILITY_COOLDOWN_MS) {
                    logViolation(
                        playerId,
                        "COOLDOWN_BYPASS",
                        severity = (MIN_ABILITY_COOLDOWN_MS - durationSinceLast) / 500.0f,
                        details = "Ability triggered in $durationSinceLast ms (Minimum cooldown is $MIN_ABILITY_COOLDOWN_MS ms)"
                    )
                }
            }
            lastAbilityTimestamps[playerId] = now
        }

        // Determine Action based on cumulative violations
        return evaluateViolations(playerId, violations)
    }

    private fun logViolation(playerId: String, type: String, severity: Float, details: String) {
        val violations = playerViolations.getOrPut(playerId) { mutableListOf() }
        val violation = CheatViolation(playerId, type, severity, details = details)
        violations.add(violation)
        Log.w("AntiCheatEngine", "[VIOLATION] Player '$playerId' flagged for '$type'. Severity: $severity. Details: $details")
    }

    private fun evaluateViolations(playerId: String, violations: List<CheatViolation>): AntiCheatAction {
        val totalSeverity = violations.sumOf { it.severity.toDouble() }.toFloat()
        val count = violations.size

        return when {
            totalSeverity >= 12.0f || count >= 8 -> {
                Log.e("AntiCheatEngine", "[SECURITY BAN] Player '$playerId' banned permanently due to severe violations.")
                AntiCheatAction.BAN
            }
            totalSeverity >= 6.0f || count >= 4 -> {
                Log.e("AntiCheatEngine", "[SECURITY SUSPENSION] Player '$playerId' temporarily suspended.")
                AntiCheatAction.SUSPEND
            }
            totalSeverity >= 3.0f || count >= 2 -> {
                Log.w("AntiCheatEngine", "[SECURITY FLAG] Player '$playerId' flagged in server database.")
                AntiCheatAction.FLAG
            }
            totalSeverity >= 1.0f -> {
                Log.w("AntiCheatEngine", "[SECURITY WARNING] Warning issued to player '$playerId'.")
                AntiCheatAction.WARNING
            }
            else -> AntiCheatAction.NONE
        }
    }

    fun clearPlayerSession(playerId: String) {
        playerViolations.remove(playerId)
        lastPlayerPositions.remove(playerId)
        lastPlayerScores.remove(playerId)
        lastPlayerLengths.remove(playerId)
        lastAbilityTimestamps.remove(playerId)
    }

    fun getViolations(playerId: String): List<CheatViolation> {
        return playerViolations[playerId] ?: emptyList()
    }
}
