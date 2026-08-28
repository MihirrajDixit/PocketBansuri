package com.pocketbansuri.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketbansuri.AudioEngine
import com.pocketbansuri.model.Swara
import com.pocketbansuri.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.*

data class ChromaticNote(
    val sargamName: String,
    val hindiName: String,
    val westernName: String,
    val octaveName: String,
    val targetFrequency: Float
)

fun getClosestChromaticNote(frequency: Float, scale: String): ChromaticNote {
    if (frequency <= 20f) {
        return ChromaticNote("--", "", "--", "MID", 0f)
    }
    // Calculate fractional MIDI note number
    val midi = 69.0 + 12.0 * log2(frequency.toDouble() / 440.0)
    val closestMidi = round(midi).toInt()
    
    val noteNames = listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B")
    val westernName = noteNames[((closestMidi % 12) + 12) % 12] + ((closestMidi / 12) - 1)
    
    val scaleOffset = when (scale.uppercase()) {
        "C" -> 0
        "C#" -> 1
        "D" -> 2
        "D#" -> 3
        "E" -> 4
        "F" -> 5
        "F#" -> 6
        "G" -> 7
        "G#" -> 8
        "A" -> 9
        "A#" -> 10
        "B" -> 11
        else -> 0
    }
    
    val relativeMidi = closestMidi - (60 + scaleOffset)
    val octaveInt = floor(relativeMidi.toDouble() / 12.0).toInt()
    val octaveName = when (octaveInt) {
        -1 -> "LOW"
        0 -> "MID"
        1 -> "HIGH"
        2 -> "V.HIGH"
        else -> if (octaveInt < -1) "LOW" else "V.HIGH"
    }
    
    val semitone = ((relativeMidi % 12) + 12) % 12
    val sargamName = when (semitone) {
        0 -> "Sa"
        1 -> "re"
        2 -> "Re"
        3 -> "ga"
        4 -> "Ga"
        5 -> "Ma"
        6 -> "ma'"
        7 -> "Pa"
        8 -> "dha"
        9 -> "Dha"
        10 -> "ni"
        11 -> "Ni"
        else -> "Sa"
    }
    
    val hindiName = when (semitone) {
        0 -> "सा"
        1 -> "रे॒"
        2 -> "रे"
        3 -> "ग॒"
        4 -> "ग"
        5 -> "म"
        6 -> "मॅ"
        7 -> "प"
        8 -> "ध॒"
        9 -> "ध"
        10 -> "नि॒"
        11 -> "नि"
        else -> "सा"
    }
    
    val targetFreq = 440.0 * Math.pow(2.0, (closestMidi - 69).toDouble() / 12.0)
    return ChromaticNote(sargamName, hindiName, westernName, octaveName, targetFreq.toFloat())
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TunerScreen(
    selectedScale: String,
    onScaleChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var detectedFrequency by remember { mutableStateOf(0.0f) }
    
    // Start and stop the native audio engine
    DisposableEffect(Unit) {
        AudioEngine.startEngine()
        onDispose {
            AudioEngine.stopEngine()
        }
    }

    // Poll the engine for the live detected frequency
    LaunchedEffect(Unit) {
        while (true) {
            detectedFrequency = AudioEngine.getDetectedFrequency()
            delay(50) // Poll every 50ms (20fps, very smooth)
        }
    }

    val closestChromaticNote = getClosestChromaticNote(detectedFrequency, selectedScale)
    
    // Auto-detect which Shuddh note is the target (what I wish to play)
    val closestShuddhResult = Swara.getClosestSwaraAndOctave(detectedFrequency, selectedScale)
    val targetSwara = closestShuddhResult.first
    val targetOctave = closestShuddhResult.second
    val targetFrequency = targetSwara.getFrequencyForScaleAndOctave(selectedScale, targetOctave)
    
    // Calculate difference in Hz relative to the auto-detected target note
    val freqDiff = if (detectedFrequency > 20f) detectedFrequency - targetFrequency else 0f
    
    // Tuning state
    // We define "in-tune" as within 3 Hz of target
    val inTuneThreshold = 3.0f
    val isSilence = detectedFrequency < 20f
    val isPerfect = abs(freqDiff) <= inTuneThreshold

    val tuningColor = when {
        isSilence -> TextSecondary
        isPerfect -> AccentGreen
        freqDiff < -inTuneThreshold -> PitchFlat
        else -> PitchSharp
    }

    val octaveDisplay = when (closestChromaticNote.octaveName.uppercase()) {
        "LOW" -> "Low"
        "MID" -> "Mid"
        "HIGH" -> "High"
        "V.HIGH" -> "V.High"
        else -> closestChromaticNote.octaveName
    }

    val statusText = when {
        isSilence -> "Play a note on your bansuri"
        isPerfect && closestChromaticNote.sargamName.uppercase() == targetSwara.displayName.uppercase() -> 
            "Perfect! In Tune & Correct Note"
        isPerfect -> 
            "In Tune! (Aiming for ${targetSwara.displayName}, played false note: ${closestChromaticNote.sargamName})"
        closestChromaticNote.sargamName.uppercase() == targetSwara.displayName.uppercase() -> 
            if (freqDiff < -inTuneThreshold) "Flat (Too Low)" else "Sharp (Too High)"
        else -> 
            "Playing false note: ${closestChromaticNote.sargamName} instead of shuddh ${targetSwara.displayName}"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 0. Header with configuration dropdown (Scale only)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Auto Tuner",
                color = BambooGold,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Scale Dropdown
                var scaleExpanded by remember { mutableStateOf(false) }
                Box {
                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(SurfaceDark)
                            .border(1.dp, CardBorder, RoundedCornerShape(4.dp))
                            .clickable { scaleExpanded = true }
                            .padding(horizontal = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Scale: $selectedScale",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("▾", fontSize = 9.sp, color = BambooGold)
                        }
                    }
                    DropdownMenu(
                        expanded = scaleExpanded,
                        onDismissRequest = { scaleExpanded = false },
                        modifier = Modifier
                            .background(SurfaceDark)
                            .border(1.dp, CardBorder, RoundedCornerShape(4.dp))
                    ) {
                        listOf("C", "C#", "D", "D#", "E", "F", "F#", "G", "G#", "A", "A#", "B").forEach { scale ->
                            DropdownMenuItem(
                                text = { Text(scale, color = TextPrimary, fontSize = 11.sp) },
                                onClick = {
                                    onScaleChanged(scale)
                                    scaleExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // 1. Tuner Info and Gauge (Dual target vs detected layout, taking full remaining space)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = BorderStroke(1.dp, CardBorder)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Comparative Note Display
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Wish to Play (Target) Column
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "WISH TO PLAY",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isSilence) "--" else targetSwara.displayName,
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Bold,
                                color = BambooGold
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isSilence) "" else "(${targetSwara.hindiName})",
                                fontSize = 20.sp,
                                color = BambooGold.copy(alpha = 0.8f)
                            )
                        }
                        val displayTargetOct = if (isSilence) "Mid" else octaveDisplay
                        Text(
                            text = if (isSilence) "--" else "${targetSwara.getWesternEquivalent(selectedScale, targetOctave)} ($displayTargetOct)",
                            fontSize = 16.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Vertical Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(70.dp)
                            .background(CardBorder)
                    )

                    // Actually Played (Detected) Column
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "ACTUALLY PLAYED",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (isSilence) "--" else closestChromaticNote.sargamName,
                                fontSize = 42.sp,
                                fontWeight = FontWeight.Bold,
                                color = tuningColor
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isSilence) "" else "(${closestChromaticNote.hindiName})",
                                fontSize = 20.sp,
                                color = tuningColor.copy(alpha = 0.8f)
                            )
                        }
                        Text(
                            text = if (isSilence) "--" else "${closestChromaticNote.westernName} ($octaveDisplay)",
                            fontSize = 16.sp,
                            color = TextPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = if (isSilence) "0.00 Hz" else String.format("%.2f Hz", detectedFrequency),
                    fontSize = 15.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Linear Tuning Meter/Gauge (shows deviation from Wish to Play target frequency)
                TuningMeter(
                    freqDiff = freqDiff,
                    maxDeviationHz = 20f,
                    tuningColor = tuningColor,
                    isSilence = isSilence
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = statusText,
                    fontSize = 14.sp,
                    color = tuningColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
fun TuningMeter(
    freqDiff: Float,
    maxDeviationHz: Float,
    tuningColor: Color,
    isSilence: Boolean,
    modifier: Modifier = Modifier
) {
    // Limit deviation for meter display
    val clampedDiff = freqDiff.coerceIn(-maxDeviationHz, maxDeviationHz)
    // Map -maxDeviationHz..maxDeviationHz to 0f..1f (center is 0.5f)
    val targetPosition = if (isSilence) 0.5f else 0.5f + (clampedDiff / (maxDeviationHz * 2))
    
    val animatedPosition by animateFloatAsState(
        targetValue = targetPosition,
        animationSpec = tween(100),
        label = "MeterPosition"
    )

    Column(
        modifier = modifier
            .fillMaxWidth(0.85f)
            .padding(vertical = 4.dp)
    ) {
        // Meter bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(CardBorder)
        ) {
            // Underlay ticks
            // Left Tick (-maxDeviationHz), Center, Right (+maxDeviationHz)
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(Color(0x44FFFFFF)))
                Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(Color(0xBBFFFFFF)))
                Box(modifier = Modifier.width(2.dp).fillMaxHeight().background(Color(0x44FFFFFF)))
            }

            // Target indicator dot
            if (!isSilence) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(0.04f)
                        .align(Alignment.CenterStart)
                        .absoluteOffset(x = (animatedPosition * 260).dp) // Responsive approximation
                        .clip(CircleShape)
                        .background(tuningColor)
                )
            }
        }

        // Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "Flat", fontSize = 10.sp, color = TextSecondary)
            Text(
                text = if (isSilence) "In Tune" else String.format("%+.1f Hz", freqDiff),
                fontSize = 11.sp,
                color = tuningColor,
                fontWeight = FontWeight.Bold
            )
            Text(text = "Sharp", fontSize = 10.sp, color = TextSecondary)
        }
    }
}
