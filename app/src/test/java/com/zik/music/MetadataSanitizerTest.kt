package com.zik.music

import com.zik.music.data.MetadataSanitizer
import org.junit.Assert.assertEquals
import org.junit.Test

class MetadataSanitizerTest {

    @Test
    fun testStripJunkFromFilename() {
        val messy = "01. Queen - Bohemian Rhapsody [Official Video] (320kbps) [y2mate.is].mp3"
        val cleaned = MetadataSanitizer.sanitize(
            rawTitle = null,
            rawArtist = null,
            rawAlbum = null,
            filePath = "/storage/emulated/0/Music/$messy"
        )

        assertEquals("Queen", cleaned.artist)
        assertEquals("Bohemian Rhapsody", cleaned.title)
    }

    @Test
    fun testStripTrackNumberAndBitrate() {
        val messy = "04 - Daft Punk - Get Lucky (Official Audio) [HQ].flac"
        val cleaned = MetadataSanitizer.sanitize(
            rawTitle = "<unknown>",
            rawArtist = "<unknown>",
            rawAlbum = "",
            filePath = "/storage/emulated/0/Music/Random/$messy"
        )

        assertEquals("Daft Punk", cleaned.artist)
        assertEquals("Get Lucky", cleaned.title)
        assertEquals("Random", cleaned.album)
    }

    @Test
    fun testFallbackToFolderWhenNoDelimiter() {
        val filename = "TrackNameWithoutArtist [lyrics].mp3"
        val cleaned = MetadataSanitizer.sanitize(
            rawTitle = null,
            rawArtist = null,
            rawAlbum = null,
            filePath = "/storage/emulated/0/Music/Coldplay/Parachutes/$filename"
        )

        assertEquals("Coldplay", cleaned.artist)
        assertEquals("TrackNameWithoutArtist", cleaned.title)
        assertEquals("Parachutes", cleaned.album)
    }

    @Test
    fun testRespectExistingCleanTags() {
        val cleaned = MetadataSanitizer.sanitize(
            rawTitle = "Comfortably Numb",
            rawArtist = "Pink Floyd",
            rawAlbum = "The Wall",
            filePath = "/storage/emulated/0/Music/track.mp3"
        )

        assertEquals("Pink Floyd", cleaned.artist)
        assertEquals("Comfortably Numb", cleaned.title)
        assertEquals("The Wall", cleaned.album)
    }
}
