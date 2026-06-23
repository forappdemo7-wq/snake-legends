package com.example

import androidx.compose.ui.graphics.Color
import com.example.game.*
import org.junit.Assert.*
import org.junit.Test

class AntiCheatTest {

    @Test
    fun testTeleportationHackDetection() {
        val antiCheat = AntiCheatManager()
        val player = Snake(
            id = "test_player",
            name = "Test Slitherer",
            isPlayer = true,
            position = Vector2D(100f, 100f),
            speed = 4f,
            score = 10,
            length = 4
        )
        val floatingTexts = mutableListOf<FloatingText>()

        // First tick: Set anchor state
        var violationTriggered = false
        antiCheat.performValidation(player, floatingTexts) {
            violationTriggered = true
        }
        
        assertEquals(0, antiCheat.teleportViolationsDetected)
        assertFalse(violationTriggered)

        // Simulate suddenly teleporting 500 pixels away
        player.position = Vector2D(600f, 600f)

        // Second tick: Validate check
        antiCheat.performValidation(player, floatingTexts) {
            violationTriggered = true
        }

        // Sentinel should catch it, reset coordinates to 100,100, increment violation, and notify
        assertTrue(violationTriggered)
        assertEquals(1, antiCheat.teleportViolationsDetected)
        assertEquals(Vector2D(100f, 100f), player.position)
        assertTrue(floatingTexts.any { it.text.contains("TELEPORT COMPENSATED!") })
    }

    @Test
    fun testSpeedOverrideHackDetection() {
        val antiCheat = AntiCheatManager()
        val player = Snake(
            id = "test_player",
            name = "Test Slitherer",
            isPlayer = true,
            position = Vector2D(100f, 100f),
            speed = 4f,
            score = 10,
            length = 4
        )
        val floatingTexts = mutableListOf<FloatingText>()

        // First tick: Valid speed within boundaries
        antiCheat.performValidation(player, floatingTexts) {}
        assertEquals(0, antiCheat.speedViolationsDetected)

        // Inject high speedhack
        player.speed = 35f

        // Second tick: Sentinel validation
        var speedViolationTriggered = false
        antiCheat.performValidation(player, floatingTexts) {
            speedViolationTriggered = true
        }

        // Sentinel should capture the speedhack and clamp the velocity back to standard
        assertTrue(speedViolationTriggered)
        assertEquals(1, antiCheat.speedViolationsDetected)
        assertTrue(player.speed <= 8.5f) // clamped because player is not in speedburst or active booster
    }

    @Test
    fun testScoreTamperingDetection() {
        val antiCheat = AntiCheatManager()
        val player = Snake(
            id = "test_player",
            name = "Test Slitherer",
            isPlayer = true,
            position = Vector2D(100f, 100f),
            speed = 4f,
            score = 50,
            length = 6
        )
        val floatingTexts = mutableListOf<FloatingText>()

        // Establish last validated baseline
        antiCheat.performValidation(player, floatingTexts) {}
        assertEquals(50, player.score)

        // Teleport/Inject a fraudulent score jump of +400 points
        player.score = 450

        // Sentinel ticks and validates
        var scoreViolationTriggered = false
        antiCheat.performValidation(player, floatingTexts) {
            scoreViolationTriggered = true
        }

        // The injection should be intercepted and rolled back to 50
        assertTrue(scoreViolationTriggered)
        assertEquals(1, antiCheat.scoreViolationsDetected)
        assertEquals(50, player.score)
        assertEquals(6, player.length)
        assertTrue(floatingTexts.any { it.text.contains("ILLEGAL SCORE RESTORED") })
    }
}
