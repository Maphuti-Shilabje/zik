package com.zik.music.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.zik.music.ui.theme.AccentMutedBlue
import com.zik.music.ui.theme.SurfaceLevel3
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun WaveformSeekBar(
    progress: Float, // 0f to 1f
    songId: Long,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
    barCount: Int = 45,
    activeColor: Color = AccentMutedBlue,
    inactiveColor: Color = SurfaceLevel3
) {
    // Generate a deterministic, musical waveform pattern unique to each songId
    val waveAmplitudes = remember(songId, barCount) {
        val random = Random(songId)
        FloatArray(barCount) { index ->
            val base = 0.25f + 0.70f * random.nextFloat()
            // Add harmonic wave contour so it looks like authentic audio waveform peaks
            val harmonic = 0.2f * sin(index.toDouble() / barCount * Math.PI).toFloat()
            (base + harmonic).coerceIn(0.15f, 1.0f)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .pointerInput(songId) {
                detectTapGestures { offset ->
                    val newProgress = (offset.x / size.width).coerceIn(0f, 1f)
                    onSeek(newProgress)
                }
            }
            .pointerInput(songId) {
                detectDragGestures { change, _ ->
                    change.consume()
                    val newProgress = (change.position.x / size.width).coerceIn(0f, 1f)
                    onSeek(newProgress)
                }
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val totalWidth = size.width
            val height = size.height
            val spacing = 4.dp.toPx()
            val totalSpacing = spacing * (barCount - 1)
            val barWidth = ((totalWidth - totalSpacing) / barCount).coerceAtLeast(2.dp.toPx())
            val cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())

            for (i in 0 until barCount) {
                val barFraction = i.toFloat() / barCount.toFloat()
                val isPlayed = barFraction <= progress

                val barHeight = (height * waveAmplitudes[i]).coerceAtLeast(4.dp.toPx())
                val top = (height - barHeight) / 2f
                val left = i * (barWidth + spacing)

                drawRoundRect(
                    color = if (isPlayed) activeColor else inactiveColor,
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = cornerRadius
                )
            }
        }
    }
}
