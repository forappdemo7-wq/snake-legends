package com.example.server

import android.util.Log
import com.example.game.ServerRegion
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

class MatchManager(
    private val friendsClansManager: FriendsClansManager,
    private val rankingManager: RankingManager
) {
    private val matchmakingQueues = ConcurrentHashMap<String, CopyOnWriteArrayList<MatchmakingTicket>>() // requestType -> Queue
    private val rooms = ConcurrentHashMap<String, AuthoritativeRoom>() // roomCode -> Room

    data class AuthoritativeRoom(
        val roomCode: String,
        val creatorId: String,
        val requestType: String,
        val region: ServerRegion,
        val playerIds: MutableList<String> = mutableListOf(),
        val minMmr: Int = 0,
        val maxMmr: Int = 10000
    )

    fun queuePlayer(
        playerId: String,
        username: String,
        requestType: String, // "QUICK", "RANKED", "PRIVATE", "CLAN"
        region: String
    ): MatchmakingTicket {
        val profile = friendsClansManager.registerOrGetProfile(playerId, username)
        val ticket = MatchmakingTicket(playerId, profile, requestType, region)
        
        val queue = matchmakingQueues.getOrPut(requestType) { CopyOnWriteArrayList() }
        
        // Remove existing tickets first
        dequeuePlayer(playerId)
        
        queue.add(ticket)
        Log.i("MatchManager", "Queued player '$username' (MMR: ${profile.mmr}) in $requestType queue (Region: $region)")
        
        processMatchmaking(requestType)
        
        return ticket
    }

    fun dequeuePlayer(playerId: String) {
        matchmakingQueues.values.forEach { queue ->
            queue.removeAll { it.playerId == playerId }
        }
    }

    private fun processMatchmaking(requestType: String) {
        val queue = matchmakingQueues[requestType] ?: return
        if (queue.size < 2) return // Need at least 2 players to form a matched room

        // Matchmaking algorithm matching players based on similar MMR (skill level) and ping
        synchronized(queue) {
            val sortedByMmr = queue.sortedBy { it.profile.mmr }
            val matchedTickets = mutableListOf<MatchmakingTicket>()

            for (i in 0 until sortedByMmr.size - 1) {
                val p1 = sortedByMmr[i]
                val p2 = sortedByMmr[i + 1]

                val mmrDifference = kotlin.math.abs(p1.profile.mmr - p2.profile.mmr)
                // MMR match threshold expands with wait time
                val waitTimeSeconds = (System.currentTimeMillis() - p1.entryTime) / 1000f
                val maxMmrDiffAllowed = 150 + (waitTimeSeconds * 12f).toInt()

                if (mmrDifference <= maxMmrDiffAllowed && p1.region == p2.region) {
                    matchedTickets.add(p1)
                    matchedTickets.add(p2)
                    break
                }
            }

            if (matchedTickets.size >= 2) {
                // Form Match Room
                val roomCode = (100000 + (Math.random() * 900000).toInt()).toString()
                val matchedRegion = ServerRegion.values().find { it.name == matchedTickets.first().region } ?: ServerRegion.NORTH_AMERICA
                
                val room = AuthoritativeRoom(
                    roomCode = roomCode,
                    creatorId = matchedTickets.first().playerId,
                    requestType = requestType,
                    region = matchedRegion
                )
                
                matchedTickets.forEach { ticket ->
                    room.playerIds.add(ticket.playerId)
                    queue.remove(ticket)
                }

                rooms[roomCode] = room
                Log.i("MatchManager", "MATCHMAKING SUCCESS: Created authoritative Match Room '$roomCode' for players: ${room.playerIds.joinToString(", ")}")
            }
        }
    }

    fun createPrivateRoom(playerId: String, username: String, region: ServerRegion): String {
        val roomCode = (100000 + (Math.random() * 900000).toInt()).toString()
        val room = AuthoritativeRoom(
            roomCode = roomCode,
            creatorId = playerId,
            requestType = "PRIVATE",
            region = region
        )
        room.playerIds.add(playerId)
        rooms[roomCode] = room
        friendsClansManager.registerOrGetProfile(playerId, username)
        Log.i("MatchManager", "Created private Match Room '$roomCode' by creator: $username")
        return roomCode
    }

    fun joinPrivateRoom(roomCode: String, playerId: String, username: String): Boolean {
        val room = rooms[roomCode] ?: return false
        if (room.requestType != "PRIVATE") return false
        if (room.playerIds.size >= 10) return false // Max slot limit
        
        if (!room.playerIds.contains(playerId)) {
            room.playerIds.add(playerId)
        }
        friendsClansManager.registerOrGetProfile(playerId, username)
        Log.i("MatchManager", "Player '$username' joined custom private Room '$roomCode'")
        return true
    }

    fun leaveRoom(roomCode: String, playerId: String) {
        val room = rooms[roomCode] ?: return
        room.playerIds.remove(playerId)
        if (room.playerIds.isEmpty()) {
            rooms.remove(roomCode)
            Log.i("MatchManager", "Destroyed empty room '$roomCode'")
        }
    }

    fun getRoom(roomCode: String): AuthoritativeRoom? = rooms[roomCode]
}
