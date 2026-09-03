package com.zik.music.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.widget.RemoteViews
import com.zik.music.MainActivity
import com.zik.music.R
import com.zik.music.model.Song
import com.zik.music.playback.PlaybackService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ZikMusicWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId, null, false)
        }
    }

    companion object {
        const val ACTION_PLAY_PAUSE = "com.zik.music.action.WIDGET_PLAY_PAUSE"
        const val ACTION_NEXT = "com.zik.music.action.WIDGET_NEXT"
        const val ACTION_PREV = "com.zik.music.action.WIDGET_PREV"

        private var lastSong: Song? = null
        private var lastIsPlaying: Boolean = false

        fun updateAllWidgets(context: Context, song: Song?, isPlaying: Boolean) {
            lastSong = song
            lastIsPlaying = isPlaying

            val appWidgetManager = AppWidgetManager.getInstance(context) ?: return
            val componentName = ComponentName(context, ZikMusicWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName) ?: return

            CoroutineScope(Dispatchers.IO).launch {
                val roundedArtBitmap = song?.albumArtUri?.let { uri ->
                    loadRoundedArtwork(context, uri)
                }

                withContext(Dispatchers.Main) {
                    for (appWidgetId in appWidgetIds) {
                        val views = RemoteViews(context.packageName, R.layout.widget_music_player)

                        // 1. Text & Track Info
                        val title = song?.title ?: "Zik Music"
                        val artist = song?.artist ?: "Tap to play music"
                        views.setTextViewText(R.id.widget_song_title, title)
                        views.setTextViewText(R.id.widget_song_artist, artist)

                        // 2. Album Art
                        if (roundedArtBitmap != null) {
                            views.setImageViewBitmap(R.id.widget_album_art, roundedArtBitmap)
                        } else {
                            views.setImageViewResource(R.id.widget_album_art, R.drawable.ic_widget_music)
                        }

                        // 3. Play / Pause State Icon
                        val playPauseIcon = if (isPlaying) R.drawable.ic_widget_pause else R.drawable.ic_widget_play
                        views.setImageViewResource(R.id.widget_btn_play_pause, playPauseIcon)

                        // 4. Click Actions
                        // Main body click -> Launch MainActivity
                        val mainIntent = Intent(context, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        }
                        val mainPendingIntent = PendingIntent.getActivity(
                            context,
                            0,
                            mainIntent,
                            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                        )
                        views.setOnClickPendingIntent(R.id.widget_root, mainPendingIntent)

                        // Transport actions directly dispatched to PlaybackService
                        views.setOnClickPendingIntent(
                            R.id.widget_btn_play_pause,
                            createServicePendingIntent(context, ACTION_PLAY_PAUSE, 1)
                        )
                        views.setOnClickPendingIntent(
                            R.id.widget_btn_next,
                            createServicePendingIntent(context, ACTION_NEXT, 2)
                        )
                        views.setOnClickPendingIntent(
                            R.id.widget_btn_prev,
                            createServicePendingIntent(context, ACTION_PREV, 3)
                        )

                        appWidgetManager.updateAppWidget(appWidgetId, views)
                    }
                }
            }
        }

        private fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            song: Song?,
            isPlaying: Boolean
        ) {
            updateAllWidgets(context, song ?: lastSong, isPlaying || lastIsPlaying)
        }

        private fun createServicePendingIntent(context: Context, actionStr: String, requestCode: Int): PendingIntent {
            val intent = Intent(context, PlaybackService::class.java).apply {
                action = actionStr
            }
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                PendingIntent.getForegroundService(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            } else {
                PendingIntent.getService(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            }
        }

        private fun loadRoundedArtwork(context: Context, uri: Uri): Bitmap? {
            return try {
                val inputStream = context.contentResolver.openInputStream(uri) ?: return null
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 2 // Downsample for memory efficiency in RemoteViews
                }
                val rawBitmap = BitmapFactory.decodeStream(inputStream, null, options)
                inputStream.close()

                rawBitmap?.let { getRoundedCornerBitmap(it, 28f) }
            } catch (e: Exception) {
                null
            }
        }

        private fun getRoundedCornerBitmap(bitmap: Bitmap, cornerRadius: Float): Bitmap {
            val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(output)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            val rect = Rect(0, 0, bitmap.width, bitmap.height)
            val rectF = RectF(rect)

            canvas.drawRoundRect(rectF, cornerRadius, cornerRadius, paint)
            paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
            canvas.drawBitmap(bitmap, rect, rect, paint)
            return output
        }
    }
}
