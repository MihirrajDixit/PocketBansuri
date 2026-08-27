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
import com.pocketbansuri.AudioEngine
import com.pocketbansuri.model.Raga
import com.pocketbansuri.model.Swara
import com.pocketbansuri.ui.components.BansuriVisualizer
import com.pocketbansuri.ui.theme.*

enum class AppTab(val title: String) {
    RIYAAZ("Riyaaz"),
    TUNER("Tuner")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(modifier: Modifier = Modifier) {
    var activeTab by remember { mutableStateOf(AppTab.RIYAAZ) }
    var selectedSwara by remember { mutableStateOf(Swara.SA) }
    var selectedRaga by remember { mutableStateOf(Raga.dummyRagas.first()) }
    
    // Config states initialized to null so user is forced to select them before playing sound
    var selectedScale by remember { mutableStateOf<String?>(null) }
    var selectedOctave by remember { mutableStateOf<String?>(null) }
    var selectedTimer by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "POCKET BANSURI",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = BambooGold,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "Riyaaz & Tuner Companion",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                actions = {
                    // Segmented Tab Switcher
                    Row(
                        modifier = Modifier
                            .padding(end = 16.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(SurfaceDark)
                            .border(1.dp, CardBorder, RoundedCornerShape(8.dp))
                    ) {
                        AppTab.values().forEach { tab ->
                            val isSelected = activeTab == tab
                            Box(
                                modifier = Modifier
                                    .padding(2.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) ForestLight else Color.Transparent)
                                    .clickable {
                                        activeTab = tab
                                        AudioEngine.stopReferenceNote()
                                    }
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = tab.title,
                                    color = if (isSelected) DeepBackground else TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DeepBackground,
                    titleContentColor = TextPrimary
                )
            )
        },
        containerColor = DeepBackground
    ) { innerPadding ->
        Row(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DeepBackground),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ================= SECTION A (20%): Flute Visualizer =================
            Box(
                modifier = Modifier
                    .weight(0.20f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    BansuriVisualizer(
                        activeSwara = selectedSwara,
                        selectedOctave = selectedOctave ?: "Mid",
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "Note: ${selectedSwara.displayName} (${selectedSwara.hindiName})",
                        color = BambooGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Text(
                        text = if (selectedScale != null && selectedOctave != null) {
                            "Freq: ${String.format("%.1f", selectedSwara.getFrequencyForScaleAndOctave(selectedScale!!, selectedOctave!!))} Hz"
                        } else {
                            "Freq: -- Hz"
                        },
                        color = TextSecondary,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }

            // Vertical divider separating Visualizer (A) from Controls (B)
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.9f)
                    .width(1.dp)
                    .background(CardBorder)
            )

            // ================= SECTION B (10%): Global Control Config Panel (Dropdowns, Non-scrollable) =================
            Box(
                modifier = Modifier
                    .weight(0.10f)
                    .fillMaxHeight()
                    .background(SurfaceDark.copy(alpha = 0.5f))
                    .padding(horizontal = 6.dp, vertical = 12.dp),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "CONFIG",
                        fontSize = 11.sp,
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
                        Text("SCALE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(30.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SurfaceDark)
                                    .border(1.dp, CardBorder, RoundedCornerShape(6.dp))
                                    .clickable { scaleExpanded = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = selectedScale ?: "-",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedScale != null) TextPrimary else TextSecondary
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
                                    .border(1.dp, CardBorder, RoundedCornerShape(6.dp))
                            ) {
                                listOf("C", "D", "E", "F", "G", "A", "B").forEach { scale ->
                                    DropdownMenuItem(
                                        text = { Text(scale, color = TextPrimary, fontSize = 12.sp) },
                                        onClick = {
                                            selectedScale = scale
                                            scaleExpanded = false
                                            AudioEngine.stopReferenceNote()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // 2. Octave Dropdown
                    var octaveExpanded by remember { mutableStateOf(false) }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("OCTAVE", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(30.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SurfaceDark)
                                    .border(1.dp, CardBorder, RoundedCornerShape(6.dp))
                                    .clickable { octaveExpanded = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = selectedOctave ?: "-",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedOctave != null) TextPrimary else TextSecondary
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("▾", fontSize = 9.sp, color = ForestLight)
                                }
                            }
                            DropdownMenu(
                                expanded = octaveExpanded,
                                onDismissRequest = { octaveExpanded = false },
                                modifier = Modifier
                                    .background(SurfaceDark)
                                    .border(1.dp, CardBorder, RoundedCornerShape(6.dp))
                            ) {
                                listOf("Low", "Mid", "High", "Max").forEach { octave ->
                                    DropdownMenuItem(
                                        text = { Text(octave, color = TextPrimary, fontSize = 12.sp) },
                                        onClick = {
                                            selectedOctave = octave
                                            octaveExpanded = false
                                            AudioEngine.stopReferenceNote()
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
                        Text("TIMER", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(30.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(SurfaceDark)
                                    .border(1.dp, CardBorder, RoundedCornerShape(6.dp))
                                    .clickable { timerExpanded = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = if (selectedTimer != null) "${selectedTimer}s" else "-",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selectedTimer != null) TextPrimary else TextSecondary
                                    )
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("▾", fontSize = 9.sp, color = Color(0xFFC88C3C))
                                }
                            }
                            DropdownMenu(
                                expanded = timerExpanded,
                                onDismissRequest = { timerExpanded = false },
                                modifier = Modifier
                                    .background(SurfaceDark)
                                    .border(1.dp, CardBorder, RoundedCornerShape(6.dp))
                            ) {
                                listOf(1, 5, 10, 15, 30, 60, 120).forEach { timer ->
                                    DropdownMenuItem(
                                        text = { Text("${timer}s", color = TextPrimary, fontSize = 12.sp) },
                                        onClick = {
                                            selectedTimer = timer
                                            timerExpanded = false
                                            AudioEngine.stopReferenceNote()
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Vertical divider separating Controls (B) from Practice Area (C)
            Box(
                modifier = Modifier
                    .fillMaxHeight(0.9f)
                    .width(1.dp)
                    .background(CardBorder)
            )

            // ================= SECTION C (70%): Practice Station Area =================
            Box(
                modifier = Modifier
                    .weight(0.70f)
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
                            selectedTimer = selectedTimer
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
