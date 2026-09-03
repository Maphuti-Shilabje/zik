package com.zik.music.data

import android.content.Context
import android.content.SharedPreferences
import com.zik.music.ui.LibraryTab

class AppPreferences(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("zik_app_prefs", Context.MODE_PRIVATE)

    // 1. Category Tab Order
    fun getTabOrder(): List<LibraryTab> {
        val raw = prefs.getString(KEY_TAB_ORDER, null) ?: return listOf(
            LibraryTab.FOLDERS,
            LibraryTab.FAVORITES,
            LibraryTab.SONGS,
            LibraryTab.ALBUMS,
            LibraryTab.ARTISTS
        )
        return try {
            val tabs = raw.split(",")
                .mapNotNull { name -> LibraryTab.entries.find { it.name == name } }
            if (tabs.isNotEmpty()) tabs else listOf(
                LibraryTab.FOLDERS,
                LibraryTab.FAVORITES,
                LibraryTab.SONGS,
                LibraryTab.ALBUMS,
                LibraryTab.ARTISTS
            )
        } catch (e: Exception) {
            listOf(
                LibraryTab.FOLDERS,
                LibraryTab.FAVORITES,
                LibraryTab.SONGS,
                LibraryTab.ALBUMS,
                LibraryTab.ARTISTS
            )
        }
    }

    fun setTabOrder(order: List<LibraryTab>) {
        val raw = order.joinToString(",") { it.name }
        prefs.edit().putString(KEY_TAB_ORDER, raw).apply()
    }

    // 2. Favorites
    fun getFavoriteSongIds(): Set<Long> {
        val rawSet = prefs.getStringSet(KEY_FAVORITE_SONG_IDS, emptySet()) ?: emptySet()
        return rawSet.mapNotNull { it.toLongOrNull() }.toSet()
    }

    fun setFavoriteSongIds(ids: Set<Long>) {
        val rawSet = ids.map { it.toString() }.toSet()
        prefs.edit().putStringSet(KEY_FAVORITE_SONG_IDS, rawSet).apply()
    }

    // 3. Last Playback State
    fun getLastPlayedSongId(): Long = prefs.getLong(KEY_LAST_SONG_ID, -1L)

    fun getLastPlayedPositionMs(): Long = prefs.getLong(KEY_LAST_POSITION_MS, 0L)

    fun getLastPlayedQueueIds(): List<Long> {
        val raw = prefs.getString(KEY_LAST_QUEUE_IDS, null) ?: return emptyList()
        return raw.split(",").mapNotNull { it.toLongOrNull() }
    }

    fun getLastPlayedQueueIndex(): Int = prefs.getInt(KEY_LAST_QUEUE_INDEX, 0)

    fun saveLastPlaybackState(songId: Long, positionMs: Long, queueIds: List<Long>, queueIndex: Int) {
        prefs.edit()
            .putLong(KEY_LAST_SONG_ID, songId)
            .putLong(KEY_LAST_POSITION_MS, positionMs)
            .putString(KEY_LAST_QUEUE_IDS, queueIds.joinToString(","))
            .putInt(KEY_LAST_QUEUE_INDEX, queueIndex)
            .apply()
    }

    // 4. Settings Preferences
    var gaplessEnabled: Boolean
        get() = prefs.getBoolean(KEY_GAPLESS, true)
        set(value) = prefs.edit().putBoolean(KEY_GAPLESS, value).apply()

    var pauseOnUnplug: Boolean
        get() = prefs.getBoolean(KEY_PAUSE_ON_UNPLUG, true)
        set(value) = prefs.edit().putBoolean(KEY_PAUSE_ON_UNPLUG, value).apply()

    var filterShortAudio: Boolean
        get() = prefs.getBoolean(KEY_FILTER_SHORT_AUDIO, true)
        set(value) = prefs.edit().putBoolean(KEY_FILTER_SHORT_AUDIO, value).apply()

    var smartFilenameCleaner: Boolean
        get() = prefs.getBoolean(KEY_SMART_FILENAME_CLEANER, true)
        set(value) = prefs.edit().putBoolean(KEY_SMART_FILENAME_CLEANER, value).apply()

    var folderHierarchyFallback: Boolean
        get() = prefs.getBoolean(KEY_FOLDER_HIERARCHY_FALLBACK, true)
        set(value) = prefs.edit().putBoolean(KEY_FOLDER_HIERARCHY_FALLBACK, value).apply()

    companion object {
        private const val KEY_TAB_ORDER = "tab_order"
        private const val KEY_FAVORITE_SONG_IDS = "favorite_song_ids"
        private const val KEY_LAST_SONG_ID = "last_song_id"
        private const val KEY_LAST_POSITION_MS = "last_position_ms"
        private const val KEY_LAST_QUEUE_IDS = "last_queue_ids"
        private const val KEY_LAST_QUEUE_INDEX = "last_queue_index"
        private const val KEY_GAPLESS = "pref_gapless"
        private const val KEY_PAUSE_ON_UNPLUG = "pref_pause_on_unplug"
        private const val KEY_FILTER_SHORT_AUDIO = "pref_filter_short_audio"
        private const val KEY_SMART_FILENAME_CLEANER = "pref_smart_filename_cleaner"
        private const val KEY_FOLDER_HIERARCHY_FALLBACK = "pref_folder_hierarchy_fallback"
    }
}
