package com.zik.music.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zik.music.data.AppPreferences
import com.zik.music.data.AudioDetails
import com.zik.music.data.AudioMetadataExtractor
import com.zik.music.data.LrcParser
import com.zik.music.data.MediaStoreScanner
import com.zik.music.model.Folder
import com.zik.music.model.LyricLine
import com.zik.music.model.Song
import com.zik.music.playback.AudioEffectsManager
import com.zik.music.playback.EqualizerUiState
import com.zik.music.playback.MusicController
import com.zik.music.playback.PlayerState
import com.zik.music.playback.SleepTimerManager
import com.zik.music.playback.SleepTimerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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
    val isEqualizerOpen: Boolean = false,
    val isSleepTimerOpen: Boolean = false,
    val inspectedSongDetails: AudioDetails? = null,
    val activeLyrics: List<LyricLine> = emptyList(),
    val selectedSongIds: Set<Long> = emptySet(),
    val hasPermission: Boolean = false,
    val gaplessEnabled: Boolean = true,
    val pauseOnUnplug: Boolean = true,
    val filterShortAudio: Boolean = true,
    val smartFilenameCleaner: Boolean = true,
    val folderHierarchyFallback: Boolean = true
) {
    val isSelectionMode: Boolean get() = selectedSongIds.isNotEmpty()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val appPrefs = AppPreferences(application)
    private val scanner = MediaStoreScanner(application)
    val musicController = MusicController(application)
    val audioEffectsManager = AudioEffectsManager.getInstance(application)
    val sleepTimerManager = SleepTimerManager(musicController)

    val eqUiState: StateFlow<EqualizerUiState> = audioEffectsManager.state
    val sleepTimerState: StateFlow<SleepTimerState> = sleepTimerManager.state

    private val _uiState = MutableStateFlow(
        LibraryUiState(
            tabOrder = appPrefs.getTabOrder(),
            favoriteSongIds = appPrefs.getFavoriteSongIds(),
            gaplessEnabled = appPrefs.gaplessEnabled,
            pauseOnUnplug = appPrefs.pauseOnUnplug,
            filterShortAudio = appPrefs.filterShortAudio,
            smartFilenameCleaner = appPrefs.smartFilenameCleaner,
            folderHierarchyFallback = appPrefs.folderHierarchyFallback
        )
    )
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
        appPrefs.setFavoriteSongIds(updated)
    }

    fun reorderTabs(fromIndex: Int, toIndex: Int) {
        val current = _uiState.value.tabOrder.toMutableList()
        if (fromIndex in current.indices && toIndex in current.indices) {
            val item = current.removeAt(fromIndex)
            current.add(toIndex, item)
            _uiState.value = _uiState.value.copy(tabOrder = current)
            appPrefs.setTabOrder(current)
        }
    }

    fun setGaplessEnabled(enabled: Boolean) {
        appPrefs.gaplessEnabled = enabled
        _uiState.value = _uiState.value.copy(gaplessEnabled = enabled)
    }

    fun setPauseOnUnplug(enabled: Boolean) {
        appPrefs.pauseOnUnplug = enabled
        _uiState.value = _uiState.value.copy(pauseOnUnplug = enabled)
    }

    fun setFilterShortAudio(enabled: Boolean) {
        appPrefs.filterShortAudio = enabled
        _uiState.value = _uiState.value.copy(filterShortAudio = enabled)
        loadLibrary()
    }

    fun setSmartFilenameCleaner(enabled: Boolean) {
        appPrefs.smartFilenameCleaner = enabled
        _uiState.value = _uiState.value.copy(smartFilenameCleaner = enabled)
        loadLibrary()
    }

    fun setFolderHierarchyFallback(enabled: Boolean) {
        appPrefs.folderHierarchyFallback = enabled
        _uiState.value = _uiState.value.copy(folderHierarchyFallback = enabled)
        loadLibrary()
    }

    fun onPermissionGranted() {
        _uiState.value = _uiState.value.copy(hasPermission = true)
        loadLibrary()
    }

    fun loadLibrary() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val songs = scanner.scanSongs(
                filterShortAudio = _uiState.value.filterShortAudio,
                smartFilenameCleaner = _uiState.value.smartFilenameCleaner,
                folderHierarchyFallback = _uiState.value.folderHierarchyFallback
            )
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

            // Restore last played state if no song is currently playing/loaded
            if (playerState.value.currentSong == null && songs.isNotEmpty()) {
                val lastSongId = appPrefs.getLastPlayedSongId()
                val lastSong = songs.find { it.id == lastSongId } ?: songs.firstOrNull()
                if (lastSong != null) {
                    val lastQueueIds = appPrefs.getLastPlayedQueueIds().toSet()
                    val restoredQueue = if (lastQueueIds.isNotEmpty()) {
                        songs.filter { it.id in lastQueueIds }
                    } else {
                        songs
                    }
                    val lastPosition = appPrefs.getLastPlayedPositionMs()
                    val lastIndex = appPrefs.getLastPlayedQueueIndex().coerceIn(0, (restoredQueue.size - 1).coerceAtLeast(0))
                    musicController.restoreLastState(lastSong, restoredQueue, lastPosition, lastIndex)
                }
            }
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

    fun playSingleSongNext(song: Song) {
        musicController.insertNext(song)
    }

    fun addSingleSongToQueue(song: Song) {
        musicController.addToEnd(song)
    }

    fun playQueueIndex(index: Int) {
        musicController.playQueueIndex(index)
        val song = playerState.value.queue.getOrNull(index)
        if (song != null) {
            loadLyricsForSong(song)
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        musicController.moveQueueItem(fromIndex, toIndex)
    }

    fun removeQueueItem(index: Int) {
        musicController.removeQueueItem(index)
    }

    fun clearUpcomingQueue() {
        musicController.clearUpcomingQueue()
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

    fun openEqualizer() {
        _uiState.value = _uiState.value.copy(isEqualizerOpen = true)
    }

    fun closeEqualizer() {
        _uiState.value = _uiState.value.copy(isEqualizerOpen = false)
    }

    fun toggleEqualizer(enabled: Boolean) {
        audioEffectsManager.setEnabled(enabled)
    }

    fun setEqBandLevel(bandIndex: Int, level: Int) {
        audioEffectsManager.setBandLevel(bandIndex, level)
    }

    fun setEqBassBoost(strength: Int) {
        audioEffectsManager.setBassBoost(strength)
    }

    fun setEqVirtualizer(strength: Int) {
        audioEffectsManager.setVirtualizer(strength)
    }

    fun setEqPreset(presetIndex: Int) {
        audioEffectsManager.usePreset(presetIndex)
    }

    fun resetEqToFlat() {
        audioEffectsManager.resetToFlat()
    }

    fun openSleepTimer() {
        _uiState.value = _uiState.value.copy(isSleepTimerOpen = true)
    }

    fun closeSleepTimer() {
        _uiState.value = _uiState.value.copy(isSleepTimerOpen = false)
    }

    fun startSleepTimer(minutes: Int) {
        sleepTimerManager.startTimer(minutes)
    }

    fun startEndOfTrackSleepTimer() {
        val duration = playerState.value.durationMs
        val currentPos = playerState.value.currentPositionMs
        sleepTimerManager.startEndOfTrackTimer(duration, currentPos)
    }

    fun cancelSleepTimer() {
        sleepTimerManager.cancelTimer()
    }

    fun inspectSong(song: Song) {
        viewModelScope.launch(Dispatchers.IO) {
            val details = AudioMetadataExtractor.extract(getApplication(), song)
            _uiState.value = _uiState.value.copy(inspectedSongDetails = details)
        }
    }

    fun closeAudioInspector() {
        _uiState.value = _uiState.value.copy(inspectedSongDetails = null)
    }

    override fun onCleared() {
        super.onCleared()
        sleepTimerManager.cancelTimer()
        audioEffectsManager.release()
        musicController.release()
    }
}
