package com.zik.music.data

import com.zik.music.model.LyricLine
import java.io.File

object LrcParser {

    private val TIME_TAG_REGEX = Regex("""\[(\d{1,2}):(\d{2})(?:[.:](\d{2,3}))?]""")

    /**
     * Parses standard LRC content into a sorted list of LyricLine.
     * Supports multiple timestamps per line (e.g. "[00:12.00][00:30.00] Repeat line")
     */
    fun parse(lrcContent: String): List<LyricLine> {
        val lines = mutableListOf<LyricLine>()

        lrcContent.lineSequence().forEach { rawLine ->
            val trimmed = rawLine.trim()
            if (trimmed.isBlank() || trimmed.startsWith("[ti:") || trimmed.startsWith("[ar:") || 
                trimmed.startsWith("[al:") || trimmed.startsWith("[by:") || trimmed.startsWith("[offset:")) {
                return@forEach
            }

            val matches = TIME_TAG_REGEX.findAll(trimmed).toList()
            if (matches.isNotEmpty()) {
                val text = trimmed.replace(TIME_TAG_REGEX, "").trim()
                for (match in matches) {
                    val minutes = match.groupValues[1].toLongOrNull() ?: 0L
                    val seconds = match.groupValues[2].toLongOrNull() ?: 0L
                    val millisStr = match.groupValues[3]
                    val millis = when (millisStr.length) {
                        2 -> millisStr.toLong() * 10
                        3 -> millisStr.toLong()
                        else -> 0L
                    }
                    val timestampMs = (minutes * 60 * 1000) + (seconds * 1000) + millis
                    lines.add(LyricLine(timestampMs = timestampMs, text = text))
                }
            }
        }

        return lines.sortedBy { it.timestampMs }
    }

    /**
     * Locates a companion .lrc file in the same directory as the audio file.
     * E.g. "/path/to/Song.mp3" -> "/path/to/Song.lrc"
     */
    fun findCompanionLrc(audioFilePath: String): File? {
        val audioFile = File(audioFilePath)
        val lrcFile = File(audioFile.parentFile, "${audioFile.nameWithoutExtension}.lrc")
        return if (lrcFile.exists() && lrcFile.canRead()) lrcFile else null
    }
}
