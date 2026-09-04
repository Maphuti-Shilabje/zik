package com.zik.music.audio

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

class AudioEngineTest {

    private lateinit var audioEngine: AudioEngine

    @Before
    fun setUp() {
        audioEngine = AudioEngine()
    }

    @Test
    fun getAudioProcessors_returnsConfiguredProcessorsArray() {
        val processors = audioEngine.getAudioProcessors()
        assertEquals(1, processors.size)
        assertNotNull(processors[0])
    }

    @Test
    fun getDiagnostics_reflectsProcessorStateAccurately() {
        var diag = audioEngine.getDiagnostics()
        assertFalse(diag.isProcessorActive)
        assertEquals(0L, diag.processedBufferCount)
        assertEquals(0L, diag.processedByteCount)

        val format = AudioFormat(44100, 2, C.ENCODING_PCM_16BIT)
        audioEngine.passthroughProcessor.configure(format)
        audioEngine.passthroughProcessor.flush()

        val data = ByteArray(256) { 7 }
        val buf = ByteBuffer.allocateDirect(256).order(ByteOrder.nativeOrder()).put(data)
        buf.flip()
        audioEngine.passthroughProcessor.queueInput(buf)

        diag = audioEngine.getDiagnostics()
        assertTrue(diag.isProcessorActive)
        assertEquals(44100, diag.sampleRate)
        assertEquals(2, diag.channelCount)
        assertEquals(C.ENCODING_PCM_16BIT, diag.encoding)
        assertEquals(1L, diag.processedBufferCount)
        assertEquals(256L, diag.processedByteCount)
    }

    @Test
    fun release_resetsAudioProcessorState() {
        val format = AudioFormat(44100, 2, C.ENCODING_PCM_16BIT)
        audioEngine.passthroughProcessor.configure(format)
        audioEngine.passthroughProcessor.flush()

        audioEngine.release()

        val diag = audioEngine.getDiagnostics()
        assertFalse(diag.isProcessorActive)
        assertEquals(0L, diag.processedBufferCount)
    }
}
