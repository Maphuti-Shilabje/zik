package com.zik.music.audio.spatial

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpatialUiStateTest {

    @Test
    fun defaultState_hasBalancedPresetAndFullIntensity() {
        val state = SpatialUiState()
        assertFalse(state.isEnabled)
        assertEquals(SpatialPreset.BALANCED, state.preset)
        assertEquals(1.0, state.intensity, 1e-6)
        assertEquals(0.10, state.speedHz, 1e-6)
        assertEquals(1.5, state.radius, 1e-6)
        assertEquals(30.0, state.spreadAngleDegrees, 1e-6)
        assertEquals(0.50, state.distanceRolloff, 1e-6)
        assertEquals(1.0, state.referenceDistance, 1e-6)
        assertEquals(0.95, state.headroom, 1e-6)
    }

    @Test
    fun balancedRegressionProtection_balancedAtFullIntensity_matchesReferenceBaselineExactly() {
        // Critical regression test: Balanced preset at 100% intensity MUST produce the exact
        // pre-preset reference baseline configuration that was validated in listening tests.
        val state = SpatialUiState(
            isEnabled = true,
            preset = SpatialPreset.BALANCED,
            intensity = 1.0,
            speedHz = 0.10,
            radius = 1.5,
            spreadAngleDegrees = 30.0,
            distanceRolloff = 0.50,
            referenceDistance = 1.0,
            headroom = 0.95
        )
        val params = state.toSpatialParameters()

        assertTrue(params.isEnabled)
        assertEquals(0.10, params.speedHz, 1e-6)
        assertEquals(1.5, params.radius, 1e-6)
        assertEquals(30.0, params.spreadAngleDegrees, 1e-6)
        assertEquals(0.50, params.distanceRolloff, 1e-6)
        assertEquals(1.0, params.referenceDistance, 1e-6)
        assertEquals(0.95, params.headroom, 1e-6)
    }

    @Test
    fun toSpatialParameters_zeroIntensity_resolvesToMinimumSpatialCuesWithoutVolumeDrop() {
        val state = SpatialUiState(
            isEnabled = true,
            preset = SpatialPreset.BALANCED,
            intensity = 0.0,
            speedHz = 0.10,
            radius = 1.5,
            spreadAngleDegrees = 30.0,
            distanceRolloff = 0.50,
            referenceDistance = 1.0,
            headroom = 0.95
        )
        val params = state.toSpatialParameters()

        assertTrue(params.isEnabled)
        // Zero distance rolloff (no distance attenuation modulation)
        assertEquals(0.0, params.distanceRolloff, 1e-6)
        // Radius contracts to reference distance (1.0m, zero excess distance)
        assertEquals(1.0, params.radius, 1e-6)
        // Spread contracts towards conservative stereo aperture
        assertEquals(20.0, params.spreadAngleDegrees, 1e-6)
        // Headroom preserved at unity headroom baseline (NO volume drop)
        assertEquals(0.95, params.headroom, 1e-6)
        // Speed reduced to subtle drift
        assertEquals(0.02, params.speedHz, 1e-6)
    }

    @Test
    fun toSpatialParameters_intermediateIntensity_interpolatesMonotonically() {
        val state50 = SpatialUiState(
            isEnabled = true,
            preset = SpatialPreset.BALANCED,
            intensity = 0.5,
            speedHz = 0.10,
            radius = 1.5,
            spreadAngleDegrees = 30.0,
            distanceRolloff = 0.50
        )
        val params50 = state50.toSpatialParameters()

        assertEquals(0.25, params50.distanceRolloff, 1e-6)
        assertEquals(1.25, params50.radius, 1e-6)
        assertEquals(25.0, params50.spreadAngleDegrees, 1e-6)
        assertEquals(0.06, params50.speedHz, 1e-6)
    }

    @Test
    fun toSpatialParameters_intensityClamping_handlesOutOfRangeValues() {
        val underflow = SpatialUiState(intensity = -0.5)
        val overflow = SpatialUiState(intensity = 1.5)

        val paramsUnderflow = underflow.toSpatialParameters()
        val paramsOverflow = overflow.toSpatialParameters()

        assertEquals(0.0, paramsUnderflow.distanceRolloff, 1e-6)
        assertEquals(0.50, paramsOverflow.distanceRolloff, 1e-6)
    }

    @Test
    fun toSpatialParameters_disabledState_preservesBypassFlag() {
        val state = SpatialUiState(isEnabled = false)
        val params = state.toSpatialParameters()
        assertFalse(params.isEnabled)
    }

    @Test
    fun toSpatialParameters_neverProducesNaNOrInfiniteValues() {
        val extremeState = SpatialUiState(
            isEnabled = true,
            intensity = 1.0,
            speedHz = 100.0,
            radius = 100.0,
            spreadAngleDegrees = 360.0,
            distanceRolloff = 50.0
        )
        val params = extremeState.toSpatialParameters()

        assertFalse(params.speedHz.isNaN())
        assertFalse(params.speedHz.isInfinite())
        assertFalse(params.radius.isNaN())
        assertFalse(params.radius.isInfinite())
        assertFalse(params.spreadAngleDegrees.isNaN())
        assertFalse(params.spreadAngleDegrees.isInfinite())
        assertFalse(params.distanceRolloff.isNaN())
        assertFalse(params.distanceRolloff.isInfinite())
    }

    @Test
    fun matchesPreset_returnsTrueForMatchingProfileAndFalseWhenModified() {
        val balancedState = SpatialUiState()
        assertTrue(balancedState.matchesPreset(SpatialPreset.BALANCED))
        assertFalse(balancedState.matchesPreset(SpatialPreset.WIDE))

        val modifiedState = balancedState.copy(speedHz = 0.35)
        assertFalse(modifiedState.matchesPreset(SpatialPreset.BALANCED))
    }
}
