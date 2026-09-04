package com.zik.music.audio.spatial.delay

import kotlin.math.floor

/**
 * Real-time safe circular fractional delay line with first-order linear interpolation.
 *
 * Buffer capacity: 512 samples (~11.6ms at 44.1kHz, ~2.6ms at 192kHz), safely exceeding
 * maximum human head ITD (~0.66ms).
 */
class FractionalDelayLine(
    val bufferSize: Int = 512
) {
    init {
        require(bufferSize > 0 && (bufferSize and (bufferSize - 1)) == 0) {
            "bufferSize must be a positive power of two, got $bufferSize"
        }
    }

    private val mask = bufferSize - 1
    private val buffer = DoubleArray(bufferSize)
    private var writeIndex = 0

    /**
     * Writes a single audio sample into the delay line ring buffer.
     */
    fun write(sample: Double) {
        buffer[writeIndex] = sample
        writeIndex = (writeIndex + 1) and mask
    }

    /**
     * Reads an interpolated sample from [delaySamples] in the past.
     * [delaySamples] = 0.0 returns the most recently written sample.
     */
    fun read(delaySamples: Double): Double {
        val clampedDelay = delaySamples.coerceIn(0.0, (bufferSize - 2).toDouble())
        val intDelay = floor(clampedDelay).toInt()
        val frac = clampedDelay - intDelay

        // writeIndex points to next write slot, so most recent sample is at writeIndex - 1
        val index0 = (writeIndex - 1 - intDelay) and mask
        val index1 = (index0 - 1) and mask

        val sample0 = buffer[index0]
        val sample1 = buffer[index1]

        // Linear interpolation: y[n] = (1 - f) * x[n - d] + f * x[n - d - 1]
        return (1.0 - frac) * sample0 + frac * sample1
    }

    /**
     * Clears all samples in the delay line and resets the write pointer.
     */
    fun reset() {
        buffer.fill(0.0)
        writeIndex = 0
    }
}
