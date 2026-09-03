package com.zik.music.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class SleepTimerState(
    val isActive: Boolean = false,
    val remainingSeconds: Long = 0L,
    val totalSeconds: Long = 0L,
    val isEndOfTrackMode: Boolean = false
) {
    val progress: Float
        get() = if (totalSeconds > 0) (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f) else 0f

    val formattedRemaining: String
        get() {
            val minutes = remainingSeconds / 60
            val seconds = remainingSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }
}

class SleepTimerManager(private val musicController: MusicController) {

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null

    private val _state = MutableStateFlow(SleepTimerState())
    val state: StateFlow<SleepTimerState> = _state.asStateFlow()

    private val fadeOutDurationSeconds = 30L

    fun startTimer(minutes: Int) {
        cancelTimer()
        val totalSec = minutes * 60L
        if (totalSec <= 0) return

        _state.value = SleepTimerState(
            isActive = true,
            remainingSeconds = totalSec,
            totalSeconds = totalSec,
            isEndOfTrackMode = false
        )

        musicController.setVolume(1f)

        timerJob = scope.launch {
            var currentSec = totalSec
            while (isActive && currentSec > 0) {
                delay(1000)
                currentSec--

                // Handle smooth gradual volume fade-out in the last 30 seconds
                if (currentSec <= fadeOutDurationSeconds) {
                    val fadeFraction = (currentSec.toFloat() / fadeOutDurationSeconds.toFloat()).coerceIn(0f, 1f)
                    musicController.setVolume(fadeFraction)
                }

                _state.value = _state.value.copy(remainingSeconds = currentSec)
            }

            if (isActive) {
                // Time is up: pause audio and restore volume for next morning
                musicController.pause()
                musicController.setVolume(1f)
                _state.value = SleepTimerState(isActive = false)
            }
        }
    }

    fun startEndOfTrackTimer(durationMs: Long, currentPositionMs: Long) {
        cancelTimer()
        val remainingMs = (durationMs - currentPositionMs).coerceAtLeast(1000L)
        val remainingSec = (remainingMs / 1000L).coerceAtLeast(1L)

        _state.value = SleepTimerState(
            isActive = true,
            remainingSeconds = remainingSec,
            totalSeconds = remainingSec,
            isEndOfTrackMode = true
        )

        musicController.setVolume(1f)
        val trackFadeOutSec = 15L.coerceAtMost(remainingSec)

        timerJob = scope.launch {
            var currentSec = remainingSec
            while (isActive && currentSec > 0) {
                delay(1000)
                currentSec--

                // Gradual fade out over the final 15s of the track
                if (currentSec <= trackFadeOutSec) {
                    val fadeFraction = (currentSec.toFloat() / trackFadeOutSec.toFloat()).coerceIn(0f, 1f)
                    musicController.setVolume(fadeFraction)
                }

                _state.value = _state.value.copy(remainingSeconds = currentSec)
            }

            if (isActive) {
                musicController.pause()
                musicController.setVolume(1f)
                _state.value = SleepTimerState(isActive = false)
            }
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
        musicController.setVolume(1f)
        _state.value = SleepTimerState(isActive = false)
    }
}
