package com.pocketbansuri

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log
import kotlin.math.sin

object AudioEngine {
    private const val TAG = "AudioEngine"
    private const val SAMPLE_RATE = 44100

    init {
        try {
            System.loadLibrary("native-lib")
            Log.i(TAG, "Native library 'native-lib' loaded successfully.")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "CRITICAL: Failed to load native library 'native-lib'", e)
        }
    }

    // Volatile control variables for the persistent sound synthesis thread
    @Volatile
    private var isEngineRunning = false
    
    @Volatile
    private var targetMidiNote = -1

    private var audioTrack: AudioTrack? = null
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
     * Ensures that the persistent synthesizer thread is active.
     */
    @Synchronized
    private fun ensureSynthRunning() {
        if (synthThread != null && synthThread!!.isAlive) {
            return
        }

        isEngineRunning = true
        synthThread = Thread {
            val bufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )

            val track = AudioTrack(
                AudioManager.STREAM_MUSIC,
                SAMPLE_RATE,
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
            
            var currentMidi = -1
            var currentFreq = 0.0
            var phaseIncrement = 0.0
            
            // Envelope variables for smooth transitions
            var gain = 0.0
            val rampStep = 1.0 / (SAMPLE_RATE * 0.03) // 30ms fade ramp to eliminate pops

            while (isEngineRunning) {
                val targetMidi = targetMidiNote
                
                for (i in buffer.indices) {
                    // Smooth crossfade logic
                    if (targetMidi != currentMidi) {
                        if (gain > 0.0) {
                            // Fade out active note
                            gain -= rampStep
                            if (gain < 0.0) gain = 0.0
                        } else {
                            // Switched at 0 volume, safe to change frequency without clicks!
                            currentMidi = targetMidi
                            if (currentMidi != -1) {
                                currentFreq = 440.0 * Math.pow(2.0, (currentMidi - 69) / 12.0)
                                phaseIncrement = 2.0 * Math.PI * currentFreq / SAMPLE_RATE
                            } else {
                                currentFreq = 0.0
                                phaseIncrement = 0.0
                            }
                        }
                    } else if (currentMidi != -1 && gain < 1.0) {
                        // Fade in new note
                        gain += rampStep
                        if (gain > 1.0) gain = 1.0
                    } else if (currentMidi == -1 && gain > 0.0) {
                        // Fade out to silence
                        gain -= rampStep
                        if (gain < 0.0) gain = 0.0
                    }

                    // Flute additive synthesis
                    val sampleVal = if (gain > 0.0 && currentFreq > 0.0) {
                        sin(phase) + 0.35 * sin(2.0 * phase) + 0.12 * sin(3.0 * phase)
                    } else {
                        0.0
                    }

                    buffer[i] = (sampleVal * 14000.0 * gain).toInt().toShort()
                    
                    if (currentFreq > 0.0) {
                        phase += phaseIncrement
                        if (phase > 2.0 * Math.PI) {
                            phase -= 2.0 * Math.PI
                        }
                    } else {
                        phase = 0.0
                    }
                }

                if (isEngineRunning) {
                    track.write(buffer, 0, buffer.size)
                }
            }

            try {
                track.stop()
                track.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping AudioTrack", e)
            }
        }.apply {
            name = "PocketBansuriSynth"
            start()
        }
    }

    /**
     * Triggers playback of a synthesized flute-like reference sound at the specified MIDI note.
     */
    fun playReferenceNote(midiNote: Int) {
        targetMidiNote = midiNote
        ensureSynthRunning()
    }

    /**
     * Stops playback of the currently active reference note.
     */
    fun stopReferenceNote() {
        targetMidiNote = -1
    }

    /**
     * Release synthesizer thread resources completely when app finishes.
     */
    fun release() {
        isEngineRunning = false
        try {
            synthThread?.join(300)
        } catch (e: Exception) {
            Log.e(TAG, "Error joining thread", e)
        }
        synthThread = null
        audioTrack = null
    }
}
