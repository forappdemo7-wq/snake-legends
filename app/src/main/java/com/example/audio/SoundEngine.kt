package com.example.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.example.game.ArenaTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.sin

object SoundEngine {
    private var isMuted = false
    private val sampleRate = 22050
    private val audioExecutor = Executors.newSingleThreadExecutor()
    private val bgScope = CoroutineScope(Dispatchers.Default)
    private var bgMusicJob: Job? = null
    private var currentTheme: ArenaTheme? = null

    fun setMuted(muted: Boolean) {
        isMuted = muted
        if (muted) {
            stopBackgroundMusic()
        }
    }

    fun isMuted(): Boolean = isMuted

    fun playEatSound(isSuper: Boolean = false, isCelestial: Boolean = false) {
        if (isMuted) return
        audioExecutor.execute {
            try {
                val startFreq = if (isCelestial) 880f else if (isSuper) 520f else 320f
                val endFreq = if (isCelestial) 1320f else if (isSuper) 780f else 480f
                val durationMs = if (isCelestial) 120 else if (isSuper) 80 else 50
                
                val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
                val samples = ShortArray(numSamples)
                
                for (i in 0 until numSamples) {
                    val progress = i.toFloat() / numSamples
                    val freq = startFreq + (endFreq - startFreq) * progress
                    val angle = 2.0 * PI * freq * i / sampleRate
                    val envelope = 1.0 - progress
                    val wave = (sin(angle) * Short.MAX_VALUE * 0.35f * envelope).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    samples[i] = wave.toShort()
                }

                playPcmBuffer(samples)
            } catch (e: Exception) {
                // Ignore audio write errors gracefully
            }
        }
    }

    fun playBoostSound() {
        if (isMuted) return
        audioExecutor.execute {
            try {
                val numSamples = (sampleRate * 0.08f).toInt()
                val samples = ShortArray(numSamples)
                
                for (i in 0 until numSamples) {
                    val progress = i.toFloat() / numSamples
                    val noise = (java.util.Random().nextFloat() * 2f - 1f)
                    val freq = 120f + noise * 40f
                    val angle = 2.0 * PI * freq * i / sampleRate
                    val envelope = sin(progress * PI)
                    val wave = (sin(angle) * Short.MAX_VALUE * 0.25f * envelope).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    samples[i] = wave.toShort()
                }

                playPcmBuffer(samples)
            } catch (e: Exception) {
                // Ignore audio errors
            }
        }
    }

    fun playCollisionSound() {
        if (isMuted) return
        audioExecutor.execute {
            try {
                val numSamples = (sampleRate * 0.22f).toInt()
                val samples = ShortArray(numSamples)
                
                for (i in 0 until numSamples) {
                    val progress = i.toFloat() / numSamples
                    val noise = (java.util.Random().nextFloat() * 2f - 1f)
                    val envelope = (1.0 - progress) * (1.0 - progress)
                    val wave = (noise * Short.MAX_VALUE * 0.45f * envelope).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    samples[i] = wave.toShort()
                }

                playPcmBuffer(samples)
            } catch (e: Exception) {
                // Ignore audio errors
            }
        }
    }

    fun playPowerUpSound() {
        if (isMuted) return
        audioExecutor.execute {
            try {
                val freqs = floatArrayOf(440f, 554.37f, 659.25f, 880f)
                val durationPerNote = (sampleRate * 0.05f).toInt()
                val totalSamples = durationPerNote * freqs.size
                val samples = ShortArray(totalSamples)

                for (n in freqs.indices) {
                    val freq = freqs[n]
                    for (i in 0 until durationPerNote) {
                        val sampleIdx = n * durationPerNote + i
                        val progress = i.toFloat() / durationPerNote
                        val angle = 2.0 * PI * freq * i / sampleRate
                        val envelope = 1.0 - progress * 0.5
                        val wave = (sin(angle) * Short.MAX_VALUE * 0.35f * envelope).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                        samples[sampleIdx] = wave.toShort()
                    }
                }

                playPcmBuffer(samples)
            } catch (e: Exception) {
                // Ignore audio errors
            }
        }
    }

    fun playKillSound() {
        if (isMuted) return
        audioExecutor.execute {
            try {
                val freqs = floatArrayOf(523.25f, 659.25f, 783.99f, 1046.50f)
                val durationPerNote = (sampleRate * 0.06f).toInt()
                val totalSamples = durationPerNote * freqs.size
                val samples = ShortArray(totalSamples)

                for (n in freqs.indices) {
                    val freq = freqs[n]
                    for (i in 0 until durationPerNote) {
                        val sampleIdx = n * durationPerNote + i
                        val progress = i.toFloat() / durationPerNote
                        val angle = 2.0 * PI * freq * i / sampleRate
                        val envelope = sin(progress * PI)
                        val wave = (sin(angle) * Short.MAX_VALUE * 0.4f * envelope).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                        samples[sampleIdx] = wave.toShort()
                    }
                }

                playPcmBuffer(samples)
            } catch (e: Exception) {
                // Ignore audio errors
            }
        }
    }

    fun startBackgroundMusic(theme: ArenaTheme) {
        if (isMuted) return
        if (currentTheme == theme && bgMusicJob?.isActive == true) return
        stopBackgroundMusic()
        currentTheme = theme

        bgMusicJob = bgScope.launch {
            val baseFreqs = when (theme) {
                ArenaTheme.CYBER_CITY -> floatArrayOf(220f, 277.18f, 329.63f, 440f)
                ArenaTheme.LAVA_WORLD -> floatArrayOf(146.83f, 174.61f, 220f, 293.66f)
                ArenaTheme.FROZEN_ARENA -> floatArrayOf(261.63f, 329.63f, 392f, 523.25f)
                ArenaTheme.JUNGLE_TEMPLE -> floatArrayOf(196f, 246.94f, 293.66f, 392f)
                ArenaTheme.SPACE_STATION -> floatArrayOf(130.81f, 164.81f, 196f, 261.63f)
                ArenaTheme.NEON_GRID -> floatArrayOf(293.66f, 369.99f, 440f, 587.33f)
            }

            var noteIdx = 0
            while (isActive && !isMuted) {
                val freq = baseFreqs[noteIdx % baseFreqs.size]
                val durationMs = 280
                val numSamples = (sampleRate * (durationMs / 1000f)).toInt()
                val samples = ShortArray(numSamples)

                for (i in 0 until numSamples) {
                    val progress = i.toFloat() / numSamples
                    val angle = 2.0 * PI * freq * i / sampleRate
                    val envelope = sin(progress * PI) * 0.15f
                    val wave = (sin(angle) * Short.MAX_VALUE * envelope).toInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
                    samples[i] = wave.toShort()
                }

                playPcmBuffer(samples)
                noteIdx++
                delay(320)
            }
        }
    }

    fun stopBackgroundMusic() {
        bgMusicJob?.cancel()
        bgMusicJob = null
        currentTheme = null
    }

    private fun playPcmBuffer(samples: ShortArray) {
        val minBufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufferSize = maxOf(minBufferSize, samples.size * 2)

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack.write(samples, 0, samples.size)
        audioTrack.play()
        
        // Release audio track after playback
        bgScope.launch {
            delay((samples.size.toFloat() / sampleRate * 1000f).toLong() + 100)
            try {
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}
