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
import com.pocketbansuri.model.FluteScaleHelper
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
    var saHole by remember { mutableStateOf(3) } // 1 to 7 hole defining Sa (default 3)
    var selectedOctave by remember { mutableStateOf<String?>(null) }
    var selectedTimer by remember { mutableStateOf<Int?>(null) }

    val fluteScale = selectedScale ?: "C"
    val effectiveScale = if (selectedScale != null) FluteScaleHelper.getScaleForHole(selectedScale!!, saHole) else null

    // Lifted playback state to synchronize highlighting between table and visualizer
    var playingSwara by remember { mutableStateOf<Swara?>(null) }
    var playingOctave by remember { mutableStateOf<String?>(null) }

    // Lifted mic pitch detection state to synchronize visualizer highlighting in Practice mode
    var detectedSwara by remember { mutableStateOf<Swara?>(null) }
    var detectedOctave by remember { mutableStateOf<String?>(null) }

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
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = BambooGold,
                    letterSpacing = 1.sp
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
                                playingOctave = null
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
            // ================= SECTION A (18%): Flute Visualizer =================
            Box(
                modifier = Modifier
                    .weight(0.18f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                BansuriVisualizer(
                    activeSwara = playingSwara ?: detectedSwara ?: selectedSwara,
                    selectedOctave = playingOctave ?: detectedOctave ?: selectedOctave ?: "Mid",
                    isPlaying = playingSwara != null, // Highlight holes when audio note is playing
                    isDetected = detectedSwara != null, // Highlight holes when mic detects note in practice mode
                    fluteScale = fluteScale,
                    saHole = saHole,
                    onSaHoleChanged = { newHole ->
                        saHole = newHole
                        if (selectedScale == null) {
                            selectedScale = "C"
                        }
                    },
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

            // ================= SECTION C (82%): Practice Station Area =================
            Box(
                modifier = Modifier
                    .weight(0.82f)
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
                            effectiveScale = effectiveScale,
                            selectedOctave = selectedOctave,
                            selectedTimer = selectedTimer,
                            playingSwara = playingSwara,
                            onPlayingSwaraChanged = { playingSwara = it },
                            playingOctave = playingOctave,
                            onPlayingOctaveChanged = { playingOctave = it },
                            detectedSwara = detectedSwara,
                            onDetectedSwaraChanged = { detectedSwara = it },
                            detectedOctave = detectedOctave,
                            onDetectedOctaveChanged = { detectedOctave = it },
                            onScaleChanged = {
                                selectedScale = it
                                saHole = 3
                            },
                            onTimerChanged = { selectedTimer = it },
                            onOctaveChanged = { selectedOctave = it }
                        )
                    }
                    AppTab.TUNER -> {
                        TunerScreen(
                            selectedScale = effectiveScale ?: selectedScale ?: "C",
                            onScaleChanged = {
                                selectedScale = it
                                saHole = 3
                            }
                        )
                    }
                }
            }
        }
    }
}
