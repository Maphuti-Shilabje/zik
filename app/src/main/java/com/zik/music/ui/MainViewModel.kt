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

enum class LibraryTab(val title: String) {
    FOLDERS("Folders"),
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
    val activeTab: LibraryTab = LibraryTab.FOLDERS, // Folder-first default for messy libraries
    val searchQuery: String = "",
    val isPlayerExpanded: Boolean = false,
    val activeLyrics: List<LyricLine> = emptyList(),
    val hasPermission: Boolean = false
)

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val scanner = MediaStoreScanner(application)
    val musicController = MusicController(application)

    private val _uiState = MutableStateFlow(LibraryUiState())
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    val playerState: StateFlow<PlayerState> = musicController.playerState

    init {
        musicController.connect()
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

    fun updateSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }

    fun setPlayerExpanded(expanded: Boolean) {
        _uiState.value = _uiState.value.copy(isPlayerExpanded = expanded)
    }

    fun playSong(song: Song, queue: List<Song>) {
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
