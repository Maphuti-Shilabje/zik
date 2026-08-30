package com.zik.music.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.zik.music.ui.LibraryTab
import com.zik.music.ui.theme.AccentMutedBlue
import com.zik.music.ui.theme.PureBlack
import kotlin.math.roundToInt

@Composable
fun ReorderableTabRow(
    tabs: List<LibraryTab>,
    activeTab: LibraryTab,
    onTabSelected: (LibraryTab) -> Unit,
    onReorderTabs: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var draggingIndex by remember { mutableStateOf<Int?>(null) }
    var dragOffsetX by remember { mutableFloatStateOf(0f) }

    val density = LocalDensity.current
    val spacingDp = 6.dp
    val spacingPx = with(density) { spacingDp.toPx() }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        val totalWidthPx = constraints.maxWidth.toFloat()
        val count = tabs.size.coerceAtLeast(1)
        val tabWidthPx = (totalWidthPx - (spacingPx * (count - 1))) / count
        val tabWidthDp = with(density) { tabWidthPx.toDp() }
        val slotStepPx = tabWidthPx + spacingPx

        val currentDragIdx = draggingIndex
        val targetSlot = if (currentDragIdx != null) {
            val shift = (dragOffsetX / slotStepPx).roundToInt()
            (currentDragIdx + shift).coerceIn(0, count - 1)
        } else null

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacingDp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEachIndexed { index, tab ->
                val isSelected = activeTab == tab
                val isDraggingThis = currentDragIdx == index

                // Real-time displacement for neighbor tabs as finger moves
                val targetDisplacementPx = when {
                    isDraggingThis -> 0f
                    currentDragIdx != null && targetSlot != null -> {
                        if (currentDragIdx < targetSlot && index in (currentDragIdx + 1)..targetSlot) {
                            -slotStepPx
                        } else if (currentDragIdx > targetSlot && index in targetSlot until currentDragIdx) {
                            slotStepPx
                        } else {
                            0f
                        }
                    }
                    else -> 0f
                }

                val animatedDisplacementPx by animateFloatAsState(
                    targetValue = targetDisplacementPx,
                    animationSpec = tween(durationMillis = 180),
                    label = "tabDisplacement"
                )

                val effectiveTranslationX = if (isDraggingThis) dragOffsetX else animatedDisplacementPx

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = if (isSelected) AccentMutedBlue else Color.White.copy(alpha = 0.09f),
                    border = BorderStroke(
                        1.dp,
                        if (isDraggingThis) AccentMutedBlue.copy(alpha = 0.85f)
                        else if (isSelected) AccentMutedBlue
                        else Color.White.copy(alpha = 0.16f)
                    ),
                    modifier = Modifier
                        .width(tabWidthDp)
                        .height(38.dp)
                        .zIndex(if (isDraggingThis) 10f else 1f)
                        .graphicsLayer {
                            translationX = effectiveTranslationX
                            scaleX = if (isDraggingThis) 1.06f else 1.0f
                            scaleY = if (isDraggingThis) 1.06f else 1.0f
                        }
                        .clip(RoundedCornerShape(18.dp))
                        .clickable {
                            if (draggingIndex == null) {
                                onTabSelected(tab)
                            }
                        }
                        .pointerInput(index, tabs) {
                            detectHorizontalDragGestures(
                                onDragStart = {
                                    draggingIndex = index
                                    dragOffsetX = 0f
                                },
                                onDragEnd = {
                                    val finalTarget = targetSlot ?: index
                                    if (finalTarget != index) {
                                        onReorderTabs(index, finalTarget)
                                    }
                                    draggingIndex = null
                                    dragOffsetX = 0f
                                },
                                onDragCancel = {
                                    draggingIndex = null
                                    dragOffsetX = 0f
                                },
                                onHorizontalDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffsetX += dragAmount
                                }
                            )
                        }
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab.title,
                            fontSize = 12.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) PureBlack else Color.White,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}
