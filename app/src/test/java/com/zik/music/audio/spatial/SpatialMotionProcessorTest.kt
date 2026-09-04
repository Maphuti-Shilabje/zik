package com.zik.music.audio.spatial

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.audio.AudioProcessor.UnhandledAudioFormatException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SpatialMotionProcessorTest {

    private lateinit var processor: SpatialMotionProcessor

    @Before
    fun setUp() {
        processor = SpatialMotionProcessor(
            initialParameters = SpatialParameters(isEnabled = false, speedHz = 0.2, radius = 2.0)
        )
    }

    @Test
    fun configure_validStereo16Bit_succeedsAndActivates() {
        val format = AudioFormat(44100, 2, C.ENCODING_PCM_16BIT)
        val outputFormat = processor.configure(format)

        assertEquals(format.sampleRate, outputFormat.sampleRate)
        assertEquals(format.channelCount, outputFormat.channelCount)
        assertEquals(format.encoding, outputFormat.encoding)
        assertTrue(processor.isActive)
    }

    @Test(expected = UnhandledAudioFormatException::class)
    fun configure_mono16Bit_throwsUnhandledAudioFormatException() {
        val format = AudioFormat(44100, 1, C.ENCODING_PCM_16BIT)
        processor.configure(format)
    }

    @Test
    fun queueInput_disabledMode_producesByteExactPassthrough() {
        val format = AudioFormat(44100, 2, C.ENCODING_PCM_16BIT)
        processor.configure(format)
        processor.flush()

        val sampleData = ByteArray(512) { (it * 3 % 255).toByte() }
        val inputBuffer = ByteBuffer.allocateDirect(sampleData.size)
            .order(ByteOrder.nativeOrder())
            .put(sampleData)
        inputBuffer.flip()

        processor.queueInput(inputBuffer)

        assertEquals(0, inputBuffer.remaining())
        val outputBuffer = processor.output
        val outputData = ByteArray(outputBuffer.remaining())
        outputBuffer.get(outputData)

        assertArrayEquals(sampleData, outputData)
        assertEquals(128L, processor.getProcessedFrameCount()) // 512 bytes / 4 bytes per stereo frame = 128 frames
    }

    @Test
    fun queueInput_enabledMode_spatiallyProcessesStereoSignal() {
        processor.parameters = SpatialParameters(isEnabled = true, speedHz = 0.5, radius = 2.0)
        val format = AudioFormat(44100, 2, C.ENCODING_PCM_16BIT)
        processor.configure(format)
        processor.flush()

        // 1024 bytes = 256 stereo frames
        val inputBuffer = ByteBuffer.allocateDirect(1024).order(ByteOrder.nativeOrder())
        for (i in 0 until 256) {
            inputBuffer.putShort(15000.toShort()) // Left sample
            inputBuffer.putShort(15000.toShort()) // Right sample
        }
        inputBuffer.flip()

        processor.queueInput(inputBuffer)

        val outputBuffer = processor.output
        assertEquals(1024, outputBuffer.remaining())

        val outputShorts = outputBuffer.asShortBuffer()
        var hasDiverged = false
        while (outputShorts.hasRemaining()) {
            val leftOut = outputShorts.get()
            val rightOut = outputShorts.get()
            // In active spatial motion, left and right will diverge as source orbits
            if (leftOut != rightOut) {
                hasDiverged = true
                break
            }
        }

        assertTrue("Stereo signal should diverge as virtual source orbits around listener", hasDiverged)
        assertEquals(256L, processor.getProcessedFrameCount())
    }

    @Test
    fun queueInput_timelineAdvancesDeterministicallyWithFrames() {
        val format = AudioFormat(48000, 2, C.ENCODING_PCM_16BIT)
        processor.configure(format)
        processor.flush()

        val bufferSize = 480 // 120 frames (480 / 4)
        for (step in 1..5) {
            val buffer = ByteBuffer.allocateDirect(bufferSize).order(ByteOrder.nativeOrder())
            buffer.position(bufferSize)
            buffer.flip()

            processor.queueInput(buffer)
            assertEquals((120 * step).toLong(), processor.getProcessedFrameCount())
        }
    }

    @Test
    fun onFlushAndReset_clearsStateCleanly() {
        val format = AudioFormat(44100, 2, C.ENCODING_PCM_16BIT)
        processor.configure(format)
        processor.flush()

        val buffer = ByteBuffer.allocateDirect(256).order(ByteOrder.nativeOrder())
        buffer.position(256)
        buffer.flip()
        processor.queueInput(buffer)

        processor.reset()

        assertFalse(processor.isActive)
        assertEquals(0L, processor.getProcessedFrameCount())
    }
}
