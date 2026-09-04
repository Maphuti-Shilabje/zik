package com.zik.music.audio.spatial.filter

import com.zik.music.audio.spatial.Vector3
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HeadShadowModelTest {

    private lateinit var model: HeadShadowModel
    private val delta = 0.01

    @Before
    fun setUp() {
        model = HeadShadowModel(minCutoffHz = 2000.0, maxCutoffHz = 20000.0)
    }

    @Test
    fun calculateCutoffFrequencies_frontCenter_producesMaximumCutoffBothEars() {
        val position = Vector3(0.0, 0.0, 1.5)
        val (leftCutoff, rightCutoff) = model.calculateCutoffFrequencies(position)

        assertEquals(20000.0, leftCutoff, delta)
        assertEquals(20000.0, rightCutoff, delta)
    }

    @Test
    fun calculateCutoffFrequencies_rightSource_attenuatesContralateralLeftCutoffOnly() {
        val position = Vector3(2.0, 0.0, 0.0) // Pure right
        val (leftCutoff, rightCutoff) = model.calculateCutoffFrequencies(position)

        assertEquals(20000.0, rightCutoff, delta) // Ipsilateral right ear remains unshadowed
        assertEquals(2000.0, leftCutoff, delta)   // Contralateral left ear receives maximum head shadow
    }

    @Test
    fun calculateCutoffFrequencies_leftSource_attenuatesContralateralRightCutoffOnly() {
        val position = Vector3(-2.0, 0.0, 0.0) // Pure left
        val (leftCutoff, rightCutoff) = model.calculateCutoffFrequencies(position)

        assertEquals(20000.0, leftCutoff, delta)  // Ipsilateral left ear remains unshadowed
        assertEquals(2000.0, rightCutoff, delta) // Contralateral right ear receives maximum head shadow
    }

    @Test
    fun calculateCutoffFrequencies_symmetryAcrossMedianPlane() {
        val rightPos = Vector3(1.5, 0.0, 1.0)
        val leftPos = Vector3(-1.5, 0.0, 1.0)

        val (rSourceLeftCutoff, rSourceRightCutoff) = model.calculateCutoffFrequencies(rightPos)
        val (lSourceLeftCutoff, lSourceRightCutoff) = model.calculateCutoffFrequencies(leftPos)

        // Ipsilateral ears match
        assertEquals(rSourceRightCutoff, lSourceLeftCutoff, delta)
        // Contralateral ears match
        assertEquals(rSourceLeftCutoff, lSourceRightCutoff, delta)
        // Contralateral ear is shadowed compared to ipsilateral ear
        assertTrue(rSourceLeftCutoff < rSourceRightCutoff)
    }

    @Test
    fun calculateCutoffFrequencies_monotonicVariationAlongAzimuth() {
        var prevContralateralCutoff = 20000.0
        val steps = 10

        for (i in 1..steps) {
            val angle = (Math.PI / 2.0) * (i.toDouble() / steps)
            val pos = Vector3(2.0 * kotlin.math.sin(angle), 0.0, 2.0 * kotlin.math.cos(angle))
            val (leftCutoff, rightCutoff) = model.calculateCutoffFrequencies(pos)

            assertEquals(20000.0, rightCutoff, delta)
            assertTrue("Left cutoff ($leftCutoff) should decrease monotonically", leftCutoff < prevContralateralCutoff)
            assertTrue("Left cutoff should remain within bounded range", leftCutoff in 2000.0..20000.0)
            prevContralateralCutoff = leftCutoff
        }
    }

    @Test
    fun calculateCutoffFrequencies_behindCenter_producesSymmetricCutoffs() {
        val position = Vector3(0.0, 0.0, -1.5)
        val (leftCutoff, rightCutoff) = model.calculateCutoffFrequencies(position)

        assertEquals(20000.0, leftCutoff, delta)
        assertEquals(20000.0, rightCutoff, delta)
    }
}
