package com.zik.music.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.zik.music.model.Song
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlayerState(
    val currentSong: Song? = null,
    val isPlaying: Boolean = false,
    val currentPositionMs: Long = 0L,
    val durationMs: Long = 0L,
    val isShuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val queue: List<Song> = emptyList(),
    val queueIndex: Int = -1
)

class MusicController(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private var currentQueue: List<Song> = emptyList()
    private var progressTrackingJob: Job? = null

    fun connect() {
        if (mediaController != null) return

        val sessionToken = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()
        controllerFuture?.addListener({
            try {
                mediaController = controllerFuture?.get()
                setupPlayerListener()
                updateStateFromPlayer()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    private fun setupPlayerListener() {
        mediaController?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateStateFromPlayer()
                if (isPlaying) {
                    startProgressTracker()
                } else {
                    stopProgressTracker()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                updateStateFromPlayer()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                updateStateFromPlayer()
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                _playerState.value = _playerState.value.copy(isShuffleEnabled = shuffleModeEnabled)
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                _playerState.value = _playerState.value.copy(repeatMode = repeatMode)
            }
        })
    }

    private fun updateStateFromPlayer() {
        val player = mediaController ?: return
        val currentMediaIndex = player.currentMediaItemIndex
        val activeSong = if (currentMediaIndex in currentQueue.indices) {
            currentQueue[currentMediaIndex]
        } else {
            _playerState.value.currentSong
        }

        val duration = if (player.duration > 0) player.duration else (activeSong?.durationMs ?: 0L)
        val position = player.currentPosition.coerceAtLeast(0L)

        _playerState.value = _playerState.value.copy(
            currentSong = activeSong,
            isPlaying = player.isPlaying,
            currentPositionMs = position,
            durationMs = duration,
            isShuffleEnabled = player.shuffleModeEnabled,
            repeatMode = player.repeatMode,
            queue = currentQueue,
            queueIndex = currentMediaIndex
        )
    }

    private fun createMediaItem(song: Song): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(song.title)
            .setArtist(song.artist)
            .setAlbumTitle(song.album)
            .setArtworkUri(song.albumArtUri)
            .build()

        return MediaItem.Builder()
            .setMediaId(song.id.toString())
            .setUri(song.contentUri)
            .setMediaMetadata(metadata)
            .build()
    }

    fun playQueue(songs: List<Song>, startIndex: Int = 0) {
        val player = mediaController ?: return
        if (songs.isEmpty()) return

        currentQueue = songs
        val mediaItems = songs.map { createMediaItem(it) }

        player.setMediaItems(mediaItems, startIndex, 0L)
        player.prepare()
        player.play()

        updateStateFromPlayer()
        startProgressTracker()
    }

    fun playQueueIndex(index: Int) {
        val player = mediaController ?: return
        if (index in currentQueue.indices) {
            player.seekToDefaultPosition(index)
            player.play()
            updateStateFromPlayer()
        }
    }

    fun moveQueueItem(fromIndex: Int, toIndex: Int) {
        val player = mediaController ?: return
        if (fromIndex in currentQueue.indices && toIndex in currentQueue.indices && fromIndex != toIndex) {
            player.moveMediaItem(fromIndex, toIndex)
            val mutable = currentQueue.toMutableList()
            val item = mutable.removeAt(fromIndex)
            mutable.add(toIndex, item)
            currentQueue = mutable
            updateStateFromPlayer()
        }
    }

    fun removeQueueItem(index: Int) {
        val player = mediaController ?: return
        if (index in currentQueue.indices) {
            player.removeMediaItem(index)
            val mutable = currentQueue.toMutableList()
            mutable.removeAt(index)
            currentQueue = mutable
            updateStateFromPlayer()
        }
    }

    fun clearUpcomingQueue() {
        val player = mediaController ?: return
        val currentIndex = player.currentMediaItemIndex
        if (currentIndex in currentQueue.indices && currentIndex + 1 < currentQueue.size) {
            player.removeMediaItems(currentIndex + 1, currentQueue.size)
            currentQueue = currentQueue.take(currentIndex + 1)
            updateStateFromPlayer()
        }
    }

    fun insertNext(song: Song) {
        val player = mediaController ?: return
        val currentIndex = player.currentMediaItemIndex.coerceAtLeast(0)
        val insertIndex = if (currentQueue.isEmpty()) 0 else (currentIndex + 1).coerceAtMost(currentQueue.size)
        
        player.addMediaItem(insertIndex, createMediaItem(song))
        val mutable = currentQueue.toMutableList()
        mutable.add(insertIndex, song)
        currentQueue = mutable
        updateStateFromPlayer()
    }

    fun addToEnd(song: Song) {
        val player = mediaController ?: return
        player.addMediaItem(createMediaItem(song))
        currentQueue = currentQueue + song
        updateStateFromPlayer()
    }

    fun togglePlayPause() {
        val player = mediaController ?: return
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_IDLE) {
                player.prepare()
            }
            player.play()
        }
        updateStateFromPlayer()
    }

    fun seekTo(positionMs: Long) {
        mediaController?.seekTo(positionMs)
        _playerState.value = _playerState.value.copy(currentPositionMs = positionMs)
    }

    fun skipToNext() {
        val player = mediaController ?: return
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
        }
    }

    fun skipToPrevious() {
        val player = mediaController ?: return
        if (player.currentPosition > 3000) {
            player.seekTo(0)
        } else if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
        } else {
            player.seekTo(0)
        }
    }

    fun toggleShuffle() {
        val player = mediaController ?: return
        player.shuffleModeEnabled = !player.shuffleModeEnabled
    }

    fun toggleRepeatMode() {
        val player = mediaController ?: return
        val nextMode = when (player.repeatMode) {
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ALL
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            else -> Player.REPEAT_MODE_OFF
        }
        player.repeatMode = nextMode
    }

    private fun startProgressTracker() {
        stopProgressTracker()
        progressTrackingJob = scope.launch {
            while (isActive) {
                mediaController?.let { player ->
                    if (player.isPlaying) {
                        _playerState.value = _playerState.value.copy(
                            currentPositionMs = player.currentPosition.coerceAtLeast(0L),
                            durationMs = if (player.duration > 0) player.duration else _playerState.value.durationMs
                        )
                    }
                }
                delay(300)
            }
        }
    }

    private fun stopProgressTracker() {
        progressTrackingJob?.cancel()
        progressTrackingJob = null
    }

    fun release() {
        stopProgressTracker()
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
    }
}
