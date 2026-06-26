package com.example.server

import android.util.Log
import com.example.game.Vector2D
import kotlinx.coroutines.*
import kotlin.random.Random

class LoadTester(
    private val gameServer: GameServer,
    private val observabilityManager: ObservabilityManager
) {
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var testingJob: Job? = null
    private val activeBotIds = mutableListOf<String>()

    fun startLoadTest(botsCount: Int) {
        stopLoadTest() // Clean run
        Log.i("ServerLoadTester", "STARTING CONCURRENCY LOAD TEST FOR $botsCount VIRTUAL PLAYERS")

        activeBotIds.clear()
        
        testingJob = scope.launch {
            // Join bots
            for (i in 1..botsCount) {
                val botId = "sim_bot_$i"
                val uName = "SimSlinky_$i"
                gameServer.joinPlayerSession(botId, uName)
                activeBotIds.add(botId)
                observabilityManager.incrementActivePlayers()
                delay(30) // Stagger joins
            }

            Log.i("ServerLoadTester", "Successfully connected all $botsCount virtual players. Simulating continuous inputs.")

            var tickCounter = 0L
            while (isActive) {
                tickCounter++
                
                // Continuous inputs simulation (random joystick angles and occasional boost/abilities triggers)
                activeBotIds.forEach { bId ->
                    val angle = Random.nextFloat() * (2f * Math.PI).toFloat()
                    val boost = Random.nextInt(8) == 0 // occasional boost
                    val ability = Random.nextInt(30) == 0 // occasional ability usage
                    
                    val inputPacket = ClientInputPacket(
                        tickNumber = tickCounter,
                        playerId = bId,
                        joystickAngle = angle,
                        isBoosting = boost,
                        triggerAbility = ability,
                        timestamp = System.currentTimeMillis(),
                        signature = "LOAD_TEST_SIG"
                    )
                    gameServer.submitPlayerInput(bId, inputPacket)
                }
                
                // Print telemetry summary every 150 frames (5 seconds at 30Hz)
                if (tickCounter % 150 == 0L) {
                    val report = observabilityManager.printObservabilityReport()
                    Log.i("ServerLoadTester", "[TELEMETRY METRICS SUMMARY]\n$report")
                }
                
                delay(33) // 30Hz simulation loop input submissions
            }
        }
    }

    fun stopLoadTest() {
        testingJob?.cancel()
        testingJob = null
        activeBotIds.forEach { bId ->
            gameServer.leavePlayerSession(bId)
            observabilityManager.decrementActivePlayers()
        }
        activeBotIds.clear()
        Log.i("ServerLoadTester", "CONCURRENCY LOAD TEST COMPLETED & CLEANED.")
    }
}
