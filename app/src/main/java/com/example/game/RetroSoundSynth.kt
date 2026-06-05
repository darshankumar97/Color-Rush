package com.example.game

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.sin

class RetroSoundSynth {
    private val sampleRate = 22050
    private val ioScope = CoroutineScope(Dispatchers.Default)

    fun playTone(frequency: Double, durationMs: Int, waveForm: WaveForm = WaveForm.SINE) {
        ioScope.launch {
            try {
                val numSamples = (durationMs * sampleRate / 1000)
                val generatedSnd = ByteArray(2 * numSamples)
                
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val sampleVal = when (waveForm) {
                        WaveForm.SINE -> sin(2.0 * Math.PI * frequency * t)
                        WaveForm.SQUARE -> if (sin(2.0 * Math.PI * frequency * t) >= 0) 0.5 else -0.5
                        WaveForm.SAWTOOTH -> 2.0 * (t * frequency - Math.floor(t * frequency + 0.5))
                    }
                    
                    // Simple linear decay envelope to prevent clicks
                    val envelope = if (i > numSamples - 1000) {
                        (numSamples - i).toDouble() / 1000.0
                    } else if (i < 500) {
                        i.toDouble() / 500.0
                    } else {
                        1.0
                    }

                    val finalVal = (sampleVal * envelope * 32767).toInt()
                    generatedSnd[2 * i] = (finalVal and 0x00ff).toByte()
                    generatedSnd[2 * i + 1] = ((finalVal and 0xff00) ushr 8).toByte()
                }

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
                    .setBufferSizeInBytes(generatedSnd.size)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(generatedSnd, 0, generatedSnd.size)
                audioTrack.play()
                
                // Allow completion then release
                kotlinx.coroutines.delay(durationMs + 50L)
                audioTrack.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    enum class WaveForm { SINE, SQUARE, SAWTOOTH }

    fun playTapCorrect() {
        playTone(659.25, 120, WaveForm.SQUARE) // E5 note, snappy square wave
    }

    fun playTapWrong() {
        playTone(150.0, 300, WaveForm.SAWTOOTH) // low buzz sawtooth
    }

    fun playLevelUp() {
        ioScope.launch {
            playTone(523.25, 65, WaveForm.SINE) // C5
            kotlinx.coroutines.delay(70)
            playTone(659.25, 65, WaveForm.SINE) // E5
            kotlinx.coroutines.delay(70)
            playTone(783.99, 65, WaveForm.SINE) // G5
            kotlinx.coroutines.delay(70)
            playTone(1046.50, 160, WaveForm.SQUARE) // C6
        }
    }
}
