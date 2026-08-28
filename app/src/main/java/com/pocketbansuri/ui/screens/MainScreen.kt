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
    var playingOctave by remember { mutableStateOf<String?>(null) }

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
            // ================= SECTION A (15%): Flute Visualizer =================
            Box(
                modifier = Modifier
                    .weight(0.15f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                BansuriVisualizer(
                    activeSwara = playingSwara ?: selectedSwara,
                    selectedOctave = playingOctave ?: selectedOctave ?: "Mid",
                    isPlaying = playingSwara != null, // Highlight holes when note is active
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

            // ================= SECTION C (85%): Practice Station Area =================
            Box(
                modifier = Modifier
                    .weight(0.85f)
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
                            onPlayingSwaraChanged = { playingSwara = it },
                            playingOctave = playingOctave,
                            onPlayingOctaveChanged = { playingOctave = it },
                            onScaleChanged = { selectedScale = it },
                            onTimerChanged = { selectedTimer = it },
                            onOctaveChanged = { selectedOctave = it }
                        )
                    }
                    AppTab.TUNER -> {
                        TunerScreen(
                            selectedScale = selectedScale ?: "C",
                            onScaleChanged = { selectedScale = it }
                        )
                    }
                }
            }
        }
    }
}
