package com.pocketbansuri.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TunerScreen(
    selectedSwara: Swara,
    onSwaraSelected: (Swara) -> Unit,
    selectedScale: String = "C",
    selectedOctave: String = "MID",
    modifier: Modifier = Modifier
) {
    var detectedFrequency by remember { mutableStateOf(0.0f) }
    var referencePlayingSwara by remember { mutableStateOf<Swara?>(null) }
    
    // Start and stop the native audio engine
    DisposableEffect(Unit) {
        AudioEngine.startEngine()
        onDispose {
            AudioEngine.stopReferenceNote()
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

    val closestSwara = Swara.getClosestSwara(detectedFrequency, selectedScale, selectedOctave)
    val targetFrequency = selectedSwara.getFrequencyForScaleAndOctave(selectedScale, selectedOctave)
    
    // Calculate difference in Hz
    // If frequency is 0 (silence), diff is 0
    val freqDiff = if (detectedFrequency > 20f) detectedFrequency - targetFrequency else 0f
    
    // Tuning state
    // We define "in-tune" as within 3 Hz of target
    val inTuneThreshold = 3.0f
    val isSilence = detectedFrequency < 20f
    val tuningColor = when {
        isSilence -> TextSecondary
        abs(freqDiff) <= inTuneThreshold -> AccentGreen
        freqDiff < -inTuneThreshold -> PitchFlat
        else -> PitchSharp
    }

    val statusText = when {
        isSilence -> "Play a note or reference"
        abs(freqDiff) <= inTuneThreshold -> "In Tune! Perfect"
        freqDiff < -inTuneThreshold -> "Flat (Too Low)"
        else -> "Sharp (Too High)"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. Tuner Info and Gauge
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(bottom = 12.dp),
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
                Text(
                    text = "TARGET SWARA: ${selectedSwara.displayName} (${selectedSwara.hindiName}) @ ${String.format("%.2f", targetFrequency)} Hz",
                    color = BambooGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Detected Note & Freq Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isSilence) "--" else closestSwara.displayName,
                        fontSize = 54.sp,
                        fontWeight = FontWeight.Black,
                        color = tuningColor
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isSilence) "" else "(${closestSwara.hindiName})",
                        fontSize = 24.sp,
                        color = tuningColor.copy(alpha = 0.8f)
                    )
                }

                Text(
                    text = if (isSilence) "0.00 Hz" else String.format("%.2f Hz", detectedFrequency),
                    fontSize = 18.sp,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Linear Tuning Meter/Gauge
                TuningMeter(
                    freqDiff = freqDiff,
                    maxDeviationHz = 15f,
                    tuningColor = tuningColor,
                    isSilence = isSilence
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = statusText,
                    fontSize = 14.sp,
                    color = tuningColor,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // 2. Swara Row Controls
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Tap to set target. Long-press to toggle Reference Sound.",
                color = TextSecondary,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 8.dp),
                textAlign = TextAlign.Center
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Swara.values().forEach { swara ->
                    val isSelected = swara == selectedSwara
                    val isRefPlaying = swara == referencePlayingSwara

                    val buttonBgColor = when {
                        isRefPlaying -> ForestLight
                        isSelected -> BambooGold
                        else -> SurfaceDark
                    }
                    val buttonTextColor = when {
                        isRefPlaying || isSelected -> DeepBackground
                        else -> TextPrimary
                    }
                    
                    val borderColor = when {
                        isRefPlaying -> ForestLight
                        isSelected -> BambooGold
                        else -> CardBorder
                    }

                    Box(
                        modifier = Modifier
                            .size(width = 54.dp, height = 54.dp)
                            .clip(CircleShape)
                            .background(buttonBgColor)
                            .border(1.dp, borderColor, CircleShape)
                            .combinedClickable(
                                onClick = {
                                    onSwaraSelected(swara)
                                },
                                onLongClick = {
                                    if (referencePlayingSwara == swara) {
                                        AudioEngine.stopReferenceNote()
                                        referencePlayingSwara = null
                                    } else {
                                        AudioEngine.stopReferenceNote()
                                        AudioEngine.playReferenceNote(swara.getMidiNoteForScaleAndOctave(selectedScale, selectedOctave))
                                        referencePlayingSwara = swara
                                        onSwaraSelected(swara)
                                    }
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = swara.hindiName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = buttonTextColor
                            )
                            Text(
                                text = swara.displayName,
                                fontSize = 10.sp,
                                color = buttonTextColor.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
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
            // Left Tick (-15Hz), Center, Right (+15Hz)
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
