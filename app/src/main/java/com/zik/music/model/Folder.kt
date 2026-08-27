package com.zik.music.model

data class Folder(
    val path: String,
    val name: String,
    val songCount: Int,
    val songs: List<Song> = emptyList()
)
