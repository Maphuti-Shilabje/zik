package com.zik.music.data

import java.io.File

object MetadataSanitizer {

    // Regex patterns for web download noise, bitrates, and video tags
    private val JUNK_PATTERNS = listOf(
        Regex("""\[.*?official.*?]""", RegexOption.IGNORE_CASE),
        Regex("""\(.*?official.*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\[.*?video.*?]""", RegexOption.IGNORE_CASE),
        Regex("""\(.*?video.*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\[.*?audio.*?]""", RegexOption.IGNORE_CASE),
        Regex("""\(.*?audio.*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\[.*?lyrics?.*?]""", RegexOption.IGNORE_CASE),
        Regex("""\(.*?lyrics?.*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\[.*?remaster.*?]""", RegexOption.IGNORE_CASE),
        Regex("""\(.*?remaster.*?\)""", RegexOption.IGNORE_CASE),
        Regex("""\[\d+kbps\]""", RegexOption.IGNORE_CASE),
        Regex("""\(\d+kbps\)""", RegexOption.IGNORE_CASE),
        Regex("""\[(hq|hd|4k|1080p|720p)\]""", RegexOption.IGNORE_CASE),
        Regex("""\((hq|hd|4k|1080p|720p)\)""", RegexOption.IGNORE_CASE),
        Regex("""\[.*?(y2mate|yt-dlp|ssyoutube|mp3download|tubidy).*?\]""", RegexOption.IGNORE_CASE),
        Regex("""\b(y2mate\.com|y2mate\.is|tubidy\.mobi)\b""", RegexOption.IGNORE_CASE),
        Regex("""(_\d+k|\.mp3|\.flac|\.m4a|\.wav|\.ogg)$""", RegexOption.IGNORE_CASE)
    )

    // Leading track number: e.g. "01.", "01 -", "01 ", "1 - "
    private val LEADING_TRACK_NUMBER_REGEX = Regex("""^\s*\d{1,3}[\s.\-_]+\s*""")

    fun isUnknown(value: String?): Boolean {
        if (value.isNullOrBlank()) return true
        val lower = value.trim().lowercase()
        return lower in listOf("<unknown>", "unknown", "unknown artist", "unknown album", "untitled")
    }

    data class CleanedMetadata(
        val title: String,
        val artist: String,
        val album: String
    )

    /**
     * Sanitizes raw tags and raw filenames with directory context.
     * Guaranteed never to return empty strings or "<unknown>" if folder/file info exists.
     */
    fun sanitize(
        rawTitle: String?,
        rawArtist: String?,
        rawAlbum: String?,
        filePath: String
    ): CleanedMetadata {
        val file = File(filePath)
        val fileNameWithoutExt = file.nameWithoutExtension

        var cleanArtist = if (!isUnknown(rawArtist)) rawArtist!!.trim() else ""
        var cleanTitle = if (!isUnknown(rawTitle)) rawTitle!!.trim() else ""
        var cleanAlbum = if (!isUnknown(rawAlbum)) rawAlbum!!.trim() else ""

        // If title is missing or identical to raw messy filename, parse from filename
        val needsFilenameParsing = cleanTitle.isEmpty() || 
                                   cleanTitle.equals(fileNameWithoutExt, ignoreCase = true) ||
                                   cleanArtist.isEmpty()

        if (needsFilenameParsing) {
            val parsed = parseFromFileName(fileNameWithoutExt)
            if (cleanTitle.isEmpty() || isUnknown(cleanTitle)) {
                cleanTitle = parsed.title
            }
            if (cleanArtist.isEmpty() || isUnknown(cleanArtist)) {
                cleanArtist = parsed.artist
            }
        }

        // Clean out download garbage and video watermarks from title
        cleanTitle = stripJunk(cleanTitle)

        // Fallback: If artist is still empty, inspect parent directory
        if (cleanArtist.isEmpty()) {
            val parent = file.parentFile
            val grandParent = parent?.parentFile
            val parentName = parent?.name ?: ""
            val grandParentName = grandParent?.name ?: ""

            cleanArtist = when {
                !isGenericFolder(grandParentName) -> grandParentName
                !isGenericFolder(parentName) -> parentName
                else -> "Unknown Artist"
            }
        }

        // Fallback: If album is still empty, use immediate folder name
        if (cleanAlbum.isEmpty()) {
            val parentName = file.parentFile?.name ?: ""
            cleanAlbum = if (!isGenericFolder(parentName)) parentName else "Local Audio"
        }

        if (cleanTitle.isBlank()) {
            cleanTitle = fileNameWithoutExt.ifBlank { "Track" }
        }

        return CleanedMetadata(
            title = cleanTitle,
            artist = cleanArtist,
            album = cleanAlbum
        )
    }

    fun stripJunk(input: String): String {
        var result = input
        for (pattern in JUNK_PATTERNS) {
            result = pattern.replace(result, "")
        }
        // Also strip leading track numbers
        result = LEADING_TRACK_NUMBER_REGEX.replace(result, "")
        // Clean multi-spaces and trailing dashes
        result = result.replace(Regex("""\s+"""), " ").trim()
        result = result.trim('-', '_', ' ')
        return result.ifBlank { input.trim() }
    }

    private fun parseFromFileName(fileName: String): CleanedMetadata {
        var base = stripJunk(fileName)

        // Check common delimiters: " - ", " ~ ", " _ "
        val delimiters = listOf(" - ", " ~ ", " — ", " _ ")
        for (delim in delimiters) {
            if (base.contains(delim)) {
                val parts = base.split(delim, limit = 2)
                val part0 = stripJunk(parts[0].trim())
                val part1 = stripJunk(parts[1].trim())
                if (part0.isNotBlank() && part1.isNotBlank()) {
                    return CleanedMetadata(
                        artist = part0,
                        title = part1,
                        album = ""
                    )
                }
            }
        }

        return CleanedMetadata(
            artist = "",
            title = base,
            album = ""
        )
    }

    private fun isGenericFolder(folderName: String): Boolean {
        val lower = folderName.lowercase()
        return lower in listOf(
            "music", "download", "downloads", "audio", "internal storage", 
            "sdcard", "bluetooth", "whatsapp audio", "telegram audio", "media", "zik"
        )
    }
}
