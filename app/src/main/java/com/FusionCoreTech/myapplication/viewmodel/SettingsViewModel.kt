package com.FusionCoreTech.myapplication.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.FusionCoreTech.myapplication.model.SettingsState

class SettingsViewModel : ViewModel() {

    private val _settingsState = mutableStateOf(
        SettingsState(
            timer = "00:30:00",
            remainingSeconds = 1800, // 30 minutes = 1800 seconds
            appVersion = "1.2.2"
        )
    )
    val settingsState: State<SettingsState> = _settingsState

    fun updateTimer(timer: String, remainingSeconds: Int) {
        _settingsState.value = _settingsState.value.copy(
            timer = timer,
            remainingSeconds = remainingSeconds
        )
    }

    fun getProgressPercentage(): Float {
        // Total time is 30 minutes = 1800 seconds
        val totalSeconds = 1800f
        val remaining = _settingsState.value.remainingSeconds.toFloat()
        return (remaining / totalSeconds).coerceIn(0f, 1f)
    }
}
