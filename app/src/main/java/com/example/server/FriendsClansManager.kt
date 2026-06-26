package com.example.server

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

class FriendsClansManager {
    private val profiles = ConcurrentHashMap<String, PlayerProfile>()
    private val clans = ConcurrentHashMap<String, ClanData>()
    private val friendLists = ConcurrentHashMap<String, MutableList<String>>() // playerId -> friendIds
    private val onlineStatus = ConcurrentHashMap<String, Boolean>() // playerId -> isOnline
    private val onlineActivity = ConcurrentHashMap<String, String>() // playerId -> Activity

    init {
        // Seed default players and clans for the global experience
        seedInitialSocialData()
    }

    private fun seedInitialSocialData() {
        val botProfiles = listOf(
            PlayerProfile("mp_GigaSlither_99", "GigaSlither_99", xp = 45000, mmr = 3850, rankTier = RankTier.LEGEND),
            PlayerProfile("mp_CosmicViper_Pro", "CosmicViper_Pro", xp = 32000, mmr = 2900, rankTier = RankTier.MASTER),
            PlayerProfile("mp_SlitherLord", "SlitherLord", xp = 28000, mmr = 2550, rankTier = RankTier.DIAMOND),
            PlayerProfile("mp_RetroPython", "RetroPython", xp = 15000, mmr = 1850, rankTier = RankTier.GOLD),
            PlayerProfile("mp_NeonConstrictor", "NeonConstrictor", xp = 8500, mmr = 1400, rankTier = RankTier.SILVER)
        )

        botProfiles.forEach { profile ->
            profiles[profile.id] = profile
            onlineStatus[profile.id] = true
            onlineActivity[profile.id] = "In Lobby"
            friendLists[profile.id] = mutableListOf()
        }

        // Create initial Clan
        val clanId = UUID.randomUUID().toString()
        val defaultClan = ClanData(
            id = clanId,
            name = "NEON REBEL LEAGUE",
            description = "Top tier neon snake enthusiasts",
            creatorId = "mp_GigaSlither_99",
            rankingScore = 15000
        )
        clans[clanId] = defaultClan
        botProfiles.forEach {
            it.clanId = clanId
            defaultClan.members.add(it.id)
        }
    }

    fun registerOrGetProfile(playerId: String, username: String): PlayerProfile {
        return profiles.getOrPut(playerId) {
            PlayerProfile(id = playerId, username = username)
        }
    }

    fun updateProfileStats(playerId: String, xpGained: Long, score: Int, isWin: Boolean) {
        profiles[playerId]?.let { profile ->
            profile.xp += xpGained
            profile.totalMatches += 1
            if (isWin) {
                profile.wins += 1
                profile.mmr += 35
            } else {
                profile.mmr = maxOf(0, profile.mmr - 15)
            }
            profile.rankTier = RankTier.fromMmr(profile.mmr)
            profile.coins += (score / 10).coerceAtLeast(5)

            // Update associated Clan score
            profile.clanId?.let { cId ->
                clans[cId]?.let { clan ->
                    clan.totalPoints += (xpGained / 10).toInt()
                }
            }
        }
    }

    // ---------- Social Operations ----------

    fun setPlayerOnline(playerId: String, isOnline: Boolean, activity: String = "Idle") {
        onlineStatus[playerId] = isOnline
        onlineActivity[playerId] = activity
    }

    fun getFriendsList(playerId: String): List<FriendData> {
        val friends = friendLists[playerId] ?: return emptyList()
        return friends.map { fId ->
            val uName = profiles[fId]?.username ?: "Unknown Snake"
            FriendData(
                id = fId,
                username = uName,
                isOnline = onlineStatus[fId] ?: false,
                currentActivity = onlineActivity[fId] ?: "Offline"
            )
        }
    }

    fun sendFriendRequest(senderId: String, receiverId: String): Boolean {
        if (!profiles.containsKey(receiverId) || senderId == receiverId) return false
        val sFriends = friendLists.getOrPut(senderId) { mutableListOf() }
        val rFriends = friendLists.getOrPut(receiverId) { mutableListOf() }
        if (!sFriends.contains(receiverId)) {
            sFriends.add(receiverId)
        }
        if (!rFriends.contains(senderId)) {
            rFriends.add(senderId)
        }
        return true
    }

    // ---------- Clan Operations ----------

    fun createClan(playerId: String, clanName: String, desc: String): ClanData? {
        val profile = profiles[playerId] ?: return null
        if (profile.clanId != null) return null // Already in a clan
        
        val id = UUID.randomUUID().toString()
        val clan = ClanData(
            id = id,
            name = clanName,
            description = desc,
            creatorId = playerId
        )
        clan.members.add(playerId)
        clans[id] = clan
        profile.clanId = id
        return clan
    }

    fun joinClan(playerId: String, clanId: String): Boolean {
        val profile = profiles[playerId] ?: return false
        val clan = clans[clanId] ?: return false
        if (profile.clanId != null) return false // Leave old first
        
        clan.members.add(playerId)
        profile.clanId = clanId
        return true
    }

    fun leaveClan(playerId: String): Boolean {
        val profile = profiles[playerId] ?: return false
        val clanId = profile.clanId ?: return false
        val clan = clans[clanId] ?: return false

        clan.members.remove(playerId)
        profile.clanId = null
        if (clan.members.isEmpty() || clan.creatorId == playerId) {
            clans.remove(clanId)
        }
        return true
    }

    fun getClansLeaderboard(): List<ClanData> {
        return clans.values.sortedByDescending { it.totalPoints }
    }

    fun simulateClanWar(): String {
        // Runs a simulation between seeded clans
        val leagues = clans.values.toList()
        if (leagues.size < 2) {
            return "No opponent clans found. Need at least 2 clans to launch a Clan War."
        }
        val clanA = leagues[0]
        val clanB = leagues[1]
        val scoreA = Random.nextInt(1200, 4500)
        val scoreB = Random.nextInt(1200, 4500)
        
        val winner = if (scoreA > scoreB) clanA else clanB
        winner.totalPoints += 500
        
        return "CLAN WAR COMPLETED: ${clanA.name} ($scoreA) vs ${clanB.name} ($scoreB). WINNER: ${winner.name} (+500 Clan Points!)"
    }
}
