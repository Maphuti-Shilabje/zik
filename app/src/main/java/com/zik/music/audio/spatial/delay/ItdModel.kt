package com.zik.music.audio.spatial.delay

import com.zik.music.audio.spatial.Vector3
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.sign
import kotlin.math.sin

/**
 * Spherical-head Interaural Time Difference (ITD) model based on the Woodworth-Schlosser approximation.
 *
 * Coordinate system:
 * Listener at (0, 0, 0)
 * +X = Right
 * -X = Left
 * +Y = Up
 * +Z = Forward
 *
 * Source at position (x, y, z):
 * Azimuth angle alpha = atan2(x, z) in [-PI, PI]
 * - alpha = 0: Front center -> ITD = 0 (both ears receive simultaneously)
 * - alpha > 0 (x > 0): Source to right -> Right ear early, Left ear delayed
 * - alpha < 0 (x < 0): Source to left -> Left ear early, Right ear delayed
 */
class ItdModel(
    val headRadiusMeters: Double = 0.0875, // Standard average human head radius (~8.75 cm)
    val speedOfSoundMps: Double = 343.0    // Speed of sound in dry air at 20 deg C (343 m/s)
) {

    /**
     * Calculates the signed ITD in seconds for a given source position.
     * Positive ITD indicates the source is to the right (left ear delayed).
     * Negative ITD indicates the source is to the left (right ear delayed).
     */
    fun calculateItdSeconds(position: Vector3): Double {
        val x = position.x
        val z = position.z

        if (x == 0.0 && z == 0.0) {
            return 0.0
        }

        // Azimuth angle in radians in [-PI, PI]
        val azimuth = atan2(x, z)
        val sinAzimuth = sin(azimuth)
        val absSin = abs(sinAzimuth)

        // Woodworth formula: ITD = (a / c) * (sin(theta) + theta)
        val normalizedTheta = asin(absSin.coerceIn(0.0, 1.0))
        val maxItdScale = (headRadiusMeters / speedOfSoundMps)
        val magnitude = maxItdScale * (absSin + normalizedTheta)

        return sign(sinAzimuth) * magnitude
    }

    /**
     * Calculates the ear delays (leftDelaySeconds, rightDelaySeconds) for a given source position.
     * The ear closer to the source receives a delay of 0.0s, while the contralateral ear receives the positive ITD delay.
     */
    fun calculateEarDelaysSeconds(position: Vector3): Pair<Double, Double> {
        val itdSeconds = calculateItdSeconds(position)
        return when {
            itdSeconds > 0.0 -> Pair(itdSeconds, 0.0) // Source on right: left ear delayed
            itdSeconds < 0.0 -> Pair(0.0, -itdSeconds) // Source on left: right ear delayed
            else -> Pair(0.0, 0.0)
        }
    }
}
