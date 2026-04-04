package com.FusionCoreTech.myapplication.viewmodel

import android.app.Application
import android.net.TrafficStats
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.FusionCoreTech.myapplication.dns.SelectedDnsPrefs
import com.FusionCoreTech.myapplication.model.ConnectionState
import com.FusionCoreTech.myapplication.model.Location
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val PREFS_NAME = "adshield_connection"
private const val KEY_REMAINING_SECONDS = "remaining_seconds"
private const val DEFAULT_SESSION_SECONDS = 30 * 60 // 30 minutes
/** Seconds added to remaining time each time user completes a rewarded ad on Home. */
private const val REWARD_AD_SECONDS = 30 * 60

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)

    private fun loadSavedRemainingSeconds(): Int {
        if (!prefs.contains(KEY_REMAINING_SECONDS)) {
            prefs.edit().putInt(KEY_REMAINING_SECONDS, DEFAULT_SESSION_SECONDS).apply()
        }
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

    private val _selectedLocation = mutableStateOf(
        Location(name = SelectedDnsPrefs.getSelectedName(application))
    )
    val selectedLocation: State<Location> = _selectedLocation

    fun selectLocation(location: Location) {
        _selectedLocation.value = location
        SelectedDnsPrefs.saveSelectedName(getApplication(), location.name)
    }

    private var countdownJob: Job? = null
    private var speedUpdateJob: Job? = null

    /** Add time when user earns reward (after watching one rewarded ad). Saves and updates UI. */
    fun addTimeFromReward() {
        val addSeconds = REWARD_AD_SECONDS
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
            _connectionState.value = current.copy(
                isConnected = false,
                downloadSpeed = 0.00f,
                uploadSpeed = 0.00f,
                timer = "00:00:00"
            )
        } else {
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
            fun totalRx(): Long = TrafficStats.getTotalRxBytes().let { if (it < 0L) 0L else it }
            fun totalTx(): Long = TrafficStats.getTotalTxBytes().let { if (it < 0L) 0L else it }

            var lastRxBytes: Long = totalRx()
            var lastTxBytes: Long = totalTx()
            var lastTimeMs: Long = System.currentTimeMillis()

            while (_connectionState.value.isConnected) {
                delay(1000L)
                val now = System.currentTimeMillis()
                val rxBytes = totalRx()
                val txBytes = totalTx()
                val elapsedSec = (now - lastTimeMs) / 1000.0
                if (elapsedSec > 0) {
                    val rawDown = ((rxBytes - lastRxBytes).toFloat() / elapsedSec.toFloat()).coerceAtLeast(0f)
                    val rawUp = ((txBytes - lastTxBytes).toFloat() / elapsedSec.toFloat()).coerceAtLeast(0f)
                    val current = _connectionState.value
                    _connectionState.value = current.copy(
                        downloadSpeed = rawDown,
                        uploadSpeed = rawUp
                    )
                }
                lastRxBytes = rxBytes
                lastTxBytes = txBytes
                lastTimeMs = now
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
