package com.pocketbansuri.model

import kotlin.math.abs
import kotlin.math.pow

enum class Swara(
    val displayName: String,
    val hindiName: String,
    val baseFrequency: Float, // Default C scale, Mid octave Frequency in Hz
    val midiNote: Int,        // Default C scale, Mid octave MIDI Note number
    val baseFingering: List<Float> // 1.0f = closed, 0.0f = open, 0.5f = half-open for 7 holes (H1 to H7)
) {
    SA("Sa", "सा", 261.63f, 60, listOf(1f, 1f, 1f, 0f, 0f, 0f, 0f)),      // H1 to H3 closed
    RE("Re", "रे", 293.66f, 62, listOf(1f, 1f, 0f, 0f, 0f, 0f, 0f)),      // H1 to H2 closed
    GA("Ga", "ग", 329.63f, 64, listOf(1f, 0f, 0f, 0f, 0f, 0f, 0f)),      // H1 closed
    MA("Ma", "म", 349.23f, 65, listOf(0.5f, 0f, 0f, 0f, 0f, 0f, 0f)),    // Half H1 closed (Shuddha Ma)
    PA("Pa", "प", 392.00f, 67, listOf(1f, 1f, 1f, 1f, 1f, 1f, 0f)),      // H1 to H6 closed
    DHA("Dha", "ध", 440.00f, 69, listOf(1f, 1f, 1f, 1f, 1f, 0f, 0f)),    // H1 to H5 closed
    NI("Ni", "नि", 493.88f, 71, listOf(1f, 1f, 1f, 1f, 0f, 0f, 0f)),      // H1 to H4 closed
    HIGH_SA("Sa'", "सां", 523.25f, 72, listOf(0.5f, 0f, 0f, 0f, 0f, 0f, 0f));

    /**
     * Gets the fingering list for a given octave.
     */
    fun getFingeringForOctave(@Suppress("UNUSED_PARAMETER") octave: String): List<Float> {
        return this.baseFingering
    }

    /**
     * Calculates the JNI MIDI note of the Swara for a given scale key and octave.
     */
    fun getMidiNoteForScaleAndOctave(scale: String, octave: String): Int {
        val rootMidi = when (scale.uppercase()) {
            "C" -> 60
            "C#" -> 61
            "D" -> 62
            "D#" -> 63
            "E" -> 64
            "F" -> 65
            "F#" -> 66
            "G" -> 67
            "G#" -> 68
            "A" -> 69
            "A#" -> 70
            "B" -> 71
            else -> 60
        }
        val octaveOffset = when (octave.uppercase()) {
            "LOW" -> -12
            "MID" -> 0
            "HIGH" -> 12
            "MAX", "V.HIGH" -> 24
            else -> 0
        }
        val swaraOffset = when (this) {
            SA -> 0
            RE -> 2
            GA -> 4
            MA -> 5 // Always Shuddha Ma (perfect 4th)
            PA -> 7
            DHA -> 9
            NI -> 11
            HIGH_SA -> 12
        }
        return rootMidi + octaveOffset + swaraOffset
    }

    /**
     * Calculates the frequency of the Swara for a given scale key and octave in Hz.
     */
    fun getFrequencyForScaleAndOctave(scale: String, octave: String): Float {
        val midi = getMidiNoteForScaleAndOctave(scale, octave)
        return (440.0 * 2.0.pow((midi - 69) / 12.0)).toFloat()
    }

    /**
     * Calculates the Western equivalent pitch (e.g. "C4", "F#5")
     */
    fun getWesternEquivalent(scale: String, octave: String): String {
        val midi = getMidiNoteForScaleAndOctave(scale, octave)
        val noteNames = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
        val noteName = noteNames[midi % 12]
        val octaveNum = (midi / 12) - 1
        return "$noteName$octaveNum"
    }

    companion object {
        fun getClosestSwara(frequency: Float, scale: String = "C", @Suppress("UNUSED_PARAMETER") octave: String = "MID"): Swara {
            if (frequency <= 0f) return SA
            val octaves = listOf("LOW", "MID", "HIGH", "V.HIGH")
            var closestSwara = SA
            var minDiff = Float.MAX_VALUE
            for (oct in octaves) {
                for (swara in values()) {
                    val targetFreq = swara.getFrequencyForScaleAndOctave(scale, oct)
                    val diff = abs(targetFreq - frequency)
                    if (diff < minDiff) {
                        minDiff = diff
                        closestSwara = swara
                    }
                }
            }
            return closestSwara
        }

        fun getClosestSwaraAndOctave(frequency: Float, scale: String = "C"): Pair<Swara, String> {
            if (frequency <= 0f) return Pair(SA, "MID")
            val octaves = listOf("LOW", "MID", "HIGH", "V.HIGH")
            var closestSwara = SA
            var closestOctave = "MID"
            var minDiff = Float.MAX_VALUE
            for (oct in octaves) {
                for (swara in values()) {
                    val targetFreq = swara.getFrequencyForScaleAndOctave(scale, oct)
                    val diff = abs(targetFreq - frequency)
                    if (diff < minDiff) {
                        minDiff = diff
                        closestSwara = swara
                        closestOctave = oct
                    }
                }
            }
            return Pair(closestSwara, closestOctave)
        }
    }
}
