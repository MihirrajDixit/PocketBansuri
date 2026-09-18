package com.pocketbansuri.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.pocketbansuri.model.FluteScaleHelper
import com.pocketbansuri.model.Swara
import com.pocketbansuri.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun BansuriVisualizer(
    activeSwara: Swara,
    selectedOctave: String = "MID",
    isPlaying: Boolean = false,
    isDetected: Boolean = false,
    fluteScale: String = "C",
    saHole: Int = 3,
    onSaHoleChanged: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val targetFingering = activeSwara.getFingeringForSaHole(saHole)

    // Animate each of the 7 hole states for smooth visual feedback
    val animatedFingering = targetFingering.mapIndexed { idx, target ->
        animateFloatAsState(
            targetValue = target,
            animationSpec = tween(durationMillis = 200),
            label = "HoleAnim_$idx"
        ).value
    }

    // Animate movable bar position (1..7 mapped to vertical hole ratio)
    val animatedSaHoleRatio by animateFloatAsState(
        targetValue = 0.24f + (saHole - 1) * ((0.94f - 0.24f) / 6f),
        animationSpec = tween(durationMillis = 200),
        label = "SaHoleBarRatio"
    )

    // Dynamic closed color: pure white when playing audio, accent green when mic detected, bright gold when selected
    val closedColor = when {
        isPlaying -> Color(0xFFFFFFFF)
        isDetected -> AccentGreen
        else -> Color(0xFFFFD56B)
    }
    val closedBorderColor = when {
        isPlaying -> Color(0xFFFFFFFF)
        isDetected -> Color(0xFF95D5B2)
        else -> Color(0xFFFFF6D0)
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 12.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(fluteScale, onSaHoleChanged) {
                    detectTapGestures { offset ->
                        val canvasHeight = size.height
                        val fluteHeight = canvasHeight * 0.98f
                        val fluteTop = (canvasHeight - fluteHeight) / 2f
                        val startY = fluteTop + fluteHeight * 0.24f
                        val endY = fluteTop + fluteHeight * 0.94f
                        val spacingY = (endY - startY) / 6f

                        val closestIndex = ((offset.y - startY) / spacingY).roundToInt().coerceIn(0, 6)
                        onSaHoleChanged?.invoke(closestIndex + 1)
                    }
                }
                .pointerInput(fluteScale, onSaHoleChanged) {
                    detectVerticalDragGestures { change, _ ->
                        change.consume()
                        val canvasHeight = size.height
                        val fluteHeight = canvasHeight * 0.98f
                        val fluteTop = (canvasHeight - fluteHeight) / 2f
                        val startY = fluteTop + fluteHeight * 0.24f
                        val endY = fluteTop + fluteHeight * 0.94f
                        val spacingY = (endY - startY) / 6f

                        val closestIndex = ((change.position.y - startY) / spacingY).roundToInt().coerceIn(0, 6)
                        onSaHoleChanged?.invoke(closestIndex + 1)
                    }
                }
        ) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Position flute body with room for the movable bar on the right
            val fluteWidth = canvasWidth * 0.22f
            val fluteLeft = canvasWidth * 0.18f
            val fluteCenterX = fluteLeft + fluteWidth / 2f
            val fluteHeight = canvasHeight * 0.98f
            val fluteTop = (canvasHeight - fluteHeight) / 2f

            // 1. Draw Flute Body with a rich 3D cylindrical wood gradient
            val bambooBrush = Brush.linearGradient(
                colors = listOf(
                    BambooDark,
                    BambooGold,
                    BambooDark
                ),
                start = Offset(fluteLeft, 0f),
                end = Offset(fluteLeft + fluteWidth, 0f)
            )

            drawRoundRect(
                brush = bambooBrush,
                topLeft = Offset(fluteLeft, fluteTop),
                size = Size(fluteWidth, fluteHeight),
                cornerRadius = CornerRadius(16f, 16f)
            )

            // Outer dark wood border
            drawRoundRect(
                color = Color(0xFF2C1E12),
                topLeft = Offset(fluteLeft, fluteTop),
                size = Size(fluteWidth, fluteHeight),
                cornerRadius = CornerRadius(16f, 16f),
                style = Stroke(width = 3f)
            )

            // 2. Draw traditional thread bindings (prevents wood splitting)
            val threadColor = Color(0xFF6B1D1D) // Traditional dark red thread
            val threadRatios = listOf(0.04f, 0.18f, 0.52f, 0.87f, 0.96f)
            val threadHeight = fluteHeight * 0.022f

            threadRatios.forEach { ratio ->
                val y = fluteTop + fluteHeight * ratio
                drawRect(
                    color = threadColor,
                    topLeft = Offset(fluteLeft, y),
                    size = Size(fluteWidth, threadHeight)
                )
                drawRect(
                    color = Color(0x22FFFFFF),
                    topLeft = Offset(fluteLeft, y),
                    size = Size(fluteWidth, threadHeight * 0.3f)
                )
                drawRect(
                    color = Color(0x33000000),
                    topLeft = Offset(fluteLeft, y + threadHeight * 0.7f),
                    size = Size(fluteWidth, threadHeight * 0.3f)
                )
            }

            // 3. Draw Blow Hole (Embouchure)
            val blowHoleY = fluteTop + fluteHeight * 0.10f
            val blowHoleRadius = fluteWidth * 0.25f

            drawCircle(
                color = Color(0xFF140D07),
                radius = blowHoleRadius,
                center = Offset(fluteCenterX, blowHoleY)
            )
            drawCircle(
                color = Color(0xFF4A3B32),
                radius = blowHoleRadius,
                center = Offset(fluteCenterX, blowHoleY),
                style = Stroke(width = 2.5f)
            )

            // 4. Hole layout geometry
            val startY = fluteTop + fluteHeight * 0.24f
            val endY = fluteTop + fluteHeight * 0.94f
            val totalHoles = 7
            val spacingY = (endY - startY) / (totalHoles - 1)
            val holeRadius = fluteWidth * 0.16f

            // 5. Draw Movable Bar Track on the right of the flute
            val trackX = fluteLeft + fluteWidth + canvasWidth * 0.06f

            // Subtle vertical rail line
            drawLine(
                color = Color(0x338E9B95),
                start = Offset(trackX, startY),
                end = Offset(trackX, endY),
                strokeWidth = 3f
            )

            // 6. Draw 7 Play Holes & Scale markers along track
            for (index in 0 until totalHoles) {
                val holeY = startY + index * spacingY
                val closedValue = animatedFingering[index] // Ranges from 0f to 1f
                val holeNum = index + 1
                val holeScale = FluteScaleHelper.getScaleForHole(fluteScale, holeNum)

                // Base open state (deep dark interior cavity)
                drawCircle(
                    color = Color(0xFF140D07),
                    radius = holeRadius,
                    center = Offset(fluteCenterX, holeY)
                )
                drawCircle(
                    color = Color(0xFF000000),
                    radius = holeRadius * 0.72f,
                    center = Offset(fluteCenterX, holeY)
                )

                if (closedValue > 0f) {
                    if (closedValue < 1f) {
                        drawArc(
                            color = closedColor,
                            startAngle = -90f,
                            sweepAngle = 180f,
                            useCenter = true,
                            topLeft = Offset(fluteCenterX - holeRadius, holeY - holeRadius),
                            size = Size(holeRadius * 2f, holeRadius * 2f)
                        )
                        drawArc(
                            color = closedBorderColor,
                            startAngle = -90f,
                            sweepAngle = 180f,
                            useCenter = true,
                            topLeft = Offset(fluteCenterX - holeRadius * 0.8f, holeY - holeRadius * 0.8f),
                            size = Size(holeRadius * 1.6f, holeRadius * 1.6f)
                        )
                    } else {
                        // Fully covered hole (bright luminous gold/white with distinct seal highlight)
                        drawCircle(
                            color = closedColor,
                            radius = holeRadius,
                            center = Offset(fluteCenterX, holeY)
                        )
                        drawCircle(
                            color = closedBorderColor,
                            radius = holeRadius * 0.8f,
                            center = Offset(fluteCenterX, holeY)
                        )
                        drawCircle(
                            color = Color(0x33000000),
                            radius = holeRadius * 0.45f,
                            center = Offset(fluteCenterX, holeY)
                        )
                    }
                }

                // Hole rim (bright rim for covered holes, dark wood rim for open holes)
                drawCircle(
                    color = if (closedValue > 0.5f) closedBorderColor else Color(0xFF2C1E12),
                    radius = holeRadius,
                    center = Offset(fluteCenterX, holeY),
                    style = Stroke(width = if (closedValue > 0.5f) 2.5f else 3.5f)
                )

                // Rail notch dot at each hole
                val isCurrentSa = holeNum == saHole
                drawCircle(
                    color = if (isCurrentSa) BambooGold else Color(0x448E9B95),
                    radius = if (isCurrentSa) 4.5f else 3f,
                    center = Offset(trackX, holeY)
                )

                // Subtle note letter next to the track (shown when not covered by the movable bar)
                if (!isCurrentSa) {
                    val noteX = trackX + canvasWidth * 0.17f
                    drawContext.canvas.nativeCanvas.apply {
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.argb(140, 142, 155, 149)
                            textSize = 24f
                            isAntiAlias = true
                            textAlign = android.graphics.Paint.Align.CENTER
                            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                        }
                        drawText(holeScale, noteX, holeY + 8f, paint)
                    }
                }
            }

            // 7. Draw the Movable Bar (Sa indicator that can slide across holes to change scale)
            val barY = fluteTop + fluteHeight * animatedSaHoleRatio
            val activeScale = FluteScaleHelper.getScaleForHole(fluteScale, saHole)

            // Connector tick from flute to the movable bar
            drawLine(
                color = BambooGold,
                start = Offset(fluteLeft + fluteWidth + 2f, barY),
                end = Offset(trackX, barY),
                strokeWidth = 3f
            )

            // Movable bar pill handle
            val barWidth = canvasWidth * 0.44f
            val barHeight = fluteWidth * 0.60f
            val barLeft = trackX + canvasWidth * 0.03f
            val barTop = barY - barHeight / 2f

            // Glowing / metallic background of movable bar
            val barBrush = Brush.horizontalGradient(
                colors = listOf(
                    Color(0xFFF7C85C),
                    Color(0xFFE5A93C),
                    Color(0xFFC48220)
                ),
                startX = barLeft,
                endX = barLeft + barWidth
            )

            // Bar body
            drawRoundRect(
                brush = barBrush,
                topLeft = Offset(barLeft, barTop),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(10f, 10f)
            )

            // Bar highlight border
            drawRoundRect(
                color = Color(0xFFFFF6E0),
                topLeft = Offset(barLeft, barTop),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(10f, 10f),
                style = Stroke(width = 1.5f)
            )

            // Inner gloss highlight
            drawLine(
                color = Color(0x55FFFFFF),
                start = Offset(barLeft + 6f, barTop + 3f),
                end = Offset(barLeft + barWidth - 6f, barTop + 3f),
                strokeWidth = 1.5f
            )

            // Text on movable bar: "Sa · B" (or "Sa · C")
            val barCenterX = barLeft + barWidth / 2f
            drawContext.canvas.nativeCanvas.apply {
                val textPaint = android.graphics.Paint().apply {
                    color = android.graphics.Color.parseColor("#1B120A")
                    textSize = 24f
                    isAntiAlias = true
                    textAlign = android.graphics.Paint.Align.CENTER
                    typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                }
                drawText("Sa·$activeScale", barCenterX, barY + 8f, textPaint)
            }
        }
    }
}
