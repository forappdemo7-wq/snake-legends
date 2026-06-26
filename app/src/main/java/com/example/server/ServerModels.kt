package com.example.server

import androidx.compose.ui.graphics.Color
import com.example.game.PowerUpType
import com.example.game.Vector2D

// ---------- Network Packets ----------

data class ClientInputPacket(
    val tickNumber: Long,
    val playerId: String,
    val joystickAngle: Float?,
    val isBoosting: Boolean,
    val triggerAbility: Boolean,
    val timestamp: Long,
    val signature: String // Cryptographic packet signing
)

data class SnakeState(
    val id: String,
    val name: String,
    val position: Vector2D,
    val angle: Float,
    val speed: Float,
    val length: Int,
    val score: Int,
    val isBoosting: Boolean,
    val body: List<Vector2D>,
    val isAlive: Boolean,
    val activePowerUpType: PowerUpType?,
    val powerUpTimer: Int,
    val activeAbilityType: String,
    val abilityCooldownRemaining: Int,
    val abilityActiveDuration: Int,
    val isFrozen: Boolean,
    val isEmped: Boolean,
    val primaryColorHex: String,
    val secondaryColorHex: String
)

data class OrbState(
    val id: String,
    val position: Vector2D,
    val points: Int,
    val isSuper: Boolean,
    val isCelestial: Boolean,
    val colorHex: String
)

data class PowerUpState(
    val id: String,
    val position: Vector2D,
    val type: PowerUpType,
    val colorHex: String
)

data class ServerStateSnapshot(
    val tick: Long,
    val timestamp: Long,
    val snakes: List<SnakeState>,
    val orbs: List<OrbState>,
    val powerUps: List<PowerUpState>,
    val activeEvent: String,
    val safeZoneRadius: Float,
    val serverSignature: String
)

// ---------- Delta Compression state ----------

data class DeltaSnapshot(
    val baseTick: Long,
    val currentTick: Long,
    val updatedSnakes: List<SnakeState>,
    val removedSnakeIds: List<String>,
    val updatedOrbs: List<OrbState>,
    val removedOrbIds: List<String>,
    val updatedPowerUps: List<PowerUpState>,
    val removedPowerUpIds: List<String>
)

// ---------- Social and Matchmaking Models ----------

data class PlayerProfile(
    val id: String,
    val username: String,
    var xp: Long = 0,
    var mmr: Int = 1000,
    var rankTier: RankTier = RankTier.BRONZE,
    var wins: Int = 0,
    var totalMatches: Int = 0,
    var coins: Int = 0,
    var inventory: List<String> = listOf("Neon Cyber"),
    var clanId: String? = null
)

enum class RankTier(val displayName: String, val threshold: Int) {
    BRONZE("Bronze", 0),
    SILVER("Silver", 1200),
    GOLD("Gold", 1600),
    PLATINUM("Platinum", 2000),
    DIAMOND("Diamond", 2400),
    MASTER("Master", 2800),
    GRANDMASTER("Grandmaster", 3200),
    LEGEND("Legend", 3600);

    companion object {
        fun fromMmr(mmr: Int): RankTier {
            return values().findLast { mmr >= it.threshold } ?: BRONZE
        }
    }
}

data class MatchmakingTicket(
    val playerId: String,
    val profile: PlayerProfile,
    val requestType: String, // "QUICK", "RANKED", "PRIVATE", "CLAN"
    val region: String,
    val entryTime: Long = System.currentTimeMillis()
)

data class ClanData(
    val id: String,
    val name: String,
    var description: String,
    var totalPoints: Int = 0,
    val creatorId: String,
    val members: MutableList<String> = mutableListOf(),
    val rankingScore: Int = 0
)

data class FriendData(
    val id: String,
    val username: String,
    val isOnline: Boolean,
    val currentActivity: String = "Idle"
)

// ---------- Replay Models ----------

data class ReplayFrame(
    val tick: Long,
    val inputs: List<ClientInputPacket>,
    val events: List<String> // "kill:p1:p2", "orb_spawn:id", etc.
)

data class MatchReplay(
    val matchId: String,
    val startTimestamp: Long,
    val durationTicks: Long,
    val frames: List<ReplayFrame>,
    val finalScores: Map<String, Int>
)
