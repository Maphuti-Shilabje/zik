package com.zik.music.audio.spatial

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

class StereoSpatialRendererTest {

    private lateinit var renderer: StereoSpatialRenderer
    private val defaultParams = SpatialParameters(
        isEnabled = true,
        radius = 1.5,
        referenceDistance = 1.0,
        distanceRolloff = 0.5,
        headroom = 0.95
    )
    private val delta = 0.001

    @Before
    fun setUp() {
        renderer = StereoSpatialRenderer()
        renderer.configure(44100, 2)
    }

    @Test
    fun calculateTargetGains_centeredFrontSource_producesBalancedStereo() {
        val position = Vector3(0.0, 0.0, 1.0)
        val (leftGain, rightGain) = renderer.calculateTargetGains(position, defaultParams)

        assertEquals(leftGain, rightGain, delta)
        assertTrue(leftGain > 0.0)
    }

    @Test
    fun calculateTargetGains_sourceToRight_producesStrongerRightOutput() {
        val position = Vector3(1.5, 0.0, 0.0) // Pure right
        val (leftGain, rightGain) = renderer.calculateTargetGains(position, defaultParams)

        assertTrue("Right gain ($rightGain) should be strictly greater than left gain ($leftGain)", rightGain > leftGain)
        assertEquals(0.0, leftGain, delta)
    }

    @Test
    fun calculateTargetGains_sourceToLeft_producesStrongerLeftOutput() {
        val position = Vector3(-1.5, 0.0, 0.0) // Pure left
        val (leftGain, rightGain) = renderer.calculateTargetGains(position, defaultParams)

        assertTrue("Left gain ($leftGain) should be strictly greater than right gain ($rightGain)", leftGain > rightGain)
        assertEquals(0.0, rightGain, delta)
    }

    @Test
    fun calculateTargetGains_fartherDistance_attenuatesAmplitude() {
        val nearPos = Vector3(0.0, 0.0, 1.0)
        val farPos = Vector3(0.0, 0.0, 4.0)

        val (nearL, nearR) = renderer.calculateTargetGains(nearPos, defaultParams)
        val (farL, farR) = renderer.calculateTargetGains(farPos, defaultParams)

        assertTrue(nearL > farL)
        assertTrue(nearR > farR)
    }

    @Test
    fun processFrame_finiteAndBoundedOutputWithoutNaN() {
        val testAngles = listOf(0.0, Math.PI / 4, Math.PI / 2, Math.PI, -Math.PI / 2)

        for (angle in testAngles) {
            val pos = Vector3(2.0 * kotlin.math.sin(angle), 0.0, 2.0 * kotlin.math.cos(angle))
            val (leftOut, rightOut) = renderer.processFrame(
                leftSample = 20000,
                rightSample = 20000,
                position = pos,
                parameters = defaultParams,
                transitionWeight = 1.0f
            )

            assertTrue(leftOut.toInt() in -32768..32767)
            assertTrue(rightOut.toInt() in -32768..32767)
            assertFalse(leftOut.toDouble().isNaN())
            assertFalse(rightOut.toDouble().isNaN())
        }
    }
}
