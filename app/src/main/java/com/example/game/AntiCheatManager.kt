package com.example.game

import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AntiCheatManager {
    // Audit counters
    var teleportViolationsDetected = 0
    var speedViolationsDetected = 0
    var scoreViolationsDetected = 0
    
    // Integrity heuristic rating (100% drops with detections)
    var securityScore = 100
    
    // Logs for presentation in dev tab or security portal
    val integrityLogs = mutableListOf<String>()
    
    // Security engine status
    var isSentinelOnline = true
    var integrityChecksProcessed = 0L

    // Internal trackers to compare single-tick transitions
    private var lastCheckedPosition: Vector2D? = null
    private var lastCheckedScore: Int = 0
    private val timeFormatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    init {
        logSystemStatus("SENTINEL COGNITIVE SECURITY SYSTEM REDIRECTED LEVEL ONLINE")
        logSystemStatus("INTEGRITY CHECKS COGNITIVE SIGNATURE: ENABLED")
    }

    private fun logSystemStatus(msg: String) {
        val dtStr = timeFormatter.format(Date())
        integrityLogs.add("[$dtStr] SEC_SRV: $msg")
    }

    fun addLog(msg: String) {
        val dtStr = timeFormatter.format(Date())
        integrityLogs.add("[$dtStr] ALERT: $msg")
        if (integrityLogs.size > 80) {
            integrityLogs.removeAt(0)
        }
    }

    fun resetTracker() {
        lastCheckedPosition = null
        lastCheckedScore = 0
        integrityChecksProcessed = 0L
    }

    /**
     * Calibrates, audits and validates player characteristics in real-time each game loop tick.
     * Prevents speed multiplier overrides, coordinate teleport injections, and core point score injection fraud.
     */
    fun performValidation(
        player: Snake,
        floatingTexts: MutableList<FloatingText>,
        onViolationOccurred: () -> Unit
    ) {
        if (!isSentinelOnline) return
        if (!player.isAlive) {
            lastCheckedPosition = null
            lastCheckedScore = 0
            return
        }

        integrityChecksProcessed++

        // 1. Coordinate Teleport Hack Check (Sudden large leaps without ghost phase abilities)
        lastCheckedPosition?.let { lastPos ->
            val distanceMoved = player.position.distance(lastPos)
            
            // Speed bursts or weather shifts are permitted some delta, but any leap above 40px in a 16ms tick
            // is physically impossible without teleportation fraud.
            val isGhostModeActive = player.activePowerUpType == PowerUpType.GHOST || player.activeAbilityType == "GHOST_PHASE"
            val teleportLimitThreshold = if (isGhostModeActive) 55f else 40f
            
            if (distanceMoved > teleportLimitThreshold) {
                teleportViolationsDetected++
                securityScore = (securityScore - 12).coerceAtLeast(10)
                addLog("Coordinate Teleport Intercepted! Leap distance: ${"%.2f".format(distanceMoved)}px. Restoring coordinates.")
                
                // Active mitigation: Hard reset player location to last known valid tick coordinates
                player.position = lastPos
                
                // Visual feedback to illustrate anti-cheat in action
                floatingTexts.add(
                    FloatingText(
                        position = player.position,
                        text = "SENTINEL SECURE: TELEPORT COMPENSATED!",
                        color = Color(0xFFEF4444)
                    )
                )
                onViolationOccurred()
            }
        }

        // 2. Velocity Limit Hack Check (Confirming speed states don't bypass engine ceilings)
        val maxFeasibleSpeed = when {
            player.activeAbilityType == "SPEED_BURST" && player.abilityActiveDuration > 0 -> 14.5f
            player.activePowerUpType == PowerUpType.SPEED_BOOST -> 12.5f
            player.isBoosting -> 8.5f
            else -> 5.0f
        }

        // Apply dynamic margin for tick differences, say 1.25 multiplier
        val speedSanityCeiling = maxFeasibleSpeed * 1.25f

        if (player.speed > speedSanityCeiling) {
            speedViolationsDetected++
            securityScore = (securityScore - 8).coerceAtLeast(10)
            addLog("Velocity Speed Limit Exceeded! Captured speed: ${"%.2f".format(player.speed)}f (Max permitted: ${"%.2f".format(speedSanityCeiling)}f). Dampening velocity.")
            
            // Active mitigation: Force clamp player speed immediately
            val clampSpeed = maxFeasibleSpeed
            player.speed = clampSpeed
            
            floatingTexts.add(
                FloatingText(
                    position = player.position,
                    text = "SENTINEL SECURE: SPEED OVERRIDE DAMPENED",
                    color = Color(0xFFFF9900)
                )
            )
            onViolationOccurred()
        }

        // 3. Score Fraud Injection Check (Sudden large point increments without eating orbs or kills)
        if (lastCheckedScore > 0) {
            val scoreDifference = player.score - lastCheckedScore
            
            // Maximum scoring action is Growth power-up potion which increases score by 75. 
            // Normal premium orbs give 15 or base score increments of up to 40.
            // Any single tick leap above 85 is mathematically fraudulent.
            if (scoreDifference > 85) {
                scoreViolationsDetected++
                securityScore = (securityScore - 15).coerceAtLeast(10)
                addLog("Fraudulent Accumulator Injection Intercepted! Tick variance: +$scoreDifference points. Reverting core integrity stats.")
                
                // Active mitigation: Roll back score and rebuild proper segments to prevent visual glitches
                player.score = lastCheckedScore
                player.length = 4 + (player.score / 25)
                
                floatingTexts.add(
                    FloatingText(
                        position = player.position,
                        text = "SENTINEL SECURE: ILLEGAL SCORE RESTORED",
                        color = Color(0xFFEF4444)
                    )
                )
                onViolationOccurred()
            }
        }

        // Record tracking variables for the current valid tick
        lastCheckedPosition = player.position
        lastCheckedScore = player.score
    }
}
