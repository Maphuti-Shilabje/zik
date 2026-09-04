package com.zik.music.audio.spatial.delay

import com.zik.music.audio.spatial.Vector3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import kotlin.math.abs

class ItdModelTest {

    private lateinit var itdModel: ItdModel
    private val delta = 0.00001

    @Before
    fun setUp() {
        itdModel = ItdModel()
    }

    @Test
    fun calculateItdSeconds_centeredFrontSource_returnsZeroItd() {
        val position = Vector3(0.0, 0.0, 2.0)
        val itd = itdModel.calculateItdSeconds(position)
        assertEquals(0.0, itd, delta)
    }

    @Test
    fun calculateItdSeconds_centeredBackSource_returnsZeroItd() {
        val position = Vector3(0.0, 0.0, -2.0)
        val itd = itdModel.calculateItdSeconds(position)
        assertEquals(0.0, itd, delta)
    }

    @Test
    fun calculateItdSeconds_rightSource_returnsPositiveItd() {
        val position = Vector3(2.0, 0.0, 0.0) // Pure right
        val itd = itdModel.calculateItdSeconds(position)
        assertTrue("ITD for right source must be strictly positive", itd > 0.0)
    }

    @Test
    fun calculateItdSeconds_leftSource_returnsNegativeItd() {
        val position = Vector3(-2.0, 0.0, 0.0) // Pure left
        val itd = itdModel.calculateItdSeconds(position)
        assertTrue("ITD for left source must be strictly negative", itd < 0.0)
    }

    @Test
    fun calculateEarDelays_rightSource_delaysLeftEarAndLeavesRightEarZero() {
        val position = Vector3(1.5, 0.0, 0.0)
        val (leftDelay, rightDelay) = itdModel.calculateEarDelaysSeconds(position)

        assertTrue(leftDelay > 0.0)
        assertEquals(0.0, rightDelay, delta)
    }

    @Test
    fun calculateEarDelays_leftSource_delaysRightEarAndLeavesLeftEarZero() {
        val position = Vector3(-1.5, 0.0, 0.0)
        val (leftDelay, rightDelay) = itdModel.calculateEarDelaysSeconds(position)

        assertEquals(0.0, leftDelay, delta)
        assertTrue(rightDelay > 0.0)
    }

    @Test
    fun symmetry_symmetricPositionsProduceEqualMagnitudeDelays() {
        val rightPos = Vector3(1.5, 0.0, 1.0)
        val leftPos = Vector3(-1.5, 0.0, 1.0)

        val itdRight = itdModel.calculateItdSeconds(rightPos)
        val itdLeft = itdModel.calculateItdSeconds(leftPos)

        assertEquals(abs(itdRight), abs(itdLeft), delta)
        assertEquals(-itdRight, itdLeft, delta)
    }

    @Test
    fun maximumDelay_boundedWithinRealisticPhysicalLimits() {
        // Maximum human head ITD is around 0.65 - 0.75 ms
        val lateralPos = Vector3(5.0, 0.0, 0.0)
        val itd = itdModel.calculateItdSeconds(lateralPos)

        assertTrue(itd > 0.0005) // at least 0.5 ms
        assertTrue(itd < 0.0008) // less than 0.8 ms
    }
}
