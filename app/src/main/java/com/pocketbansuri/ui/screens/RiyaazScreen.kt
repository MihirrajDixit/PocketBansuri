package com.pocketbansuri.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pocketbansuri.AudioEngine
import com.pocketbansuri.model.Raga
import com.pocketbansuri.model.Swara
import com.pocketbansuri.ui.theme.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class RiyaazSection {
    PLAIN_SCALE,
    RAGA_LIBRARY
}

@Composable
fun RiyaazScreen(
    selectedRaga: Raga,
    onRagaSelected: (Raga) -> Unit,
    onSwaraSelected: (Swara) -> Unit,
    activeSwara: Swara,
    selectedScale: String?,
    selectedOctave: String?,
    selectedTimer: Int?,
    modifier: Modifier = Modifier
) {
    var activeSection by remember { mutableStateOf(RiyaazSection.PLAIN_SCALE) }
    
    // Manage coroutine job for timer-based playback
    val coroutineScope = rememberCoroutineScope()
    var playbackJob by remember { mutableStateOf<Job?>(null) }
    var playingSwara by remember { mutableStateOf<Swara?>(null) }

    val isConfigured = selectedScale != null && selectedOctave != null && selectedTimer != null

    DisposableEffect(Unit) {
        onDispose {
            playbackJob?.cancel()
            AudioEngine.stopReferenceNote()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(6.dp) // Reduced outer padding
    ) {
        // Section Switcher Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pill/Tab Switcher
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceDark)
                    .padding(1.dp)
            ) {
                RiyaazSection.values().forEach { section ->
                    val isSelected = activeSection == section
                    val label = when (section) {
                        RiyaazSection.PLAIN_SCALE -> "Plain Scale"
                        RiyaazSection.RAGA_LIBRARY -> "Raga Practice"
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSelected) ForestLight else Color.Transparent)
                            .clickable {
                                activeSection = section
                                playbackJob?.cancel()
                                AudioEngine.stopReferenceNote()
                                playingSwara = null
                            }
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) DeepBackground else TextPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Quick Status Indicator displaying config warning inside the switcher row
            Badge(
                containerColor = if (isConfigured) ForestLight.copy(alpha = 0.2f) else Color(0x33EA5454),
                contentColor = if (isConfigured) ForestLight else Color(0xFFEA5454)
            ) {
                Text(
                    text = if (isConfigured) "Tuner Ready" else "Configure scale, octave, timer",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }

        // Section Content
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (activeSection) {
                RiyaazSection.PLAIN_SCALE -> {
                    PlainScaleTable(
                        selectedScale = selectedScale,
                        selectedOctave = selectedOctave,
                        selectedTimer = selectedTimer,
                        activeSwara = activeSwara,
                        onSwaraSelected = onSwaraSelected,
                        playingSwara = playingSwara,
                        onPlaySwara = { swara ->
                            if (isConfigured) {
                                playbackJob?.cancel()
                                AudioEngine.stopReferenceNote()
                                
                                onSwaraSelected(swara)
                                playingSwara = swara
                                
                                val midi = swara.getMidiNoteForScaleAndOctave(selectedScale!!, selectedOctave!!)
                                AudioEngine.playReferenceNote(midi)
                                
                                playbackJob = coroutineScope.launch {
                                    delay(selectedTimer!! * 1000L)
                                    AudioEngine.stopReferenceNote()
                                    playingSwara = null
                                }
                            }
                        }
                    )
                }
                RiyaazSection.RAGA_LIBRARY -> {
                    RagaLibraryPractice(
                        selectedRaga = selectedRaga,
                        selectedScale = selectedScale,
                        selectedOctave = selectedOctave,
                        selectedTimer = selectedTimer,
                        activeSwara = activeSwara,
                        onRagaSelected = onRagaSelected,
                        onSwaraSelected = onSwaraSelected,
                        onPlaySwara = { swara ->
                            if (isConfigured) {
                                playbackJob?.cancel()
                                AudioEngine.stopReferenceNote()
                                
                                onSwaraSelected(swara)
                                val midi = swara.getMidiNoteForScaleAndOctave(selectedScale!!, selectedOctave!!)
                                AudioEngine.playReferenceNote(midi)
                                
                                playbackJob = coroutineScope.launch {
                                    delay(selectedTimer!! * 1000L)
                                    AudioEngine.stopReferenceNote()
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun PlainScaleTable(
    selectedScale: String?,
    selectedOctave: String?,
    selectedTimer: Int?,
    activeSwara: Swara,
    onSwaraSelected: (Swara) -> Unit,
    playingSwara: Swara?,
    onPlaySwara: (Swara) -> Unit
) {
    val swaras = Swara.values().take(7)
    val isConfigured = selectedScale != null && selectedOctave != null && selectedTimer != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark, RoundedCornerShape(8.dp))
            .padding(8.dp) // Increased padding
    ) {
        // Table Header (3 Columns: Swara, Frequency, Sound) - Spacious
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DeepBackground, RoundedCornerShape(6.dp))
                .padding(vertical = 6.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Swara", modifier = Modifier.weight(1f), color = BambooGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(text = "Frequency", modifier = Modifier.weight(1.2f), color = BambooGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(text = "Sound", modifier = Modifier.weight(1f), color = BambooGold, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Table Rows
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            swaras.forEach { swara ->
                val isSelected = swara == activeSwara
                val isPlaying = playingSwara == swara
                
                val frequency = if (isConfigured) {
                    swara.getFrequencyForScaleAndOctave(selectedScale!!, selectedOctave!!)
                } else {
                    null
                }
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            when {
                                isPlaying -> ForestLight.copy(alpha = 0.2f)
                                isSelected -> ForestLight.copy(alpha = 0.08f)
                                else -> Color.Transparent
                            }
                        )
                        .clickable(enabled = isConfigured) {
                            onPlaySwara(swara)
                        }
                        .padding(vertical = 5.dp, horizontal = 8.dp), // More spacious row height
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 1. Swara Name Column
                    Text(
                        text = "${swara.displayName} (${swara.hindiName})",
                        modifier = Modifier.weight(1f),
                        color = if (isConfigured) (if (isSelected) BambooGold else TextPrimary) else TextSecondary.copy(alpha = 0.35f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // 2. Frequency Column
                    Text(
                        text = if (frequency != null) String.format("%.2f Hz", frequency) else "-- Hz",
                        modifier = Modifier.weight(1.2f),
                        color = if (isConfigured) TextPrimary else TextSecondary.copy(alpha = 0.35f),
                        fontSize = 11.sp
                    )
                    
                    // 3. Play Button Column (Super compact button height)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .wrapContentWidth(Alignment.Start)
                    ) {
                        when {
                            isPlaying -> {
                                Text(
                                    text = "Playing",
                                    color = ForestLight,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(ForestLight.copy(alpha = 0.15f))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            isConfigured -> {
                                Text(
                                    text = "Play (${selectedTimer}s)",
                                    color = BambooGold,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(CardBorder)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            else -> {
                                Text(
                                    text = "Locked 🔒",
                                    color = TextSecondary.copy(alpha = 0.35f),
                                    fontSize = 8.sp,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(SurfaceDark.copy(alpha = 0.2f))
                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RagaLibraryPractice(
    selectedRaga: Raga,
    selectedScale: String?,
    selectedOctave: String?,
    selectedTimer: Int?,
    activeSwara: Swara,
    onRagaSelected: (Raga) -> Unit,
    onSwaraSelected: (Swara) -> Unit,
    onPlaySwara: (Swara) -> Unit
) {
    val ragas = Raga.dummyRagas
    val isConfigured = selectedScale != null && selectedOctave != null && selectedTimer != null

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(ragas) { raga ->
            val isSelected = raga.name == selectedRaga.name

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        onRagaSelected(raga)
                        AudioEngine.stopReferenceNote()
                    },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) SurfaceDark else DeepBackground
                ),
                border = BorderStroke(
                    width = 1.dp,
                    color = if (isSelected) BambooGold else CardBorder
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = raga.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) BambooGold else TextPrimary
                            )
                            Text(
                                text = "Vadi: ${raga.vadi} | Samvadi: ${raga.samvadi}",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }

                        if (isSelected && isConfigured) {
                            Badge(
                                containerColor = ForestLight,
                                contentColor = TextPrimary
                            ) {
                                Text(
                                    text = "Scale: $selectedScale | $selectedOctave",
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontWeight = FontWeight.Bold,
                                    color = DeepBackground
                                )
                            }
                        }
                    }

                    AnimatedVisibility(visible = isSelected) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                        ) {
                            Text(
                                text = raga.description,
                                fontSize = 12.sp,
                                color = TextSecondary,
                                lineHeight = 16.sp
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Aaroh (Ascent)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BambooGold
                                    )
                                    Text(
                                        text = raga.aaroh,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Avroh (Descent)",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BambooGold
                                    )
                                    Text(
                                        text = raga.avroh,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = if (isConfigured) {
                                    "Practice Notes in $selectedScale ($selectedOctave) (Tap to hear for ${selectedTimer}s):"
                                } else {
                                    "Practice Notes (Configure scale/octave/timer in B to play):"
                                },
                                fontSize = 10.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                raga.swarasUsed.forEach { swara ->
                                    val isSwaraActive = swara == activeSwara
                                    val swaraBg = if (isSwaraActive && isConfigured) ForestLight else CardBorder
                                    val swaraText = if (isSwaraActive && isConfigured) DeepBackground else TextPrimary

                                    Box(
                                        modifier = Modifier
                                            .size(width = 56.dp, height = 38.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(swaraBg)
                                            .clickable(enabled = isConfigured) {
                                                onPlaySwara(swara)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = swara.displayName,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = swaraText
                                            )
                                            if (isConfigured) {
                                                Text(
                                                    text = String.format("%.0fHz", swara.getFrequencyForScaleAndOctave(selectedScale!!, selectedOctave!!)),
                                                    fontSize = 8.sp,
                                                    color = if (isSwaraActive) DeepBackground.copy(alpha = 0.7f) else TextSecondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
