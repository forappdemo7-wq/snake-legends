package com.example.server

import java.util.concurrent.ConcurrentHashMap

class RankingManager {
    
    // InMemory store of leaderboards
    private val globalLeaderboard = ConcurrentHashMap<String, Int>() // playerId -> MMR
    private val seasonStats = ConcurrentHashMap<String, MatchStats>()
    
    data class MatchStats(
        val matchesPlayed: Int,
        val kills: Int,
        val top1Placements: Int,
        val avgSurvivalSeconds: Float
    )

    init {
        // Seed initial ranks for leaderboard presentation
        globalLeaderboard["mp_GigaSlither_99"] = 3850
        globalLeaderboard["mp_CosmicViper_Pro"] = 2900
        globalLeaderboard["mp_SlitherLord"] = 2550
        globalLeaderboard["mp_RetroPython"] = 1850
        globalLeaderboard["mp_NeonConstrictor"] = 1400
    }

    fun getPlayerRankTier(mmr: Int): RankTier {
        return RankTier.fromMmr(mmr)
    }

    /**
     * Calculates Elo/MMR rating adjustments after a multiplayer battle completes.
     * Higher placement increases rating; lower placement drops rating.
     */
    fun updateCompetitiveRating(
        playerId: String,
        placement: Int,
        totalPlayers: Int,
        kills: Int,
        score: Int
    ): Int {
        val currentMmr = globalLeaderboard[playerId] ?: 1000
        
        // Base change depends on placement position
        val midPoint = totalPlayers / 2.0f
        val placementFactor = (midPoint - placement) / totalPlayers // positive for better placements, negative for poor
        
        val kFactor = 32
        val placementGain = (placementFactor * kFactor * 2.0f).toInt()
        val killGain = (kills * 4).coerceAtMost(25)
        val scoreGain = (score / 150).coerceAtMost(15)

        val totalChange = placementGain + killGain + scoreGain
        val nextMmr = (currentMmr + totalChange).coerceAtLeast(0)
        
        globalLeaderboard[playerId] = nextMmr

        // Update Season Stats
        val stats = seasonStats.getOrPut(playerId) { MatchStats(0, 0, 0, 0f) }
        val updatedStats = MatchStats(
            matchesPlayed = stats.matchesPlayed + 1,
            kills = stats.kills + kills,
            top1Placements = stats.top1Placements + (if (placement == 1) 1 else 0),
            avgSurvivalSeconds = (stats.avgSurvivalSeconds * stats.matchesPlayed + 90f) / (stats.matchesPlayed + 1)
        )
        seasonStats[playerId] = updatedStats

        return totalChange
    }

    fun getLeaderboard(friendsClansManager: FriendsClansManager): List<LeaderboardEntry> {
        return globalLeaderboard.entries
            .map { entry ->
                val profile = friendsClansManager.registerOrGetProfile(entry.key, entry.key.removePrefix("mp_"))
                LeaderboardEntry(
                    playerId = entry.key,
                    username = profile.username,
                    mmr = entry.value,
                    tier = RankTier.fromMmr(entry.value).displayName,
                    clanTag = if (profile.clanId != null) "NEON" else null
                )
            }
            .sortedByDescending { it.mmr }
    }

    fun resetSeason(): String {
        // Soft reset MMR towards the baseline of 1000 MMR (compression to prevent runaway inflation)
        for ((pId, mmr) in globalLeaderboard) {
            val delta = mmr - 1000
            val softResetMmr = 1000 + (delta * 0.5f).toInt() // Pull 50% closer to 1000
            globalLeaderboard[pId] = softResetMmr
        }
        seasonStats.clear()
        return "SEASON COMPLETED: All MMR ratings compressed by 50% toward 1000. Season statistics reset successfully!"
    }
}

data class LeaderboardEntry(
    val playerId: String,
    val username: String,
    val mmr: Int,
    val tier: String,
    val clanTag: String?
)
