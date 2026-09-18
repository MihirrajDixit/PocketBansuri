package com.pocketbansuri.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

enum class PracticeMode(val displayName: String) {
    PLAY("Play"),
    LISTEN("Practice"),
    BOTH("Both")
}

fun findClosestCell(
    detectedFreq: Float,
    selectedScale: String,
    swaras: List<Swara>,
    octaveIds: List<String>
): Pair<Swara, String>? {
    if (detectedFreq <= 0f) return null
    var closestSwara: Swara? = null
    var closestOctave: String? = null
    var minDiff = Double.MAX_VALUE
    
    octaveIds.forEach { oct ->
        swaras.forEach { swara ->
            val cellFreq = swara.getFrequencyForScaleAndOctave(selectedScale, oct)
            if (cellFreq != null) {
                val diff = kotlin.math.abs(detectedFreq.toDouble() - cellFreq)
                if (diff < minDiff) {
                    minDiff = diff
                    closestSwara = swara
                    closestOctave = oct
                }
            }
        }
    }
    
    val closestFreq = closestSwara?.getFrequencyForScaleAndOctave(selectedScale, closestOctave!!) ?: return null
    val ratio = detectedFreq / closestFreq
    if (ratio in 0.91..1.09) {
        return Pair(closestSwara!!, closestOctave!!)
    }
    return null
}

