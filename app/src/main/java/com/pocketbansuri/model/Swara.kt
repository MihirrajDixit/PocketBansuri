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
     * Gets the fingering list for a given Sa hole position (1 to 7) and octave.
     */
    fun getFingeringForSaHole(saHole: Int): List<Float> {
        return when (saHole) {
            3 -> this.baseFingering // Standard: Sa=3, Re=2, Ga=1, Ma=0.5, Pa=6, Dha=5, Ni=4
            4 -> when (this) {
                // B scale on C flute: Sa=4, Re=3, Ga=2, Ma=1, Pa=7, Dha=6, Ni=5
                SA -> listOf(1f, 1f, 1f, 1f, 0f, 0f, 0f)
                RE -> listOf(1f, 1f, 1f, 0f, 0f, 0f, 0f)
                GA -> listOf(1f, 1f, 0f, 0f, 0f, 0f, 0f)
                MA -> listOf(1f, 0f, 0f, 0f, 0f, 0f, 0f)
                PA -> listOf(1f, 1f, 1f, 1f, 1f, 1f, 1f)
                DHA -> listOf(1f, 1f, 1f, 1f, 1f, 1f, 0f)
                NI -> listOf(1f, 1f, 1f, 1f, 1f, 0f, 0f)
                HIGH_SA -> listOf(1f, 1f, 1f, 0.5f, 0f, 0f, 0f)
            }
            5 -> when (this) {
                // A scale on C flute: Sa=5, Re=4, Ga=3, Ma=2, Pa=1, Dha=7, Ni=6
                SA -> listOf(1f, 1f, 1f, 1f, 1f, 0f, 0f)
                RE -> listOf(1f, 1f, 1f, 1f, 0f, 0f, 0f)
                GA -> listOf(1f, 1f, 1f, 0f, 0f, 0f, 0f)
                MA -> listOf(1f, 1f, 0f, 0f, 0f, 0f, 0f)
                PA -> listOf(1f, 0f, 0f, 0f, 0f, 0f, 0f)
                DHA -> listOf(1f, 1f, 1f, 1f, 1f, 1f, 1f)
                NI -> listOf(1f, 1f, 1f, 1f, 1f, 1f, 0f)
                HIGH_SA -> listOf(1f, 1f, 1f, 1f, 0.5f, 0f, 0f)
            }
            6 -> when (this) {
                // G scale on C flute: Sa=6, Re=5, Ga=4, Ma=3, Pa=2, Dha=1, Ni=0
                SA -> listOf(1f, 1f, 1f, 1f, 1f, 1f, 0f)
                RE -> listOf(1f, 1f, 1f, 1f, 1f, 0f, 0f)
                GA -> listOf(1f, 1f, 1f, 1f, 0f, 0f, 0f)
                MA -> listOf(1f, 1f, 1f, 0f, 0f, 0f, 0f)
                PA -> listOf(1f, 1f, 0f, 0f, 0f, 0f, 0f)
                DHA -> listOf(1f, 0f, 0f, 0f, 0f, 0f, 0f)
                NI -> listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f)
                HIGH_SA -> listOf(1f, 1f, 1f, 1f, 1f, 0.5f, 0f)
            }
            7 -> when (this) {
                // F# scale on C flute: Sa=7, Re=6, Ga=5, Ma=4, Pa=3, Dha=2, Ni=1
                SA -> listOf(1f, 1f, 1f, 1f, 1f, 1f, 1f)
                RE -> listOf(1f, 1f, 1f, 1f, 1f, 1f, 0f)
                GA -> listOf(1f, 1f, 1f, 1f, 1f, 0f, 0f)
                MA -> listOf(1f, 1f, 1f, 1f, 0f, 0f, 0f)
                PA -> listOf(1f, 1f, 1f, 0f, 0f, 0f, 0f)
                DHA -> listOf(1f, 1f, 0f, 0f, 0f, 0f, 0f)
                NI -> listOf(1f, 0f, 0f, 0f, 0f, 0f, 0f)
                HIGH_SA -> listOf(1f, 1f, 1f, 1f, 1f, 1f, 0.5f)
            }
            2 -> when (this) {
                // D scale on C flute: Sa=2, Re=1, Ga=0.5, Ma=0, Pa=5, Dha=4, Ni=3
                SA -> listOf(1f, 1f, 0f, 0f, 0f, 0f, 0f)
                RE -> listOf(1f, 0f, 0f, 0f, 0f, 0f, 0f)
                GA -> listOf(0.5f, 0f, 0f, 0f, 0f, 0f, 0f)
                MA -> listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f)
                PA -> listOf(1f, 1f, 1f, 1f, 1f, 0f, 0f)
                DHA -> listOf(1f, 1f, 1f, 1f, 0f, 0f, 0f)
                NI -> listOf(1f, 1f, 1f, 0f, 0f, 0f, 0f)
                HIGH_SA -> listOf(1f, 0.5f, 0f, 0f, 0f, 0f, 0f)
            }
            1 -> when (this) {
                // E scale on C flute: Sa=1, Re=0.5, Ga=0, Ma=6, Pa=4, Dha=3, Ni=2
                SA -> listOf(1f, 0f, 0f, 0f, 0f, 0f, 0f)
                RE -> listOf(0.5f, 0f, 0f, 0f, 0f, 0f, 0f)
                GA -> listOf(0f, 0f, 0f, 0f, 0f, 0f, 0f)
                MA -> listOf(1f, 1f, 1f, 1f, 1f, 1f, 0f)
                PA -> listOf(1f, 1f, 1f, 1f, 0f, 0f, 0f)
                DHA -> listOf(1f, 1f, 1f, 0f, 0f, 0f, 0f)
                NI -> listOf(1f, 1f, 0f, 0f, 0f, 0f, 0f)
                HIGH_SA -> listOf(0.5f, 0f, 0f, 0f, 0f, 0f, 0f)
            }
            else -> this.baseFingering
        }
    }

    /**
     * Gets the fingering list for a given octave and Sa hole position.
     */
    fun getFingeringForOctave(@Suppress("UNUSED_PARAMETER") octave: String, saHole: Int = 3): List<Float> {
        return this.getFingeringForSaHole(saHole)
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

object FluteScaleHelper {
    val CHROMATIC_SCALES = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")

    /**
     * Semitone offsets from the flute's nominal scale (where Hole 3 = 0):
     * Hole 1: +4 semitones (Ga of flute, e.g. E on C flute)
     * Hole 2: +2 semitones (Re of flute, e.g. D on C flute)
     * Hole 3: 0 semitones (Sa of flute - default, e.g. C on C flute)
     * Hole 4: -1 semitone (Ni of flute, e.g. B on C flute)
     * Hole 5: -3 semitones (Dha of flute, e.g. A on C flute)
     * Hole 6: -5 semitones (Pa of flute, e.g. G on C flute)
     * Hole 7: -6 semitones (Tivra Ma of flute, e.g. F# on C flute)
     */
    fun getSemitoneOffsetForHole(hole: Int): Int {
        return when (hole) {
            1 -> 4
            2 -> 2
            3 -> 0
            4 -> -1
            5 -> -3
            6 -> -5
            7 -> -6
            else -> 0
        }
    }

    /**
     * Calculates the scale produced when Hole [hole] is used to define Sa on a flute of [fluteScale].
     */
    fun getScaleForHole(fluteScale: String, hole: Int): String {
        val rootIdx = CHROMATIC_SCALES.indexOf(fluteScale.uppercase()).let { if (it == -1) 0 else it }
        val offset = getSemitoneOffsetForHole(hole)
        val noteIdx = (rootIdx + offset).mod(12)
        return CHROMATIC_SCALES[noteIdx]
    }

    /**
     * Finds which hole on a flute of [fluteScale] gives [targetScale], or defaults to 3.
     */
    fun getHoleForScale(fluteScale: String, targetScale: String): Int {
        for (h in 1..7) {
            if (getScaleForHole(fluteScale, h).equals(targetScale, ignoreCase = true)) {
                return h
            }
        }
        return 3
    }
}
