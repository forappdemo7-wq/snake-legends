package com.example.data

import kotlinx.coroutines.flow.Flow

class GameRepository(private val gameDao: GameDao) {
    val userProfile: Flow<UserProfile?> = gameDao.getUserProfile()
    val unlockedCosmetics: Flow<List<UnlockedCosmetic>> = gameDao.getUnlockedCosmetics()
    val matchRecords: Flow<List<MatchRecord>> = gameDao.getMatchRecords()
    val clans: Flow<List<Clan>> = gameDao.getClans()
    val achievements: Flow<List<Achievement>> = gameDao.getAchievements()

    suspend fun updateProfile(profile: UserProfile) {
        gameDao.insertUserProfile(profile)
    }

    suspend fun unlockCosmetic(name: String, type: String, price: Int): Boolean {
        val user = gameDao.getUserProfileSync() ?: return false
        if (user.coins >= price) {
            val updatedUser = user.copy(coins = user.coins - price)
            gameDao.insertUserProfile(updatedUser)
            gameDao.insertUnlockedCosmetic(UnlockedCosmetic(name, type))
            return true
        }
        return false
    }

    suspend fun saveMatchRecord(record: MatchRecord) {
        gameDao.insertMatchRecord(record)
        // Adjust User Stats
        val user = gameDao.getUserProfileSync() ?: return
        
        // Calculate dynamic XP and Coins update
        var newXp = user.xp + record.xpEarned
        var newLevel = user.level
        var xpRequired = getXpForLevel(newLevel)
        while (newXp >= xpRequired) {
            newXp -= xpRequired
            newLevel++
            xpRequired = getXpForLevel(newLevel)
        }

        // Adjust Rank score based on performance
        val rankChange = when {
            record.placement == 1 -> 50
            record.placement <= 3 -> 25
            record.placement > 8 -> -15
            else -> 5
        }
        val newRankedScore = (user.rankedScore + rankChange).coerceAtLeast(100)

        val updatedUser = user.copy(
            level = newLevel,
            xp = newXp,
            coins = user.coins + record.coinsEarned,
            highestScore = maxOf(user.highestScore, record.score),
            rankedScore = newRankedScore,
            matchesPlayed = user.matchesPlayed + 1
        )
        gameDao.insertUserProfile(updatedUser)

        // Achievement progress updates
        updateAchievement("level_5", newLevel)
        if (record.score >= 500) {
            updateAchievement("score_500", record.score)
        }
        if (record.mode == "Battle Royale" && record.placement == 1) {
            updateAchievement("br_win", 1)
        }
    }

    suspend fun updateAchievement(id: String, progress: Int) {
        // Query to check existing status
        val matches = gameDao.getUserProfileSync() ?: return
        // We can look at individual achievements or directly update progress
    }

    suspend fun updateAchievementProgressDirect(id: String, currentValue: Int, completed: Boolean) {
        gameDao.updateAchievementProgress(id, currentValue, completed)
    }

    suspend fun claimAchievementReward(id: String, reward: Int) {
        val user = gameDao.getUserProfileSync() ?: return
        val updatedUser = user.copy(coins = user.coins + reward)
        gameDao.insertUserProfile(updatedUser)
    }

    suspend fun joinClan(clanName: String) {
        val user = gameDao.getUserProfileSync() ?: return
        val updatedUser = user.copy(clanName = clanName)
        gameDao.insertUserProfile(updatedUser)
    }

    suspend fun leaveClan() {
        val user = gameDao.getUserProfileSync() ?: return
        val updatedUser = user.copy(clanName = null)
        gameDao.insertUserProfile(updatedUser)
    }

    suspend fun createClan(name: String, tag: String): Boolean {
        val user = gameDao.getUserProfileSync() ?: return false
        if (user.coins >= 300) {
            val updatedUser = user.copy(coins = user.coins - 300, clanName = name)
            gameDao.insertUserProfile(updatedUser)
            gameDao.insertClan(Clan(name, tag, totalScore = user.rankedScore, memberCount = 1))
            return true
        }
        return false
    }

    private fun getXpForLevel(level: Int): Int {
        return level * 1000 // Simple progression scale
    }
}
