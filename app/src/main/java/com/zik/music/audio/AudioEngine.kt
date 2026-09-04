package com.zik.music.audio

import androidx.annotation.OptIn
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import com.zik.music.audio.processors.ZikPassthroughProcessor

data class AudioEngineDiagnostics(
    val isProcessorActive: Boolean,
    val sampleRate: Int,
    val channelCount: Int,
    val encoding: Int,
    val processedBufferCount: Long,
    val processedByteCount: Long
)

@OptIn(UnstableApi::class)
class AudioEngine {

    val passthroughProcessor: ZikPassthroughProcessor = ZikPassthroughProcessor()

    fun getAudioProcessors(): Array<AudioProcessor> {
        return arrayOf(passthroughProcessor)
    }

    fun getDiagnostics(): AudioEngineDiagnostics {
        val format = passthroughProcessor.getActiveAudioFormat()
        return AudioEngineDiagnostics(
            isProcessorActive = passthroughProcessor.isActive,
            sampleRate = format.sampleRate,
            channelCount = format.channelCount,
            encoding = format.encoding,
            processedBufferCount = passthroughProcessor.processedBufferCount,
            processedByteCount = passthroughProcessor.processedByteCount
        )
    }

    fun release() {
        passthroughProcessor.reset()
    }
}
