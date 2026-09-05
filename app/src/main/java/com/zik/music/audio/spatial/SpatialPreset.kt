package com.zik.music.audio.spatial

enum class SpatialPreset(val displayName: String) {
    SUBTLE("Subtle"),
    BALANCED("Balanced"),
    WIDE("Wide"),
    IMMERSIVE("Immersive"),
    CUSTOM("Custom");

    fun defaultProfile(): SpatialPresetProfile = when (this) {
        SUBTLE -> SpatialPresetProfile(
            speedHz = 0.05,
            radius = 1.2,
            spreadAngleDegrees = 20.0,
            distanceRolloff = 0.25,
            referenceDistance = 1.0,
            headroom = 0.95
        )
        BALANCED -> SpatialPresetProfile(
            speedHz = 0.10,
            radius = 1.5,
            spreadAngleDegrees = 30.0,
            distanceRolloff = 0.50,
            referenceDistance = 1.0,
            headroom = 0.95
        )
        WIDE -> SpatialPresetProfile(
            speedHz = 0.08,
            radius = 1.8,
            spreadAngleDegrees = 55.0,
            distanceRolloff = 0.40,
            referenceDistance = 1.0,
            headroom = 0.95
        )
        IMMERSIVE -> SpatialPresetProfile(
            speedHz = 0.15,
            radius = 2.2,
            spreadAngleDegrees = 35.0,
            distanceRolloff = 0.75,
            referenceDistance = 1.0,
            headroom = 0.95
        )
        CUSTOM -> SpatialPresetProfile(
            speedHz = 0.10,
            radius = 1.5,
            spreadAngleDegrees = 30.0,
            distanceRolloff = 0.50,
            referenceDistance = 1.0,
            headroom = 0.95
        )
    }
}

data class SpatialPresetProfile(
    val speedHz: Double,
    val radius: Double,
    val spreadAngleDegrees: Double,
    val distanceRolloff: Double = 0.50,
    val referenceDistance: Double = 1.0,
    val headroom: Double = 0.95
)
