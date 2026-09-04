package com.zik.music.audio.processors

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.AudioProcessor.UnhandledAudioFormatException
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicLong

@OptIn(UnstableApi::class)
class ZikPassthroughProcessor : BaseAudioProcessor() {

    @Volatile
    private var configuredInputFormat: AudioFormat = AudioFormat.NOT_SET

    private val _processedBufferCount = AtomicLong(0)
    val processedBufferCount: Long
        get() = _processedBufferCount.get()

    private val _processedByteCount = AtomicLong(0)
    val processedByteCount: Long
        get() = _processedByteCount.get()

    override fun onConfigure(inputAudioFormat: AudioFormat): AudioFormat {
        val encoding = inputAudioFormat.encoding
        // Explicitly validate supported PCM encodings for safe byte-for-byte passthrough
        if (encoding != C.ENCODING_PCM_16BIT &&
            encoding != C.ENCODING_PCM_FLOAT &&
            encoding != C.ENCODING_PCM_24BIT &&
            encoding != C.ENCODING_PCM_32BIT
        ) {
            configuredInputFormat = AudioFormat.NOT_SET
            throw UnhandledAudioFormatException(inputAudioFormat)
        }

        configuredInputFormat = inputAudioFormat
        // Passthrough preserves input audio format unchanged
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) {
            return
        }

        // Allocate or reuse pooled buffer managed by BaseAudioProcessor
        val outputBuffer = replaceOutputBuffer(remaining)
        outputBuffer.put(inputBuffer)
        outputBuffer.flip()

        // Update real-time safe atomic diagnostic counters
        _processedBufferCount.incrementAndGet()
        _processedByteCount.addAndGet(remaining.toLong())
    }

    fun getActiveAudioFormat(): AudioFormat = configuredInputFormat

    override fun onReset() {
        configuredInputFormat = AudioFormat.NOT_SET
        _processedBufferCount.set(0)
        _processedByteCount.set(0)
    }
}
