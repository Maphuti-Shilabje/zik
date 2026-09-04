package com.zik.music.audio.spatial

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpatialUiStateTest {

    @Test
    fun defaultState_hasSensibleAcousticDefaults() {
        val state = SpatialUiState()
        assertFalse(state.isEnabled)
        assertEquals(0.1, state.speedHz, 1e-6)
        assertEquals(1.5, state.radius, 1e-6)
        assertEquals(30.0, state.spreadAngleDegrees, 1e-6)
        assertEquals(0.5, state.distanceRolloff, 1e-6)
        assertEquals(1.0, state.referenceDistance, 1e-6)
        assertEquals(0.95, state.headroom, 1e-6)
    }

    @Test
    fun toSpatialParameters_mapsAllFieldsAccurately() {
        val state = SpatialUiState(
            isEnabled = true,
            speedHz = 0.25,
            radius = 2.0,
            spreadAngleDegrees = 45.0,
            distanceRolloff = 0.6,
            referenceDistance = 1.2,
            headroom = 0.90
        )
        val params = state.toSpatialParameters()

        assertTrue(params.isEnabled)
        assertEquals(0.25, params.speedHz, 1e-6)
        assertEquals(2.0, params.radius, 1e-6)
        assertEquals(45.0, params.spreadAngleDegrees, 1e-6)
        assertEquals(0.6, params.distanceRolloff, 1e-6)
        assertEquals(1.2, params.referenceDistance, 1e-6)
        assertEquals(0.90, params.headroom, 1e-6)
    }

    @Test
    fun toSpatialParameters_disabledState_preservesBypassFlag() {
        val state = SpatialUiState(isEnabled = false)
        val params = state.toSpatialParameters()
        assertFalse(params.isEnabled)
    }
}
