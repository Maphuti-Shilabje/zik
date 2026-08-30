package com.zik.music.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zik.music.model.Folder
import com.zik.music.model.Song
import com.zik.music.playback.PlayerState
import com.zik.music.ui.LibraryTab
import com.zik.music.ui.LibraryUiState
import com.zik.music.ui.components.FastScroller
import com.zik.music.ui.components.FolderItem
import com.zik.music.ui.components.SongItem
import com.zik.music.ui.theme.AccentMutedBlue
import com.zik.music.ui.theme.PureBlack
import kotlinx.coroutines.launch

@Composable
fun LibraryScreen(
    uiState: LibraryUiState,
    playerState: PlayerState,
    onTabSelected: (LibraryTab) -> Unit,
    onFolderSelected: (Folder) -> Unit,
    onCloseFolder: () -> Unit,
    onSongSelected: (Song, List<Song>) -> Unit,
    onToggleSongSelection: (Long) -> Unit,
    onSelectAll: (List<Song>) -> Unit,
    onClearSelection: () -> Unit,
    onPlaySelectedNext: () -> Unit,
    onAddSelectedToQueue: () -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val songsListState = rememberLazyListState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF141926),
                        Color(0xFF0C0E14),
                        PureBlack
                    )
                )
            )
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Selection Mode Context Bar OR Standard Frosted Header
            if (uiState.isSelectionMode) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onClearSelection) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cancel Selection",
                                tint = Color.White
                            )
                        }

                        Text(
                            text = "${uiState.selectedSongIds.size} selected",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 8.dp)
                        )

                        // Select All
                        IconButton(onClick = { onSelectAll(uiState.songs) }) {
                            Icon(
                                imageVector = Icons.Default.SelectAll,
                                contentDescription = "Select All",
                                tint = Color.White
                            )
                        }

                        // Play Next
                        IconButton(onClick = onPlaySelectedNext) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Play Next",
                                tint = AccentMutedBlue
                            )
                        }

                        // Add to Queue
                        IconButton(onClick = onAddSelectedToQueue) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.PlaylistAdd,
                                contentDescription = "Add to Queue",
                                tint = Color.White
                            )
                        }
                    }
                }
            } else {
                // Glassmorphic Header & Search Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Zik",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    // Frosted Search Pill
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = onSearchQueryChanged,
                        placeholder = {
                            Text(
                                text = "Search tracks, folders...",
                                fontSize = 13.sp,
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search",
                                tint = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotBlank()) {
                                IconButton(onClick = { onSearchQueryChanged("") }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(22.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentMutedBlue,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.18f),
                            focusedContainerColor = Color.White.copy(alpha = 0.10f),
                            unfocusedContainerColor = Color.White.copy(alpha = 0.08f),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Frosted Circular Settings Button
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.20f)),
                        modifier = Modifier.size(44.dp)
                    ) {
                        IconButton(onClick = onOpenSettings) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Settings",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            // Drill-down Folder Header or Frosted Category Tabs
            if (uiState.selectedFolder != null && !uiState.isSelectionMode) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                        modifier = Modifier.size(38.dp)
                    ) {
                        IconButton(onClick = onCloseFolder) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back to Folders",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = uiState.selectedFolder.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = "${uiState.selectedFolder.songCount} tracks",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.65f)
                        )
                    }
                }
            } else if (!uiState.isSelectionMode) {
                // Frosted Pill Tab Selector
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(LibraryTab.values().toList()) { tab ->
                        val isSelected = uiState.activeTab == tab
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (isSelected) AccentMutedBlue else Color.White.copy(alpha = 0.08f),
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) AccentMutedBlue else Color.White.copy(alpha = 0.15f)
                            ),
                            modifier = Modifier
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { onTabSelected(tab) }
                        ) {
                            Text(
                                text = tab.title,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) PureBlack else Color.White,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Content Area
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AccentMutedBlue)
                }
            } else {
                val query = uiState.searchQuery.trim().lowercase()

                when {
                    // Inside selected folder
                    uiState.selectedFolder != null -> {
                        val folderSongs = uiState.selectedFolder.songs.filter {
                            query.isEmpty() || it.title.lowercase().contains(query) || it.artist.lowercase().contains(query)
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 100.dp)
                        ) {
                            items(folderSongs, key = { it.id }) { song ->
                                val isCurrent = playerState.currentSong?.id == song.id
                                val isSelected = uiState.selectedSongIds.contains(song.id)
                                SongItem(
                                    song = song,
                                    isPlaying = playerState.isPlaying,
                                    isCurrent = isCurrent,
                                    isSelected = isSelected,
                                    isSelectionMode = uiState.isSelectionMode,
                                    onClick = { onSongSelected(song, folderSongs) },
                                    onLongClick = { onToggleSongSelection(song.id) }
                                )
                            }
                        }
                    }

                    // Main Tab: FOLDERS
                    uiState.activeTab == LibraryTab.FOLDERS -> {
                        val filteredFolders = uiState.folders.filter {
                            query.isEmpty() || it.name.lowercase().contains(query)
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 100.dp)
                        ) {
                            items(filteredFolders, key = { it.path }) { folder ->
                                FolderItem(
                                    folder = folder,
                                    onClick = { onFolderSelected(folder) }
                                )
                            }
                        }
                    }

                    // Main Tab: SONGS
                    uiState.activeTab == LibraryTab.SONGS -> {
                        val filteredSongs = uiState.songs.filter {
                            query.isEmpty() || it.title.lowercase().contains(query) || it.artist.lowercase().contains(query)
                        }

                        Box(modifier = Modifier.fillMaxSize()) {
                            LazyColumn(
                                state = songsListState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 100.dp)
                            ) {
                                items(filteredSongs, key = { it.id }) { song ->
                                    val isCurrent = playerState.currentSong?.id == song.id
                                    val isSelected = uiState.selectedSongIds.contains(song.id)
                                    SongItem(
                                        song = song,
                                        isPlaying = playerState.isPlaying,
                                        isCurrent = isCurrent,
                                        isSelected = isSelected,
                                        isSelectionMode = uiState.isSelectionMode,
                                        onClick = { onSongSelected(song, filteredSongs) },
                                        onLongClick = { onToggleSongSelection(song.id) }
                                    )
                                }
                            }

                            // Alphabet Fast-Scroller Sidebar
                            if (filteredSongs.size > 10) {
                                FastScroller(
                                    onLetterSelected = { letter ->
                                        val targetIndex = if (letter == '#') {
                                            filteredSongs.indexOfFirst { !it.title.first().isLetter() }
                                        } else {
                                            filteredSongs.indexOfFirst {
                                                it.title.startsWith(letter, ignoreCase = true)
                                            }
                                        }
                                        if (targetIndex != -1) {
                                            coroutineScope.launch {
                                                songsListState.scrollToItem(targetIndex)
                                            }
                                        }
                                    },
                                    modifier = Modifier.align(Alignment.CenterEnd)
                                )
                            }
                        }
                    }

                    // Main Tab: ALBUMS
                    uiState.activeTab == LibraryTab.ALBUMS -> {
                        val filteredAlbums = uiState.albums.filter { (albumName, _) ->
                            query.isEmpty() || albumName.lowercase().contains(query)
                        }.toList()
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 100.dp)
                        ) {
                            items(filteredAlbums, key = { it.first }) { (albumName, albumSongs) ->
                                SongItem(
                                    song = albumSongs.first(),
                                    isPlaying = false,
                                    isCurrent = false,
                                    onClick = { onSongSelected(albumSongs.first(), albumSongs) }
                                )
                            }
                        }
                    }

                    // Main Tab: ARTISTS
                    uiState.activeTab == LibraryTab.ARTISTS -> {
                        val filteredArtists = uiState.artists.filter { (artistName, _) ->
                            query.isEmpty() || artistName.lowercase().contains(query)
                        }.toList()
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 100.dp)
                        ) {
                            items(filteredArtists, key = { it.first }) { (artistName, artistSongs) ->
                                SongItem(
                                    song = artistSongs.first(),
                                    isPlaying = false,
                                    isCurrent = false,
                                    onClick = { onSongSelected(artistSongs.first(), artistSongs) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
