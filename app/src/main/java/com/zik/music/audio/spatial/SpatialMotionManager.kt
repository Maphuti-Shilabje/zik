package com.zik.music.audio.spatial

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class SpatialUiState(
    val isEnabled: Boolean = false,
    val speedHz: Double = 0.1,
    val radius: Double = 1.5,
    val spreadAngleDegrees: Double = 30.0,
    val distanceRolloff: Double = 0.5,
    val referenceDistance: Double = 1.0,
    val headroom: Double = 0.95
) {
    fun toSpatialParameters(): SpatialParameters = SpatialParameters(
        isEnabled = isEnabled,
        speedHz = speedHz,
        radius = radius,
        distanceRolloff = distanceRolloff,
        referenceDistance = referenceDistance,
        headroom = headroom,
        spreadAngleDegrees = spreadAngleDegrees
    )
}

class SpatialMotionManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("zik_spatial_prefs", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(
        SpatialUiState(
            isEnabled = prefs.getBoolean(KEY_ENABLED, false),
            speedHz = prefs.getFloat(KEY_SPEED_HZ, 0.1f).toDouble(),
            radius = prefs.getFloat(KEY_RADIUS, 1.5f).toDouble(),
            spreadAngleDegrees = prefs.getFloat(KEY_SPREAD_DEGREES, 30.0f).toDouble(),
            distanceRolloff = prefs.getFloat(KEY_DISTANCE_ROLLOFF, 0.5f).toDouble(),
            referenceDistance = prefs.getFloat(KEY_REF_DISTANCE, 1.0f).toDouble(),
            headroom = prefs.getFloat(KEY_HEADROOM, 0.95f).toDouble()
        )
    )
    val state: StateFlow<SpatialUiState> = _state.asStateFlow()

    @Volatile
    private var attachedProcessor: SpatialMotionProcessor? = null

    companion object {
        private const val KEY_ENABLED = "spatial_enabled"
        private const val KEY_SPEED_HZ = "spatial_speed_hz"
        private const val KEY_RADIUS = "spatial_radius"
        private const val KEY_SPREAD_DEGREES = "spatial_spread_degrees"
        private const val KEY_DISTANCE_ROLLOFF = "spatial_distance_rolloff"
        private const val KEY_REF_DISTANCE = "spatial_ref_distance"
        private const val KEY_HEADROOM = "spatial_headroom"

        @Volatile
        private var instance: SpatialMotionManager? = null

        fun getInstance(context: Context): SpatialMotionManager {
            return instance ?: synchronized(this) {
                instance ?: SpatialMotionManager(context).also { instance = it }
            }
        }
    }

    fun attachProcessor(processor: SpatialMotionProcessor) {
        attachedProcessor = processor
        processor.parameters = _state.value.toSpatialParameters()
    }

    fun detachProcessor() {
        attachedProcessor = null
    }

    fun setEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_ENABLED, enabled).apply()
        val newState = _state.value.copy(isEnabled = enabled)
        _state.value = newState
        attachedProcessor?.parameters = newState.toSpatialParameters()
    }

    fun setSpeedHz(speedHz: Double) {
        val clamped = speedHz.coerceIn(0.01, 1.0)
        prefs.edit().putFloat(KEY_SPEED_HZ, clamped.toFloat()).apply()
        val newState = _state.value.copy(speedHz = clamped)
        _state.value = newState
        attachedProcessor?.parameters = newState.toSpatialParameters()
    }

    fun setRadius(radius: Double) {
        val clamped = radius.coerceIn(0.5, 5.0)
        prefs.edit().putFloat(KEY_RADIUS, clamped.toFloat()).apply()
        val newState = _state.value.copy(radius = clamped)
        _state.value = newState
        attachedProcessor?.parameters = newState.toSpatialParameters()
    }

    fun setSpreadAngleDegrees(degrees: Double) {
        val clamped = degrees.coerceIn(10.0, 90.0)
        prefs.edit().putFloat(KEY_SPREAD_DEGREES, clamped.toFloat()).apply()
        val newState = _state.value.copy(spreadAngleDegrees = clamped)
        _state.value = newState
        attachedProcessor?.parameters = newState.toSpatialParameters()
    }
}
