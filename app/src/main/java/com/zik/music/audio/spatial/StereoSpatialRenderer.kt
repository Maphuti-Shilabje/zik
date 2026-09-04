package com.zik.music.audio.spatial

import com.zik.music.audio.spatial.delay.FractionalDelayLine
import com.zik.music.audio.spatial.delay.ItdModel
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sin

class StereoSpatialRenderer(
    val itdModel: ItdModel = ItdModel()
) : SpatialRenderer {

    private var sampleRate: Int = 44100
    private var channelCount: Int = 2
    private var gainSmoothingCoeff: Double = 0.005
    private var delaySmoothingCoeff: Double = 0.005

    private var currentLeftGain: Double = 1.0
    private var currentRightGain: Double = 1.0

    private var currentLeftDelaySamples: Double = 0.0
    private var currentRightDelaySamples: Double = 0.0

    private val leftDelayLine = FractionalDelayLine(512)
    private val rightDelayLine = FractionalDelayLine(512)

    override fun configure(sampleRate: Int, channelCount: Int) {
        this.sampleRate = sampleRate
        this.channelCount = channelCount

        // Exponential smoothing coefficients (~20ms time constant for gain, ~25ms for ITD delay)
        val gainTauSeconds = 0.020
        val delayTauSeconds = 0.025
        gainSmoothingCoeff = 1.0 - exp(-1.0 / (sampleRate * gainTauSeconds))
        delaySmoothingCoeff = 1.0 - exp(-1.0 / (sampleRate * delayTauSeconds))

        reset()
    }

    override fun reset() {
        currentLeftGain = 1.0
        currentRightGain = 1.0
        currentLeftDelaySamples = 0.0
        currentRightDelaySamples = 0.0
        leftDelayLine.reset()
        rightDelayLine.reset()
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

    fun calculateTargetDelays(position: Vector3): Pair<Double, Double> {
        val (leftDelaySec, rightDelaySec) = itdModel.calculateEarDelaysSeconds(position)
        return Pair(leftDelaySec * sampleRate, rightDelaySec * sampleRate)
    }

    override fun processFrame(
        leftSample: Short,
        rightSample: Short,
        position: Vector3,
        parameters: SpatialParameters,
        transitionWeight: Float
    ): Pair<Short, Short> {
        val (targetLeftGain, targetRightGain) = calculateTargetGains(position, parameters)
        val (targetLeftDelay, targetRightDelay) = calculateTargetDelays(position)

        // Real-time parameter smoothing to eliminate clicks and pitch-modulation artifacts
        currentLeftGain += gainSmoothingCoeff * (targetLeftGain - currentLeftGain)
        currentRightGain += gainSmoothingCoeff * (targetRightGain - currentRightGain)

        currentLeftDelaySamples += delaySmoothingCoeff * (targetLeftDelay - currentLeftDelaySamples)
        currentRightDelaySamples += delaySmoothingCoeff * (targetRightDelay - currentRightDelaySamples)

        // Virtual mono point source derived from input stereo channels
        val sourceMono = (leftSample.toDouble() + rightSample.toDouble()) * 0.5

        // Feed fractional delay lines
        leftDelayLine.write(sourceMono)
        rightDelayLine.write(sourceMono)

        // Read fractional delayed samples for both ears
        val delayedLeft = leftDelayLine.read(currentLeftDelaySamples)
        val delayedRight = rightDelayLine.read(currentRightDelaySamples)

        // Apply ILD gains on delayed audio streams
        val spatialLeft = delayedLeft * currentLeftGain
        val spatialRight = delayedRight * currentRightGain

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
