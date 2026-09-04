package com.zik.music.audio.spatial.filter

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.abs
import kotlin.math.sin

class HeadShadowFilterTest {

    private lateinit var filter: HeadShadowFilter
    private val sampleRate = 44100

    @Before
    fun setUp() {
        filter = HeadShadowFilter(smoothingTauSeconds = 0.025)
        filter.configure(sampleRate)
    }

    @Test
    fun process_atMaxCutoff_passesSignalWithoutAttenuation() {
        // At 20000Hz cutoff (near Nyquist), alpha should be 1.0 (direct passthrough)
        val input = 15000.0
        val output = filter.process(input, targetCutoffHz = 20000.0)

        assertEquals(input, output, 0.01)
    }

    @Test
    fun process_lowFrequencySineWave_preservedAtShadowCutoff() {
        // 100 Hz sine wave through 2000 Hz low-pass filter
        val freq = 100.0
        val amplitude = 10000.0
        val numSamples = 5000
        var maxObservedOutput = 0.0

        for (n in 0 until numSamples) {
            val sample = amplitude * sin(2.0 * Math.PI * freq * n / sampleRate)
            val out = filter.process(sample, targetCutoffHz = 2000.0)
            // Measure peak after filter settling (last 1000 samples)
            if (n >= 4000) {
                maxObservedOutput = maxOf(maxObservedOutput, abs(out))
            }
        }

        // Low frequency should be preserved with >95% amplitude (> -0.5 dB)
        assertTrue(
            "Low frequency output ($maxObservedOutput) should be >= 95% of input ($amplitude)",
            maxObservedOutput >= amplitude * 0.95
        )
    }

    @Test
    fun process_highFrequencySineWave_stronglyAttenuatedAtShadowCutoff() {
        // 10000 Hz sine wave through 2000 Hz low-pass filter
        val freq = 10000.0
        val amplitude = 10000.0
        val numSamples = 5000
        var maxObservedOutput = 0.0

        for (n in 0 until numSamples) {
            val sample = amplitude * sin(2.0 * Math.PI * freq * n / sampleRate)
            val out = filter.process(sample, targetCutoffHz = 2000.0)
            // Measure peak after filter settling (last 1000 samples)
            if (n >= 4000) {
                maxObservedOutput = maxOf(maxObservedOutput, abs(out))
            }
        }

        // High frequency (10kHz) should be strongly attenuated (<30% amplitude, > -10 dB)
        assertTrue(
            "High frequency output ($maxObservedOutput) should be < 30% of input ($amplitude)",
            maxObservedOutput < amplitude * 0.30
        )
    }

    @Test
    fun process_numericalStability_noNaNOrInfinity() {
        val testInputs = listOf(
            32767.0, -32768.0, 0.0, 1.0, -1.0, 100000.0, -100000.0
        )

        for (input in testInputs) {
            val out = filter.process(input, targetCutoffHz = 2000.0)
            assertFalse("Filter output should not be NaN", out.isNaN())
            assertFalse("Filter output should not be Infinite", out.isInfinite())
        }
    }

    @Test
    fun process_parameterSmoothing_noDiscontinuitiesWhenCutoffSteps() {
        // Prime filter at 20000 Hz
        for (i in 0 until 500) {
            filter.process(10000.0, 20000.0)
        }

        // Abruptly change target cutoff to 2000 Hz
        var prevOut = filter.getState()
        for (i in 0 until 200) {
            val out = filter.process(10000.0, 2000.0)
            val stepDifference = abs(out - prevOut)
            // The step difference between adjacent samples should be continuous and smooth
            assertTrue("Step difference ($stepDifference) should be smooth without Dirac spikes", stepDifference < 500.0)
            prevOut = out
        }
    }

    @Test
    fun reset_clearsFilterStateAndAlpha() {
        // Run signal
        for (i in 0 until 500) {
            filter.process(15000.0, 2000.0)
        }
        assertTrue(filter.getState() > 0.0)

        filter.reset()

        assertEquals(0.0, filter.getState(), 0.0001)
        assertEquals(1.0, filter.getCurrentAlpha(), 0.0001)
    }
}
