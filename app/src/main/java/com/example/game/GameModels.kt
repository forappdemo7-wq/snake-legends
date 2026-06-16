package com.example.game

import androidx.compose.ui.graphics.Color
import kotlin.math.sqrt

data class Vector2D(val x: Float, val y: Float) {
    operator fun plus(other: Vector2D) = Vector2D(this.x + other.x, this.y + other.y)
    operator fun minus(other: Vector2D) = Vector2D(this.x - other.x, this.y - other.y)
    operator fun times(factor: Float) = Vector2D(this.x * factor, this.y * factor)
    
    fun length(): Float = sqrt(x * x + y * y)
    
    fun distance(other: Vector2D): Float {
        val dx = this.x - other.x
        val dy = this.y - other.y
        return sqrt(dx * dx + dy * dy)
    }

    fun normalized(): Vector2D {
        val len = length()
        return if (len == 0f) Vector2D(0f, 0f) else Vector2D(x / len, y / len)
    }
}

data class SnakeNode(val position: Vector2D, val width: Float)

enum class PowerUpType {
    MAGNET,
    DOUBLE_POINTS,
    SHIELD,
    GROWTH,
    GHOST,
    SPEED_BOOST
}

data class PowerUp(
    val id: String,
    val position: Vector2D,
    val type: PowerUpType,
    val color: Color,
    val size: Float = 14f,
    var durationFrames: Int = 450
)

data class Snake(
    val id: String,
    val name: String,
    val isPlayer: Boolean = false,
    var isAlive: Boolean = true,
    var position: Vector2D,
    var angle: Float = 0f, // radians
    var speed: Float = 4.0f,
    val body: MutableList<Vector2D> = mutableListOf(),
    var length: Int = 4,
    var score: Int = 0,
    var skinName: String = "Neon Cyber",
    var primaryColor: Color = Color(0xFF00FFCC),
    var secondaryColor: Color = Color(0xFF0099FF),
    var isBoosting: Boolean = false,
    var botTargetTimer: Int = 0, // for bot ai re-targeting
    var botTarget: Vector2D? = null,
    var specialAbilityActive: Boolean = false,
    var shieldDuration: Int = 0,
    var activePowerUpType: PowerUpType? = null,
    var powerUpTimer: Int = 0,
    // Tactical Ability Mechanics
    var activeAbilityType: String = "SHIELD", // "SHIELD", "FREEZE_PULSE", "EMP_BLAST", "SPEED_BURST", "GHOST_PHASE"
    var abilityCooldownRemaining: Int = 0,
    var abilityActiveDuration: Int = 0,
    // Debuff states
    var isFrozen: Boolean = false,
    var freezeTimer: Int = 0,
    var isEmped: Boolean = false,
    var empTimer: Int = 0
)

data class Orb(
    val id: String,
    var position: Vector2D,
    val size: Float,
    val color: Color,
    val points: Int,
    val isSuperOrb: Boolean = false,
    val isCelestialOrb: Boolean = false
)

data class Particle(
    var position: Vector2D,
    val velocity: Vector2D,
    val color: Color,
    var alpha: Float = 1.0f,
    val fadeSpeed: Float = 0.04f,
    val size: Float = 8f
)

data class FloatingText(
    var position: Vector2D,
    val text: String,
    val color: Color,
    var alpha: Float = 1.0f,
    val speedY: Float = -2f,
    var life: Int = 40
)

data class Joystick(
    var isDragging: Boolean = false,
    var basePosition: Vector2D = Vector2D(0f, 0f),
    var dragPosition: Vector2D = Vector2D(0f, 0f),
    var dragOffset: Vector2D = Vector2D(0f, 0f)
)

enum class ArenaTheme(val displayName: String) {
    CYBER_CITY("Cyber City"),
    LAVA_WORLD("Lava World"),
    FROZEN_ARENA("Frozen Arena"),
    JUNGLE_TEMPLE("Jungle Temple"),
    SPACE_STATION("Space Station"),
    NEON_GRID("Neon Grid")
}

data class Hazard(
    val id: String,
    val type: String, // "lava_pit", "ice_spike", "totem", "quantum_vortex", "electro_gate"
    val position: Vector2D,
    val size: Float,
    var state: Int = 0 // for animation or active ticking
)

data class KillEvent(
    val id: String,
    val killerName: String?,
    val victimName: String,
    val weaponOrCause: String,
    val timestamp: Long = System.currentTimeMillis()
)

