package com.zik.music.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.AudioProcessor.UnhandledAudioFormatException
import com.zik.music.audio.processors.ZikPassthroughProcessor
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class ZikPassthroughProcessorTest {

    private lateinit var processor: ZikPassthroughProcessor

    @Before
    fun setUp() {
        processor = ZikPassthroughProcessor()
    }

    @Test
    fun configure_supportedPcm16Bit_preservesFormatAndActivates() {
        val inputFormat = AudioFormat(44100, 2, C.ENCODING_PCM_16BIT)
        val outputFormat = processor.configure(inputFormat)

        assertEquals(inputFormat.sampleRate, outputFormat.sampleRate)
        assertEquals(inputFormat.channelCount, outputFormat.channelCount)
        assertEquals(inputFormat.encoding, outputFormat.encoding)
        assertTrue(processor.isActive)
        assertEquals(inputFormat, processor.getActiveAudioFormat())
    }

    @Test
    fun configure_supportedPcmFloat_preservesFormatAndActivates() {
        val inputFormat = AudioFormat(48000, 2, C.ENCODING_PCM_FLOAT)
        val outputFormat = processor.configure(inputFormat)

        assertEquals(inputFormat.sampleRate, outputFormat.sampleRate)
        assertEquals(inputFormat.channelCount, outputFormat.channelCount)
        assertEquals(inputFormat.encoding, outputFormat.encoding)
        assertTrue(processor.isActive)
    }

    @Test(expected = UnhandledAudioFormatException::class)
    fun configure_unsupportedEncoding_throwsUnhandledAudioFormatException() {
        val inputFormat = AudioFormat(44100, 2, C.ENCODING_INVALID)
        processor.configure(inputFormat)
    }

    @Test
    fun queueInput_passesBytesThroughExactly() {
        val format = AudioFormat(44100, 2, C.ENCODING_PCM_16BIT)
        processor.configure(format)
        processor.flush()

        val sampleData = ByteArray(512) { (it % 255).toByte() }
        val inputBuffer = ByteBuffer.allocateDirect(sampleData.size)
            .order(ByteOrder.nativeOrder())
            .put(sampleData)
        inputBuffer.flip()

        processor.queueInput(inputBuffer)

        // Verify input buffer consumed completely
        assertEquals(0, inputBuffer.remaining())
        assertEquals(sampleData.size, inputBuffer.position())

        // Verify output buffer matches exact input bytes
        val outputBuffer = processor.output
        val outputData = ByteArray(outputBuffer.remaining())
        outputBuffer.get(outputData)

        assertArrayEquals(sampleData, outputData)
        assertEquals(1L, processor.processedBufferCount)
        assertEquals(sampleData.size.toLong(), processor.processedByteCount)
    }

    @Test
    fun queueInput_multipleConsecutiveBuffers_tracksDiagnosticsAccurately() {
        val format = AudioFormat(44100, 2, C.ENCODING_PCM_16BIT)
        processor.configure(format)
        processor.flush()

        val bufferSizes = listOf(256, 512, 1024)
        var totalBytes = 0L

        for ((index, size) in bufferSizes.withIndex()) {
            val data = ByteArray(size) { ((it + index) % 128).toByte() }
            val inputBuffer = ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder()).put(data)
            inputBuffer.flip()

            processor.queueInput(inputBuffer)
            totalBytes += size

            val outputBuffer = processor.output
            val outputData = ByteArray(outputBuffer.remaining())
            outputBuffer.get(outputData)
            assertArrayEquals(data, outputData)
        }

        assertEquals(3L, processor.processedBufferCount)
        assertEquals(totalBytes, processor.processedByteCount)
    }

    @Test
    fun queueEndOfStream_marksProcessorAsEndedAfterOutputConsumed() {
        val format = AudioFormat(44100, 2, C.ENCODING_PCM_16BIT)
        processor.configure(format)
        processor.flush()

        val data = ByteArray(128) { 1 }
        val inputBuffer = ByteBuffer.allocateDirect(128).order(ByteOrder.nativeOrder()).put(data)
        inputBuffer.flip()

        processor.queueInput(inputBuffer)
        processor.queueEndOfStream()

        val output = processor.output
        // Drain output
        output.position(output.limit())

        assertTrue(processor.isEnded)
    }

    @Test
    fun reset_clearsDiagnosticsAndActiveFormat() {
        val format = AudioFormat(44100, 2, C.ENCODING_PCM_16BIT)
        processor.configure(format)
        processor.flush()

        val data = ByteArray(64) { 42 }
        val inputBuffer = ByteBuffer.allocateDirect(64).order(ByteOrder.nativeOrder()).put(data)
        inputBuffer.flip()
        processor.queueInput(inputBuffer)

        processor.reset()

        assertFalse(processor.isActive)
        assertEquals(0L, processor.processedBufferCount)
        assertEquals(0L, processor.processedByteCount)
        assertEquals(AudioFormat.NOT_SET, processor.getActiveAudioFormat())
    }
}
