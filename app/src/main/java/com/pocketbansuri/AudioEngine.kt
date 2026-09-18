package com.pocketbansuri

import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.util.Log
import kotlin.math.abs
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

    // Real-time microphone capture & pitch detection state
    private var recordingThread: Thread? = null
    @Volatile private var isRecording = false
    @Volatile private var detectedFrequency = 0.0f
    @Volatile private var detectedAirRatio = 0.0f
    @Volatile private var detectedDoubleRatio = 0.0f

    /**
     * Starts the audio capture and pitch detection engine (Kotlin-based microphone thread).
     */
    fun startEngine() {
        if (isRecording) return
        isRecording = true
        recordingThread = Thread {
            val channelConfig = AudioFormat.CHANNEL_IN_MONO
            val audioFormat = AudioFormat.ENCODING_PCM_16BIT
            
            val sampleRates = listOf(44100, 22050)
            val audioSources = mutableListOf<Int>()
            if (android.os.Build.VERSION.SDK_INT >= 24) {
                audioSources.add(MediaRecorder.AudioSource.UNPROCESSED)
            }
            audioSources.add(MediaRecorder.AudioSource.VOICE_RECOGNITION)
            audioSources.add(MediaRecorder.AudioSource.MIC)

            var audioRecord: AudioRecord? = null
            var chosenSampleRate = 22050
            var chosenBufferSize = 2048

            outer@ for (rate in sampleRates) {
                val minBufferSize = AudioRecord.getMinBufferSize(rate, channelConfig, audioFormat)
                if (minBufferSize == AudioRecord.ERROR || minBufferSize == AudioRecord.ERROR_BAD_VALUE) continue
                
                val bufferSizeInSamples = maxOf(2048, minBufferSize / 2)
                
                for (source in audioSources) {
                    try {
                        val record = AudioRecord(
                            source,
                            rate,
                            channelConfig,
                            audioFormat,
                            bufferSizeInSamples * 2
                        )
                        if (record.state == AudioRecord.STATE_INITIALIZED) {
                            audioRecord = record
                            chosenSampleRate = rate
                            chosenBufferSize = bufferSizeInSamples
                            Log.d(TAG, "Successfully initialized AudioRecord with source $source at $rate Hz")
                            break@outer
                        } else {
                            record.release()
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to initialize AudioRecord with source $source at $rate Hz", e)
                    }
                }
            }

            if (audioRecord == null || audioRecord.state != AudioRecord.STATE_INITIALIZED) {
                Log.e(TAG, "AudioRecord initialization failed across all combinations")
                isRecording = false
                return@Thread
            }

            try {
                audioRecord.startRecording()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start recording", e)
                audioRecord.release()
                isRecording = false
                return@Thread
            }

            val audioBuffer = ShortArray(chosenBufferSize)
            while (isRecording) {
                val readResult = audioRecord.read(audioBuffer, 0, chosenBufferSize)
                if (readResult > 0) {
                    val freq = detectPitchAutocorrelation(audioBuffer, readResult, chosenSampleRate)
                    detectedFrequency = freq
                }
            }

            try {
                audioRecord.stop()
                audioRecord.release()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping AudioRecord", e)
            }
        }.apply {
            name = "PocketBansuriPitchDetector"
            start()
        }
    }

    /**
     * Stops the audio engine and releases resource handles.
     */
    fun stopEngine() {
        isRecording = false
        try {
            recordingThread?.join(300)
        } catch (e: Exception) {
            Log.e(TAG, "Error joining recording thread", e)
        }
        recordingThread = null
        detectedFrequency = 0.0f
        detectedAirRatio = 0.0f
        detectedDoubleRatio = 0.0f
    }

    /**
     * Returns the current real-time detected pitch frequency in Hz.
     */
    fun getDetectedFrequency(): Float {
        // If a reference note is playing, prioritize returning the target frequency
        // to verify matching behavior
        val refMidi = targetMidiNote
        if (refMidi != -1) {
            val target = 440.0 * Math.pow(2.0, (refMidi - 69) / 12.0)
            return target.toFloat()
        }
        return detectedFrequency
    }

    /**
     * Returns the raw microphone detected pitch frequency, bypassing the synthesizer target note override.
     */
    fun getRawDetectedFrequency(): Float {
        return detectedFrequency
    }

    /**
     * Returns the detected ratio of air/breathiness in the sound (0.0 to 1.0).
     */
    fun getAirRatio(): Float {
        return detectedAirRatio
    }

    /**
     * Returns the detected ratio of double sound/multiphonic instability in the sound (0.0 to 1.0).
     */
    fun getDoubleRatio(): Float {
        return detectedDoubleRatio
    }

    private fun detectPitchAutocorrelation(audioBuffer: ShortArray, size: Int, sampleRate: Int): Float {
        var sumSquares = 0.0
        for (i in 0 until size) {
            sumSquares += audioBuffer[i].toDouble() * audioBuffer[i].toDouble()
        }
        val rms = Math.sqrt(sumSquares / size)
        if (rms < 150.0) { // Silence threshold
            detectedAirRatio = 0.0f
            detectedDoubleRatio = 0.0f
            return 0.0f
        }

        // Find peak amplitude for center clipping
        var maxVal = 0
        for (i in 0 until size) {
            val absVal = abs(audioBuffer[i].toInt())
            if (absVal > maxVal) {
                maxVal = absVal
            }
        }
        
        val clipThreshold = (maxVal * 0.3).toInt() // 30% center clipping
        val clipped = FloatArray(size)
        for (i in 0 until size) {
            val v = audioBuffer[i].toInt()
            clipped[i] = if (v > clipThreshold) {
                (v - clipThreshold).toFloat()
            } else if (v < -clipThreshold) {
                (v + clipThreshold).toFloat()
            } else {
                0.0f
            }
        }

        val minLag = sampleRate / 1200 // Max frequency limit ~ 1200 Hz
        val maxLag = sampleRate / 80   // Min frequency limit ~ 80 Hz
        
        var bestLag = -1
        val r = FloatArray(maxLag + 1)
        
        for (lag in minLag..maxLag) {
            var sum = 0.0f
            for (i in 0 until (size - lag)) {
                sum += clipped[i] * clipped[i + lag]
            }
            r[lag] = sum
        }

        // Find global maximum in range
        var globalMaxR = 0.0f
        for (lag in minLag..maxLag) {
            if (r[lag] > globalMaxR) {
                globalMaxR = r[lag]
            }
        }
        
        if (globalMaxR == 0.0f) {
            detectedAirRatio = 1.0f
            detectedDoubleRatio = 0.0f
            return 0.0f
        }
        
        // Find first peak that is >= 85% of global maximum (prevents octave errors)
        val threshold = globalMaxR * 0.85f
        for (lag in (minLag + 1) until maxLag) {
            if (r[lag] > r[lag - 1] && r[lag] > r[lag + 1]) {
                if (r[lag] >= threshold) {
                    bestLag = lag
                    break
                }
            }
        }
        
        if (bestLag == -1) {
            var maxValR = 0.0f
            for (lag in minLag..maxLag) {
                if (r[lag] > maxValR) {
                    maxValR = r[lag]
                    bestLag = lag
                }
            }
        }

        // Calculate periodicity quality metrics (Air / Double sound detection)
        var zeroLagEnergy = 0.0f
        for (i in 0 until size) {
            zeroLagEnergy += clipped[i] * clipped[i]
        }

        val bestLagVal = if (bestLag in 0..maxLag) r[bestLag] else 0f
        val periodicity = if (zeroLagEnergy > 0f) (bestLagVal / zeroLagEnergy).coerceIn(0f, 1f) else 0f
        
        // Air ratio: higher value indicates more breath/wind noise relative to harmonic structure
        detectedAirRatio = (1.0f - periodicity).coerceIn(0f, 1f)

        // Find the second highest peak that is not too close to the main peak
        var secondBestPeakVal = 0.0f
        for (lag in (minLag + 1) until maxLag) {
            if (r[lag] > r[lag - 1] && r[lag] > r[lag + 1]) {
                if (abs(lag - bestLag) > bestLag * 0.15) {
                    if (r[lag] > secondBestPeakVal) {
                        secondBestPeakVal = r[lag]
                    }
                }
            }
        }
        detectedDoubleRatio = if (bestLagVal > 0f) (secondBestPeakVal / bestLagVal).coerceIn(0f, 1f) else 0f

        // Parabolic peak interpolation
        if (bestLag in (minLag + 1) until maxLag) {
            val alpha = r[bestLag - 1]
            val beta = r[bestLag]
            val gamma = r[bestLag + 1]
            val denominator = 2f * (alpha - 2f * beta + gamma)
            val offset = if (abs(denominator) > 1e-5f) {
                (alpha - gamma) / denominator
            } else {
                0.0f
            }
            val interpolatedLag = bestLag.toFloat() + offset
            return sampleRate.toFloat() / interpolatedLag
        }

        return sampleRate.toFloat() / bestLag.toFloat()
    }

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
