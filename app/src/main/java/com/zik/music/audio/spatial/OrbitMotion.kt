package com.zik.music.audio.spatial

import kotlin.math.cos
import kotlin.math.sin

class OrbitMotion(
    val radius: Double = 1.5,
    val speedHz: Double = 0.1, // 0.1 Hz = 1 full 360 degree orbit every 10 seconds
    val initialPhase: Double = 0.0
) : MotionModel {

    override fun positionAt(timeSeconds: Double): Vector3 {
        val angularVelocity = 2.0 * Math.PI * speedHz
        val angle = initialPhase + angularVelocity * timeSeconds
        val x = radius * sin(angle)
        val y = 0.0
        val z = radius * cos(angle)
        return Vector3(x, y, z)
    }
}
