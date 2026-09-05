package com.zik.music.audio.spatial

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpatialPresetTest {

    @Test
    fun allExpectedPresets_existWithProperDisplayNames() {
        val presets = SpatialPreset.entries
        assertEquals(5, presets.size)
        assertTrue(presets.contains(SpatialPreset.SUBTLE))
        assertTrue(presets.contains(SpatialPreset.BALANCED))
        assertTrue(presets.contains(SpatialPreset.WIDE))
        assertTrue(presets.contains(SpatialPreset.IMMERSIVE))
        assertTrue(presets.contains(SpatialPreset.CUSTOM))

        assertEquals("Subtle", SpatialPreset.SUBTLE.displayName)
        assertEquals("Balanced", SpatialPreset.BALANCED.displayName)
        assertEquals("Wide", SpatialPreset.WIDE.displayName)
        assertEquals("Immersive", SpatialPreset.IMMERSIVE.displayName)
        assertEquals("Custom", SpatialPreset.CUSTOM.displayName)
    }

    @Test
    fun balancedPreset_resolvesToExactBaselineReferenceValues() {
        val profile = SpatialPreset.BALANCED.defaultProfile()
        assertEquals(0.10, profile.speedHz, 1e-6)
        assertEquals(1.5, profile.radius, 1e-6)
        assertEquals(30.0, profile.spreadAngleDegrees, 1e-6)
        assertEquals(0.50, profile.distanceRolloff, 1e-6)
        assertEquals(1.0, profile.referenceDistance, 1e-6)
        assertEquals(0.95, profile.headroom, 1e-6)
    }

    @Test
    fun subtlePreset_resolvesToConservativeProfileWithinValidBounds() {
        val subtle = SpatialPreset.SUBTLE.defaultProfile()
        val balanced = SpatialPreset.BALANCED.defaultProfile()

        assertTrue("Subtle speed should be gentler than Balanced", subtle.speedHz < balanced.speedHz)
        assertTrue("Subtle spread should be narrower than Balanced", subtle.spreadAngleDegrees < balanced.spreadAngleDegrees)
        assertTrue("Subtle distance rolloff should be softer than Balanced", subtle.distanceRolloff < balanced.distanceRolloff)
        assertTrue(subtle.speedHz > 0.0)
        assertTrue(subtle.radius > 0.5)
        assertTrue(subtle.spreadAngleDegrees >= 10.0)
    }

    @Test
    fun widePreset_resolvesToBroadSoundstageWithinValidBounds() {
        val wide = SpatialPreset.WIDE.defaultProfile()
        val balanced = SpatialPreset.BALANCED.defaultProfile()

        assertTrue("Wide spread should exceed Balanced spread", wide.spreadAngleDegrees > balanced.spreadAngleDegrees)
        assertTrue(wide.spreadAngleDegrees <= 90.0)
        assertTrue(wide.speedHz > 0.0)
        assertTrue(wide.radius > 0.5)
    }

    @Test
    fun immersivePreset_resolvesToStrongerSpatialMotionWithinValidBounds() {
        val immersive = SpatialPreset.IMMERSIVE.defaultProfile()
        val balanced = SpatialPreset.BALANCED.defaultProfile()

        assertTrue("Immersive speed should exceed Balanced speed", immersive.speedHz > balanced.speedHz)
        assertTrue("Immersive radius should exceed Balanced radius", immersive.radius > balanced.radius)
        assertTrue("Immersive rolloff should exceed Balanced rolloff", immersive.distanceRolloff > balanced.distanceRolloff)
        assertTrue(immersive.spreadAngleDegrees in 10.0..90.0)
    }
}
