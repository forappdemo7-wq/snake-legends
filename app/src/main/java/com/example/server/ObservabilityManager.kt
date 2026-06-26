package com.example.server

import android.util.Log
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

class ObservabilityManager {
    
    // Core Atomic Counters
    private val activePlayersCount = AtomicInteger(0)
    private val totalMatchesCreated = AtomicInteger(0)
    private val processedTicksCount = AtomicLong(0)
    
    // Server Performance metrics
    private var averageTickDurationMs = 0f
    private var averageLatencyMs = 24f
    private var averagePacketLoss = 0.0f
    
    private val exceptionCounts = ConcurrentHashMap<String, Int>()

    fun incrementActivePlayers() {
        activePlayersCount.incrementAndGet()
    }

    fun decrementActivePlayers() {
        activePlayersCount.decrementAndGet().coerceAtLeast(0)
    }

    fun incrementMatches() {
        totalMatchesCreated.incrementAndGet()
    }

    fun recordTick(durationMs: Long) {
        processedTicksCount.incrementAndGet()
        // Running average calculation
        averageTickDurationMs = (averageTickDurationMs * 99f + durationMs) / 100f
    }

    fun updateNetworkPerformance(avgPingMs: Float, avgLoss: Float) {
        averageLatencyMs = avgPingMs
        averagePacketLoss = avgLoss
    }

    fun logException(exception: Throwable) {
        val name = exception.javaClass.simpleName
        exceptionCounts[name] = (exceptionCounts[name] ?: 0) + 1
        Log.e("ServerObservability", "CRITICAL METRIC EXCEPTION: $name: ${exception.message}", exception)
    }

    fun printObservabilityReport(): String {
        val runtime = Runtime.getRuntime()
        val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val maxMemory = runtime.maxMemory() / (1024 * 1024)

        return """
            =================== SERVER OBSERVABILITY REPORT ===================
            Active Connections : ${activePlayersCount.get()} players
            Matches Initiated  : ${totalMatchesCreated.get()}
            Total Frames Ticked: ${processedTicksCount.get()}
            Average Tick Time  : ${String.format("%.3f", averageTickDurationMs)} ms (Target: 33.3ms for 30Hz)
            Simulated Latency  : ${averageLatencyMs.roundToInt()} ms (Packet Loss: ${String.format("%.2f", averagePacketLoss * 100f)}%)
            System Heap Memory : $usedMemory MB / $maxMemory MB
            Registered Faults  : ${exceptionCounts.entries.joinToString(", ") { "${it.key}:${it.value}" }.ifEmpty { "None" }}
            ==================================================================
        """.trimIndent()
    }

    private fun Float.roundToInt(): Int = this.toInt()
}
