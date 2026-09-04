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
    fun calculateTargetDelays_frontSource_producesZeroDelayBothEars() {
        val position = Vector3(0.0, 0.0, 1.0)
        val (leftDelay, rightDelay) = renderer.calculateTargetDelays(position)

        assertEquals(0.0, leftDelay, delta)
        assertEquals(0.0, rightDelay, delta)
    }

    @Test
    fun calculateTargetDelays_sourceToRight_delaysLeftEarOnly() {
        val position = Vector3(1.5, 0.0, 0.0)
        val (leftDelay, rightDelay) = renderer.calculateTargetDelays(position)

        assertEquals(0.0, rightDelay, delta)
        assertTrue("Left ear delay should be positive for right source", leftDelay > 0.0)
        // Max ITD at 44.1kHz is ~28.9 samples
        assertTrue(leftDelay in 25.0..32.0)
    }

    @Test
    fun calculateTargetDelays_sourceToLeft_delaysRightEarOnly() {
        val position = Vector3(-1.5, 0.0, 0.0)
        val (leftDelay, rightDelay) = renderer.calculateTargetDelays(position)

        assertEquals(0.0, leftDelay, delta)
        assertTrue("Right ear delay should be positive for left source", rightDelay > 0.0)
        assertTrue(rightDelay in 25.0..32.0)
    }

    @Test
    fun calculateTargetDelays_symmetryAcrossMedianPlane() {
        val rightPos = Vector3(1.0, 0.0, 1.0)
        val leftPos = Vector3(-1.0, 0.0, 1.0)

        val (rSourceLDelay, rSourceRDelay) = renderer.calculateTargetDelays(rightPos)
        val (lSourceLDelay, lSourceRDelay) = renderer.calculateTargetDelays(leftPos)

        assertEquals(0.0, rSourceRDelay, delta)
        assertEquals(0.0, lSourceLDelay, delta)
        assertEquals(rSourceLDelay, lSourceRDelay, delta)
    }

    @Test
    fun calculateTargetCutoffs_frontSource_producesMaxCutoffBothEars() {
        val position = Vector3(0.0, 0.0, 1.0)
        val (leftCutoff, rightCutoff) = renderer.calculateTargetCutoffs(position)

        assertEquals(20000.0, leftCutoff, delta)
        assertEquals(20000.0, rightCutoff, delta)
    }

    @Test
    fun calculateTargetCutoffs_sourceToRight_attenuatesLeftEarCutoff() {
        val position = Vector3(1.5, 0.0, 0.0)
        val (leftCutoff, rightCutoff) = renderer.calculateTargetCutoffs(position)

        assertEquals(20000.0, rightCutoff, delta)
        assertEquals(2000.0, leftCutoff, delta)
    }

    @Test
    fun calculateTargetCutoffs_sourceToLeft_attenuatesRightEarCutoff() {
        val position = Vector3(-1.5, 0.0, 0.0)
        val (leftCutoff, rightCutoff) = renderer.calculateTargetCutoffs(position)

        assertEquals(20000.0, leftCutoff, delta)
        assertEquals(2000.0, rightCutoff, delta)
    }

    @Test
    fun calculateTargetCutoffs_symmetryAcrossMedianPlane() {
        val rightPos = Vector3(1.0, 0.0, 1.0)
        val leftPos = Vector3(-1.0, 0.0, 1.0)

        val (rSourceLeftCutoff, rSourceRightCutoff) = renderer.calculateTargetCutoffs(rightPos)
        val (lSourceLeftCutoff, lSourceRightCutoff) = renderer.calculateTargetCutoffs(leftPos)

        assertEquals(rSourceRightCutoff, lSourceLeftCutoff, delta)
        assertEquals(rSourceLeftCutoff, lSourceRightCutoff, delta)
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

    @Test
    fun reset_clearsInternalGainsAndDelayState() {
        // Run some frames with offset source
        val pos = Vector3(2.0, 0.0, 0.0)
        for (i in 0 until 100) {
            renderer.processFrame(10000, 10000, pos, defaultParams, 1.0f)
        }

        renderer.reset()

        // After reset, front source with 0 transition weight should match clean passthrough
        val (leftOut, rightOut) = renderer.processFrame(
            leftSample = 12345,
            rightSample = -12345,
            position = Vector3(0.0, 0.0, 1.0),
            parameters = defaultParams,
            transitionWeight = 0.0f
        )
        assertEquals(12345.toShort(), leftOut)
        assertEquals((-12345).toShort(), rightOut)
    }
}
