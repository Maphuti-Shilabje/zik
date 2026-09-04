package com.zik.music.audio

import androidx.annotation.OptIn
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import com.zik.music.audio.processors.ZikPassthroughProcessor
import com.zik.music.audio.spatial.SpatialMotionProcessor
import com.zik.music.audio.spatial.SpatialParameters

data class AudioEngineDiagnostics(
    val isProcessorActive: Boolean,
    val sampleRate: Int,
    val channelCount: Int,
    val encoding: Int,
    val processedBufferCount: Long,
    val processedByteCount: Long,
    val isSpatialMotionEnabled: Boolean,
    val spatialFrameCount: Long
)

@OptIn(UnstableApi::class)
class AudioEngine {

    val passthroughProcessor: ZikPassthroughProcessor = ZikPassthroughProcessor()
    val spatialMotionProcessor: SpatialMotionProcessor = SpatialMotionProcessor()

    fun getAudioProcessors(): Array<AudioProcessor> {
        return arrayOf(passthroughProcessor, spatialMotionProcessor)
    }

    fun setSpatialParameters(parameters: SpatialParameters) {
        spatialMotionProcessor.parameters = parameters
    }

    fun getDiagnostics(): AudioEngineDiagnostics {
        val format = passthroughProcessor.getActiveAudioFormat()
        return AudioEngineDiagnostics(
            isProcessorActive = passthroughProcessor.isActive,
            sampleRate = format.sampleRate,
            channelCount = format.channelCount,
            encoding = format.encoding,
            processedBufferCount = passthroughProcessor.processedBufferCount,
            processedByteCount = passthroughProcessor.processedByteCount,
            isSpatialMotionEnabled = spatialMotionProcessor.parameters.isEnabled,
            spatialFrameCount = spatialMotionProcessor.getProcessedFrameCount()
        )
    }

    fun release() {
        passthroughProcessor.reset()
        spatialMotionProcessor.reset()
    }
}
