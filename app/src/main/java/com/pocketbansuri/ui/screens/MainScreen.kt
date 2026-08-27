package com.pocketbansuri.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.style.TextAlign
import com.pocketbansuri.AudioEngine
import com.pocketbansuri.model.Raga
import com.pocketbansuri.model.Swara
import com.pocketbansuri.ui.components.BansuriVisualizer
import com.pocketbansuri.ui.theme.*

enum class AppTab(val title: String) {
    RIYAAZ("Riyaaz"),
    TUNER("Tuner")
}

@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    var activeTab by remember { mutableStateOf(AppTab.RIYAAZ) }
    var selectedSwara by remember { mutableStateOf(Swara.SA) }
    var selectedRaga by remember { mutableStateOf(Raga.dummyRagas.first()) }
    
    // Config states initialized to null so user is forced to select them before playing sound
    var selectedScale by remember { mutableStateOf<String?>(null) }
    var selectedOctave by remember { mutableStateOf<String?>(null) }
    var selectedTimer by remember { mutableStateOf<Int?>(null) }

    // Lifted playback state to synchronize highlighting between table and visualizer
    var playingSwara by remember { mutableStateOf<Swara?>(null) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DeepBackground)
    ) {
        // Custom Compact Title Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
                .background(DeepBackground)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "POCKET BANSURI",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = BambooGold,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Riyaaz & Tuner",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }

            // Segmented Tab Switcher
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(SurfaceDark)
                    .border(1.dp, CardBorder, RoundedCornerShape(6.dp))
            ) {
                AppTab.values().forEach { tab ->
                    val isSelected = activeTab == tab
                    Box(
                        modifier = Modifier
                            .padding(1.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (isSelected) ForestLight else Color.Transparent)
                            .clickable {
                                activeTab = tab
                                AudioEngine.stopReferenceNote()
                                playingSwara = null
                            }
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.title,
                            color = if (isSelected) DeepBackground else TextPrimary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Horizontal Divider
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(CardBorder)
        )

        // Main Content Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(DeepBackground),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ================= SECTION A (15%): Flute Visualizer =================
            Box(
                modifier = Modifier
                    .weight(0.15f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                BansuriVisualizer(
                    activeSwara = selectedSwara,
                    selectedOctave = selectedOctave ?: "Mid",
                    isPlaying = playingSwara == selectedSwara, // Highlight holes when note is active
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Vertical divider
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(CardBorder)
            )

            // ================= SECTION B (10%): Global Control Config Panel =================
            Box(
                modifier = Modifier
                    .weight(0.10f)
                    .fillMaxHeight()
                    .background(SurfaceDark.copy(alpha = 0.5f))
                    .padding(horizontal = 6.dp, vertical = 8.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "CONFIG",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BambooGold,
                        letterSpacing = 1.sp
                    )

                    // 1. Scale Dropdown
                    var scaleExpanded by remember { mutableStateOf(false) }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("SCALE", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = TextSecondary, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(28.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SurfaceDark)
                                    .border(1.dp, CardBorder, RoundedCornerShape(4.dp))
                                    .clickable { scaleExpanded = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = selectedScale ?: "-",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedScale != null) TextPrimary else TextSecondary
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("▾", fontSize = 8.sp, color = BambooGold)
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
                                            selectedScale = scale
                                            scaleExpanded = false
                                            AudioEngine.stopReferenceNote()
                                            playingSwara = null
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 2. Octave Dropdown (Saptak equivalent)
                    var octaveExpanded by remember { mutableStateOf(false) }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("OCTAVE (SAPTAK)", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = TextSecondary, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            val shortOctaveLabel = when (selectedOctave) {
                                "Low" -> "Low (Mandra)"
                                "Mid" -> "Mid (Madhya)"
                                "High" -> "High (Taar)"
                                "V.High" -> "V.High (Ati Taar)"
                                else -> "-"
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(28.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SurfaceDark)
                                    .border(1.dp, CardBorder, RoundedCornerShape(4.dp))
                                    .clickable { octaveExpanded = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.padding(horizontal = 2.dp)
                                ) {
                                    Text(
                                        text = shortOctaveLabel,
                                        fontSize = 8.sp, // Small font to fit text cleanly
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedOctave != null) TextPrimary else TextSecondary
                                    )
                                    Spacer(modifier = Modifier.width(1.dp))
                                    Text("▾", fontSize = 8.sp, color = ForestLight)
                                }
                            }
                            DropdownMenu(
                                expanded = octaveExpanded,
                                onDismissRequest = { octaveExpanded = false },
                                modifier = Modifier
                                    .background(SurfaceDark)
                                    .border(1.dp, CardBorder, RoundedCornerShape(4.dp))
                            ) {
                                val octavePairs = listOf(
                                    "Low" to "Low (Mandra)",
                                    "Mid" to "Mid (Madhya)",
                                    "High" to "High (Taar)",
                                    "V.High" to "V.High (Ati Taar)"
                                )
                                octavePairs.forEach { pair ->
                                    DropdownMenuItem(
                                        text = { Text(pair.second, color = TextPrimary, fontSize = 11.sp) },
                                        onClick = {
                                            selectedOctave = pair.first
                                            octaveExpanded = false
                                            AudioEngine.stopReferenceNote()
                                            playingSwara = null
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 3. Timer Dropdown
                    var timerExpanded by remember { mutableStateOf(false) }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("TIMER", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = TextSecondary, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(2.dp))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(28.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(SurfaceDark)
                                    .border(1.dp, CardBorder, RoundedCornerShape(4.dp))
                                    .clickable { timerExpanded = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = if (selectedTimer != null) "${selectedTimer}s" else "-",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedTimer != null) TextPrimary else TextSecondary
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("▾", fontSize = 8.sp, color = Color(0xFFC88C3C))
                                }
                            }
                            DropdownMenu(
                                expanded = timerExpanded,
                                onDismissRequest = { timerExpanded = false },
                                modifier = Modifier
                                    .background(SurfaceDark)
                                    .border(1.dp, CardBorder, RoundedCornerShape(4.dp))
                            ) {
                                listOf(1, 5, 10, 15, 30, 60, 120).forEach { timer ->
                                    DropdownMenuItem(
                                        text = { Text("${timer}s", color = TextPrimary, fontSize = 11.sp) },
                                        onClick = {
                                            selectedTimer = timer
                                            timerExpanded = false
                                            AudioEngine.stopReferenceNote()
                                            playingSwara = null
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Vertical divider
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(1.dp)
                    .background(CardBorder)
            )

            // ================= SECTION C (75%): Practice Station Area =================
            Box(
                modifier = Modifier
                    .weight(0.75f)
                    .fillMaxHeight()
            ) {
                when (activeTab) {
                    AppTab.RIYAAZ -> {
                        RiyaazScreen(
                            selectedRaga = selectedRaga,
                            onRagaSelected = { selectedRaga = it },
                            onSwaraSelected = { selectedSwara = it },
                            activeSwara = selectedSwara,
                            selectedScale = selectedScale,
                            selectedOctave = selectedOctave,
                            selectedTimer = selectedTimer,
                            playingSwara = playingSwara,
                            onPlayingSwaraChanged = { playingSwara = it }
                        )
                    }
                    AppTab.TUNER -> {
                        TunerScreen(
                            selectedSwara = selectedSwara,
                            onSwaraSelected = { selectedSwara = it },
                            selectedScale = selectedScale ?: "C",
                            selectedOctave = selectedOctave ?: "Mid"
                        )
                    }
                }
            }
        }
    }
}
