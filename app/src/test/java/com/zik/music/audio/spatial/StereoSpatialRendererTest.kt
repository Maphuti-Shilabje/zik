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
        headroom = 0.95,
        spreadAngleDegrees = 30.0
    )
    private val delta = 0.001

    @Before
    fun setUp() {
        renderer = StereoSpatialRenderer()
        renderer.configure(44100, 2)
    }

    @Test
    fun calculateVirtualEmitterPositions_frontHeading_producesSymmetricPositions() {
        val heading = Vector3(0.0, 0.0, 1.5)
        val (leftEmitter, rightEmitter) = renderer.calculateVirtualEmitterPositions(heading, 30.0)

        // S_L should be to the left (x < 0) and S_R to the right (x > 0)
        assertTrue(leftEmitter.x < 0.0)
        assertTrue(rightEmitter.x > 0.0)
        assertEquals(abs(leftEmitter.x), rightEmitter.x, delta)
        assertEquals(leftEmitter.z, rightEmitter.z, delta)
        assertEquals(1.5, leftEmitter.length(), delta)
        assertEquals(1.5, rightEmitter.length(), delta)
    }

    @Test
    fun calculateVirtualEmitterPositions_rightHeading_rotatesBothEmittersTogether() {
        val heading = Vector3(1.5, 0.0, 0.0) // Pure right (+90 deg)
        val (leftEmitter, rightEmitter) = renderer.calculateVirtualEmitterPositions(heading, 30.0)

        // At +90 deg heading with 30 deg spread: S_L is at +60 deg, S_R is at +120 deg
        // Both emitters have positive X
        assertTrue("Left emitter should have x > 0", leftEmitter.x > 0.0)
        assertTrue("Right emitter should have x > 0", rightEmitter.x > 0.0)
        assertTrue("Left emitter should have z > 0 (60 deg)", leftEmitter.z > 0.0)
        assertTrue("Right emitter should have z < 0 (120 deg)", rightEmitter.z < 0.0)
        assertEquals(1.5, leftEmitter.length(), delta)
        assertEquals(1.5, rightEmitter.length(), delta)
    }

    @Test
    fun processFrame_hardLeftInput_recoversSignalWithoutMonoCollapse() {
        val heading = Vector3(0.0, 0.0, 1.5)
        var lastLeftOut: Short = 0
        var lastRightOut: Short = 0

        for (i in 0 until 3000) {
            val (lOut, rOut) = renderer.processFrame(
                leftSample = 10000,
                rightSample = 0,
                position = heading,
                parameters = defaultParams,
                transitionWeight = 1.0f
            )
            lastLeftOut = lOut
            lastRightOut = rOut
        }

        // Left ear receives direct path LL (~4965); Right ear receives crossfeed path LR (~2057)
        assertTrue("Left ear output ($lastLeftOut) should be substantially positive", lastLeftOut > 4000)
        assertTrue("Right ear output ($lastRightOut) should receive non-zero crossfeed", lastRightOut > 1500)
        assertTrue("Left ear should be louder than Right ear for hard-left input", lastLeftOut > lastRightOut)
    }

    @Test
    fun processFrame_hardRightInput_recoversSignalWithoutMonoCollapse() {
        val heading = Vector3(0.0, 0.0, 1.5)
        var lastLeftOut: Short = 0
        var lastRightOut: Short = 0

        for (i in 0 until 3000) {
            val (lOut, rOut) = renderer.processFrame(
                leftSample = 0,
                rightSample = 10000,
                position = heading,
                parameters = defaultParams,
                transitionWeight = 1.0f
            )
            lastLeftOut = lOut
            lastRightOut = rOut
        }

        assertTrue("Right ear output ($lastRightOut) should be substantially positive", lastRightOut > 4000)
        assertTrue("Left ear output ($lastLeftOut) should receive non-zero crossfeed", lastLeftOut > 1500)
        assertTrue("Right ear should be louder than Left ear for hard-right input", lastRightOut > lastLeftOut)
    }

    @Test
    fun processFrame_antiPhaseInput_doesNotCancelToSilence() {
        // Critical test: In the old mono (L+R)/2 architecture, L = 10000 and R = -10000 would cancel to exactly 0.
        // In the 4-path dual virtual loudspeaker architecture, the signals must remain fully audible.
        val heading = Vector3(0.0, 0.0, 1.5)
        var maxObservedLeft = 0
        var maxObservedRight = 0

        for (i in 0 until 3000) {
            val (lOut, rOut) = renderer.processFrame(
                leftSample = 10000,
                rightSample = (-10000).toShort(),
                position = heading,
                parameters = defaultParams,
                transitionWeight = 1.0f
            )
            if (i >= 2000) {
                maxObservedLeft = maxOf(maxObservedLeft, abs(lOut.toInt()))
                maxObservedRight = maxOf(maxObservedRight, abs(rOut.toInt()))
            }
        }

        assertTrue(
            "Anti-phase input must NOT cancel to silence on Left ear (observed: $maxObservedLeft)",
            maxObservedLeft > 2000
        )
        assertTrue(
            "Anti-phase input must NOT cancel to silence on Right ear (observed: $maxObservedRight)",
            maxObservedRight > 2000
        )
    }

    @Test
    fun processFrame_distinctChannelSignals_contributeIndependently() {
        val heading = Vector3(0.0, 0.0, 1.5)
        var leftEarOut: Short = 0
        var rightEarOut: Short = 0

        for (i in 0 until 3000) {
            val (lOut, rOut) = renderer.processFrame(
                leftSample = 12000,
                rightSample = 4000,
                position = heading,
                parameters = defaultParams,
                transitionWeight = 1.0f
            )
            leftEarOut = lOut
            rightEarOut = rOut
        }

        // Left ear should reflect dominant left input (12000) + crossfeed from right (4000)
        // Right ear should reflect dominant right input (4000) + crossfeed from left (12000)
        assertTrue("Left ear should be louder than Right ear", leftEarOut > rightEarOut)
        assertTrue("Both ears should have non-zero output", leftEarOut > 0 && rightEarOut > 0)
    }

    @Test
    fun processFrame_symmetryAcrossMedianPlane() {
        val heading = Vector3(0.0, 0.0, 1.5)

        // Case 1: L=8000, R=2000
        renderer.reset()
        var l1 = 0
        var r1 = 0
        for (i in 0 until 3000) {
            val (lOut, rOut) = renderer.processFrame(8000, 2000, heading, defaultParams, 1.0f)
            l1 = lOut.toInt()
            r1 = rOut.toInt()
        }

        // Case 2: Swapped inputs L=2000, R=8000
        renderer.reset()
        var l2 = 0
        var r2 = 0
        for (i in 0 until 3000) {
            val (lOut, rOut) = renderer.processFrame(2000, 8000, heading, defaultParams, 1.0f)
            l2 = lOut.toInt()
            r2 = rOut.toInt()
        }

        // Symmetrical swap check
        assertEquals("Left ear (case 1) should match Right ear (case 2)", l1.toDouble(), r2.toDouble(), 10.0)
        assertEquals("Right ear (case 1) should match Left ear (case 2)", r1.toDouble(), l2.toDouble(), 10.0)
    }

    @Test
    fun calculateTargetGains_lateralPositions_producesCorrectGains() {
        val rightPos = Vector3(1.5, 0.0, 0.0)
        val (leftGain, rightGain) = renderer.calculateTargetGains(rightPos, defaultParams)

        assertTrue("Right gain should be greater than Left gain for right position", rightGain > leftGain)
        assertEquals(0.0, leftGain, delta)
    }

    @Test
    fun calculateTargetDelays_lateralPositions_producesCorrectDelays() {
        val rightPos = Vector3(1.5, 0.0, 0.0)
        val (leftDelay, rightDelay) = renderer.calculateTargetDelays(rightPos)

        assertEquals(0.0, rightDelay, delta)
        assertTrue("Left ear delay should be positive for right position", leftDelay > 0.0)
    }

    @Test
    fun calculateTargetCutoffs_lateralPositions_producesCorrectCutoffs() {
        val rightPos = Vector3(1.5, 0.0, 0.0)
        val (leftCutoff, rightCutoff) = renderer.calculateTargetCutoffs(rightPos)

        assertEquals(20000.0, rightCutoff, delta)
        assertEquals(2000.0, leftCutoff, delta)
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
    fun reset_clearsInternalStateCleanly() {
        val pos = Vector3(2.0, 0.0, 0.0)
        for (i in 0 until 100) {
            renderer.processFrame(10000, 10000, pos, defaultParams, 1.0f)
        }

        renderer.reset()

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
