package com.zik.music.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zik.music.ui.theme.AccentMutedBlue
import com.zik.music.ui.theme.PureBlack
import com.zik.music.ui.theme.SurfaceLevel2
import com.zik.music.ui.theme.TextDisabled
import com.zik.music.ui.theme.TextPrimary
import com.zik.music.ui.theme.TextSecondary

private val ALPHABET = listOf('#') + ('A'..'Z').toList()

@Composable
fun FastScroller(
    onLetterSelected: (Char) -> Unit,
    modifier: Modifier = Modifier
) {
    var isDragging by remember { mutableStateOf(false) }
    var activeLetter by remember { mutableStateOf<Char?>(null) }
    var columnHeightPx by remember { mutableStateOf(1f) }
    var touchY by remember { mutableStateOf(0f) }

    fun processTouch(y: Float) {
        touchY = y.coerceIn(0f, columnHeightPx)
        val fraction = (touchY / columnHeightPx).coerceIn(0f, 1f)
        val index = (fraction * (ALPHABET.size - 1)).toInt().coerceIn(0, ALPHABET.size - 1)
        val letter = ALPHABET[index]
        if (letter != activeLetter) {
            activeLetter = letter
            onLetterSelected(letter)
        }
    }

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(end = 4.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        // Floating Letter Preview Bubble (Appears next to the touch point)
        AnimatedVisibility(
            visible = isDragging && activeLetter != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .offset { IntOffset(x = -150, y = (touchY - (columnHeightPx / 2)).toInt()) }
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(AccentMutedBlue),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = activeLetter?.toString() ?: "",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = PureBlack
                )
            }
        }

        // Alphabet Sidebar Column
        Column(
            modifier = Modifier
                .width(20.dp)
                .fillMaxHeight(0.85f)
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceLevel2.copy(alpha = 0.5f))
                .padding(vertical = 4.dp)
                .onGloballyPositioned { coordinates ->
                    columnHeightPx = coordinates.size.height.toFloat()
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { offset ->
                            isDragging = true
                            processTouch(offset.y)
                            tryAwaitRelease()
                            isDragging = false
                            activeLetter = null
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            isDragging = true
                            processTouch(offset.y)
                        },
                        onDragEnd = {
                            isDragging = false
                            activeLetter = null
                        },
                        onDragCancel = {
                            isDragging = false
                            activeLetter = null
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            processTouch(change.position.y)
                        }
                    )
                },
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ALPHABET.forEach { letter ->
                val isCurrent = letter == activeLetter
                Text(
                    text = letter.toString(),
                    fontSize = 9.sp,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                    color = if (isCurrent) AccentMutedBlue else TextDisabled,
                    lineHeight = 10.sp
                )
            }
        }
    }
}
