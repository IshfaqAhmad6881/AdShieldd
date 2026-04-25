package com.digitalnestapps.adshield.model

data class ConnectionState(
    val isConnected: Boolean = false,
    val downloadSpeed: Float = 0.00f,
    val uploadSpeed: Float = 0.00f,
    val timer: String = "00:00:00"
)
