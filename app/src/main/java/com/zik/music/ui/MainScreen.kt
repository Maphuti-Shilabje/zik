package com.zik.music.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zik.music.ui.components.ExpandedPlayer
import com.zik.music.ui.components.MiniPlayer
import com.zik.music.ui.screens.LibraryScreen
import com.zik.music.ui.screens.SettingsScreen
import com.zik.music.ui.theme.AccentMutedBlue
import com.zik.music.ui.theme.PureBlack
import com.zik.music.ui.theme.TextPrimary
import com.zik.music.ui.theme.TextSecondary

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onRequestPermission: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val playerState by viewModel.playerState.collectAsState()

    // Handle back button hierarchically
    BackHandler(
        enabled = uiState.isSettingsOpen || uiState.isSelectionMode || 
                  uiState.isPlayerExpanded || uiState.selectedFolder != null
    ) {
        when {
            uiState.isSettingsOpen -> viewModel.closeSettings()
            uiState.isSelectionMode -> viewModel.clearSelection()
            uiState.isPlayerExpanded -> viewModel.setPlayerExpanded(false)
            uiState.selectedFolder != null -> viewModel.closeFolder()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        if (!uiState.hasPermission) {
            // Respectful, minimal permission gate
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.layout.Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Zik needs audio access",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(8.dp))
                    Text(
                        text = "To scan and play your offline music without ads or cloud uploads.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                    androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(16.dp))
                    Button(
                        onClick = onRequestPermission,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentMutedBlue)
                    ) {
                        Text(text = "Grant Access", color = PureBlack)
                    }
                }
            }
        } else {
            // Main Library View
            LibraryScreen(
                uiState = uiState,
                playerState = playerState,
                onTabSelected = { viewModel.selectTab(it) },
                onReorderTabs = { from, to -> viewModel.reorderTabs(from, to) },
                onFolderSelected = { viewModel.openFolder(it) },
                onCloseFolder = { viewModel.closeFolder() },
                onSongSelected = { song, queue -> viewModel.playSong(song, queue) },
                onToggleSongSelection = { viewModel.toggleSongSelection(it) },
                onSelectAll = { viewModel.selectAll(it) },
                onClearSelection = { viewModel.clearSelection() },
                onPlaySelectedNext = { viewModel.playSelectedNext() },
                onAddSelectedToQueue = { viewModel.addSelectedToQueue() },
                onPlaySingleSongNext = { viewModel.playSingleSongNext(it) },
                onAddSingleSongToQueue = { viewModel.addSingleSongToQueue(it) },
                onToggleFavorite = { viewModel.toggleFavorite(it) },
                onSearchQueryChanged = { viewModel.updateSearchQuery(it) },
                onOpenSettings = { viewModel.openSettings() }
            )

            // Persistent Mini-Player Bar at bottom
            if (playerState.currentSong != null && !uiState.isPlayerExpanded) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                ) {
                    MiniPlayer(
                        playerState = playerState,
                        onTogglePlayPause = { viewModel.musicController.togglePlayPause() },
                        onSkipNext = { viewModel.musicController.skipToNext() },
                        onClick = { viewModel.setPlayerExpanded(true) }
                    )
                }
            }

            // Expanding Full-Screen Player Sheet (Modal with Gestures & Waveform)
            AnimatedVisibility(
                visible = uiState.isPlayerExpanded,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                ExpandedPlayer(
                    playerState = playerState,
                    lyrics = uiState.activeLyrics,
                    isFavorite = playerState.currentSong?.id?.let { uiState.favoriteSongIds.contains(it) } == true,
                    onToggleFavorite = { playerState.currentSong?.let { viewModel.toggleFavorite(it.id) } },
                    onTogglePlayPause = { viewModel.musicController.togglePlayPause() },
                    onSkipNext = { viewModel.musicController.skipToNext() },
                    onSkipPrevious = { viewModel.musicController.skipToPrevious() },
                    onSeekTo = { viewModel.musicController.seekTo(it) },
                    onToggleShuffle = { viewModel.musicController.toggleShuffle() },
                    onToggleRepeat = { viewModel.musicController.toggleRepeatMode() },
                    onPlayQueueIndex = { viewModel.playQueueIndex(it) },
                    onRemoveQueueIndex = { viewModel.removeQueueItem(it) },
                    onClearUpcomingQueue = { viewModel.clearUpcomingQueue() },
                    onCollapse = { viewModel.setPlayerExpanded(false) }
                )
            }

            // Settings Screen Overlay
            AnimatedVisibility(
                visible = uiState.isSettingsOpen,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
            ) {
                SettingsScreen(
                    onRescanLibrary = { viewModel.loadLibrary() },
                    onBack = { viewModel.closeSettings() }
                )
            }
        }
    }
}
