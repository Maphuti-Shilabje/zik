package com.zik.music.audio.spatial

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.abs

data class SpatialUiState(
    val isEnabled: Boolean = false,
    val preset: SpatialPreset = SpatialPreset.BALANCED,
    val intensity: Double = 1.0,
    val speedHz: Double = 0.10,
    val radius: Double = 1.5,
    val spreadAngleDegrees: Double = 30.0,
    val distanceRolloff: Double = 0.50,
    val referenceDistance: Double = 1.0,
    val headroom: Double = 0.95
) {
    /**
     * Resolves the current UI state and perceptual intensity into real-time SpatialParameters.
     * At preset = BALANCED and intensity = 1.0, resolves exactly to the reference baseline.
     */
    fun toSpatialParameters(): SpatialParameters {
        val clampedIntensity = intensity.coerceIn(0.0, 1.0)

        // 1. Distance rolloff scales linearly from 0.0 (no distance modulation) to target distanceRolloff
        val effectiveRolloff = distanceRolloff * clampedIntensity

        // 2. Radius interpolates from referenceDistance (1.0m, no excess distance) to target radius
        val effectiveRadius = referenceDistance + (radius - referenceDistance) * clampedIntensity

        // 3. Soundstage spread contracts towards a conservative 20 deg aperture at 0% intensity
        val minSpreadDegrees = 20.0
        val effectiveSpread = minSpreadDegrees + (spreadAngleDegrees - minSpreadDegrees) * clampedIntensity

        // 4. Orbit speed scales gracefully (20% base movement at 0% intensity up to 100% full speed)
        val effectiveSpeed = speedHz * (0.2 + 0.8 * clampedIntensity)

        return SpatialParameters(
            isEnabled = isEnabled,
            speedHz = effectiveSpeed.coerceIn(0.001, 2.0),
            radius = effectiveRadius.coerceIn(0.5, 10.0),
            distanceRolloff = effectiveRolloff.coerceIn(0.0, 2.0),
            referenceDistance = referenceDistance,
            headroom = headroom,
            spreadAngleDegrees = effectiveSpread.coerceIn(10.0, 90.0)
        )
    }

    /**
     * Checks if current parameters match the given preset profile within tolerance.
     */
    fun matchesPreset(targetPreset: SpatialPreset, tolerance: Double = 0.001): Boolean {
        val profile = targetPreset.defaultProfile()
        return abs(speedHz - profile.speedHz) < tolerance &&
                abs(radius - profile.radius) < 0.01 &&
                abs(spreadAngleDegrees - profile.spreadAngleDegrees) < 0.1 &&
                abs(distanceRolloff - profile.distanceRolloff) < 0.01
    }
}

