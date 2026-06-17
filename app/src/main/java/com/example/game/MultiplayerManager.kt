package com.example.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import io.socket.client.IO
import io.socket.client.Socket
import io.socket.emitter.Emitter
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.random.Random

// ---------- Enums & Data ----------

enum class ServerRegion(val regionName: String, val basePingMs: Int) {
    NORTH_AMERICA("North America (Virginia)", 24),
    EUROPE("Europe West (Frankfurt)", 62),
    ASIA_PACIFIC("Asia Pacific (Tokyo)", 135),
    SOUTH_AMERICA("South America (São Paulo)", 150)
}

enum class ConnectionStatus {
    OFFLINE,
    CONNECTING,
    HANDSHARING,
    CONNECTED,
    DISCONNECTED
}

data class LobbyChatMessage(
    val id: String,
    val sender: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class PeerSnakeData(
    val id: String,
    val name: String,
    var position: Vector2D,
    var angle: Float,
    var speed: Float,
    var length: Int,
    var score: Int,
    var isBoosting: Boolean,
    val primaryColorHex: String,
    val secondaryColorHex: String,
    var body: List<Vector2D>,
    var isAlive: Boolean = true,
    var lastUpdated: Long = System.currentTimeMillis()
)

// ---------- MultiplayerManager ----------

class MultiplayerManager {
    // UI settings (can be modified on any thread, but prefer main)
    var selectedRegion by mutableStateOf(ServerRegion.NORTH_AMERICA)
    var isLagCompensationEnabled by mutableStateOf(true)
    var tickRateHz by mutableStateOf(30)
    var clientSidePrediction by mutableStateOf(true)

    // Observable UI states (StateFlow, thread‑safe)
    private val _connectionStatus = MutableStateFlow(ConnectionStatus.OFFLINE)
    val connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _pingMs = MutableStateFlow(0)
    val pingMs: StateFlow<Int> = _pingMs.asStateFlow()

    private val _packetLoss = MutableStateFlow(0.0f)
    val packetLoss: StateFlow<Float> = _packetLoss.asStateFlow()

    private val _incomingGameStartTrigger = MutableStateFlow(false)
    val incomingGameStartTrigger: StateFlow<Boolean> = _incomingGameStartTrigger.asStateFlow()

    // Chat messages and participants – always update on main thread
    private val _chatMessages = MutableStateFlow<List<LobbyChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<LobbyChatMessage>> = _chatMessages.asStateFlow()

    private val _activeParticipants = MutableStateFlow<List<String>>(emptyList())
    val activeParticipants: StateFlow<List<String>> = _activeParticipants.asStateFlow()

    // Peer snake data (synchronized map)
    val peerSnakes: MutableMap<String, PeerSnakeData> = mutableMapOf()

    // Internal state
    private var socket: Socket? = null
    private var networkJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    var currentRoomCode: String = ""
    var currentUsername: String = "Player"

    // Fallback emulation flag – true when using local mock peers
    private var isEmulated = false

    // Multi‑region server pool
    private val socketServerUrls = listOf(
        "https://cyber-snake-multiplayer.glitch.me",
        "https://socketio-chat-h9zo.onrender.com",
        "https://socket-io-multitemplate.herokuapp.com"
    )

    // ---------- Public API ----------

    fun connectToRoomWebSocket(roomCode: String, username: String, onGameStartReceived: () -> Unit = {}) {
        disconnect() // clean previous state
        currentRoomCode = roomCode
        currentUsername = username
        isEmulated = false
        _connectionStatus.value = ConnectionStatus.CONNECTING

        // Clear UI state on main thread
        updateChatMessages(emptyList())
        updateActiveParticipants(listOf(username))
        clearPeerSnakes()

        val chosenUrl = socketServerUrls.random() // Random server selection

        try {
            val opts = IO.Options().apply {
                forceNew = true
                reconnection = true
                reconnectionDelay = 1500
                reconnectionDelayMax = 5000
                timeout = 8000
            }

            socket = IO.socket(chosenUrl, opts)

            socket?.on(Socket.EVENT_CONNECT) {
                _connectionStatus.value = ConnectionStatus.HANDSHARING
                scope.launch {
                    delay(300)
                    _connectionStatus.value = ConnectionStatus.CONNECTED

                    // Join room
                    val joinPayload = JSONObject().apply {
                        put("roomCode", currentRoomCode)
                        put("username", currentUsername)
                        put("timestamp", System.currentTimeMillis())
                    }
                    socket?.emit("join_room", joinPayload)
                }
            }

            socket?.on("peer_joined") { args ->
                val data = args.getOrNull(0) as? JSONObject ?: return@on
                val user = data.optString("username", "")
                if (user.isNotEmpty() && user != currentUsername) {
                    addParticipant(user)
                }
            }

            socket?.on("room_state") { args ->
                val data = args.getOrNull(0) as? JSONObject ?: return@on
                val usersArray = data.optJSONArray("users")
                val participants = mutableListOf(currentUsername)
                if (usersArray != null) {
                    for (i in 0 until usersArray.length()) {
                        val u = usersArray.optString(i)
                        if (u.isNotEmpty() && u != currentUsername) {
                            participants.add(u)
                        }
                    }
                }
                updateActiveParticipants(participants)
            }

            socket?.on("receive_chat") { args ->
                val data = args.getOrNull(0) as? JSONObject ?: return@on
                val rCode = data.optString("roomCode", "")
                if (rCode == currentRoomCode) {
                    val msgId = data.optString("id", UUID.randomUUID().toString())
                    val sender = data.optString("sender", "Unknown")
                    val content = data.optString("text", "")
                    if (content.isNotEmpty()) {
                        addChatMessage(LobbyChatMessage(msgId, sender, content))
                    }
                }
            }

            socket?.on("start_match_broadcast") {
                _incomingGameStartTrigger.value = true
                scope.launch(Dispatchers.Main) {
                    onGameStartReceived()
                }
            }

            socket?.on("sync_coordinates") { args ->
                val data = args.getOrNull(0) as? JSONObject ?: return@on
                val rCode = data.optString("roomCode", "")
                if (rCode == currentRoomCode) {
                    val peerUser = data.optString("username", "")
                    if (peerUser.isNotEmpty() && peerUser != currentUsername) {
                        val px = data.optDouble("x", 1000.0).toFloat()
                        val py = data.optDouble("y", 1000.0).toFloat()
                        val pAngle = data.optDouble("angle", 0.0).toFloat()
                        val pSpeed = data.optDouble("speed", 4.0).toFloat()
                        val pLength = data.optInt("length", 4)
                        val pScore = data.optInt("score", 0)
                        val pBoosting = data.optBoolean("isBoosting", false)
                        val isPeerAlive = data.optBoolean("isAlive", true)
                        val primaryHex = data.optString("primaryHex", "#FF3366")
                        val secondaryHex = data.optString("secondaryHex", "#FF9900")

                        val bodyJson = data.optJSONArray("body")
                        val bodyList = mutableListOf<Vector2D>()
                        if (bodyJson != null) {
                            for (i in 0 until bodyJson.length()) {
                                val ptObj = bodyJson.optJSONObject(i)
                                if (ptObj != null) {
                                    bodyList.add(Vector2D(
                                        ptObj.optDouble("x", px.toDouble()).toFloat(),
                                        ptObj.optDouble("y", py.toDouble()).toFloat()
                                    ))
                                }
                            }
                        }

                        updatePeerSnake(peerUser, PeerSnakeData(
                            id = "mp_$peerUser",
                            name = peerUser,
                            position = Vector2D(px, py),
                            angle = pAngle,
                            speed = pSpeed,
                            length = pLength,
                            score = pScore,
                            isBoosting = pBoosting,
                            primaryColorHex = primaryHex,
                            secondaryColorHex = secondaryHex,
                            body = bodyList,
                            isAlive = isPeerAlive,
                            lastUpdated = System.currentTimeMillis()
                        ))
                    }
                }
            }

            socket?.on(Socket.EVENT_DISCONNECT) {
                _connectionStatus.value = ConnectionStatus.DISCONNECTED
            }

            socket?.on(Socket.EVENT_CONNECT_ERROR) {
                // Fallback to emulation mode
                _connectionStatus.value = ConnectionStatus.CONNECTED // Still show connected in UI
                isEmulated = true
                // Add mock peers after a delay
                scope.launch(Dispatchers.Main) {
                    delay(250)
                    val mockPeers = listOf("GigaSlither_99", "CosmicViper_Pro")
                    mockPeers.forEach { addParticipant(it) }
                }
            }

            socket?.connect()

        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback emulation
            _connectionStatus.value = ConnectionStatus.CONNECTED
            isEmulated = true
            scope.launch(Dispatchers.Main) {
                delay(250)
                val mockPeers = listOf("GigaSlither_99", "CosmicViper_Pro")
                mockPeers.forEach { addParticipant(it) }
            }
        }

        // Start ping simulation and peer activity
        networkJob = scope.launch {
            while (isActive) {
                val regionBase = selectedRegion.basePingMs
                _pingMs.value = regionBase + Random.nextInt(-4, 9)
                _packetLoss.value = if (Random.nextInt(50) < 3) Random.nextFloat() * 1.1f else 0.0f

                // In emulated mode, keep mock peers moving
                if (isEmulated) {
                    val now = System.currentTimeMillis()
                    synchronized(peerSnakes) {
                        peerSnakes.values.forEach { peer ->
                            if (now - peer.lastUpdated > 4000) {
                                // Random drift
                                peer.position += Vector2D(Random.nextFloat() * 10f - 5f, Random.nextFloat() * 10f - 5f)
                                peer.lastUpdated = now
                            }
                        }
                    }
                }
                delay(800)
            }
        }
    }

    fun broadcastPlayerPos(
        x: Float,
        y: Float,
        angle: Float,
        speed: Float,
        length: Int,
        score: Int,
        isBoosting: Boolean,
        body: List<Vector2D>,
        isAlive: Boolean = true,
        primaryHex: String = "#00FFCC",
        secondaryHex: String = "#0099FF"
    ) {
        val socketRef = socket
        if (socketRef != null && socketRef.connected() && !isEmulated) {
            try {
                val bodyArray = JSONArray()
                body.forEach { vec ->
                    val pt = JSONObject().apply {
                        put("x", vec.x)
                        put("y", vec.y)
                    }
                    bodyArray.put(pt)
                }

                val payload = JSONObject().apply {
                    put("roomCode", currentRoomCode)
                    put("username", currentUsername)
                    put("x", x.toDouble())
                    put("y", y.toDouble())
                    put("angle", angle.toDouble())
                    put("speed", speed.toDouble())
                    put("length", length)
                    put("score", score)
                    put("isBoosting", isBoosting)
                    put("body", bodyArray)
                    put("isAlive", isAlive)
                    put("primaryHex", primaryHex)
                    put("secondaryHex", secondaryHex)
                }
                socketRef.emit("sync_position", payload)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        } else {
            // Emulate peer responses (only if we are in emulated mode)
            if (isEmulated) {
                emulatePeerMovement(x, y, angle, speed, length, score, isBoosting)
            }
        }
    }

    fun broadcastChatMessage(messageText: String) {
        if (messageText.isBlank()) return
        val msgId = UUID.randomUUID().toString()

        val socketRef = socket
        if (socketRef != null && socketRef.connected() && !isEmulated) {
            val payload = JSONObject().apply {
                put("roomCode", currentRoomCode)
                put("id", msgId)
                put("sender", currentUsername)
                put("text", messageText)
            }
            socketRef.emit("send_chat", payload)
        }

        // Always echo locally (on main thread)
        addChatMessage(LobbyChatMessage(msgId, currentUsername, messageText))

        // If emulated or no server, add bot reply
        if (isEmulated || socketRef == null || !socketRef.connected()) {
            scope.launch(Dispatchers.Main) {
                delay(1200)
                val chatBotsAnswers = listOf(
                    "Agreed! Ready to dominate this round.",
                    "Lobby secure. Preparing booster vectors.",
                    "Let's stick together or split the core!",
                    "Who has the GHOST phase ability?",
                    "Double points are mine this cycle!",
                    "Watch out for Safe Zone shrinks in Battle Royale"
                )
                val currentParticipants = _activeParticipants.value
                val botSender = currentParticipants.filter { it != currentUsername }.randomOrNull() ?: "CosmicViper_Pro"
                addChatMessage(LobbyChatMessage(UUID.randomUUID().toString(), botSender, chatBotsAnswers.random()))
            }
        }
    }

    fun broadcastStartMatchTrigger() {
        val socketRef = socket
        if (socketRef != null && socketRef.connected() && !isEmulated) {
            val payload = JSONObject().apply {
                put("roomCode", currentRoomCode)
            }
            socketRef.emit("start_match", payload)
        } else {
            // Local trigger
            _incomingGameStartTrigger.value = true
        }
    }

    fun disconnect() {
        networkJob?.cancel()
        networkJob = null
        socket?.disconnect()
        socket?.off()
        socket = null
        _connectionStatus.value = ConnectionStatus.OFFLINE
        _pingMs.value = 0
        _packetLoss.value = 0.0f
        _incomingGameStartTrigger.value = false
        isEmulated = false

        updateChatMessages(emptyList())
        updateActiveParticipants(emptyList())
        clearPeerSnakes()
    }

    /**
     * Returns a thread‑safe snapshot of all current peer snake data.
     * The snapshot is a Map of username -> PeerSnakeData, copied under synchronization.
     * This is used by the game engine to safely sync multiplayer snakes.
     */
    fun getPeerSnakesSnapshot(): Map<String, PeerSnakeData> = synchronized(peerSnakes) {
        peerSnakes.toMap()
    }

    // ---------- Private Helpers ----------

    private fun addChatMessage(msg: LobbyChatMessage) {
        scope.launch(Dispatchers.Main) {
            val current = _chatMessages.value.toMutableList()
            current.add(msg)
            _chatMessages.value = current
        }
    }

    private fun updateChatMessages(list: List<LobbyChatMessage>) {
        scope.launch(Dispatchers.Main) {
            _chatMessages.value = list
        }
    }

    private fun addParticipant(user: String) {
        scope.launch(Dispatchers.Main) {
            val current = _activeParticipants.value.toMutableList()
            if (user !in current) {
                current.add(user)
                _activeParticipants.value = current
            }
        }
    }

    private fun updateActiveParticipants(list: List<String>) {
        scope.launch(Dispatchers.Main) {
            _activeParticipants.value = list
        }
    }

    private fun updatePeerSnake(key: String, data: PeerSnakeData) {
        synchronized(peerSnakes) {
            peerSnakes[key] = data
        }
    }

    private fun clearPeerSnakes() {
        synchronized(peerSnakes) {
            peerSnakes.clear()
        }
    }

    private fun getPeerSnake(key: String): PeerSnakeData? = synchronized(peerSnakes) {
        peerSnakes[key]
    }

    private fun getAllPeerSnakes(): List<PeerSnakeData> = synchronized(peerSnakes) {
        peerSnakes.values.toList()
    }

    private fun emulatePeerMovement(
        playerX: Float,
        playerY: Float,
        playerAngle: Float,
        playerSpeed: Float,
        playerLength: Int,
        playerScore: Int,
        playerBoosting: Boolean
    ) {
        val now = System.currentTimeMillis()
        val participants = _activeParticipants.value
        synchronized(peerSnakes) {
            participants.forEach { user ->
                if (user == currentUsername) return@forEach
                val peer = peerSnakes[user]
                if (peer == null) {
                    // Create new mock peer
                    val startPos = Vector2D(
                        playerX + Random.nextInt(-400, 400),
                        playerY + Random.nextInt(-400, 400)
                    )
                    val bodyList = mutableListOf<Vector2D>()
                    for (i in 0 until playerLength) {
                        bodyList.add(Vector2D(startPos.x, startPos.y + i * 15f))
                    }
                    peerSnakes[user] = PeerSnakeData(
                        id = "mp_$user",
                        name = user,
                        position = startPos,
                        angle = playerAngle + Random.nextFloat() * 2f - 1f,
                        speed = playerSpeed,
                        length = playerLength,
                        score = playerScore + Random.nextInt(-50, 200),
                        isBoosting = playerBoosting && Random.nextBoolean(),
                        primaryColorHex = if (user == "GigaSlither_99") "#FF3366" else "#FFFF33",
                        secondaryColorHex = "#FFCC00",
                        body = bodyList,
                        lastUpdated = now
                    )
                } else {
                    // Update existing mock peer
                    var ndDiff = playerAngle - peer.angle
                    peer.angle += ndDiff * 0.08f + (Random.nextFloat() * 0.4f - 0.2f)
                    val fDirX = kotlin.math.cos(peer.angle.toDouble()).toFloat()
                    val fDirY = kotlin.math.sin(peer.angle.toDouble()).toFloat()
                    peer.position += Vector2D(fDirX * peer.speed, fDirY * peer.speed)

                    val updatedBody = mutableListOf<Vector2D>()
                    updatedBody.add(peer.position)
                    if (peer.body.isNotEmpty()) {
                        for (i in 0 until peer.length - 1) {
                            val currentSeg = peer.body.getOrNull(i) ?: peer.position
                            val nextSeg = peer.body.getOrNull(i + 1) ?: currentSeg
                            val dist = currentSeg.distance(nextSeg)
                            if (dist > 15f) {
                                val pullDir = (currentSeg - nextSeg).normalized()
                                updatedBody.add(currentSeg - pullDir * 15f)
                            } else {
                                updatedBody.add(nextSeg)
                            }
                        }
                    }
                    peer.body = updatedBody
                    peer.lastUpdated = now
                }
            }
        }
    }
}