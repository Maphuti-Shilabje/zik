package com.zik.music.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.zik.music.model.Folder
import com.zik.music.model.Song
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MediaStoreScanner(private val context: Context) {

    private val albumArtBaseUri = Uri.parse("content://media/external/audio/albumart")

    suspend fun scanSongs(): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA, // File path
            MediaStore.Audio.Media.TRACK,
            MediaStore.Audio.Media.YEAR
        )

        // Ignore short audio clips (< 15 seconds) like ringtones or WhatsApp audio
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= 15000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val trackCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
                val yearCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val rawTitle = cursor.getString(titleCol)
                    val rawArtist = cursor.getString(artistCol)
                    val rawAlbum = cursor.getString(albumCol)
                    val albumId = cursor.getLong(albumIdCol)
                    val duration = cursor.getLong(durationCol)
                    val filePath = cursor.getString(dataCol) ?: ""
                    val trackNumber = cursor.getInt(trackCol)
                    val year = cursor.getInt(yearCol)

                    // Robust heuristic sanitization for messy filenames and missing tags
                    val cleaned = MetadataSanitizer.sanitize(
                        rawTitle = rawTitle,
                        rawArtist = rawArtist,
                        rawAlbum = rawAlbum,
                        filePath = filePath
                    )

                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    
                    // Album art resolution: first check MediaStore album art URI, then folder cover.jpg
                    var artUriStr: String? = ContentUris.withAppendedId(albumArtBaseUri, albumId).toString()
                    val folderCover = findFolderCover(filePath)
                    if (folderCover != null) {
                        artUriStr = Uri.fromFile(folderCover).toString()
                    }

                    val file = File(filePath)
                    val folderName = file.parentFile?.name ?: "Unknown Folder"

                    songs.add(
                        Song(
                            id = id,
                            title = cleaned.title,
                            artist = cleaned.artist,
                            album = cleaned.album,
                            albumId = albumId,
                            durationMs = duration,
                            contentUriString = contentUri.toString(),
                            filePath = filePath,
                            folderName = folderName,
                            trackNumber = trackNumber,
                            year = year,
                            albumArtUriString = artUriStr
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        songs
    }

    /**
     * Groups scanned songs into directory folders for zero-effort, first-class folder navigation.
     */
    fun groupIntoFolders(songs: List<Song>): List<Folder> {
        return songs.groupBy { 
            File(it.filePath).parent ?: "Unknown"
        }.map { (path, folderSongs) ->
            val folderName = File(path).name.ifBlank { "Root" }
            Folder(
                path = path,
                name = folderName,
                songCount = folderSongs.size,
                songs = folderSongs.sortedBy { it.title.lowercase() }
            )
        }.sortedBy { it.name.lowercase() }
    }

    private fun findFolderCover(audioPath: String): File? {
        if (audioPath.isBlank()) return null
        val parent = File(audioPath).parentFile ?: return null
        val candidates = listOf("cover.jpg", "cover.png", "folder.jpg", "folder.png", "albumart.jpg", "front.jpg")
        for (candidate in candidates) {
            val file = File(parent, candidate)
            if (file.exists() && file.canRead()) {
                return file
            }
        }
        return null
    }
}
