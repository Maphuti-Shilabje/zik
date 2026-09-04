package com.zik.music.audio.spatial

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sin

class StereoSpatialRenderer : SpatialRenderer {

    private var sampleRate: Int = 44100
    private var channelCount: Int = 2
    private var smoothingCoeff: Double = 0.005

    private var currentLeftGain: Double = 1.0
    private var currentRightGain: Double = 1.0

    override fun configure(sampleRate: Int, channelCount: Int) {
        this.sampleRate = sampleRate
        this.channelCount = channelCount
        // Exponential smoothing coefficient for ~20ms time constant tau
        val tauSeconds = 0.020
        smoothingCoeff = 1.0 - exp(-1.0 / (sampleRate * tauSeconds))
        reset()
    }

    override fun reset() {
        currentLeftGain = 1.0
        currentRightGain = 1.0
    }

    fun calculateTargetGains(position: Vector3, parameters: SpatialParameters): Pair<Double, Double> {
        val x = position.x
        val z = position.z
        val distance = position.length()

        // Azimuth angle in [-PI, PI]:
        // 0 = Front, +PI/2 = Right (+x), -PI/2 = Left (-x), +/-PI = Behind
        val azimuth = atan2(x, z)

        // Equal-power pan factor: 0.0 = full left, 0.5 = center, 1.0 = full right
        val pan = (sin(azimuth) + 1.0) * 0.5

        // Equal-power constant-energy panning law: cos^2(p * PI/2) + sin^2(p * PI/2) = 1.0
        val pannedLeft = cos(pan * (Math.PI / 2.0))
        val pannedRight = sin(pan * (Math.PI / 2.0))

        // Distance attenuation model with configurable rolloff and reference distance
        val excessDistance = max(0.0, distance - parameters.referenceDistance)
        val distanceAtten = 1.0 / (1.0 + parameters.distanceRolloff * excessDistance)

        val targetLeft = parameters.headroom * distanceAtten * pannedLeft
        val targetRight = parameters.headroom * distanceAtten * pannedRight

        return Pair(targetLeft, targetRight)
    }

    override fun processFrame(
        leftSample: Short,
        rightSample: Short,
        position: Vector3,
        parameters: SpatialParameters,
        transitionWeight: Float
    ): Pair<Short, Short> {
        val (targetLeft, targetRight) = calculateTargetGains(position, parameters)

        // Real-time parameter smoothing to eliminate zipper noise and clicks
        currentLeftGain += smoothingCoeff * (targetLeft - currentLeftGain)
        currentRightGain += smoothingCoeff * (targetRight - currentRightGain)

        // Virtual mono point source derived from input stereo channels
        val sourceMono = (leftSample.toDouble() + rightSample.toDouble()) * 0.5

        val spatialLeft = sourceMono * currentLeftGain
        val spatialRight = sourceMono * currentRightGain

        // Smooth transition blend: 0.0 = true passthrough, 1.0 = fully spatialized
        val weight = transitionWeight.toDouble().coerceIn(0.0, 1.0)
        val finalLeft = leftSample.toDouble() * (1.0 - weight) + spatialLeft * weight
        val finalRight = rightSample.toDouble() * (1.0 - weight) + spatialRight * weight

        return Pair(clamp16(finalLeft), clamp16(finalRight))
    }

    private fun clamp16(value: Double): Short {
        return when {
            value > 32767.0 -> 32767
            value < -32768.0 -> -32768
            else -> value.toInt().toShort()
        }
    }
}
