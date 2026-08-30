package com.zik.music.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zik.music.data.LrcParser
import com.zik.music.data.MediaStoreScanner
import com.zik.music.model.Folder
import com.zik.music.model.LyricLine
import com.zik.music.model.Song
import com.zik.music.playback.MusicController
import com.zik.music.playback.PlayerState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class LibraryTab(val title: String, val isFavorite: Boolean = false) {
    FOLDERS("Folders"),
    FAVORITES("Favorites", isFavorite = true),
    SONGS("Songs"),
    ALBUMS("Albums"),
    ARTISTS("Artists")
}

data class LibraryUiState(
    val isLoading: Boolean = true,
    val songs: List<Song> = emptyList(),
    val folders: List<Folder> = emptyList(),
    val albums: Map<String, List<Song>> = emptyMap(),
    val artists: Map<String, List<Song>> = emptyMap(),
    val selectedFolder: Folder? = null,
    val activeTab: LibraryTab = LibraryTab.FOLDERS,
    val tabOrder: List<LibraryTab> = listOf(
        LibraryTab.FOLDERS,
        LibraryTab.FAVORITES,
        LibraryTab.SONGS,
        LibraryTab.ALBUMS,
        LibraryTab.ARTISTS
    ),
    val favoriteSongIds: Set<Long> = emptySet(),
    val searchQuery: String = "",
    val isPlayerExpanded: Boolean = false,
    val isSettingsOpen: Boolean = false,
    val activeLyrics: List<LyricLine> = emptyList(),
    val selectedSongIds: Set<Long> = emptySet(),
    val hasPermission: Boolean = false
) {
    val isSelectionMode: Boolean get() = selectedSongIds.isNotEmpty()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val scanner = MediaStoreScanner(application)
    val musicController = MusicController(application)

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    val playerState: StateFlow<PlayerState> = musicController.playerState

    init {
        musicController.connect()
    }

    fun toggleFavorite(songId: Long) {
        val current = _uiState.value.favoriteSongIds
        val updated = if (current.contains(songId)) {
            current - songId
        } else {
            current + songId
        }
        _uiState.value = _uiState.value.copy(favoriteSongIds = updated)
    }

    fun reorderTabs(fromIndex: Int, toIndex: Int) {
        val current = _uiState.value.tabOrder.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            _uiState.value = _uiState.value.copy(tabOrder = current)
        }
    }

    fun onPermissionGranted() {
        _uiState.value = _uiState.value.copy(hasPermission = true)
        loadLibrary()
    }

    fun loadLibrary() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val songs = scanner.scanSongs()
            val folders = scanner.groupIntoFolders(songs)
            val albums = songs.groupBy { it.album.ifBlank { "Unknown Album" } }
            val artists = songs.groupBy { it.artist.ifBlank { "Unknown Artist" } }

            _uiState.value = _uiState.value.copy(
                isLoading = false,
                songs = songs,
                folders = folders,
                albums = albums,
                artists = artists
            )
        }
    }

    fun selectTab(tab: LibraryTab) {
        _uiState.value = _uiState.value.copy(activeTab = tab, selectedFolder = null)
    }

    fun openFolder(folder: Folder) {
        _uiState.value = _uiState.value.copy(selectedFolder = folder)
    }

    fun closeFolder() {
        _uiState.value = _uiState.value.copy(selectedFolder = null)
    }

    fun openSettings() {
        _uiState.value = _uiState.value.copy(isSettingsOpen = true)
    }

    fun closeSettings() {
        _uiState.value = _uiState.value.copy(isSettingsOpen = false)
    }

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun setPlayerExpanded(expanded: Boolean) {
        _uiState.value = _uiState.value.copy(isPlayerExpanded = expanded)
    }

    fun toggleSongSelection(songId: Long) {
        val current = _uiState.value.selectedSongIds
        val updated = if (current.contains(songId)) {
            current - songId
        } else {
            current + songId
        }
        _uiState.value = _uiState.value.copy(selectedSongIds = updated)
    }

    fun selectAll(songs: List<Song>) {
        val allIds = songs.map { it.id }.toSet()
        _uiState.value = _uiState.value.copy(selectedSongIds = allIds)
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(selectedSongIds = emptySet())
    }

    fun playSelectedNext() {
        val selectedSongs = _uiState.value.songs.filter { it.id in _uiState.value.selectedSongIds }
        if (selectedSongs.isNotEmpty()) {
            val currentQueue = playerState.value.queue
            val currentIndex = playerState.value.queueIndex.coerceAtLeast(0)
            val newQueue = currentQueue.toMutableList().apply {
                addAll(currentIndex + 1, selectedSongs)
            }
            musicController.playQueue(newQueue, currentIndex)
        }
        clearSelection()
    }

    fun addSelectedToQueue() {
        val selectedSongs = _uiState.value.songs.filter { it.id in _uiState.value.selectedSongIds }
        if (selectedSongs.isNotEmpty()) {
            val currentQueue = playerState.value.queue
            val newQueue = currentQueue + selectedSongs
            val currentIndex = playerState.value.queueIndex.coerceAtLeast(0)
            musicController.playQueue(newQueue, currentIndex)
        }
        clearSelection()
    }

    fun playSong(song: Song, queue: List<Song>) {
        if (_uiState.value.isSelectionMode) {
            toggleSongSelection(song.id)
            return
        }
        val index = queue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        musicController.playQueue(queue, index)
        loadLyricsForSong(song)
    }

    private fun loadLyricsForSong(song: Song) {
        viewModelScope.launch {
            val companionLrc = LrcParser.findCompanionLrc(song.filePath)
            if (companionLrc != null) {
                try {
                    val content = companionLrc.readText()
                    val lyrics = LrcParser.parse(content)
                    _uiState.value = _uiState.value.copy(activeLyrics = lyrics)
                    return@launch
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            _uiState.value = _uiState.value.copy(activeLyrics = emptyList())
        }
    }

    override fun onCleared() {
        super.onCleared()
        musicController.release()
    }
}
