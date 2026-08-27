package com.zik.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.zik.music.model.Folder
import com.zik.music.model.Song
import com.zik.music.playback.PlayerState
import com.zik.music.ui.LibraryTab
import com.zik.music.ui.LibraryUiState
import com.zik.music.ui.components.FolderItem
import com.zik.music.ui.components.SongItem
import com.zik.music.ui.theme.AccentMutedBlue
import com.zik.music.ui.theme.PureBlack
import com.zik.music.ui.theme.SurfaceLevel1
import com.zik.music.ui.theme.SurfaceLevel2
import com.zik.music.ui.theme.TextDisabled
import com.zik.music.ui.theme.TextPrimary
import com.zik.music.ui.theme.TextSecondary

@Composable
fun LibraryScreen(
    uiState: LibraryUiState,
    playerState: PlayerState,
    onTabSelected: (LibraryTab) -> Unit,
    onFolderSelected: (Folder) -> Unit,
    onCloseFolder: () -> Unit,
    onSongSelected: (Song, List<Song>) -> Unit,
    onSearchQueryChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PureBlack)
            .statusBarsPadding()
    ) {
        // App Title & Search Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Zik",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.width(16.dp))

            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChanged,
                placeholder = {
                    Text(
                        text = "Search tracks, folders, artists...",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextDisabled
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotBlank()) {
                        IconButton(onClick = { onSearchQueryChanged("") }) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear search",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentMutedBlue,
                    unfocusedBorderColor = SurfaceLevel2,
                    focusedContainerColor = SurfaceLevel1,
                    unfocusedContainerColor = SurfaceLevel1,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            )
        }

        // Drill-down Folder Header or Library Tabs
        if (uiState.selectedFolder != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCloseFolder) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back to Folders",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = uiState.selectedFolder.name,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary
                    )
                    Text(
                        text = "${uiState.selectedFolder.songCount} tracks",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }
        } else {
            // Reorderable/Customizable Tabs (UI.md: fixed height, AMOLED styling)
            ScrollableTabRow(
                selectedTabIndex = uiState.activeTab.ordinal,
                containerColor = PureBlack,
                contentColor = AccentMutedBlue,
                edgePadding = 16.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[uiState.activeTab.ordinal]),
                        color = AccentMutedBlue,
                        height = 2.dp
                    )
                },
                divider = {}
            ) {
                LibraryTab.values().forEach { tab ->
                    val isSelected = uiState.activeTab == tab
                    Tab(
                        selected = isSelected,
                        onClick = { onTabSelected(tab) },
                        text = {
                            Text(
                                text = tab.title,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isSelected) AccentMutedBlue else TextSecondary
                            )
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

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
                // If viewing inside a selected folder
                uiState.selectedFolder != null -> {
                    val folderSongs = uiState.selectedFolder.songs.filter {
                        query.isEmpty() || it.title.lowercase().contains(query) || it.artist.lowercase().contains(query)
                    }
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(folderSongs, key = { it.id }) { song ->
                            val isCurrent = playerState.currentSong?.id == song.id
                            SongItem(
                                song = song,
                                isPlaying = playerState.isPlaying,
                                isCurrent = isCurrent,
                                onClick = { onSongSelected(song, folderSongs) }
                            )
                        }
                    }
                }

                // Main Tab: FOLDERS (First-class view per Mandate.md)
                uiState.activeTab == LibraryTab.FOLDERS -> {
                    val filteredFolders = uiState.folders.filter {
                        query.isEmpty() || it.name.lowercase().contains(query)
                    }
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
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
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(filteredSongs, key = { it.id }) { song ->
                            val isCurrent = playerState.currentSong?.id == song.id
                            SongItem(
                                song = song,
                                isPlaying = playerState.isPlaying,
                                isCurrent = isCurrent,
                                onClick = { onSongSelected(song, filteredSongs) }
                            )
                        }
                    }
                }

                // Main Tab: ALBUMS
                uiState.activeTab == LibraryTab.ALBUMS -> {
                    val filteredAlbums = uiState.albums.filter { (albumName, _) ->
                        query.isEmpty() || albumName.lowercase().contains(query)
                    }.toList()
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
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
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
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
