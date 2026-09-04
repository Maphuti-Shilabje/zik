package com.zik.music.playback

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.zik.music.MainActivity
import com.zik.music.audio.AudioEngine
import com.zik.music.model.Song
import com.zik.music.widget.ZikMusicWidgetProvider

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var player: ExoPlayer
    private var audioEngine: AudioEngine? = null
    private var audioEffectsManager: AudioEffectsManager? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()

        val engine = AudioEngine().also { this.audioEngine = it }

        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioTrackPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setAudioProcessors(engine.getAudioProcessors())
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioTrackPlaybackParams)
                    .build()
            }
        }

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()

        player = ExoPlayer.Builder(this, renderersFactory)
            .setAudioAttributes(audioAttributes, true) // Handles audio focus automatically
            .setHandleAudioBecomingNoisy(true) // Pauses automatically when headphones are unplugged
            .build()

        audioEffectsManager = AudioEffectsManager.getInstance(applicationContext).apply {
            attachAudioSession(player.audioSessionId)
        }

        // Listen for session ID changes, playback events, and widget synchronization
        player.addListener(object : Player.Listener {
            override fun onAudioSessionIdChanged(audioSessionId: Int) {
                if (audioSessionId > 0) {
                    audioEffectsManager?.attachAudioSession(audioSessionId)
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                val sessionId = player.audioSessionId
                if (sessionId > 0) {
                    audioEffectsManager?.attachAudioSession(sessionId)
                }
                updateWidget()
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                updateWidget()
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                val sessionId = player.audioSessionId
                if (sessionId > 0) {
                    audioEffectsManager?.attachAudioSession(sessionId)
                }
                updateWidget()
            }
        })

        // Intent to launch MainActivity when clicking the playback notification
        val sessionActivityPendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        mediaSession = MediaSession.Builder(this, player)
            .setSessionActivity(sessionActivityPendingIntent)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ZikMusicWidgetProvider.ACTION_PLAY_PAUSE -> {
                if (player.isPlaying) {
                    player.pause()
                } else {
                    if (player.playbackState == Player.STATE_IDLE) {
                        player.prepare()
                    }
                    player.play()
                }
                updateWidget()
            }
            ZikMusicWidgetProvider.ACTION_NEXT -> {
                if (player.hasNextMediaItem()) {
                    player.seekToNextMediaItem()
                }
                updateWidget()
            }
            ZikMusicWidgetProvider.ACTION_PREV -> {
                if (player.currentPosition > 3000) {
                    player.seekTo(0)
                } else if (player.hasPreviousMediaItem()) {
                    player.seekToPreviousMediaItem()
                } else {
                    player.seekTo(0)
                }
                updateWidget()
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun updateWidget() {
        val currentSong = currentSongFromPlayer()
        ZikMusicWidgetProvider.updateAllWidgets(applicationContext, currentSong, player.isPlaying)
    }

    private fun currentSongFromPlayer(): Song? {
        val item = player.currentMediaItem ?: return null
        val meta = item.mediaMetadata
        val extras = meta.extras
        return Song(
            id = item.mediaId.toLongOrNull() ?: 0L,
            title = meta.title?.toString() ?: "Unknown Title",
            artist = meta.artist?.toString() ?: "Unknown Artist",
            album = meta.albumTitle?.toString() ?: "Unknown Album",
            albumId = extras?.getLong("albumId") ?: 0L,
            durationMs = extras?.getLong("durationMs") ?: player.duration.coerceAtLeast(0L),
            contentUriString = extras?.getString("contentUri") ?: "",
            filePath = extras?.getString("filePath") ?: "",
            folderName = extras?.getString("folderName") ?: "",
            albumArtUriString = meta.artworkUri?.toString()
        )
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onDestroy() {
        audioEffectsManager?.release()
        audioEffectsManager = null
        audioEngine?.release()
        audioEngine = null
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
