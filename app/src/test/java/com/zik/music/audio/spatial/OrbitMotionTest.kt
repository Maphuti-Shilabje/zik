package com.zik.music.audio.spatial

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class OrbitMotionTest {

    private val delta = 0.0001

    @Test
    fun positionAt_timeZero_placesSourceDirectlyInFront() {
        val motion = OrbitMotion(radius = 2.0, speedHz = 0.1, initialPhase = 0.0)
        val pos = motion.positionAt(0.0)

        assertEquals(0.0, pos.x, delta)
        assertEquals(0.0, pos.y, delta)
        assertEquals(2.0, pos.z, delta)
        assertEquals(2.0, pos.length(), delta)
    }

    @Test
    fun positionAt_quarterOrbit_placesSourceToRight() {
        val speedHz = 0.1 // Period T = 10s -> T/4 = 2.5s
        val motion = OrbitMotion(radius = 1.5, speedHz = speedHz, initialPhase = 0.0)
        val pos = motion.positionAt(2.5)

        assertEquals(1.5, pos.x, delta)
        assertEquals(0.0, pos.y, delta)
        assertEquals(0.0, pos.z, delta)
    }

    @Test
    fun positionAt_halfOrbit_placesSourceBehind() {
        val speedHz = 0.1 // T/2 = 5.0s
        val motion = OrbitMotion(radius = 1.5, speedHz = speedHz, initialPhase = 0.0)
        val pos = motion.positionAt(5.0)

        assertEquals(0.0, pos.x, delta)
        assertEquals(0.0, pos.y, delta)
        assertEquals(-1.5, pos.z, delta)
    }

    @Test
    fun positionAt_threeQuarterOrbit_placesSourceToLeft() {
        val speedHz = 0.1 // 3T/4 = 7.5s
        val motion = OrbitMotion(radius = 1.5, speedHz = speedHz, initialPhase = 0.0)
        val pos = motion.positionAt(7.5)

        assertEquals(-1.5, pos.x, delta)
        assertEquals(0.0, pos.y, delta)
        assertEquals(0.0, pos.z, delta)
    }

    @Test
    fun positionAt_fullOrbit_returnsToStartingPosition() {
        val speedHz = 0.2 // Period T = 5s
        val motion = OrbitMotion(radius = 3.0, speedHz = speedHz, initialPhase = 0.0)
        val startPos = motion.positionAt(0.0)
        val fullPos = motion.positionAt(5.0)

        assertEquals(startPos.x, fullPos.x, delta)
        assertEquals(startPos.y, fullPos.y, delta)
        assertEquals(startPos.z, fullPos.z, delta)
    }

    @Test
    fun continuousTrajectory_maintainsConstantDistanceEverywhere() {
        val radius = 2.5
        val motion = OrbitMotion(radius = radius, speedHz = 0.25)

        for (i in 0..100) {
            val t = i * 0.1
            val pos = motion.positionAt(t)
            assertEquals(radius, pos.length(), delta)
        }
    }
}
