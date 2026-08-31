package com.zik.music.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FavoriteBorder
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.zik.music.model.Folder
import com.zik.music.model.Song
import com.zik.music.playback.PlayerState
import com.zik.music.ui.LibraryTab
import com.zik.music.ui.LibraryUiState
import com.zik.music.ui.components.FastScroller
import com.zik.music.ui.components.FolderItem
import com.zik.music.ui.components.ReorderableTabRow
import com.zik.music.ui.components.SongItem
import com.zik.music.ui.theme.AccentMutedBlue
import com.zik.music.ui.theme.PureBlack
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun LibraryScreen(
    uiState: LibraryUiState,
    playerState: PlayerState,
    onTabSelected: (LibraryTab) -> Unit,
    onReorderTabs: (Int, Int) -> Unit,
    onFolderSelected: (Folder) -> Unit,
    onCloseFolder: () -> Unit,
    onSongSelected: (Song, List<Song>) -> Unit,
    onToggleSongSelection: (Long) -> Unit,
    onSelectAll: (List<Song>) -> Unit,
    onClearSelection: () -> Unit,
    onPlaySelectedNext: () -> Unit,
    onAddSelectedToQueue: () -> Unit,
    onPlaySingleSongNext: (Song) -> Unit = {},
    onAddSingleSongToQueue: (Song) -> Unit = {},
    onToggleFavorite: (Long) -> Unit = {},
    onSearchQueryChanged: (String) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val songsListState = rememberLazyListState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
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

            // Drill-down Folder Header or Reorderable Frosted Category Tabs
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
                // Interactive 4-Tab Reorderable Tab Row (Fits screen width with 16dp margins)
                ReorderableTabRow(
                    tabs = uiState.tabOrder,
                    activeTab = uiState.activeTab,
                    onTabSelected = onTabSelected,
                    onReorderTabs = onReorderTabs
                )
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
                                val isFavorite = uiState.favoriteSongIds.contains(song.id)
                                SongItem(
                                    song = song,
                                    isPlaying = playerState.isPlaying,
                                    isCurrent = isCurrent,
                                    isSelected = isSelected,
                                    isSelectionMode = uiState.isSelectionMode,
                                    isFavorite = isFavorite,
                                    onClick = { onSongSelected(song, folderSongs) },
                                    onLongClick = { onToggleSongSelection(song.id) },
                                    onPlayNext = { onPlaySingleSongNext(song) },
                                    onAddToQueue = { onAddSingleSongToQueue(song) },
                                    onToggleFavorite = { onToggleFavorite(song.id) }
                                )
                            }
                        }
                    }

                    // Main Tab: FAVORITES
                    uiState.activeTab == LibraryTab.FAVORITES -> {
                        val favoriteSongs = uiState.songs.filter {
                            it.id in uiState.favoriteSongIds &&
                            (query.isEmpty() || it.title.lowercase().contains(query) || it.artist.lowercase().contains(query))
                        }

                        if (favoriteSongs.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(24.dp),
                                    color = Color.White.copy(alpha = 0.08f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(28.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.FavoriteBorder,
                                            contentDescription = null,
                                            tint = Color(0xFFFF4081),
                                            modifier = Modifier.size(48.dp)
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Text(
                                            text = "No Favorites Yet",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Tap the heart in the Now Playing screen or use the 3-dots menu to add songs here.",
                                            fontSize = 13.sp,
                                            color = Color.White.copy(alpha = 0.65f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(bottom = 100.dp)
                            ) {
                                items(favoriteSongs, key = { it.id }) { song ->
                                    val isCurrent = playerState.currentSong?.id == song.id
                                    val isSelected = uiState.selectedSongIds.contains(song.id)
                                    SongItem(
                                        song = song,
                                        isPlaying = playerState.isPlaying,
                                        isCurrent = isCurrent,
                                        isSelected = isSelected,
                                        isSelectionMode = uiState.isSelectionMode,
                                        isFavorite = true,
                                        onClick = { onSongSelected(song, favoriteSongs) },
                                        onLongClick = { onToggleSongSelection(song.id) },
                                        onPlayNext = { onPlaySingleSongNext(song) },
                                        onAddToQueue = { onAddSingleSongToQueue(song) },
                                        onToggleFavorite = { onToggleFavorite(song.id) }
                                    )
                                }
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
                                    val isFavorite = uiState.favoriteSongIds.contains(song.id)
                                    SongItem(
                                        song = song,
                                        isPlaying = playerState.isPlaying,
                                        isCurrent = isCurrent,
                                        isSelected = isSelected,
                                        isSelectionMode = uiState.isSelectionMode,
                                        isFavorite = isFavorite,
                                        onClick = { onSongSelected(song, filteredSongs) },
                                        onLongClick = { onToggleSongSelection(song.id) },
                                        onPlayNext = { onPlaySingleSongNext(song) },
                                        onAddToQueue = { onAddSingleSongToQueue(song) },
                                        onToggleFavorite = { onToggleFavorite(song.id) }
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
                                val firstSong = albumSongs.first()
                                val isFavorite = uiState.favoriteSongIds.contains(firstSong.id)
                                SongItem(
                                    song = firstSong,
                                    isPlaying = false,
                                    isCurrent = false,
                                    isFavorite = isFavorite,
                                    onClick = { onSongSelected(firstSong, albumSongs) },
                                    onPlayNext = { onPlaySingleSongNext(firstSong) },
                                    onAddToQueue = { onAddSingleSongToQueue(firstSong) },
                                    onToggleFavorite = { onToggleFavorite(firstSong.id) }
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
                                val firstSong = artistSongs.first()
                                val isFavorite = uiState.favoriteSongIds.contains(firstSong.id)
                                SongItem(
                                    song = firstSong,
                                    isPlaying = false,
                                    isCurrent = false,
                                    isFavorite = isFavorite,
                                    onClick = { onSongSelected(firstSong, artistSongs) },
                                    onPlayNext = { onPlaySingleSongNext(firstSong) },
                                    onAddToQueue = { onAddSingleSongToQueue(firstSong) },
                                    onToggleFavorite = { onToggleFavorite(firstSong.id) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
