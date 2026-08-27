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
     * Teevra Ma in High/Max octaves colors all holes H1 to H7.
     */
    fun getFingeringForOctave(octave: String): List<Float> {
        if (this == MA && (octave.uppercase() == "HIGH" || octave.uppercase() == "MAX")) {
            return listOf(1f, 1f, 1f, 1f, 1f, 1f, 1f) // H1 to H7 closed for Teevra Ma
        }
        return this.baseFingering
    }

    /**
     * Calculates the JNI MIDI note of the Swara for a given scale key and octave.
     * MA in High/Max octaves plays as Teevra Ma (+6 semitones instead of +5).
     */
    fun getMidiNoteForScaleAndOctave(scale: String, octave: String): Int {
        val rootMidi = when (scale.uppercase()) {
            "C" -> 60
            "D" -> 62
            "E" -> 64
            "F" -> 65
            "G" -> 67
            "A" -> 69
            "B" -> 71
            else -> 60
        }
        val octaveOffset = when (octave.uppercase()) {
            "LOW" -> -12
            "MID" -> 0
            "HIGH" -> 12
            "MAX" -> 24
            else -> 0
        }
        val swaraOffset = when (this) {
            SA -> 0
            RE -> 2
            GA -> 4
            MA -> {
                if (octave.uppercase() == "HIGH" || octave.uppercase() == "MAX") {
                    6 // Teevra Ma (augmented 4th)
                } else {
                    5 // Shuddha Ma (perfect 4th)
                }
            }
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

    companion object {
        fun getClosestSwara(frequency: Float, scale: String = "C", octave: String = "MID"): Swara {
            if (frequency <= 0f) return SA
            return values().minByOrNull { abs(it.getFrequencyForScaleAndOctave(scale, octave) - frequency) } ?: SA
        }
    }
}
