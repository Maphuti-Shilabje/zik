package com.zik.music.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.SurroundSound
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zik.music.playback.EqualizerUiState
import com.zik.music.ui.theme.AccentMutedBlue
import com.zik.music.ui.theme.PureBlack
import kotlin.math.roundToInt

@Composable
fun EqualizerScreen(
    eqState: EqualizerUiState,
    onToggleEnabled: (Boolean) -> Unit,
    onBandLevelChanged: (Int, Int) -> Unit,
    onBassBoostChanged: (Int) -> Unit,
    onVirtualizerChanged: (Int) -> Unit,
    onPresetSelected: (Int) -> Unit,
    onResetToFlat: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(bottom = 32.dp)
        ) {
            // 1. Top Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                        modifier = Modifier.size(44.dp)
                    ) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Equalizer",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                // Master EQ Switch
                Switch(
                    checked = eqState.isEnabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = PureBlack,
                        checkedTrackColor = AccentMutedBlue,
                        uncheckedThumbColor = Color.White.copy(alpha = 0.5f),
                        uncheckedTrackColor = Color.White.copy(alpha = 0.15f)
                    )
                )
            }

            // 2. Preset Pills Selector
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(eqState.presets) { index, presetName ->
                    val isSelected = eqState.currentPreset == index
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = if (isSelected && eqState.isEnabled) AccentMutedBlue else Color.White.copy(alpha = 0.06f),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected && eqState.isEnabled) AccentMutedBlue else Color.White.copy(alpha = 0.14f)
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(enabled = eqState.isEnabled) {
                                onPresetSelected(index)
                            }
                    ) {
                        Text(
                            text = presetName,
                            fontSize = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected && eqState.isEnabled) PureBlack else Color.White,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Frequency Response Visualizer Curve
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White.copy(alpha = 0.04f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(110.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    val width = size.width
                    val height = size.height
                    val bands = eqState.bandLevels
                    if (bands.isEmpty()) return@Canvas

                    val range = (eqState.maxBandLevel - eqState.minBandLevel).toFloat().coerceAtLeast(1f)
                    val points = bands.mapIndexed { idx, level ->
                        val x = if (bands.size > 1) idx * (width / (bands.size - 1)) else width / 2f
                        val normalized = (level - eqState.minBandLevel) / range
                        val y = height - (normalized * height)
                        Offset(x, y)
                    }

                    // Draw smooth frequency curve path
                    val curvePath = Path()
                    curvePath.moveTo(points.first().x, points.first().y)

                    for (i in 0 until points.size - 1) {
                        val p0 = points[i]
                        val p1 = points[i + 1]
                        val cx = (p0.x + p1.x) / 2f
                        curvePath.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                    }

                    // Draw filled glowing area under curve
                    val fillPath = Path()
                    fillPath.addPath(curvePath)
                    fillPath.lineTo(points.last().x, height)
                    fillPath.lineTo(points.first().x, height)
                    fillPath.close()

                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                (if (eqState.isEnabled) AccentMutedBlue else Color.White).copy(alpha = 0.25f),
                                Color.Transparent
                            )
                        )
                    )

                    // Draw curve line
                    drawPath(
                        path = curvePath,
                        color = if (eqState.isEnabled) AccentMutedBlue else Color.White.copy(alpha = 0.5f),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Draw node circles
                    points.forEach { pt ->
                        drawCircle(
                            color = Color.White,
                            radius = 4.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = if (eqState.isEnabled) AccentMutedBlue else PureBlack,
                            radius = 2.dp.toPx(),
                            center = pt
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 4. Interactive Graphic Equalizer Bands
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color.White.copy(alpha = 0.04f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "GRAPHIC EQUALIZER",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentMutedBlue
                        )

                        TextButton(
                            onClick = onResetToFlat,
                            enabled = eqState.isEnabled
                        ) {
                            Icon(
                                imageVector = Icons.Default.RestartAlt,
                                contentDescription = null,
                                tint = if (eqState.isEnabled) AccentMutedBlue else Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Flat",
                                color = if (eqState.isEnabled) AccentMutedBlue else Color.White.copy(alpha = 0.3f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        eqState.bandLevels.forEachIndexed { bandIndex, levelMillibels ->
                            val freqHz = eqState.bandFrequencies.getOrElse(bandIndex) { 1000 }
                            val freqLabel = if (freqHz >= 1000) "${freqHz / 1000}k" else "${freqHz}Hz"
                            val gainDb = (levelMillibels / 100f).roundToInt()

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                            ) {
                                // dB level label
                                Text(
                                    text = if (gainDb > 0) "+$gainDb" else "$gainDb",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (eqState.isEnabled) Color.White else Color.White.copy(alpha = 0.4f)
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                // Custom Touch-Draggable Vertical Slider Bar
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .width(28.dp)
                                        .pointerInput(eqState.isEnabled, bandIndex) {
                                            if (!eqState.isEnabled) return@pointerInput
                                            detectDragGestures { change, _ ->
                                                change.consume()
                                                val totalH = size.height.toFloat()
                                                val touchY = change.position.y.coerceIn(0f, totalH)
                                                val fraction = 1f - (touchY / totalH)
                                                val range = eqState.maxBandLevel - eqState.minBandLevel
                                                val newLevel = (eqState.minBandLevel + fraction * range).roundToInt()
                                                onBandLevelChanged(bandIndex, newLevel)
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        val barW = 4.dp.toPx()
                                        val centerX = size.width / 2f
                                        val totalH = size.height
                                        val range = (eqState.maxBandLevel - eqState.minBandLevel).toFloat().coerceAtLeast(1f)
                                        val normalized = (levelMillibels - eqState.minBandLevel) / range
                                        val thumbY = totalH - (normalized * totalH)

                                        // Background Track
                                        drawLine(
                                            color = Color.White.copy(alpha = 0.15f),
                                            start = Offset(centerX, 0f),
                                            end = Offset(centerX, totalH),
                                            strokeWidth = barW,
                                            cap = StrokeCap.Round
                                        )

                                        // Active gain track from center to thumb
                                        val centerY = totalH / 2f
                                        drawLine(
                                            color = if (eqState.isEnabled) AccentMutedBlue else Color.White.copy(alpha = 0.4f),
                                            start = Offset(centerX, centerY),
                                            end = Offset(centerX, thumbY),
                                            strokeWidth = barW,
                                            cap = StrokeCap.Round
                                        )

                                        // Thumb Scrubber Handle
                                        drawCircle(
                                            color = Color.White,
                                            radius = 7.dp.toPx(),
                                            center = Offset(centerX, thumbY)
                                        )
                                        drawCircle(
                                            color = if (eqState.isEnabled) AccentMutedBlue else Color.Gray,
                                            radius = 4.dp.toPx(),
                                            center = Offset(centerX, thumbY)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Frequency Label
                                Text(
                                    text = freqLabel,
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.65f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 5. Sound Enhancement Controls (Bass Boost & 3D Virtualizer)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Bass Boost Card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White.copy(alpha = 0.04f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Speaker,
                                    contentDescription = null,
                                    tint = if (eqState.isEnabled) AccentMutedBlue else Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Bass Boost",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "${eqState.bassBoostStrength / 10}%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (eqState.isEnabled) AccentMutedBlue else Color.White.copy(alpha = 0.4f)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Slider(
                            value = eqState.bassBoostStrength.toFloat(),
                            onValueChange = { onBassBoostChanged(it.roundToInt()) },
                            valueRange = 0f..1000f,
                            enabled = eqState.isEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = AccentMutedBlue,
                                inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                            )
                        )
                    }
                }

                // Virtualizer (3D Surround) Card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White.copy(alpha = 0.04f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.SurroundSound,
                                    contentDescription = null,
                                    tint = if (eqState.isEnabled) AccentMutedBlue else Color.White.copy(alpha = 0.4f),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "3D Surround Virtualizer",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "${eqState.virtualizerStrength / 10}%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (eqState.isEnabled) AccentMutedBlue else Color.White.copy(alpha = 0.4f)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Slider(
                            value = eqState.virtualizerStrength.toFloat(),
                            onValueChange = { onVirtualizerChanged(it.roundToInt()) },
                            valueRange = 0f..1000f,
                            enabled = eqState.isEnabled,
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = AccentMutedBlue,
                                inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        }
    }
}
