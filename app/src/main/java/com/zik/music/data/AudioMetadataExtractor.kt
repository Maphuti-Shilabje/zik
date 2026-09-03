package com.zik.music.data

import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMetadataRetriever
import android.os.Build
import com.zik.music.model.Song
import java.io.File
import java.util.Locale

data class AudioDetails(
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String?,
    val genre: String?,
    val year: String?,
    val trackNumber: String?,
    val discNumber: String?,
    val composer: String?,
    val codec: String,
    val mimeType: String,
    val bitrateKbps: Int?,
    val sampleRateHz: Int?,
    val channelCount: Int?,
    val bitDepth: Int?,
    val fileSizeFormatted: String,
    val filePath: String,
    val fileName: String,
    val durationMs: Long,
    val isLossless: Boolean,
    val isHiRes: Boolean
)

object AudioMetadataExtractor {

    fun extract(context: Context, song: Song): AudioDetails {
        val file = File(song.filePath)
        val fileExists = file.exists()
        val fileLength = if (fileExists) file.length() else 0L
        val fileName = if (fileExists) file.name else song.title
        val filePath = song.filePath

        var albumArtist: String? = null
        var genre: String? = null
        var year: String? = if (song.year > 0) song.year.toString() else null
        var trackNumber: String? = if (song.trackNumber > 0) song.trackNumber.toString() else null
        var discNumber: String? = null
        var composer: String? = null
        var extractedBitrate: Int? = null
        var extractedSampleRate: Int? = null
        var extractedMime: String? = null
        var channelCount: Int? = null
        var bitDepth: Int? = null

        val retriever = MediaMetadataRetriever()
        try {
            if (fileExists) {
                retriever.setDataSource(song.filePath)
            } else {
                retriever.setDataSource(context, song.contentUri)
            }

            albumArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
            genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
            val extractedYear = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)
                ?: retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DATE)
            if (!extractedYear.isNullOrBlank()) year = extractedYear

            val extractedTrack = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
            if (!extractedTrack.isNullOrBlank()) trackNumber = extractedTrack

            discNumber = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER)
            composer = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER)

            val bitrateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)
            extractedBitrate = bitrateStr?.toIntOrNull()?.let { it / 1000 }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val sampleRateStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)
                extractedSampleRate = sampleRateStr?.toIntOrNull()
            }

            extractedMime = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                retriever.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Detailed hardware format probe using MediaExtractor
        val extractor = MediaExtractor()
        try {
            if (fileExists) {
                extractor.setDataSource(song.filePath)
            } else {
                extractor.setDataSource(context, song.contentUri, null)
            }

            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    if (extractedMime.isNullOrEmpty()) extractedMime = mime
                    if (extractedSampleRate == null && format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        extractedSampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    }
                    if (format.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) {
                        channelCount = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                    }
                    if (format.containsKey(MediaFormat.KEY_BIT_RATE) && extractedBitrate == null) {
                        extractedBitrate = format.getInteger(MediaFormat.KEY_BIT_RATE) / 1000
                    }
                    if (format.containsKey("bits-per-sample")) {
                        bitDepth = format.getInteger("bits-per-sample")
                    }
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                extractor.release()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val extension = file.extension.lowercase(Locale.ROOT)
        val codec = determineCodecName(extractedMime, extension)
        val isLossless = isLosslessFormat(codec, extension)
        val sampleRate = extractedSampleRate ?: 44100
        val isHiRes = (sampleRate >= 48000 && (bitDepth ?: 16) >= 24) || sampleRate >= 96000

        return AudioDetails(
            title = song.title,
            artist = song.artist,
            album = song.album,
            albumArtist = albumArtist,
            genre = genre,
            year = year,
            trackNumber = trackNumber,
            discNumber = discNumber,
            composer = composer,
            codec = codec,
            mimeType = extractedMime ?: "audio/$extension",
            bitrateKbps = extractedBitrate,
            sampleRateHz = extractedSampleRate,
            channelCount = channelCount ?: 2,
            bitDepth = bitDepth ?: if (isLossless) 16 else null,
            fileSizeFormatted = formatFileSize(fileLength),
            filePath = filePath,
            fileName = fileName,
            durationMs = song.durationMs,
            isLossless = isLossless,
            isHiRes = isHiRes
        )
    }

    private fun determineCodecName(mime: String?, ext: String): String {
        return when {
            mime?.contains("flac") == true || ext == "flac" -> "FLAC"
            mime?.contains("mp4a-latm") == true || mime?.contains("aac") == true || ext == "aac" || ext == "m4a" -> "AAC"
            mime?.contains("mpeg") == true || ext == "mp3" -> "MP3"
            mime?.contains("opus") == true || ext == "opus" -> "Opus"
            mime?.contains("vorbis") == true || ext == "ogg" -> "OGG Vorbis"
            mime?.contains("raw") == true || mime?.contains("wav") == true || ext == "wav" -> "WAV / PCM"
            ext == "alac" -> "ALAC"
            ext == "aiff" || ext == "aif" -> "AIFF"
            ext == "wma" -> "WMA"
            else -> ext.uppercase(Locale.ROOT).ifEmpty { "Audio" }
        }
    }

    private fun isLosslessFormat(codec: String, ext: String): Boolean {
        return codec in listOf("FLAC", "WAV / PCM", "ALAC", "AIFF") || ext in listOf("flac", "wav", "alac", "aiff", "aif")
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "Unknown"
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format(Locale.US, "%.2f GB", gb)
            mb >= 1.0 -> String.format(Locale.US, "%.1f MB", mb)
            else -> String.format(Locale.US, "%.0f KB", kb)
        }
    }
}
