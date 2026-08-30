package com.zik.music.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zik.music.model.LyricLine
import com.zik.music.ui.theme.AccentMutedBlue

@Composable
fun LyricsView(
    lyrics: List<LyricLine>,
    currentPositionMs: Long,
    onSeekTo: (Long) -> Unit,
    onTap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (lyrics.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .clickable(onClick = onTap)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color.White.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                modifier = Modifier.fillMaxWidth(0.92f)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = AccentMutedBlue.copy(alpha = 0.2f),
                        border = BorderStroke(1.dp, AccentMutedBlue.copy(alpha = 0.4f)),
                        modifier = Modifier.size(60.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Lyrics,
                                contentDescription = null,
                                tint = AccentMutedBlue,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "No Synced Lyrics Found",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Zik automatically reads companion .lrc files stored in the same folder as your songs.",
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.75f),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Tap anywhere to flip back to cover",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = AccentMutedBlue,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        return
    }

    val activeIndex = lyrics.indexOfLast { it.timestampMs <= currentPositionMs }.coerceAtLeast(0)
    val listState = rememberLazyListState()

    // Smooth kinetic auto-scrolling to keep the active line centered (Apple Music style)
    LaunchedEffect(activeIndex) {
        if (activeIndex in lyrics.indices) {
            listState.animateScrollToItem(
                index = activeIndex,
                scrollOffset = -220
            )
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .clickable(onClick = onTap),
        contentPadding = PaddingValues(top = 80.dp, bottom = 180.dp, start = 20.dp, end = 20.dp)
    ) {
        itemsIndexed(lyrics) { index, line ->
            val isActive = index == activeIndex
            val animatedScale by animateFloatAsState(
                targetValue = if (isActive) 1.12f else 1.0f,
                animationSpec = tween(durationMillis = 220),
                label = "lyricScale"
            )
            val animatedAlpha by animateFloatAsState(
                targetValue = if (isActive) 1.0f else 0.35f,
                animationSpec = tween(durationMillis = 220),
                label = "lyricAlpha"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .clickable { onSeekTo(line.timestampMs) }
                    .graphicsLayer {
                        scaleX = animatedScale
                        scaleY = animatedScale
                        alpha = animatedAlpha
                    },
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = line.text,
                    fontSize = if (isActive) 22.sp else 18.sp,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isActive) Color.White else Color.White.copy(alpha = 0.6f),
                    lineHeight = 30.sp,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}
