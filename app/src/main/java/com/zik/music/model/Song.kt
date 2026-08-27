package com.zik.music.model

import android.net.Uri

data class Song(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val contentUriString: String,
    val filePath: String,
    val folderName: String,
    val trackNumber: Int = 0,
    val year: Int = 0,
    val albumArtUriString: String? = null,
    val playCount: Int = 0,
    val skipCount: Int = 0,
    val lastPlayedTimestamp: Long = 0L
) {
    val contentUri: Uri get() = Uri.parse(contentUriString)
    val albumArtUri: Uri? get() = albumArtUriString?.let { Uri.parse(it) }
}
