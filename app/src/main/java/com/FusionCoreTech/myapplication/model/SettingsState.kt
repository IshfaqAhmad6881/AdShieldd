package com.FusionCoreTech.myapplication.model

data class SettingsState(
    val timer: String = "00:30:00", // Timer format HH:MM:SS
    val remainingSeconds: Int = 1800, // 30 minutes in seconds
    val appVersion: String = "1.2.2"
)
