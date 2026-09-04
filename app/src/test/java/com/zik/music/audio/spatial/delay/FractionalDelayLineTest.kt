package com.zik.music.audio.spatial.delay

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

class FractionalDelayLineTest {

    private lateinit var delayLine: FractionalDelayLine
    private val delta = 0.0001

    @Before
    fun setUp() {
        delayLine = FractionalDelayLine(bufferSize = 64)
    }

    @Test
    fun read_zeroDelay_returnsMostRecentWrittenSample() {
        delayLine.write(10.0)
        delayLine.write(20.0)
        delayLine.write(30.0)

        val out = delayLine.read(0.0)
        assertEquals(30.0, out, delta)
    }

    @Test
    fun read_integerDelay_returnsExactPastSample() {
        delayLine.write(100.0) // 2 samples ago (delay = 2.0)
        delayLine.write(200.0) // 1 sample ago (delay = 1.0)
        delayLine.write(300.0) // 0 samples ago (delay = 0.0)

        assertEquals(300.0, delayLine.read(0.0), delta)
        assertEquals(200.0, delayLine.read(1.0), delta)
        assertEquals(100.0, delayLine.read(2.0), delta)
    }

    @Test
    fun read_fractionalDelay_interpolatesLinearlyBetweenAdjacentSamples() {
        delayLine.write(10.0) // 1 sample ago
        delayLine.write(20.0) // 0 samples ago (most recent)

        // Delay of 0.5 should interpolate halfway between 20.0 (delay 0) and 10.0 (delay 1)
        val interpolated = delayLine.read(0.5)
        assertEquals(15.0, interpolated, delta)

        // Delay of 0.25 -> 0.75 * 20.0 + 0.25 * 10.0 = 17.5
        assertEquals(17.5, delayLine.read(0.25), delta)
    }

    @Test
    fun ringBuffer_wraparoundMaintainsSampleIntegrity() {
        // Write 100 samples into size-64 ring buffer
        for (i in 1..100) {
            delayLine.write(i.toDouble())
        }

        // Most recent is 100.0 (delay 0), 99.0 (delay 1), 98.0 (delay 2), etc.
        assertEquals(100.0, delayLine.read(0.0), delta)
        assertEquals(99.0, delayLine.read(1.0), delta)
        assertEquals(95.0, delayLine.read(5.0), delta)
    }

    @Test
    fun reset_clearsBufferToZero() {
        delayLine.write(50.0)
        delayLine.write(60.0)
        delayLine.reset()

        assertEquals(0.0, delayLine.read(0.0), delta)
        assertEquals(0.0, delayLine.read(1.0), delta)
    }

    @Test
    fun read_clampedOutOfBounds_doesNotCrashOrProduceNaN() {
        delayLine.write(42.0)
        val outLarge = delayLine.read(1000.0)
        val outNegative = delayLine.read(-5.0)

        assertFalse(outLarge.isNaN())
        assertFalse(outNegative.isNaN())
    }
}
