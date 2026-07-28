package com.example.server

import android.util.Log
import com.example.game.Vector2D
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.*
import java.util.concurrent.ConcurrentHashMap

class LanP2PServer(
    private val gameServer: GameServer,
    val port: Int = 8888
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var serverSocket: ServerSocket? = null
    @Volatile var isRunning = false
    private var listenJob: Job? = null
    private var beaconJob: Job? = null

    private val connectedClients = ConcurrentHashMap<String, ClientHandler>()

    fun startLanServer(hostUsername: String, roomCode: String = "LAN888") {
        if (isRunning) return
        isRunning = true
        try {
            serverSocket = ServerSocket(port)
            Log.i("LanP2PServer", "LAN TCP Socket Server listening on port $port")

            listenJob = scope.launch {
                while (isRunning && serverSocket != null && !serverSocket!!.isClosed) {
                    try {
                        val clientSocket = serverSocket!!.accept()
                        clientSocket.tcpNoDelay = true
                        val handler = ClientHandler(clientSocket)
                        handler.start()
                    } catch (e: Exception) {
                        if (!isRunning) break
                    }
                }
            }

            startUdpBeacon(hostUsername, roomCode)

            gameServer.registerSnapshotListener { snapshot ->
                broadcastStateSnapshot(snapshot)
            }

        } catch (e: Exception) {
            Log.e("LanP2PServer", "Failed to start LAN Socket Server on port $port: ${e.message}")
        }
    }

    fun stopLanServer() {
        isRunning = false
        listenJob?.cancel()
        beaconJob?.cancel()
        try {
            serverSocket?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        connectedClients.values.forEach { it.close() }
        connectedClients.clear()
        Log.i("LanP2PServer", "LAN TCP Socket Server stopped.")
    }

    private fun startUdpBeacon(hostUsername: String, roomCode: String) {
        beaconJob = scope.launch {
            val localIp = getLocalIpAddress() ?: "127.0.0.1"
            var socket: DatagramSocket? = null
            try {
                socket = DatagramSocket()
                socket.broadcast = true

                val payload = JSONObject().apply {
                    put("type", "LAN_BEACON")
                    put("roomCode", roomCode)
                    put("hostName", hostUsername)
                    put("ip", localIp)
                    put("port", port)
                }.toString().toByteArray()

                val broadcastAddr = InetAddress.getByName("255.255.255.255")

                while (isRunning) {
                    val packet = DatagramPacket(payload, payload.size, broadcastAddr, 8889)
                    try {
                        socket.send(packet)
                    } catch (e: Exception) {
                        // ignore subnet socket errors
                    }
                    delay(1200)
                }
            } catch (e: Exception) {
                Log.e("LanP2PServer", "UDP Beacon error: ${e.message}")
            } finally {
                socket?.close()
            }
        }
    }

    private fun broadcastStateSnapshot(snapshot: ServerStateSnapshot) {
        if (connectedClients.isEmpty()) return

        val json = JSONObject().apply {
            put("type", "SNAPSHOT")
            put("tick", snapshot.tick)
            put("timestamp", snapshot.timestamp)
            put("safeZoneRadius", snapshot.safeZoneRadius.toDouble())
            put("activeEvent", snapshot.activeEvent)

            val snakesArr = JSONArray()
            snapshot.snakes.forEach { s ->
                val sObj = JSONObject().apply {
                    put("id", s.id)
                    put("name", s.name)
                    put("x", s.position.x.toDouble())
                    put("y", s.position.y.toDouble())
                    put("angle", s.angle.toDouble())
                    put("speed", s.speed.toDouble())
                    put("length", s.length)
                    put("score", s.score)
                    put("isBoosting", s.isBoosting)
                    put("isAlive", s.isAlive)
                    put("primaryHex", s.primaryColorHex)
                    put("secondaryHex", s.secondaryColorHex)

                    val bodyArr = JSONArray()
                    s.body.forEach { seg ->
                        val bObj = JSONObject().apply {
                            put("x", seg.x.toDouble())
                            put("y", seg.y.toDouble())
                        }
                        bodyArr.put(bObj)
                    }
                    put("body", bodyArr)
                }
                snakesArr.put(sObj)
            }
            put("snakes", snakesArr)

            val orbsArr = JSONArray()
            snapshot.orbs.forEach { orb ->
                val oObj = JSONObject().apply {
                    put("id", orb.id)
                    put("x", orb.position.x.toDouble())
                    put("y", orb.position.y.toDouble())
                    put("pts", orb.points)
                    put("super", orb.isSuper)
                    put("celestial", orb.isCelestial)
                    put("color", orb.colorHex)
                }
                orbsArr.put(oObj)
            }
            put("orbs", orbsArr)
        }.toString()

        connectedClients.values.forEach { client ->
            client.sendLine(json)
        }
    }

    private inner class ClientHandler(private val socket: Socket) {
        private var writer: PrintWriter? = null
        private var reader: BufferedReader? = null
        private var playerId: String? = null

        fun start() {
            scope.launch {
                try {
                    writer = PrintWriter(socket.getOutputStream(), true)
                    reader = BufferedReader(InputStreamReader(socket.getInputStream()))

                    var line: String? = null
                    while (isRunning) {
                        line = reader?.readLine() ?: break
                        processIncomingPacket(line)
                    }
                } catch (e: Exception) {
                    // Client disconnected
                } finally {
                    close()
                }
            }
        }

        private fun processIncomingPacket(line: String) {
            try {
                val json = JSONObject(line)
                when (json.optString("type")) {
                    "JOIN" -> {
                        val pId = json.optString("playerId", "p_lan_${System.currentTimeMillis()}")
                        val username = json.optString("username", "LAN_Player")
                        playerId = pId
                        connectedClients[pId] = this

                        gameServer.joinPlayerSession(pId, username)

                        val ack = JSONObject().apply {
                            put("type", "JOIN_ACK")
                            put("playerId", pId)
                            put("arenaWidth", gameServer.arenaWidth.toDouble())
                            put("arenaHeight", gameServer.arenaHeight.toDouble())
                        }.toString()
                        sendLine(ack)
                        Log.i("LanP2PServer", "LAN Client '$username' ($pId) joined session!")
                    }

                    "INPUT" -> {
                        val pId = playerId ?: json.optString("playerId")
                        val angle = if (json.has("angle")) json.optDouble("angle").toFloat() else null
                        val isBoosting = json.optBoolean("isBoosting", false)
                        val triggerAbility = json.optBoolean("triggerAbility", false)

                        val packet = ClientInputPacket(
                            tickNumber = json.optLong("tick", 0L),
                            playerId = pId,
                            joystickAngle = angle,
                            isBoosting = isBoosting,
                            triggerAbility = triggerAbility,
                            timestamp = System.currentTimeMillis(),
                            signature = ""
                        )
                        gameServer.submitPlayerInput(pId, packet)
                    }

                    "PING" -> {
                        val ack = JSONObject().apply {
                            put("type", "PONG")
                            put("clientTime", json.optLong("clientTime"))
                        }.toString()
                        sendLine(ack)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun sendLine(text: String) {
            try {
                writer?.println(text)
            } catch (e: Exception) {
                // Ignore output errors
            }
        }

        fun close() {
            playerId?.let {
                connectedClients.remove(it)
                gameServer.leavePlayerSession(it)
            }
            try {
                socket.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        fun getLocalIpAddress(): String? {
            try {
                val interfaces = NetworkInterface.getNetworkInterfaces()
                while (interfaces.hasMoreElements()) {
                    val intf = interfaces.nextElement()
                    val addrs = intf.inetAddresses
                    while (addrs.hasMoreElements()) {
                        val addr = addrs.nextElement()
                        if (!addr.isLoopbackAddress && addr is Inet4Address) {
                            return addr.hostAddress
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            return null
        }
    }
}
