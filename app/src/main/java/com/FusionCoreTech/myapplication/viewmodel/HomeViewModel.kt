package com.FusionCoreTech.myapplication.viewmodel

import android.app.Application
import android.net.TrafficStats
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.FusionCoreTech.myapplication.model.ConnectionState
import com.FusionCoreTech.myapplication.model.Location
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val PREFS_NAME = "adshield_connection"
private const val KEY_REMAINING_SECONDS = "remaining_seconds"
private const val DEFAULT_SESSION_SECONDS = 30 * 60 // 30 minutes
private const val SMOOTH_ALPHA = 0.22f // 0–1: smaller = slower ramp, number ahista ahista badhega

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)

    private fun loadSavedRemainingSeconds(): Int {
        return prefs.getInt(KEY_REMAINING_SECONDS, DEFAULT_SESSION_SECONDS).coerceAtLeast(0)
    }

    private fun saveRemainingSeconds(seconds: Int) {
        prefs.edit().putInt(KEY_REMAINING_SECONDS, seconds).apply()
    }

    private val _remainingSeconds = mutableStateOf(loadSavedRemainingSeconds())
    val remainingSeconds: State<Int> = _remainingSeconds

    private val _connectionState = mutableStateOf(
        ConnectionState(
            isConnected = false,
            downloadSpeed = 0.00f,
            uploadSpeed = 0.00f,
            timer = "00:00:00"
        )
    )
    val connectionState: State<ConnectionState> = _connectionState

    private val _selectedLocation = mutableStateOf(Location(name = "Open DNS"))
    val selectedLocation: State<Location> = _selectedLocation

    fun selectLocation(location: Location) {
        _selectedLocation.value = location
    }

    private var countdownJob: Job? = null
    private var speedUpdateJob: Job? = null
    private var smoothedDownloadSpeed = 0f
    private var smoothedUploadSpeed = 0f

    /** Add 30 minutes when user earns reward (e.g. after watching ad). Saves and updates UI. */
    fun addTimeFromReward() {
        val addSeconds = 30 * 60 // 30 minutes
        val newTotal = _remainingSeconds.value + addSeconds
        _remainingSeconds.value = newTotal
        saveRemainingSeconds(newTotal)
        val current = _connectionState.value
        if (current.isConnected) {
            _connectionState.value = current.copy(timer = formatTimer(_remainingSeconds.value))
        }
    }

    fun toggleConnection() {
        val current = _connectionState.value
        if (current.isConnected) {
            countdownJob?.cancel()
            speedUpdateJob?.cancel()
            saveRemainingSeconds(_remainingSeconds.value)
            smoothedDownloadSpeed = 0f
            smoothedUploadSpeed = 0f
            _connectionState.value = current.copy(
                isConnected = false,
                downloadSpeed = 0.00f,
                uploadSpeed = 0.00f,
                timer = "00:00:00"
            )
        } else {
            smoothedDownloadSpeed = 0f
            smoothedUploadSpeed = 0f
            var startSeconds = _remainingSeconds.value
            if (startSeconds <= 0) {
                startSeconds = DEFAULT_SESSION_SECONDS
                _remainingSeconds.value = startSeconds
                saveRemainingSeconds(startSeconds)
            }
            _connectionState.value = current.copy(
                isConnected = true,
                downloadSpeed = 0.00f,
                uploadSpeed = 0.00f,
                timer = formatTimer(startSeconds)
            )
            startCountdown()
            startSpeedUpdates()
        }
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (_remainingSeconds.value > 0) {
                delay(1000L)
                val next = (_remainingSeconds.value - 1).coerceAtLeast(0)
                _remainingSeconds.value = next
                saveRemainingSeconds(next)
                _connectionState.value = _connectionState.value.copy(timer = formatTimer(next))
            }
            // Time ended – disconnect
            speedUpdateJob?.cancel()
            smoothedDownloadSpeed = 0f
            smoothedUploadSpeed = 0f
            _connectionState.value = _connectionState.value.copy(
                isConnected = false,
                downloadSpeed = 0f,
                uploadSpeed = 0f,
                timer = "00:00:00"
            )
        }
    }

    /**
     * Real-time device network speed via TrafficStats.
     * Download = getTotalRxBytes delta, Upload = getTotalTxBytes delta.
     */
    private fun startSpeedUpdates() {
        speedUpdateJob?.cancel()
        speedUpdateJob = viewModelScope.launch {
            var lastRxBytes: Long = TrafficStats.getTotalRxBytes().coerceAtLeast(0L)
            var lastTxBytes: Long = TrafficStats.getTotalTxBytes().coerceAtLeast(0L)
            var lastTimeMs: Long = System.currentTimeMillis()
            delay(300L)
            while (_connectionState.value.isConnected) {
                val now = System.currentTimeMillis()
                val rxBytes = TrafficStats.getTotalRxBytes().coerceAtLeast(0L)
                val txBytes = TrafficStats.getTotalTxBytes().coerceAtLeast(0L)
                val elapsedSec = (now - lastTimeMs) / 1000.0
                if (elapsedSec > 0) {
                    val rawDown = ((rxBytes - lastRxBytes).toFloat() / elapsedSec.toFloat()).coerceAtLeast(0f)
                    val rawUp = ((txBytes - lastTxBytes).toFloat() / elapsedSec.toFloat()).coerceAtLeast(0f)
                    smoothedDownloadSpeed = SMOOTH_ALPHA * rawDown + (1f - SMOOTH_ALPHA) * smoothedDownloadSpeed
                    smoothedUploadSpeed = SMOOTH_ALPHA * rawUp + (1f - SMOOTH_ALPHA) * smoothedUploadSpeed
                    val current = _connectionState.value
                    _connectionState.value = current.copy(
                        downloadSpeed = smoothedDownloadSpeed,
                        uploadSpeed = smoothedUploadSpeed
                    )
                }
                lastRxBytes = rxBytes
                lastTxBytes = txBytes
                lastTimeMs = now
                delay(500L)
            }
        }
    }

    private fun formatTimer(seconds: Int): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return "%02d:%02d:%02d".format(h, m, s)
    }
}
