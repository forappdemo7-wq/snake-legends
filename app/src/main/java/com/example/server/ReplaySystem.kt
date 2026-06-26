package com.example.server

import com.example.game.Vector2D
import java.io.ByteArrayOutputStream
import java.util.concurrent.ConcurrentHashMap

class ReplaySystem {
    private val activeReplays = ConcurrentHashMap<String, MutableList<ReplayFrame>>()
    private val replayMetadata = ConcurrentHashMap<String, MatchMetadata>()

    data class MatchMetadata(
        val matchId: String,
        val startTime: Long,
        val mapTheme: String,
        val playersCount: Int
    )

    fun startRecording(matchId: String, mapTheme: String, playersCount: Int) {
        activeReplays[matchId] = mutableListOf()
        replayMetadata[matchId] = MatchMetadata(
            matchId = matchId,
            startTime = System.currentTimeMillis(),
            mapTheme = mapTheme,
            playersCount = playersCount
        )
    }

    fun recordFrame(matchId: String, tick: Long, inputs: List<ClientInputPacket>, events: List<String>) {
        val frames = activeReplays[matchId] ?: return
        val frame = ReplayFrame(
            tick = tick,
            inputs = inputs.map { it.copy() },
            events = events.toList()
        )
        frames.add(frame)
    }

    fun finishRecording(matchId: String, finalScores: Map<String, Int>): MatchReplay? {
        val frames = activeReplays.remove(matchId) ?: return null
        val meta = replayMetadata.remove(matchId) ?: return null
        
        return MatchReplay(
            matchId = matchId,
            startTimestamp = meta.startTime,
            durationTicks = frames.size.toLong(),
            frames = frames,
            finalScores = finalScores
        )
    }

    /**
     * Serializes deterministic match data into lightweight compressed strings.
     */
    fun serializeReplay(replay: MatchReplay): String {
        val sb = StringBuilder()
        sb.append("${replay.matchId};${replay.startTimestamp};${replay.durationTicks}\n")
        
        // Final scores
        val scoreStr = replay.finalScores.entries.joinToString(",") { "${it.key}:${it.value}" }
        sb.append("$scoreStr\n")
        
        // Frames
        replay.frames.forEach { f ->
            if (f.inputs.isNotEmpty() || f.events.isNotEmpty()) {
                sb.append("F:${f.tick}|")
                val inputParts = f.inputs.joinToString(",") { inp ->
                    "${inp.playerId}:${inp.joystickAngle ?: "N"}:${if (inp.isBoosting) 1 else 0}:${if (inp.triggerAbility) 1 else 0}"
                }
                sb.append("I:$inputParts|")
                val eventParts = f.events.joinToString(",")
                sb.append("E:$eventParts\n")
            }
        }
        return sb.toString()
    }

    fun deserializeReplay(serialized: String): MatchReplay {
        val lines = serialized.trim().split("\n")
        val metaParts = lines[0].split(";")
        val matchId = metaParts[0]
        val startTime = metaParts[1].toLong()
        val duration = metaParts[2].toLong()

        val scoreParts = lines[1].split(",")
        val scores = scoreParts.associate { part ->
            val sub = part.split(":")
            sub[0] to sub[1].toInt()
        }

        val frames = mutableListOf<ReplayFrame>()
        for (i in 2 until lines.size) {
            val line = lines[i]
            if (line.startsWith("F:")) {
                val sections = line.split("|")
                val tick = sections[0].removePrefix("F:").toLong()
                
                val inputs = mutableListOf<ClientInputPacket>()
                if (sections.size > 1 && sections[1].startsWith("I:") && sections[1].length > 2) {
                    val inputsContent = sections[1].removePrefix("I:")
                    inputsContent.split(",").forEach { item ->
                        val parts = item.split(":")
                        if (parts.size >= 4) {
                            inputs.add(
                                ClientInputPacket(
                                    tickNumber = tick,
                                    playerId = parts[0],
                                    joystickAngle = parts[1].toFloatOrNull(),
                                    isBoosting = parts[2] == "1",
                                    triggerAbility = parts[3] == "1",
                                    timestamp = System.currentTimeMillis(),
                                    signature = ""
                                )
                            )
                        }
                    }
                }

                val events = mutableListOf<String>()
                if (sections.size > 2 && sections[2].startsWith("E:") && sections[2].length > 2) {
                    val eventsContent = sections[2].removePrefix("E:")
                    eventsContent.split(",").forEach { events.add(it) }
                }

                frames.add(ReplayFrame(tick, inputs, events))
            }
        }

        return MatchReplay(
            matchId = matchId,
            startTimestamp = startTime,
            durationTicks = duration,
            frames = frames,
            finalScores = scores
        )
    }
}
