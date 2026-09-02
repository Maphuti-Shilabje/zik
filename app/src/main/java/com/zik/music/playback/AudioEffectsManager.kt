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
    val currentPreset: Int = -1, // -1 is custom
    val presets: List<String> = emptyList(),
    val bandFrequencies: List<Int> = emptyList(), // in Hz, e.g. [60, 230, 910, 3600, 14000]
    val bandLevels: List<Int> = emptyList(), // in mB (millibels, e.g. -1500 to +1500)
    val minBandLevel: Int = -1500,
    val maxBandLevel: Int = 1500
)

class AudioEffectsManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("zik_equalizer_prefs", Context.MODE_PRIVATE)

    private var equalizer: Equalizer? = null
    private var bassBoost: BassBoost? = null
    private var virtualizer: Virtualizer? = null

    private val _state = MutableStateFlow(EqualizerUiState())
    val state: StateFlow<EqualizerUiState> = _state.asStateFlow()

    private var currentSessionId: Int = 0

    fun attachAudioSession(audioSessionId: Int) {
        if (audioSessionId == 0 && currentSessionId != 0) return
        currentSessionId = audioSessionId

        release()

        try {
            equalizer = Equalizer(0, audioSessionId)
            bassBoost = BassBoost(0, audioSessionId)
            virtualizer = Virtualizer(0, audioSessionId)

            val isEnabled = prefs.getBoolean("eq_enabled", false)
            val bassVal = prefs.getInt("eq_bass", 0)
            val virtVal = prefs.getInt("eq_virt", 0)
            val presetIdx = prefs.getInt("eq_preset", -1)

            equalizer?.enabled = isEnabled
            bassBoost?.enabled = isEnabled
            virtualizer?.enabled = isEnabled

            if (bassBoost?.strengthSupported == true) {
                bassBoost?.setStrength(bassVal.toShort())
            }
            if (virtualizer?.strengthSupported == true) {
                virtualizer?.setStrength(virtVal.toShort())
            }

            val numBands = equalizer?.numberOfBands?.toInt() ?: 5
            val minLevel = equalizer?.bandLevelRange?.get(0)?.toInt() ?: -1500
            val maxLevel = equalizer?.bandLevelRange?.get(1)?.toInt() ?: 1500

            val freqs = mutableListOf<Int>()
            val levels = mutableListOf<Int>()

            for (i in 0 until numBands) {
                val centerFreq = (equalizer?.getCenterFreq(i.toShort()) ?: (1000 * (i + 1))) / 1000
                freqs.add(centerFreq)

                val savedLevel = prefs.getInt("eq_band_$i", 0)
                equalizer?.setBandLevel(i.toShort(), savedLevel.toShort())
                levels.add(savedLevel)
            }

            val numPresets = equalizer?.numberOfPresets?.toInt() ?: 0
            val presetList = mutableListOf<String>()
            for (p in 0 until numPresets) {
                presetList.add(equalizer?.getPresetName(p.toShort()) ?: "Preset $p")
            }
            presetList.add("Custom")

            if (presetIdx in 0 until numPresets) {
                equalizer?.usePreset(presetIdx.toShort())
            }

            _state.value = EqualizerUiState(
                isEnabled = isEnabled,
                bassBoostStrength = bassVal,
                virtualizerStrength = virtVal,
                currentPreset = presetIdx,
                presets = presetList,
                bandFrequencies = freqs,
                bandLevels = levels,
                minBandLevel = minLevel,
                maxBandLevel = maxLevel
            )
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback default state
            _state.value = EqualizerUiState(
                bandFrequencies = listOf(60, 230, 910, 3600, 14000),
                bandLevels = listOf(0, 0, 0, 0, 0)
            )
        }
    }

    fun setEnabled(enabled: Boolean) {
        equalizer?.enabled = enabled
        bassBoost?.enabled = enabled
        virtualizer?.enabled = enabled
        prefs.edit().putBoolean("eq_enabled", enabled).apply()
        _state.value = _state.value.copy(isEnabled = enabled)
    }

    fun setBandLevel(bandIndex: Int, levelMillibels: Int) {
        val clampedLevel = levelMillibels.coerceIn(_state.value.minBandLevel, _state.value.maxBandLevel)
        equalizer?.setBandLevel(bandIndex.toShort(), clampedLevel.toShort())

        val currentLevels = _state.value.bandLevels.toMutableList()
        if (bandIndex in currentLevels.indices) {
            currentLevels[bandIndex] = clampedLevel
        }

        prefs.edit()
            .putInt("eq_band_$bandIndex", clampedLevel)
            .putInt("eq_preset", -1)
            .apply()

        _state.value = _state.value.copy(
            bandLevels = currentLevels,
            currentPreset = -1
        )
    }

    fun setBassBoost(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        if (bassBoost?.strengthSupported == true) {
            bassBoost?.setStrength(clamped.toShort())
        }
        prefs.edit().putInt("eq_bass", clamped).apply()
        _state.value = _state.value.copy(bassBoostStrength = clamped)
    }

    fun setVirtualizer(strength: Int) {
        val clamped = strength.coerceIn(0, 1000)
        if (virtualizer?.strengthSupported == true) {
            virtualizer?.setStrength(clamped.toShort())
        }
        prefs.edit().putInt("eq_virt", clamped).apply()
        _state.value = _state.value.copy(virtualizerStrength = clamped)
    }

    fun usePreset(presetIndex: Int) {
        val numPresets = equalizer?.numberOfPresets?.toInt() ?: 0
        if (presetIndex in 0 until numPresets) {
            equalizer?.usePreset(presetIndex.toShort())

            val levels = mutableListOf<Int>()
            val numBands = equalizer?.numberOfBands?.toInt() ?: _state.value.bandLevels.size
            for (i in 0 until numBands) {
                val lvl = equalizer?.getBandLevel(i.toShort())?.toInt() ?: 0
                levels.add(lvl)
                prefs.edit().putInt("eq_band_$i", lvl).apply()
            }

            prefs.edit().putInt("eq_preset", presetIndex).apply()
            _state.value = _state.value.copy(
                currentPreset = presetIndex,
                bandLevels = levels
            )
        }
    }

    fun resetToFlat() {
        val numBands = _state.value.bandLevels.size
        val flatLevels = List(numBands) { 0 }
        for (i in 0 until numBands) {
            equalizer?.setBandLevel(i.toShort(), 0)
            prefs.edit().putInt("eq_band_$i", 0).apply()
        }
        setBassBoost(0)
        setVirtualizer(0)
        prefs.edit().putInt("eq_preset", -1).apply()
        _state.value = _state.value.copy(
            bandLevels = flatLevels,
            currentPreset = -1,
            bassBoostStrength = 0,
            virtualizerStrength = 0
        )
    }

    fun release() {
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
}
