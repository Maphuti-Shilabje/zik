package com.zik.music.audio.spatial.filter

import com.zik.music.audio.spatial.Vector3
import kotlin.math.atan2
import kotlin.math.pow
import kotlin.math.sin

/**
 * Frequency-dependent head-shadow acoustic model.
 *
 * Approximates the acoustic shielding of the human head for lateral sound sources:
 * - When source is on the Right (+X, azimuth > 0), the Left (contralateral) ear is shadowed,
 *   reducing its cutoff frequency to attenuate high frequencies.
 * - When source is on the Left (-X, azimuth < 0), the Right (contralateral) ear is shadowed.
 * - When source is centered (X = 0), both ears receive unattenuated high frequencies.
 *
 * Coordinate system:
 * Listener at (0, 0, 0) facing +Z (Forward)
 * +X = Right
 * -X = Left
 * +Y = Up
 */
class HeadShadowModel(
    val minCutoffHz: Double = 2000.0,
    val maxCutoffHz: Double = 20000.0
) {

    /**
     * Calculates the target low-pass filter cutoff frequencies (leftCutoffHz, rightCutoffHz)
     * based on the 3D source position.
     */
    fun calculateCutoffFrequencies(position: Vector3): Pair<Double, Double> {
        val x = position.x
        val z = position.z

        if (x == 0.0 && z == 0.0) {
            return Pair(maxCutoffHz, maxCutoffHz)
        }

        // Azimuth angle in radians in [-PI, PI]
        // 0 = Front, +PI/2 = Right (+x), -PI/2 = Left (-x), +/-PI = Behind
        val azimuth = atan2(x, z)
        val lateralFactor = sin(azimuth)

        // Shadow intensity for each ear in [0.0, 1.0]
        // lateralFactor > 0 (Right source) -> Left ear shadowed (intensity = lateralFactor), Right ear unshadowed (0.0)
        // lateralFactor < 0 (Left source) -> Right ear shadowed (intensity = -lateralFactor), Left ear unshadowed (0.0)
        val leftShadowIntensity = if (lateralFactor > 0.0) lateralFactor else 0.0
        val rightShadowIntensity = if (lateralFactor < 0.0) -lateralFactor else 0.0

        val leftCutoff = calculateEarCutoff(leftShadowIntensity)
        val rightCutoff = calculateEarCutoff(rightShadowIntensity)

        return Pair(leftCutoff, rightCutoff)
    }

    private fun calculateEarCutoff(shadowIntensity: Double): Double {
        if (shadowIntensity <= 0.0) {
            return maxCutoffHz
        }
        val intensity = shadowIntensity.coerceIn(0.0, 1.0)
        // Logarithmic/exponential cutoff interpolation across human hearing range
        return maxCutoffHz * (minCutoffHz / maxCutoffHz).pow(intensity)
    }
}
