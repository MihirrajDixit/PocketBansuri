package com.pocketbansuri

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import kotlin.math.sin

object AudioEngine {
    private const val TAG = "AudioEngine"

    init {
        try {
            System.loadLibrary("native-lib")
            Log.i(TAG, "Native library 'native-lib' loaded successfully.")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "CRITICAL: Failed to load native library 'native-lib'", e)
        }
    }

    // Volatile control variables for the sound synthesis thread
    private var audioTrack: AudioTrack? = null
    @Volatile
    private var isPlaying = false
    private var synthThread: Thread? = null

    /**
     * Starts the audio capture and pitch detection engine (C++).
     */
    external fun startEngine()

    /**
     * Stops the audio engine and releases resource handles (C++).
     */
    external fun stopEngine()

    /**
     * Returns the current real-time detected pitch frequency in Hz (C++).
     */
    external fun getDetectedFrequency(): Float

    /**
     * Triggers playback of a synthesized flute-like reference sound at the specified MIDI note.
     * Implemented in Kotlin for robust, low-latency, offline synthesis on all devices.
     */
    fun playReferenceNote(midiNote: Int) {
        stopReferenceNote()

        // MIDI to Frequency conversion formula: f = 440 * 2^((midi - 69)/12)
        val frequency = (440.0 * Math.pow(2.0, (midiNote - 69) / 12.0)).toFloat()
        isPlaying = true

        synthThread = Thread {
            val sampleRate = 44100
            val bufferSize = AudioTrack.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            val track = AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize,
                AudioTrack.MODE_STREAM
            )

            audioTrack = track

            try {
                track.play()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start AudioTrack playback", e)
                return@Thread
            }

            val buffer = ShortArray(bufferSize)
            var phase = 0.0
            val phaseIncrement = 2.0 * Math.PI * frequency / sampleRate

            // Envelope counters for soft attack (fade-in) to prevent pop sound
            var sampleCount = 0
            val attackSamples = sampleRate * 0.1 // 100ms fade-in

            while (isPlaying) {
                for (i in buffer.indices) {
                    // Flute Additive Synthesis: Fundamental + 2nd harmonic + 3rd harmonic
                    val sampleVal = sin(phase) + 0.35 * sin(2.0 * phase) + 0.12 * sin(3.0 * phase)

                    val gain = if (sampleCount < attackSamples) {
                        sampleCount / attackSamples
                    } else {
                        1.0
                    }
                    sampleCount++

                    // Scale output to 16-bit range (max 32767)
                    buffer[i] = (sampleVal * 14000.0 * gain).toInt().toShort()
                    phase += phaseIncrement
                    if (phase > 2.0 * Math.PI) {
                        phase -= 2.0 * Math.PI
                    }
                }
                
                if (isPlaying) {
                    track.write(buffer, 0, buffer.size)
                }
            }

            // Soft decay envelope on stop (fade-out) to prevent click artifacts
            val releaseSamples = (sampleRate * 0.05).toInt() // 50ms fade-out
            val releaseBuffer = ShortArray(releaseSamples)
            for (i in releaseBuffer.indices) {
                val decay = 1.0 - (i.toDouble() / releaseSamples)
                val sampleVal = sin(phase) + 0.35 * sin(2.0 * phase) + 0.12 * sin(3.0 * phase)
                releaseBuffer[i] = (sampleVal * 14000.0 * decay).toInt().toShort()
                phase += phaseIncrement
                if (phase > 2.0 * Math.PI) {
                    phase -= 2.0 * Math.PI
                }
            }
            track.write(releaseBuffer, 0, releaseBuffer.size)

            try {
                track.stop()
                track.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping or releasing AudioTrack", e)
            }
        }.apply {
            name = "PocketBansuriSynth"
            start()
        }
    }

    /**
     * Stops playback of the currently active reference note.
     */
    fun stopReferenceNote() {
        if (isPlaying) {
            isPlaying = false
            try {
                synthThread?.join(500)
            } catch (e: InterruptedException) {
                Log.e(TAG, "Synth thread join interrupted", e)
            }
            synthThread = null
            audioTrack = null
        }
    }
}
