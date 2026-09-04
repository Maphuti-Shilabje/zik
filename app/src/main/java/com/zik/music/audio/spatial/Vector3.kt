package com.zik.music.audio.spatial

import kotlin.math.sqrt

data class Vector3(
    val x: Double = 0.0,
    val y: Double = 0.0,
    val z: Double = 0.0
) {
    fun length(): Double = sqrt(x * x + y * y + z * z)

    companion object {
        val ZERO = Vector3(0.0, 0.0, 0.0)
        val FORWARD = Vector3(0.0, 0.0, 1.0)
        val RIGHT = Vector3(1.0, 0.0, 0.0)
        val LEFT = Vector3(-1.0, 0.0, 0.0)
    }
}
