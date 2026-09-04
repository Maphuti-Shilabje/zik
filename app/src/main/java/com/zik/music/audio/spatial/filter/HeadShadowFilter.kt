package com.zik.music.audio.spatial.filter

import kotlin.math.exp

/**
 * Real-time discrete first-order Low-Pass Filter (One-Pole IIR Filter)
 * for head-shadow spectral filtering.
 *
 * Difference equation:
 * y[n] = y[n-1] + alpha * (x[n] - y[n-1])
 *
 * Characteristics:
 * - Zero heap allocations in audio processing loop
 * - DC gain is exactly unity (0 dB)
 * - Exponential parameter smoothing to prevent coefficient zippering
 * - Unconditionally stable for all positive cutoff frequencies
 */
class HeadShadowFilter(
    val smoothingTauSeconds: Double = 0.025
) {
    private var sampleRate: Int = 44100
    private var smoothingCoeff: Double = 0.005

    private var currentAlpha: Double = 1.0
    private var state: Double = 0.0

    fun configure(sampleRate: Int) {
        this.sampleRate = sampleRate
        smoothingCoeff = 1.0 - exp(-1.0 / (sampleRate * smoothingTauSeconds))
        reset()
    }

    fun reset() {
        currentAlpha = 1.0
        state = 0.0
    }

    /**
     * Processes a single audio sample through the one-pole low-pass filter.
     *
     * @param sample The input PCM audio sample in Double precision
     * @param targetCutoffHz Target cutoff frequency in Hertz
     * @return Filtered sample value
     */
    fun process(sample: Double, targetCutoffHz: Double): Double {
        val targetAlpha = calculateAlpha(targetCutoffHz)

        // Exponential smoothing of filter coefficient to eliminate zipper noise
        currentAlpha += smoothingCoeff * (targetAlpha - currentAlpha)

        // Filter step: y[n] = y[n-1] + alpha * (x[n] - y[n-1])
        state += currentAlpha * (sample - state)
        return state
    }

    /**
     * Calculates the alpha filter coefficient from cutoff frequency and sample rate.
     */
    fun calculateAlpha(cutoffHz: Double): Double {
        // Nyquist frequency limit
        val nyquist = sampleRate * 0.5
        if (cutoffHz >= nyquist || cutoffHz >= 20000.0) {
            return 1.0
        }
        val clampedCutoff = cutoffHz.coerceAtLeast(20.0)
        val normalizedFreq = (2.0 * Math.PI * clampedCutoff) / sampleRate
        return (1.0 - exp(-normalizedFreq)).coerceIn(0.0, 1.0)
    }

    fun getState(): Double = state
    fun getCurrentAlpha(): Double = currentAlpha
}