class SpatialMotionManager private constructor(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences("zik_spatial_prefs", Context.MODE_PRIVATE)

    private val _state: MutableStateFlow<SpatialUiState>

    init {
        val enabled = prefs.getBoolean(KEY_ENABLED, false)
        val intensity = prefs.getFloat(KEY_INTENSITY, 1.0f).toDouble().coerceIn(0.0, 1.0)
        val speedHz = prefs.getFloat(KEY_SPEED_HZ, 0.1f).toDouble()
        val radius = prefs.getFloat(KEY_RADIUS, 1.5f).toDouble()
        val spreadDegrees = prefs.getFloat(KEY_SPREAD_DEGREES, 30.0f).toDouble()
        val distanceRolloff = prefs.getFloat(KEY_DISTANCE_ROLLOFF, 0.5f).toDouble()
        val refDistance = prefs.getFloat(KEY_REF_DISTANCE, 1.0f).toDouble()
        val headroom = prefs.getFloat(KEY_HEADROOM, 0.95f).toDouble()

        val preset = if (prefs.contains(KEY_PRESET)) {
            val savedPresetName = prefs.getString(KEY_PRESET, null)
            SpatialPreset.entries.find { it.name == savedPresetName } ?: SpatialPreset.BALANCED
        } else {
            // Migration / first run: check if settings correspond to Balanced
            if (abs(speedHz - 0.1) < 0.005 && abs(spreadDegrees - 30.0) < 0.5 && abs(radius - 1.5) < 0.05) {
                SpatialPreset.BALANCED
            } else {
                SpatialPreset.CUSTOM
            }
        }

        _state = MutableStateFlow(
            SpatialUiState(
                isEnabled = enabled,
                preset = preset,
                intensity = intensity,
                speedHz = speedHz,
                radius = radius,
                spreadAngleDegrees = spreadDegrees,
                distanceRolloff = distanceRolloff,
                referenceDistance = refDistance,
                headroom = headroom
            )
        )
    }

    val state: StateFlow<SpatialUiState> = _state.asStateFlow()

    @Volatile
    private var attachedProcessor: SpatialMotionProcessor? = null

    companion object {
        const val KEY_ENABLED = "spatial_enabled"
        const val KEY_PRESET = "spatial_preset"
        const val KEY_INTENSITY = "spatial_intensity"
        const val KEY_SPEED_HZ = "spatial_speed_hz"
        const val KEY_RADIUS = "spatial_radius"
        const val KEY_SPREAD_DEGREES = "spatial_spread_degrees"
        const val KEY_DISTANCE_ROLLOFF = "spatial_distance_rolloff"
        const val KEY_REF_DISTANCE = "spatial_ref_distance"
        const val KEY_HEADROOM = "spatial_headroom"

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

    fun selectPreset(preset: SpatialPreset) {
        if (preset == SpatialPreset.CUSTOM) {
            val newState = _state.value.copy(preset = SpatialPreset.CUSTOM)
            prefs.edit().putString(KEY_PRESET, SpatialPreset.CUSTOM.name).apply()
            _state.value = newState
            attachedProcessor?.parameters = newState.toSpatialParameters()
            return
        }

        val profile = preset.defaultProfile()
        prefs.edit()
            .putString(KEY_PRESET, preset.name)
            .putFloat(KEY_SPEED_HZ, profile.speedHz.toFloat())
            .putFloat(KEY_RADIUS, profile.radius.toFloat())
            .putFloat(KEY_SPREAD_DEGREES, profile.spreadAngleDegrees.toFloat())
            .putFloat(KEY_DISTANCE_ROLLOFF, profile.distanceRolloff.toFloat())
            .apply()

        val newState = _state.value.copy(
            preset = preset,
            speedHz = profile.speedHz,
            radius = profile.radius,
            spreadAngleDegrees = profile.spreadAngleDegrees,
            distanceRolloff = profile.distanceRolloff
        )
        _state.value = newState
        attachedProcessor?.parameters = newState.toSpatialParameters()
    }

    fun setIntensity(intensity: Double) {
        val clamped = intensity.coerceIn(0.0, 1.0)
        prefs.edit().putFloat(KEY_INTENSITY, clamped.toFloat()).apply()
        val newState = _state.value.copy(intensity = clamped)
        _state.value = newState
        attachedProcessor?.parameters = newState.toSpatialParameters()
    }

    fun setSpeedHz(speedHz: Double) {
        val clamped = speedHz.coerceIn(0.01, 1.0)
        val currentPreset = _state.value.preset
        val updatedPreset = if (currentPreset != SpatialPreset.CUSTOM) {
            val profile = currentPreset.defaultProfile()
            if (abs(clamped - profile.speedHz) > 0.001) SpatialPreset.CUSTOM else currentPreset
        } else {
            SpatialPreset.CUSTOM
        }

        prefs.edit()
            .putFloat(KEY_SPEED_HZ, clamped.toFloat())
            .putString(KEY_PRESET, updatedPreset.name)
            .apply()

        val newState = _state.value.copy(speedHz = clamped, preset = updatedPreset)
        _state.value = newState
        attachedProcessor?.parameters = newState.toSpatialParameters()
    }

    fun setRadius(radius: Double) {
        val clamped = radius.coerceIn(0.5, 5.0)
        val currentPreset = _state.value.preset
        val updatedPreset = if (currentPreset != SpatialPreset.CUSTOM) {
            val profile = currentPreset.defaultProfile()
            if (abs(clamped - profile.radius) > 0.01) SpatialPreset.CUSTOM else currentPreset
        } else {
            SpatialPreset.CUSTOM
        }

        prefs.edit()
            .putFloat(KEY_RADIUS, clamped.toFloat())
            .putString(KEY_PRESET, updatedPreset.name)
            .apply()

        val newState = _state.value.copy(radius = clamped, preset = updatedPreset)
        _state.value = newState
        attachedProcessor?.parameters = newState.toSpatialParameters()
    }

    fun setSpreadAngleDegrees(degrees: Double) {
        val clamped = degrees.coerceIn(10.0, 90.0)
        val currentPreset = _state.value.preset
        val updatedPreset = if (currentPreset != SpatialPreset.CUSTOM) {
            val profile = currentPreset.defaultProfile()
            if (abs(clamped - profile.spreadAngleDegrees) > 0.1) SpatialPreset.CUSTOM else currentPreset
        } else {
            SpatialPreset.CUSTOM
        }

        prefs.edit()
            .putFloat(KEY_SPREAD_DEGREES, clamped.toFloat())
            .putString(KEY_PRESET, updatedPreset.name)
            .apply()

        val newState = _state.value.copy(spreadAngleDegrees = clamped, preset = updatedPreset)
        _state.value = newState
        attachedProcessor?.parameters = newState.toSpatialParameters()
    }
}
