package com.zik.music.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil.compose.AsyncImage
import com.zik.music.model.LyricLine
import com.zik.music.playback.PlayerState
import com.zik.music.ui.theme.AccentMutedBlue
import com.zik.music.ui.theme.PureBlack

@Composable
fun ExpandedPlayer(
    playerState: PlayerState,
    lyrics: List<LyricLine>,
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    onTogglePlayPause: () -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onPlayQueueIndex: (Int) -> Unit = {},
    onRemoveQueueIndex: (Int) -> Unit = {},
    onClearUpcomingQueue: () -> Unit = {},
    onOpenEqualizer: () -> Unit = {},
    onOpenSleepTimer: () -> Unit = {},
    isSleepTimerActive: Boolean = false,
    onCollapse: () -> Unit,
    modifier: Modifier = Modifier
) {
    val song = playerState.currentSong ?: return
    var showLyrics by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }

    val currentProgress = if (playerState.durationMs > 0) {
        (playerState.currentPositionMs.toFloat() / playerState.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    // 3D Card Flip Animation (0 to 180 degrees)
    val flipRotation by animateFloatAsState(
        targetValue = if (showLyrics) 180f else 0f,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "cardFlip"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // 1. Full-bleed Immersive Background Artwork with Soft Scrim
        if (song.albumArtUri != null) {
            AsyncImage(
                model = song.albumArtUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(radius = 3.dp)
            )
        }

        // Dark Ambient Gradient Overlay for Glassmorphic Depth
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.35f),
                            Color.Black.copy(alpha = 0.50f),
                            Color.Black.copy(alpha = 0.85f),
                            Color.Black.copy(alpha = 0.96f)
                        )
                    )
                )
        )

        // 2. Main Screen Layout
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top Bar: Back Button, "Now Playing", Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Frosted Circular Back Button
                Surface(
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                    modifier = Modifier.size(42.dp)
                ) {
                    IconButton(onClick = onCollapse) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Text(
                    text = if (showLyrics) "Lyrics" else "Now Playing",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )

                // Action Buttons: Favorite, Sleep Timer, Equalizer & Queue
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // Frosted Circular Favorite Button
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        modifier = Modifier.size(38.dp)
                    ) {
                        IconButton(onClick = onToggleFavorite) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = if (isFavorite) Color(0xFFFF4081) else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Frosted Circular Sleep Timer Button
                    Surface(
                        shape = CircleShape,
                        color = if (isSleepTimerActive) AccentMutedBlue else Color.White.copy(alpha = 0.15f),
                        border = BorderStroke(
                            1.dp,
                            if (isSleepTimerActive) AccentMutedBlue else Color.White.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier.size(38.dp)
                    ) {
                        IconButton(onClick = onOpenSleepTimer) {
                            Icon(
                                imageVector = Icons.Default.Bedtime,
                                contentDescription = "Sleep Timer",
                                tint = if (isSleepTimerActive) PureBlack else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Frosted Circular Equalizer Button
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                        modifier = Modifier.size(38.dp)
                    ) {
                        IconButton(onClick = onOpenEqualizer) {
                            Icon(
                                imageVector = Icons.Default.Equalizer,
                                contentDescription = "Equalizer",
                                tint = AccentMutedBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    // Frosted Circular Queue Button
                    Surface(
                        shape = CircleShape,
                        color = if (showQueue) AccentMutedBlue else Color.White.copy(alpha = 0.15f),
                        border = BorderStroke(
                            1.dp,
                            if (showQueue) AccentMutedBlue else Color.White.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier.size(38.dp)
                    ) {
                        IconButton(onClick = { showQueue = !showQueue }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = "Queue",
                                tint = if (showQueue) PureBlack else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // Middle Area: 3D Flippable Album Art & Synced Lyrics
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 12.dp)
                    .pointerInput(song.id) {
                        var dragDistanceX = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { dragDistanceX = 0f },
                            onDragEnd = {
                                if (dragDistanceX < -100f) {
                                    onSkipNext()
                                } else if (dragDistanceX > 100f) {
                                    onSkipPrevious()
                                }
                                dragDistanceX = 0f
                            },
                            onDragCancel = { dragDistanceX = 0f },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                dragDistanceX += dragAmount
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            showLyrics = !showLyrics
                        }
                        .graphicsLayer {
                            rotationY = flipRotation
                            cameraDistance = 14f * density
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (flipRotation <= 90f) {
                        // Front Side: Album Art Card
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color.White.copy(alpha = 0.10f),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)),
                            modifier = Modifier
                                .fillMaxWidth(0.88f)
                                .aspectRatio(1f)
                        ) {
                            if (song.albumArtUri != null) {
                                AsyncImage(
                                    model = song.albumArtUri,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = null,
                                        tint = Color.White.copy(alpha = 0.4f),
                                        modifier = Modifier.size(72.dp)
                                    )
                                }
                            }
                        }
                    } else {
                        // Back Side: Synced Lyrics View
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer { rotationY = 180f }
                        ) {
                            LyricsView(
                                lyrics = lyrics,
                                currentPositionMs = playerState.currentPositionMs,
                                onSeekTo = onSeekTo,
                                onTap = { showLyrics = false }
                            )
                        }
                    }
                }
            }

            // Bottom Section: Floating Frosted Cards
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Card 1: Track Metadata & Sine Wave Progress Bar
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = Color.White.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp)
                    ) {
                        Text(
                            text = song.title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = song.artist,
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Continuous Sine Wave Seekbar
                        SineWaveSeekBar(
                            progress = currentProgress,
                            isPlaying = playerState.isPlaying,
                            onSeek = { fraction ->
                                val seekMs = (fraction * playerState.durationMs).toLong()
                                onSeekTo(seekMs)
                            },
                            activeColor = AccentMutedBlue,
                            inactiveColor = Color.White.copy(alpha = 0.28f),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Elapsed and Total Duration Labels
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatDuration(playerState.currentPositionMs),
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                            Text(
                                text = formatDuration(playerState.durationMs),
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Card 2: Floating Pill Transport Controls
                Surface(
                    shape = RoundedCornerShape(36.dp),
                    color = Color.White.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Shuffle Button
                        IconButton(
                            onClick = onToggleShuffle,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Shuffle",
                                tint = if (playerState.isShuffleEnabled) AccentMutedBlue else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Previous Track Button
                        IconButton(
                            onClick = onSkipPrevious,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipPrevious,
                                contentDescription = "Previous Track",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Prominent White Circular Play/Pause Button
                        Surface(
                            shape = CircleShape,
                            color = Color.White,
                            modifier = Modifier.size(54.dp)
                        ) {
                            IconButton(
                                onClick = onTogglePlayPause,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Icon(
                                    imageVector = if (playerState.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (playerState.isPlaying) "Pause" else "Play",
                                    tint = PureBlack,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        // Next Track Button
                        IconButton(
                            onClick = onSkipNext,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SkipNext,
                                contentDescription = "Next Track",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        // Loop / Repeat Mode Toggle Button
                        IconButton(
                            onClick = onToggleRepeat,
                            modifier = Modifier.size(44.dp)
                        ) {
                            val repeatIcon = if (playerState.repeatMode == Player.REPEAT_MODE_ONE) {
                                Icons.Default.RepeatOne
                            } else {
                                Icons.Default.Repeat
                            }
                            val isRepeatActive = playerState.repeatMode != Player.REPEAT_MODE_OFF
                            Icon(
                                imageVector = repeatIcon,
                                contentDescription = "Loop Mode",
                                tint = if (isRepeatActive) AccentMutedBlue else Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }

        // 3. Queue Bottom Sheet Modal Overlay
        AnimatedVisibility(
            visible = showQueue,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            QueueSheet(
                playerState = playerState,
                onPlayIndex = { index ->
                    onPlayQueueIndex(index)
                    showQueue = false
                },
                onRemoveIndex = onRemoveQueueIndex,
                onClearUpcoming = onClearUpcomingQueue,
                onClose = { showQueue = false }
            )
        }
    }
}
