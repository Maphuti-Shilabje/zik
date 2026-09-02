package com.zik.music.playback

import android.content.Context
import android.content.SharedPreferences
import android.media.audiofx.BassBoost
import android.media.audiofx.Equalizer
import android.media.audiofx.Virtualizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class EqualizerUiState(
    val isEnabled: Boolean = false,
    val bassBoostStrength: Int = 0, // 0..1000
    val virtualizerStrength: Int = 0, // 0..1000
    val currentPreset: Int = 0, // 0 = Flat by default
    val presets: List<String> = listOf("Flat", "Bass Boost", "Rock", "Pop", "Jazz", "Electronic", "Vocal", "Classical", "Custom"),
    val bandFrequencies: List<Int> = listOf(60, 230, 910, 3600, 14000), // in Hz
    val bandLevels: List<Int> = listOf(0, 0, 0, 0, 0), // in mB (millibels: -1500..+1500)
    val minBandLevel: Int = -1500,
    val maxBandLevel: Int = 1500
)

class AudioEffectsManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences("zik_equalizer_prefs", Context.MODE_PRIVATE)

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    // Standard preset gain curves (in millibels: 100 mB = 1 dB)
    private val defaultPresetCurves = listOf(
        listOf(0, 0, 0, 0, 0),             // Flat
        listOf(700, 500, 100, 0, -200),    // Bass Boost
        listOf(500, 300, -100, 400, 600),   // Rock
        listOf(-100, 200, 500, 300, -100),  // Pop
        listOf(300, 200, 100, 200, 400),    // Jazz
        listOf(600, 400, 0, 300, 500),      // Electronic
        listOf(-200, 300, 700, 400, 0),     // Vocal
        listOf(400, 300, -100, 300, 400),   // Classical
        listOf(0, 0, 0, 0, 0)              // Custom
    )

    private val _state = MutableStateFlow(
        EqualizerUiState(
            isEnabled = prefs.getBoolean("eq_enabled", false),
            bassBoostStrength = prefs.getInt("eq_bass", 0),
            virtualizerStrength = prefs.getInt("eq_virt", 0),
            currentPreset = prefs.getInt("eq_preset", 0),
            bandLevels = (0 until 5).map { prefs.getInt("eq_band_$it", 0) }
        )
    )
    val state: StateFlow<EqualizerUiState> = _state.asStateFlow()

    private var currentSessionId: Int = 0

    companion object {
        @Volatile
        private var instance: AudioEffectsManager? = null

        fun getInstance(context: Context): AudioEffectsManager {
            return instance ?: synchronized(this) {
                instance ?: AudioEffectsManager(context).also { instance = it }
            }
        }
    }

    fun attachAudioSession(audioSessionId: Int) {
        if (audioSessionId == 0 && currentSessionId != 0) return
        currentSessionId = audioSessionId

        releaseNativeEffects()

        try {
            equalizer = Equalizer(0, audioSessionId)
            bassBoost = BassBoost(0, audioSessionId)
            virtualizer = Virtualizer(0, audioSessionId)

            val isEnabled = _state.value.isEnabled
            equalizer?.enabled = isEnabled
            bassBoost?.enabled = isEnabled
            virtualizer?.enabled = isEnabled

            // Apply Bass Boost & Virtualizer
            if (bassBoost?.strengthSupported == true) {
                bassBoost?.setStrength(_state.value.bassBoostStrength.toShort())
            }
            if (virtualizer?.strengthSupported == true) {
                virtualizer?.setStrength(_state.value.virtualizerStrength.toShort())
            }

            // Extract hardware bands & frequency spectrum
            val numBands = (equalizer?.numberOfBands?.toInt() ?: 5).coerceAtLeast(1)
            val minLevel = equalizer?.bandLevelRange?.get(0)?.toInt() ?: -1500
            val maxLevel = equalizer?.bandLevelRange?.get(1)?.toInt() ?: 1500

            val freqs = mutableListOf<Int>()
            val levels = mutableListOf<Int>()

            for (i in 0 until numBands) {
                val centerFreq = (equalizer?.getCenterFreq(i.toShort()) ?: (1000 * (i + 1))) / 1000
                freqs.add(centerFreq)

                val savedLevel = prefs.getInt("eq_band_$i", _state.value.bandLevels.getOrElse(i) { 0 })
                    .coerceIn(minLevel, maxLevel)
                equalizer?.setBandLevel(i.toShort(), savedLevel.toShort())
                levels.add(savedLevel)
            }

            // Extract hardware presets if available, or keep rich defaults
            val numPresets = equalizer?.numberOfPresets?.toInt() ?: 0
            val presetList = if (numPresets > 0) {
                val list = mutableListOf<String>()
                for (p in 0 until numPresets) {
                    list.add(equalizer?.getPresetName(p.toShort()) ?: "Preset $p")
                }
                list.add("Custom")
                list
            } else {
                _state.value.presets
            }

            _state.value = _state.value.copy(
                bandFrequencies = if (freqs.isNotEmpty()) freqs else _state.value.bandFrequencies,
                bandLevels = if (levels.isNotEmpty()) levels else _state.value.bandLevels,
                presets = presetList,
                minBandLevel = minLevel,
                maxBandLevel = maxLevel
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun setEnabled(enabled: Boolean) {
        try {
            equalizer?.enabled = enabled
            bassBoost?.enabled = enabled
            virtualizer?.enabled = enabled
        } catch (e: Exception) {
            e.printStackTrace()
        }
        prefs.edit().putBoolean("eq_enabled", enabled).apply()
        _state.value = _state.value.copy(isEnabled = enabled)
    }

    fun setBandLevel(bandIndex: Int, levelMillibels: Int) {
        val clampedLevel = levelMillibels.coerceIn(_state.value.minBandLevel, _state.value.maxBandLevel)
        try {
            equalizer?.setBandLevel(bandIndex.toShort(), clampedLevel.toShort())
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val currentLevels = _state.value.bandLevels.toMutableList()
        if (bandIndex in currentLevels.indices) {
            currentLevels[bandIndex] = clampedLevel
        }

        val customIndex = _state.value.presets.size - 1
        prefs.edit()
            .putInt("eq_band_$bandIndex", clampedLevel)
            .putInt("eq_preset", customIndex)
            .apply()

        _state.value = _state.value.copy(
            bandLevels = currentLevels,
            currentPreset = customIndex
        )
    }

    fun setBassBoost(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        try {
            if (bassBoost?.strengthSupported == true) {
                bassBoost?.setStrength(clamped.toShort())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        prefs.edit().putInt("eq_bass", clamped).apply()
        _state.value = _state.value.copy(bassBoostStrength = clamped)
    }

    fun setVirtualizer(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        try {
            if (virtualizer?.strengthSupported == true) {
                virtualizer?.setStrength(clamped.toShort())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        prefs.edit().putInt("eq_virt", clamped).apply()
        _state.value = _state.value.copy(virtualizerStrength = clamped)
    }

    fun usePreset(presetIndex: Int) {
        val numPresets = equalizer?.numberOfPresets?.toInt() ?: 0

        val newLevels = if (presetIndex in 0 until numPresets) {
            try {
                equalizer?.usePreset(presetIndex.toShort())
            } catch (e: Exception) {
                e.printStackTrace()
            }
            val numBands = equalizer?.numberOfBands?.toInt() ?: _state.value.bandLevels.size
            (0 until numBands).map { equalizer?.getBandLevel(it.toShort())?.toInt() ?: 0 }
        } else if (presetIndex in defaultPresetCurves.indices) {
            val curve = defaultPresetCurves[presetIndex]
            curve.forEachIndexed { bandIdx, lvl ->
                try {
                    equalizer?.setBandLevel(bandIdx.toShort(), lvl.toShort())
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            curve
        } else {
            _state.value.bandLevels
        }

        val editor = prefs.edit()
        newLevels.forEachIndexed { idx, lvl -> editor.putInt("eq_band_$idx", lvl) }
        editor.putInt("eq_preset", presetIndex).apply()

        _state.value = _state.value.copy(
            currentPreset = presetIndex,
            bandLevels = newLevels
        )
    }

    fun resetToFlat() {
        usePreset(0) // 0 is Flat
        setBassBoost(0)
        setVirtualizer(0)
    }

    private fun releaseNativeEffects() {
        try {
            equalizer?.release()
            bassBoost?.release()
            virtualizer?.release()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        equalizer = null
        bassBoost = null
        virtualizer = null
    }

    fun release() {
        releaseNativeEffects()
        instance = null
    }
}
