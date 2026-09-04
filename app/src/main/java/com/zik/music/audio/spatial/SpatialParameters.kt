package com.zik.music.audio.spatial

data class SpatialParameters(
    val isEnabled: Boolean = false,
    val speedHz: Double = 0.1,
    val radius: Double = 1.5,
    val distanceRolloff: Double = 0.5,
    val referenceDistance: Double = 1.0,
    val headroom: Double = 0.95
)
