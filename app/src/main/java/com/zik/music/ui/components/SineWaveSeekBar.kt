package com.zik.music.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.zik.music.ui.theme.AccentMutedBlue
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun SineWaveSeekBar(
    progress: Float, // 0f to 1f
    isPlaying: Boolean,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    activeColor: Color = AccentMutedBlue,
    inactiveColor: Color = Color(0x44FFFFFF),
    waveCount: Float = 3.5f,
    amplitudeDp: Float = 6f
) {
    // Subtle live sine wave phase animation when audio is playing
    val infiniteTransition = rememberInfiniteTransition(label = "sinePhase")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (isPlaying) (2 * PI).toFloat() else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phaseAnimation"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newProgress = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeek(newProgress)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val newProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                    onSeek(newProgress)
                }
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val width = size.width
            val height = size.height
            val centerY = height / 2f
            val amplitude = amplitudeDp.dp.toPx()
            val scrubX = width * progress.coerceIn(0f, 1f)

            val activePath = Path()
            val inactivePath = Path()

            var scrubY = centerY

            val stepPx = 2f
            var x = 0f
            var firstActive = true
            var firstInactive = true

            while (x <= width) {
                // Sine wave equation: y = centerY + amplitude * sin(2*PI * waveCount * (x / width) + phase)
                val normalizedX = x / width
                val y = centerY + amplitude * sin((2 * PI * waveCount * normalizedX + phase).toFloat())

                if (x <= scrubX) {
                    if (firstActive) {
                        activePath.moveTo(x, y)
                        firstActive = false
                    } else {
                        activePath.lineTo(x, y)
                    }
                    scrubY = y
                } else {
                    if (firstInactive) {
                        inactivePath.moveTo(x, y)
                        firstInactive = false
                    } else {
                        inactivePath.lineTo(x, y)
                    }
                }
                x += stepPx
            }

            // Draw unplayed inactive portion (dimmed sine wave)
            drawPath(
                path = inactivePath,
                color = inactiveColor,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Draw played active portion (vibrant sine wave)
            drawPath(
                path = activePath,
                color = activeColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Draw Thumb Scrubber Ball at the exact waveform position
            drawCircle(
                color = Color.White,
                radius = 6.dp.toPx(),
                center = Offset(scrubX, scrubY)
            )
            drawCircle(
                color = activeColor,
                radius = 3.5.dp.toPx(),
                center = Offset(scrubX, scrubY)
            )
        }
    }
}
