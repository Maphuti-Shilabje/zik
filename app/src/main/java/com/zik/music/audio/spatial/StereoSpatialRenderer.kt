package com.zik.music.audio.spatial

import com.zik.music.audio.spatial.delay.FractionalDelayLine
import com.zik.music.audio.spatial.delay.ItdModel
import com.zik.music.audio.spatial.filter.HeadShadowFilter
import com.zik.music.audio.spatial.filter.HeadShadowModel
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Stereo-preserving spatial audio renderer implementing a 4-path dual virtual loudspeaker model:
 *
 * E_L = H_LL(p_L)·L + H_RL(p_R)·R
 * E_R = H_LR(p_L)·L + H_RR(p_R)·R
 *
 * - Left channel input (L) feeds the Virtual Left Emitter (S_L).
 * - Right channel input (R) feeds the Virtual Right Emitter (S_R).
 * - The input stereo mix is never downmixed to mono, preserving 100% of stereo separation,
 *   panning contrast, and out-of-phase ambient/side content.
 * - S_L and S_R rotate as a rigid soundstage around the listener's head.
 */
class StereoSpatialRenderer(
    val itdModel: ItdModel = ItdModel(),
    val headShadowModel: HeadShadowModel = HeadShadowModel()
) : SpatialRenderer {

    private var sampleRate: Int = 44100
    private var channelCount: Int = 2
    private var gainSmoothingCoeff: Double = 0.005
    private var delaySmoothingCoeff: Double = 0.005

    // Smoothed gains for all 4 paths
    private var currentGainLL: Double = 1.0 / sqrt(2.0)
    private var currentGainLR: Double = 0.0
    private var currentGainRL: Double = 0.0
    private var currentGainRR: Double = 1.0 / sqrt(2.0)

    // Smoothed delays for all 4 paths
    private var currentDelayLL: Double = 0.0
    private var currentDelayLR: Double = 0.0
    private var currentDelayRL: Double = 0.0
    private var currentDelayRR: Double = 0.0

    // Independent delay lines for Left and Right source channels
    private val leftSourceDelayLine = FractionalDelayLine(512)
    private val rightSourceDelayLine = FractionalDelayLine(512)

    // Independent head-shadow filter states for all 4 acoustic paths
    private val filterLL = HeadShadowFilter()
    private val filterLR = HeadShadowFilter()
    private val filterRL = HeadShadowFilter()
    private val filterRR = HeadShadowFilter()

    override fun configure(sampleRate: Int, channelCount: Int) {
        this.sampleRate = sampleRate
        this.channelCount = channelCount

        val gainTauSeconds = 0.020
        val delayTauSeconds = 0.025
        gainSmoothingCoeff = 1.0 - exp(-1.0 / (sampleRate * gainTauSeconds))
        delaySmoothingCoeff = 1.0 - exp(-1.0 / (sampleRate * delayTauSeconds))

        filterLL.configure(sampleRate)
        filterLR.configure(sampleRate)
        filterRL.configure(sampleRate)
        filterRR.configure(sampleRate)

        reset()
    }

    override fun reset() {
        val initialGain = 1.0 / sqrt(2.0)
        currentGainLL = initialGain
        currentGainLR = 0.0
        currentGainRL = 0.0
        currentGainRR = initialGain

        currentDelayLL = 0.0
        currentDelayLR = 0.0
        currentDelayRL = 0.0
        currentDelayRR = 0.0

        leftSourceDelayLine.reset()
        rightSourceDelayLine.reset()

        filterLL.reset()
        filterLR.reset()
        filterRL.reset()
        filterRR.reset()
    }

    /**
     * Calculates the 3D positions of the Virtual Left Emitter and Virtual Right Emitter
     * given the soundstage heading position and angular spread.
     */
    fun calculateVirtualEmitterPositions(
        headingPosition: Vector3,
        spreadDegrees: Double = 30.0
    ): Pair<Vector3, Vector3> {
        val x = headingPosition.x
        val y = headingPosition.y
        val z = headingPosition.z
        val distance = headingPosition.length()

        val headingAzimuth = if (x == 0.0 && z == 0.0) 0.0 else atan2(x, z)
        val spreadRad = Math.toRadians(spreadDegrees)

        val leftAzimuth = headingAzimuth - spreadRad
        val rightAzimuth = headingAzimuth + spreadRad

        val leftPos = Vector3(distance * sin(leftAzimuth), y, distance * cos(leftAzimuth))
        val rightPos = Vector3(distance * sin(rightAzimuth), y, distance * cos(rightAzimuth))

        return Pair(leftPos, rightPos)
    }

    /**
     * Calculates target ILD and distance attenuation gains (leftEarGain, rightEarGain)
     * for a single virtual emitter position.
     */
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

        // Normalization factor (1.0 / sqrt(2.0)) ensures dual loudspeaker summation preserves headroom
        val baseGain = (parameters.headroom / sqrt(2.0)) * distanceAtten

        val targetLeft = baseGain * pannedLeft
        val targetRight = baseGain * pannedRight

        return Pair(targetLeft, targetRight)
    }

    /**
     * Calculates target ITD arrival delays in samples (leftEarDelaySamples, rightEarDelaySamples)
     * for a single virtual emitter position.
     */
    fun calculateTargetDelays(position: Vector3): Pair<Double, Double> {
        val (leftDelaySec, rightDelaySec) = itdModel.calculateEarDelaysSeconds(position)
        return Pair(leftDelaySec * sampleRate, rightDelaySec * sampleRate)
    }

    /**
     * Calculates target head-shadow cutoff frequencies (leftEarCutoffHz, rightEarCutoffHz)
     * for a single virtual emitter position.
     */
    fun calculateTargetCutoffs(position: Vector3): Pair<Double, Double> {
        return headShadowModel.calculateCutoffFrequencies(position)
    }

    override fun processFrame(
        leftSample: Short,
        rightSample: Short,
        position: Vector3,
        parameters: SpatialParameters,
        transitionWeight: Float
    ): Pair<Short, Short> {
        // Compute positions of the rigid virtual loudspeaker pair
        val (leftPos, rightPos) = calculateVirtualEmitterPositions(position, parameters.spreadAngleDegrees)

        // Path LL & LR targets (from virtual Left Emitter)
        val (targetGainLL, targetGainLR) = calculateTargetGains(leftPos, parameters)
        val (targetDelayLL, targetDelayLR) = calculateTargetDelays(leftPos)
        val (targetCutoffLL, targetCutoffLR) = calculateTargetCutoffs(leftPos)

        // Path RL & RR targets (from virtual Right Emitter)
        val (targetGainRL, targetGainRR) = calculateTargetGains(rightPos, parameters)
        val (targetDelayRL, targetDelayRR) = calculateTargetDelays(rightPos)
        val (targetCutoffRL, targetCutoffRR) = calculateTargetCutoffs(rightPos)

        // Parameter smoothing across all 4 paths
        currentGainLL += gainSmoothingCoeff * (targetGainLL - currentGainLL)
        currentGainLR += gainSmoothingCoeff * (targetGainLR - currentGainLR)
        currentGainRL += gainSmoothingCoeff * (targetGainRL - currentGainRL)
        currentGainRR += gainSmoothingCoeff * (targetGainRR - currentGainRR)

        currentDelayLL += delaySmoothingCoeff * (targetDelayLL - currentDelayLL)
        currentDelayLR += delaySmoothingCoeff * (targetDelayLR - currentDelayLR)
        currentDelayRL += delaySmoothingCoeff * (targetDelayRL - currentDelayRL)
        currentDelayRR += delaySmoothingCoeff * (targetDelayRR - currentDelayRR)

        // 1. Independent source delay buffer writes (NO mono downmix)
        val leftSourceIn = leftSample.toDouble()
        val rightSourceIn = rightSample.toDouble()

        leftSourceDelayLine.write(leftSourceIn)
        rightSourceDelayLine.write(rightSourceIn)

        // 2. Fractional delay reads for all 4 paths
        val delayedLL = leftSourceDelayLine.read(currentDelayLL)
        val delayedLR = leftSourceDelayLine.read(currentDelayLR)

        val delayedRL = rightSourceDelayLine.read(currentDelayRL)
        val delayedRR = rightSourceDelayLine.read(currentDelayRR)

        // 3. Path-specific head-shadow filtering
        val filteredLL = filterLL.process(delayedLL, targetCutoffLL)
        val filteredLR = filterLR.process(delayedLR, targetCutoffLR)

        val filteredRL = filterRL.process(delayedRL, targetCutoffRL)
        val filteredRR = filterRR.process(delayedRR, targetCutoffRR)

        // 4. Path gain scaling and binaural ear summation
        // Left Ear  = H_LL·L + H_RL·R
        // Right Ear = H_LR·L + H_RR·R
        val spatialLeft = (filteredLL * currentGainLL) + (filteredRL * currentGainRL)
        val spatialRight = (filteredLR * currentGainLR) + (filteredRR * currentGainRR)

        // 5. Smooth transition blend: 0.0 = true passthrough, 1.0 = fully spatialized
        val weight = transitionWeight.toDouble().coerceIn(0.0, 1.0)
        val finalLeft = leftSourceIn * (1.0 - weight) + spatialLeft * weight
        val finalRight = rightSourceIn * (1.0 - weight) + spatialRight * weight

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