@Composable
fun RiyaazScreen(
    selectedRaga: Raga,
    onRagaSelected: (Raga) -> Unit,
    onSwaraSelected: (Swara) -> Unit,
    activeSwara: Swara,
    selectedScale: String?,
    effectiveScale: String? = null,
    selectedOctave: String?,
    selectedTimer: Int?,
    playingSwara: Swara?,
    onPlayingSwaraChanged: (Swara?) -> Unit,
    playingOctave: String?,
    onPlayingOctaveChanged: (String?) -> Unit,
    detectedSwara: Swara? = null,
    onDetectedSwaraChanged: ((Swara?) -> Unit)? = null,
    detectedOctave: String? = null,
    onDetectedOctaveChanged: ((String?) -> Unit)? = null,
    onScaleChanged: (String) -> Unit,
    onTimerChanged: (Int) -> Unit,
    onOctaveChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activePlayScale = effectiveScale ?: selectedScale
    var activeSection by remember { mutableStateOf(RiyaazSection.PLAIN_SCALE) }
    var practiceMode by remember { mutableStateOf(PracticeMode.PLAY) }
    
    var internalDetectedSwara by remember { mutableStateOf<Swara?>(null) }
    var internalDetectedOctave by remember { mutableStateOf<String?>(null) }

    val currentDetectedSwara = detectedSwara ?: internalDetectedSwara
    val currentDetectedOctave = detectedOctave ?: internalDetectedOctave

    fun updateDetected(swara: Swara?, octave: String?) {
        internalDetectedSwara = swara
        internalDetectedOctave = octave
        onDetectedSwaraChanged?.invoke(swara)
        onDetectedOctaveChanged?.invoke(octave)
    }
    
    val recentlyPlayed = remember { mutableStateMapOf<String, Long>() }
    var fadeTrigger by remember { mutableStateOf(0) }
    
    // Manage coroutine job for timer-based playback
    val coroutineScope = rememberCoroutineScope()
    var playbackJob by remember { mutableStateOf<Job?>(null) }

    val view = LocalView.current

    // Globally mute Android platform touch feedback / click sounds while Riyaaz screen is active
    DisposableEffect(view) {
        val root = view.rootView
        val originalSoundEffects = view.isSoundEffectsEnabled
        val originalRootSoundEffects = root.isSoundEffectsEnabled
        
        view.isSoundEffectsEnabled = false
        root.isSoundEffectsEnabled = false
        
        onDispose {
            playbackJob?.cancel()
            AudioEngine.stopReferenceNote()
            AudioEngine.stopEngine()
            onPlayingSwaraChanged(null)
            onPlayingOctaveChanged(null)
            view.isSoundEffectsEnabled = originalSoundEffects
            root.isSoundEffectsEnabled = originalRootSoundEffects
        }
    }

    // Microphone audio engine recording thread control based on section/mode
    LaunchedEffect(activeSection, practiceMode) {
        if (activeSection == RiyaazSection.PLAIN_SCALE && (practiceMode == PracticeMode.LISTEN || practiceMode == PracticeMode.BOTH)) {
            AudioEngine.startEngine()
        } else {
            AudioEngine.stopEngine()
            updateDetected(null, null)
            recentlyPlayed.clear()
        }
    }

    // Real-time microphone audio polling & mapping loop
    if (activeSection == RiyaazSection.PLAIN_SCALE && (practiceMode == PracticeMode.LISTEN || practiceMode == PracticeMode.BOTH)) {
        LaunchedEffect(activePlayScale) {
            val scale = activePlayScale ?: "C"
            val swaras = Swara.values().take(7)
            val octaveIds = listOf("Low", "Mid", "High", "V.High")
            while (true) {
                val freq = AudioEngine.getRawDetectedFrequency()
                val now = System.currentTimeMillis()
                if (freq > 0f) {
                    val match = findClosestCell(freq, scale, swaras, octaveIds)
                    if (match != null) {
                        if (currentDetectedSwara != match.first || currentDetectedOctave != match.second) {
                            updateDetected(match.first, match.second)
                            onSwaraSelected(match.first)
                            onOctaveChanged(match.second)
                        }
                        recentlyPlayed["${match.first.name}_${match.second}"] = now
                    } else {
                        updateDetected(null, null)
                    }
                } else {
                    updateDetected(null, null)
                }
                
                // Clean up keys older than 10 seconds
                val keysToRemove = recentlyPlayed.filter { now - it.value >= 10000L }.keys
                keysToRemove.forEach { recentlyPlayed.remove(it) }
                
                fadeTrigger++
                delay(100L)
            }
        }
    } else {
        updateDetected(null, null)
        DisposableEffect(Unit) {
            onDispose {
                recentlyPlayed.clear()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(4.dp) // Compact screen padding
    ) {
        // Section Switcher Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
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
                                onPlayingSwaraChanged(null)
                                onPlayingOctaveChanged(null)
                            }
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) DeepBackground else TextPrimary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Quick Configuration Dropdowns Row
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Octave Dropdown (only visible in Raga Practice)
                if (activeSection == RiyaazSection.RAGA_LIBRARY) {
                    var octaveExpanded by remember { mutableStateOf(false) }
                    Box {
                        Box(
                            modifier = Modifier
                                .height(26.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(SurfaceDark)
                                .border(1.dp, CardBorder, RoundedCornerShape(4.dp))
                                .clickable { octaveExpanded = true }
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Octave: ${selectedOctave ?: "-"}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedOctave != null) TextPrimary else TextSecondary
                                )
                                Spacer(modifier = Modifier.width(2.dp))
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
                            listOf("Low", "Mid", "High", "V.High").forEach { octave ->
                                DropdownMenuItem(
                                    text = { Text(octave, color = TextPrimary, fontSize = 11.sp) },
                                    onClick = {
                                        onOctaveChanged(octave)
                                        octaveExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Mode Dropdown (only visible in Plain Scale, on the left of Timer)
                if (activeSection == RiyaazSection.PLAIN_SCALE) {
                    var modeExpanded by remember { mutableStateOf(false) }
                    Box {
                        Box(
                            modifier = Modifier
                                .height(26.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(SurfaceDark)
                                .border(1.dp, CardBorder, RoundedCornerShape(4.dp))
                                .clickable { modeExpanded = true }
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Mode: ${practiceMode.displayName}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("▾", fontSize = 8.sp, color = ForestLight)
                            }
                        }
                        DropdownMenu(
                            expanded = modeExpanded,
                            onDismissRequest = { modeExpanded = false },
                            modifier = Modifier
                                .background(SurfaceDark)
                                .border(1.dp, CardBorder, RoundedCornerShape(4.dp))
                        ) {
                            PracticeMode.values().forEach { mode ->
                                DropdownMenuItem(
                                    text = { Text(mode.displayName, color = TextPrimary, fontSize = 11.sp) },
                                    onClick = {
                                        practiceMode = mode
                                        modeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Timer Dropdown (only visible in Plain Scale practice)
                if (activeSection == RiyaazSection.PLAIN_SCALE) {
                    var timerExpanded by remember { mutableStateOf(false) }
                    Box {
                        Box(
                            modifier = Modifier
                                .height(26.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(SurfaceDark)
                                .border(1.dp, CardBorder, RoundedCornerShape(4.dp))
                                .clickable { timerExpanded = true }
                                .padding(horizontal = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Timer: ${selectedTimer?.let { "${it}s" } ?: "-"}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (selectedTimer != null) TextPrimary else TextSecondary
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text("▾", fontSize = 8.sp, color = ForestLight)
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
                                        onTimerChanged(timer)
                                        timerExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Scale Dropdown
                var scaleExpanded by remember { mutableStateOf(false) }
                Box {
                    Box(
                        modifier = Modifier
                            .height(26.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(SurfaceDark)
                            .border(1.dp, CardBorder, RoundedCornerShape(4.dp))
                            .clickable { scaleExpanded = true }
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val scaleLabel = if (selectedScale == null) {
                                "Scale: -"
                            } else if (effectiveScale != null && effectiveScale != selectedScale) {
                                "Flute: $selectedScale ($effectiveScale)"
                            } else {
                                "Scale: $selectedScale"
                            }
                            Text(
                                text = scaleLabel,
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
                                    onScaleChanged(scale)
                                    scaleExpanded = false
                                }
                            )
                        }
                    }
                }
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
                        selectedScale = activePlayScale,
                        selectedOctave = selectedOctave,
                        selectedTimer = selectedTimer,
                        playingSwara = playingSwara,
                        playingOctave = playingOctave,
                        detectedSwara = currentDetectedSwara,
                        detectedOctave = currentDetectedOctave,
                        practiceMode = practiceMode,
                        recentlyPlayed = recentlyPlayed,
                        fadeTrigger = fadeTrigger,
                        onSwaraSelected = onSwaraSelected,
                        onOctaveSelected = onOctaveChanged,
                        onPlaySwara = { swara, octave ->
                            if (activePlayScale != null && selectedTimer != null) {
                                playbackJob?.cancel()
                                AudioEngine.stopReferenceNote()
                                
                                onSwaraSelected(swara)
                                onPlayingSwaraChanged(swara)
                                onPlayingOctaveChanged(octave)
                                onOctaveChanged(octave) // Highlight the row and update visualizer on click!
                                
                                val midi = swara.getMidiNoteForScaleAndOctave(activePlayScale, octave)
                                AudioEngine.playReferenceNote(midi)
                                
                                playbackJob = coroutineScope.launch {
                                    delay(selectedTimer * 1000L)
                                    AudioEngine.stopReferenceNote()
                                    onPlayingSwaraChanged(null)
                                    onPlayingOctaveChanged(null)
                                }
                            }
                        }
                    )
                }
                RiyaazSection.RAGA_LIBRARY -> {
                    RagaLibraryPractice(
                        selectedScale = activePlayScale,
                        selectedOctave = selectedOctave,
                        onSwaraSelected = onSwaraSelected
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
    playingSwara: Swara?,
    playingOctave: String?,
    detectedSwara: Swara?,
    detectedOctave: String?,
    practiceMode: PracticeMode,
    recentlyPlayed: Map<String, Long>,
    fadeTrigger: Int,
    onSwaraSelected: (Swara) -> Unit = {},
    onOctaveSelected: (String) -> Unit = {},
    onPlaySwara: (Swara, String) -> Unit
) {
    val swaras = Swara.values().take(7)
    val isConfigured = selectedScale != null && (practiceMode == PracticeMode.LISTEN || selectedTimer != null)

    val octaves = listOf(
        OctaveConfig("Low", "Low (Mandra)"),
        OctaveConfig("Mid", "Mid (Madhya)"),
        OctaveConfig("High", "High (Taar)"),
        OctaveConfig("V.High", "V.High (Ati)")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceDark, RoundedCornerShape(8.dp))
            .padding(horizontal = 4.dp, vertical = 0.dp),
        verticalArrangement = Arrangement.spacedBy(0.5.dp)
    ) {
        // Table Header (8 columns: 1 Label + 7 Swaras)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(DeepBackground, RoundedCornerShape(6.dp))
                .padding(vertical = 1.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Octave",
                modifier = Modifier.weight(0.9f),
                color = BambooGold,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            swaras.forEach { swara ->
                Text(
                    text = "${swara.displayName} (${swara.hindiName})",
                    modifier = Modifier.weight(1f),
                    color = BambooGold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(1.dp))

        // Table Rows (4 Octaves * 2 Rows = 8 Rows total)
        octaves.forEach { oct ->
            val isCurrentOctaveSelected = selectedOctave?.uppercase() == oct.id.uppercase()
            val rowBgColor = if (isCurrentOctaveSelected) {
                ForestLight.copy(alpha = 0.06f)
            } else {
                Color.Transparent
            }

            val octaveBorder = if (isCurrentOctaveSelected) {
                BorderStroke(1.dp, ForestLight.copy(alpha = 0.35f))
            } else {
                null
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(4.dp))
                    .background(rowBgColor)
                    .then(if (octaveBorder != null) Modifier.border(octaveBorder, RoundedCornerShape(4.dp)) else Modifier)
                    .padding(vertical = 0.dp)
            ) {
                // Row 1/3/5/7: Swara cell row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Octave Label Column
                    Box(
                        modifier = Modifier
                            .weight(0.9f)
                            .padding(horizontal = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = oct.displayName,
                            color = if (isCurrentOctaveSelected) BambooGold else TextSecondary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }

                    // 7 Swara buttons
                    swaras.forEach { swara ->
                        val isPlayingThis = playingSwara == swara && playingOctave?.uppercase() == oct.id.uppercase()
                        val isDetectedThis = (practiceMode == PracticeMode.LISTEN || practiceMode == PracticeMode.BOTH) &&
                                detectedSwara == swara && detectedOctave?.uppercase() == oct.id.uppercase()

                        // Fading trace calculation
                        val now = System.currentTimeMillis()
                        val lastPlayedTime = recentlyPlayed["${swara.name}_${oct.id}"]
                        val elapsed = if (lastPlayedTime != null) now - lastPlayedTime else Long.MAX_VALUE
                        val isRecent = (practiceMode == PracticeMode.LISTEN || practiceMode == PracticeMode.BOTH) &&
                                elapsed < 10000L && !isDetectedThis && !isPlayingThis
                        
                        val fadePercent = if (isRecent) 1f - (elapsed.toFloat() / 10000f) else 0f

                        val westernPitch = if (selectedScale != null) {
                            swara.getWesternEquivalent(selectedScale, oct.id)
                        } else {
                            ""
                        }

                        val cellBg = when {
                            isPlayingThis && isDetectedThis -> AccentGreen.copy(alpha = 0.35f)
                            isDetectedThis -> PitchSharp.copy(alpha = 0.25f)
                            isPlayingThis -> CardBorder
                            isRecent -> BambooGold.copy(alpha = 0.15f * fadePercent)
                            else -> Color.Transparent
                        }

                        val cellBorder = when {
                            isPlayingThis && isDetectedThis -> BorderStroke(2.dp, AccentGreen)
                            isDetectedThis -> BorderStroke(1.5.dp, PitchSharp)
                            isPlayingThis -> BorderStroke(1.dp, ForestLight)
                            isRecent -> BorderStroke(1.dp, BambooGold.copy(alpha = 0.4f * fadePercent))
                            else -> BorderStroke(0.5.dp, CardBorder.copy(alpha = 0.5f))
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(1.dp)
                                .clip(RoundedCornerShape(3.dp))
                                .background(cellBg)
                                .then(if (cellBorder != null) Modifier.border(cellBorder, RoundedCornerShape(3.dp)) else Modifier)
                                .clickable(enabled = isConfigured) {
                                    onSwaraSelected(swara)
                                    onOctaveSelected(oct.id)
                                    if (practiceMode != PracticeMode.LISTEN) {
                                        onPlaySwara(swara, oct.id)
                                    }
                                }
                                .padding(vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = westernPitch.ifEmpty { "-" },
                                color = when {
                                    isPlayingThis && isDetectedThis -> AccentGreen
                                    isPlayingThis -> ForestLight
                                    isDetectedThis -> PitchSharp
                                    isRecent -> BambooGold.copy(alpha = 0.4f + 0.6f * fadePercent)
                                    isConfigured -> TextPrimary
                                    else -> TextSecondary.copy(alpha = 0.4f)
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Row 2/4/6/8: Frequency row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Freq Label Column
                    Box(
                        modifier = Modifier
                            .weight(0.9f)
                            .padding(horizontal = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Freq",
                            color = TextSecondary.copy(alpha = 0.7f),
                            fontSize = 9.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    // 7 Freq cells
                    swaras.forEach { swara ->
                        val frequency = if (selectedScale != null) {
                            swara.getFrequencyForScaleAndOctave(selectedScale, oct.id)
                        } else {
                            null
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 0.5.dp, horizontal = 1.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (frequency != null) String.format("%.1f", frequency) else "--",
                                color = if (isConfigured) TextSecondary else TextSecondary.copy(alpha = 0.4f),
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RagaLibraryPractice(
    selectedScale: String?,
    selectedOctave: String?,
    onSwaraSelected: (Swara) -> Unit,
    modifier: Modifier = Modifier
) {
    val ragas = Raga.dummyRagas
    val coroutineScope = rememberCoroutineScope()
    var playingRaga by remember { mutableStateOf<Raga?>(null) }
    var playingNoteIndex by remember { mutableStateOf<Int?>(null) }
    var playbackJob by remember { mutableStateOf<Job?>(null) }
    
    // Ensure playback is stopped on dispose
    DisposableEffect(Unit) {
        onDispose {
            playbackJob?.cancel()
            AudioEngine.stopReferenceNote()
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(ragas) { raga ->
            val isCurrentRagaPlaying = playingRaga?.name == raga.name
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                border = BorderStroke(1.dp, if (isCurrentRagaPlaying) ForestLight else CardBorder),
                shape = RoundedCornerShape(10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp)
                ) {
                    // Row 1: Raga Name, Category Tag, Play Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = raga.name,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                color = BambooGold
                            )
                            // Category Tag
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(CardBorder)
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = raga.category,
                                    color = TextSecondary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        
                        // Play/Stop Button
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isCurrentRagaPlaying) PitchFlat else ForestLight)
                                .clickable {
                                    if (isCurrentRagaPlaying) {
                                        playbackJob?.cancel()
                                        AudioEngine.stopReferenceNote()
                                        playingRaga = null
                                        playingNoteIndex = null
                                    } else {
                                        playbackJob?.cancel()
                                        playingRaga = raga
                                        playingNoteIndex = 0
                                        
                                        playbackJob = coroutineScope.launch {
                                            val scale = selectedScale ?: "C"
                                            val octave = selectedOctave ?: "Mid"
                                            val allNotes = raga.aarohNotes + raga.avrohNotes
                                            
                                            for (index in allNotes.indices) {
                                                playingNoteIndex = index
                                                val note = allNotes[index]
                                                
                                                // Convert scale and octave to MIDI offset
                                                val scaleOffset = when (scale.uppercase()) {
                                                    "C" -> 0; "C#" -> 1; "D" -> 2; "D#" -> 3; "E" -> 4; "F" -> 5; "F#" -> 6; "G" -> 7; "G#" -> 8; "A" -> 9; "A#" -> 10; "B" -> 11; else -> 0
                                                }
                                                val octaveOffset = when (octave.lowercase()) {
                                                    "low" -> -12; "mid" -> 0; "high" -> 12; "v.high" -> 24; else -> 0
                                                }
                                                val baseMidi = 60 + scaleOffset + octaveOffset
                                                
                                                val baseOffset = when {
                                                    note.startsWith("Sa") || note.startsWith("SA") -> 0
                                                    note.startsWith("re") -> 1
                                                    note.startsWith("Re") -> 2
                                                    note.startsWith("ga") -> 3
                                                    note.startsWith("Ga") -> 4
                                                    note.startsWith("Ma#") || note.startsWith("ma#") -> 6
                                                    note.startsWith("Ma") || note.startsWith("ma") -> 5
                                                    note.startsWith("Pa") -> 7
                                                    note.startsWith("dha") -> 8
                                                    note.startsWith("Dha") -> 9
                                                    note.startsWith("ni") -> 10
                                                    note.startsWith("Ni") -> 11
                                                    else -> 0
                                                }
                                                
                                                val octaveModifier = when {
                                                    note.endsWith("'") -> 12
                                                    note.endsWith("_") -> -12
                                                    else -> 0
                                                }
                                                
                                                val midiNote = baseMidi + baseOffset + octaveModifier
                                                AudioEngine.playReferenceNote(midiNote)
                                                
                                                // Update flute visualizer
                                                val swara = when {
                                                    note.startsWith("Sa'") -> Swara.HIGH_SA
                                                    note.startsWith("Sa") || note.startsWith("SA") -> Swara.SA
                                                    note.startsWith("re") || note.startsWith("Re") -> Swara.RE
                                                    note.startsWith("ga") || note.startsWith("Ga") -> Swara.GA
                                                    note.startsWith("Ma") || note.startsWith("ma") -> Swara.MA
                                                    note.startsWith("Pa") -> Swara.PA
                                                    note.startsWith("dha") || note.startsWith("Dha") -> Swara.DHA
                                                    note.startsWith("ni") || note.startsWith("Ni") -> Swara.NI
                                                    else -> Swara.SA
                                                }
                                                onSwaraSelected(swara)
                                                
                                                delay(700L)
                                            }
                                            
                                            AudioEngine.stopReferenceNote()
                                            playingRaga = null
                                            playingNoteIndex = null
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (isCurrentRagaPlaying) "■" else "▶",
                                color = DeepBackground,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Row 2: Vadi & Samvadi Configs
                    Text(
                        text = "Vadi: ${raga.vadi}   |   Samvadi: ${raga.samvadi}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // Row 3: Aaroh notes
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Aaroh: ",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = BambooGold
                        )
                        raga.aarohNotes.forEachIndexed { idx, note ->
                            val isHighlighted = isCurrentRagaPlaying && playingNoteIndex == idx
                            Text(
                                text = note.replace("ma#", "Ma#").replace("Ma#", "M'"),
                                fontSize = if (isHighlighted) 22.sp else 18.sp,
                                fontWeight = if (isHighlighted) FontWeight.Black else FontWeight.Bold,
                                color = if (isHighlighted) ForestLight else TextPrimary,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Row 4: Avroh notes
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Avroh: ",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = BambooGold
                        )
                        raga.avrohNotes.forEachIndexed { idx, note ->
                            val combinedIndex = raga.aarohNotes.size + idx
                            val isHighlighted = isCurrentRagaPlaying && playingNoteIndex == combinedIndex
                            Text(
                                text = note.replace("ma#", "Ma#").replace("Ma#", "M'"),
                                fontSize = if (isHighlighted) 22.sp else 18.sp,
                                fontWeight = if (isHighlighted) FontWeight.Black else FontWeight.Bold,
                                color = if (isHighlighted) ForestLight else TextPrimary,
                                modifier = Modifier.padding(horizontal = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

data class OctaveConfig(val id: String, val displayName: String)
