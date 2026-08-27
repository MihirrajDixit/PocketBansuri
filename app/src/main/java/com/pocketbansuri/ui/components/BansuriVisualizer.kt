package com.pocketbansuri.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.nativeCanvas
import com.pocketbansuri.model.Swara
import com.pocketbansuri.ui.theme.*

@Composable
fun BansuriVisualizer(
    activeSwara: Swara,
    selectedOctave: String = "MID",
    isPlaying: Boolean = false,
    modifier: Modifier = Modifier
) {
    val targetFingering = activeSwara.getFingeringForOctave(selectedOctave)

    // Animate each of the 7 hole states for smooth visual feedback
    val animatedFingering = targetFingering.mapIndexed { idx, target ->
        animateFloatAsState(
            targetValue = target,
            animationSpec = tween(durationMillis = 200),
            label = "HoleAnim_$idx"
        ).value
    }

    // Dynamic closed color: turns to white when the sound plays
    val closedColor = if (isPlaying) Color.White else BambooGold

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            // Calculate responsive dimensions (narrower and longer)
            val fluteWidth = canvasWidth * 0.20f
            val fluteLeft = (canvasWidth - fluteWidth) / 2f
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
            val threadRatios = listOf(0.04f, 0.18f, 0.52f, 0.87f, 0.96f) // Moved 0.82 to 0.87 to clear H6
            val threadHeight = fluteHeight * 0.022f

            threadRatios.forEach { ratio ->
                val y = fluteTop + fluteHeight * ratio
                // Main thread body
                drawRect(
                    color = threadColor,
                    topLeft = Offset(fluteLeft, y),
                    size = Size(fluteWidth, threadHeight)
                )
                // Subtle thread highlight
                drawRect(
                    color = Color(0x22FFFFFF),
                    topLeft = Offset(fluteLeft, y),
                    size = Size(fluteWidth, threadHeight * 0.3f)
                )
                // Subtle thread bottom shadow
                drawRect(
                    color = Color(0x33000000),
                    topLeft = Offset(fluteLeft, y + threadHeight * 0.7f),
                    size = Size(fluteWidth, threadHeight * 0.3f)
                )
            }

            // 3. Draw Blow Hole (Embouchure)
            val blowHoleY = fluteTop + fluteHeight * 0.10f
            val blowHoleRadius = fluteWidth * 0.25f
            
            // Inside depth
            drawCircle(
                color = Color(0xFF140D07),
                radius = blowHoleRadius,
                center = Offset(canvasWidth / 2f, blowHoleY)
            )
            // Outer shadow ring
            drawCircle(
                color = Color(0xFF4A3B32),
                radius = blowHoleRadius,
                center = Offset(canvasWidth / 2f, blowHoleY),
                style = Stroke(width = 2.5f)
            )

            // 4. Draw the 7 play holes (Increased spacing and stretched distribution)
            val startY = fluteTop + fluteHeight * 0.24f
            val endY = fluteTop + fluteHeight * 0.94f
            val totalHoles = 7
            val spacingY = (endY - startY) / (totalHoles - 1)
            val holeRadius = fluteWidth * 0.16f

            for (index in 0 until totalHoles) {
                val holeY = startY + index * spacingY
                val centerX = canvasWidth / 2f
                val closedValue = animatedFingering[index] // Ranges from 0f to 1f

                // Base open state (dark interior)
                drawCircle(
                    color = Color(0xFF1C130C),
                    radius = holeRadius,
                    center = Offset(centerX, holeY)
                )

                if (closedValue > 0f) {
                    if (closedValue < 1f) {
                        // Drawing half-closed hole (e.g. for High Sa)
                        // Left side remains open (dark), right side filled (closed)
                        drawArc(
                            color = closedColor,
                            startAngle = -90f,
                            sweepAngle = 180f,
                            useCenter = true,
                            topLeft = Offset(centerX - holeRadius, holeY - holeRadius),
                            size = Size(holeRadius * 2f, holeRadius * 2f)
                        )
                        // Add glow highlight on the closed part
                        drawArc(
                            color = Color(0x40FFFFFF),
                            startAngle = -90f,
                            sweepAngle = 180f,
                            useCenter = true,
                            topLeft = Offset(centerX - holeRadius * 0.8f, holeY - holeRadius * 0.8f),
                            size = Size(holeRadius * 1.6f, holeRadius * 1.6f)
                        )
                    } else {
                        // Fully closed hole
                        drawCircle(
                            color = closedColor,
                            radius = holeRadius,
                            center = Offset(centerX, holeY)
                        )
                        // Add an inner highlight to give it a 3D bubble/pressed look
                        drawCircle(
                            color = Color(0x30FFFFFF),
                            radius = holeRadius * 0.8f,
                            center = Offset(centerX, holeY)
                        )
                        drawCircle(
                            color = Color(0x20000000),
                            radius = holeRadius * 0.5f,
                            center = Offset(centerX, holeY)
                        )
                    }
                }

                // Hole rim
                drawCircle(
                    color = Color(0xFF2C1E12),
                    radius = holeRadius,
                    center = Offset(centerX, holeY),
                    style = Stroke(width = 3.5f)
                )

                // Hole number label (subtle indicators on the side of the flute)
                val labelX = centerX + fluteWidth * 0.65f
                drawContext.canvas.nativeCanvas.apply {
                    val paint = android.graphics.Paint().apply {
                        color = if (isPlaying && closedValue > 0.3f) {
                            android.graphics.Color.parseColor("#EA5454")
                        } else {
                            android.graphics.Color.argb(160, 142, 155, 149)
                        }
                        textSize = 28f
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    }
                    drawText("H${index + 1}", labelX, holeY + 10f, paint)
                }
            }
        }
    }
}
