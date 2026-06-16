package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profile")
data class UserProfile(
    @PrimaryKey val id: Int = 1,
    val username: String = "SnakeLover",
    val level: Int = 1,
    val xp: Int = 0,
    val coins: Int = 500,
    val currentSkin: String = "Neon Cyber",
    val currentTrail: String = "Neon Sparkle",
    val highestScore: Int = 0,
    val rankedScore: Int = 1000, // starting ELO
    val matchesPlayed: Int = 0,
    val clanName: String? = null
)

@Entity(tableName = "unlocked_cosmetic")
data class UnlockedCosmetic(
    @PrimaryKey val name: String,
    val type: String // "skin" or "trail"
)

@Entity(tableName = "match_record")
data class MatchRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val mode: String, // Casual, Ranked, Battle Royale
    val score: Int,
    val placement: Int,
    val xpEarned: Int,
    val coinsEarned: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "clan")
data class Clan(
    @PrimaryKey val name: String,
    val tag: String,
    val totalScore: Int,
    val memberCount: Int
)

@Entity(tableName = "achievement")
data class Achievement(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val currentValue: Int,
    val targetValue: Int,
    val completed: Boolean = false,
    val rewardCoins: Int
)
