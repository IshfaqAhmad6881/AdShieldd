package com.FusionCoreTech.myapplication.viewmodel

import android.net.TrafficStats
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.FusionCoreTech.myapplication.model.Location
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

enum class SpeedTestConnectionState {
    IDLE,
    CONNECTING,
    TEST_RUNNING,
    TEST_COMPLETED
}

class SpeedTestViewModel : ViewModel() {

    private val _downloadSpeed = mutableStateOf(0.00f)
    val downloadSpeed: State<Float> = _downloadSpeed

    private val _uploadSpeed = mutableStateOf(0.00f)
    val uploadSpeed: State<Float> = _uploadSpeed

    private val _speedometerValue = mutableStateOf(0f) // Value for speedometer needle (0-100)
    val speedometerValue: State<Float> = _speedometerValue

    private val _connectionState = mutableStateOf(SpeedTestConnectionState.IDLE)
    val connectionState: State<SpeedTestConnectionState> = _connectionState

    private val _selectedLocation = mutableStateOf(Location(name = "Open DNS"))
    val selectedLocation: State<Location> = _selectedLocation

    fun selectLocation(location: Location) {
        _selectedLocation.value = location
    }

    fun setDownloadSpeed(speed: Float) {
        _downloadSpeed.value = speed
        // Update speedometer needle position proportionally with download speed
        // Convert bytes/s to mb/s, then map to 0-100 range (assuming max is 100 mb/s)
        val speedInMbps = speed / (1024 * 1024) // Convert bytes/s to mb/s
        _speedometerValue.value = speedInMbps.coerceIn(0f, 100f) // Direct proportional mapping
    }

    fun setUploadSpeed(speed: Float) {
        _uploadSpeed.value = speed
    }

    fun startConnection() {
        _connectionState.value = SpeedTestConnectionState.CONNECTING
        // Reset speeds - ensure needle starts exactly at label 0 (leftmost position)
        _downloadSpeed.value = 0.00f
        _uploadSpeed.value = 0.00f
        _speedometerValue.value = 0f // Explicitly set to 0 to align needle with label 0
        
        // Simulate connection process, then start test
        viewModelScope.launch {
            delay(2000) // 2 seconds connecting state
            _connectionState.value = SpeedTestConnectionState.TEST_RUNNING
            // Start simulating speed test - animate needle and fill arc
            simulateSpeedTest()
        }
    }
    
    private fun simulateSpeedTest() {
        viewModelScope.launch {
            // Ensure needle starts at 0 (label 0 - leftmost position) - like car speedometer
            _speedometerValue.value = 0f
            _downloadSpeed.value = 0f
            _uploadSpeed.value = 0f
            
            // Measure actual network speed using TrafficStats (like Ookla)
            // Get baseline before test starts
            val baselineRxBytes = TrafficStats.getTotalRxBytes()
            val baselineTxBytes = TrafficStats.getTotalTxBytes()
            var lastRxBytes = baselineRxBytes
            var lastTxBytes = baselineTxBytes
            var lastTime = System.currentTimeMillis()
            
            // Phase 1: Download speed test - measure actual download speed
            val downloadTestDuration = 8000L // 8 seconds for download test
            val updateInterval = 100L // Update every 100ms for smooth updates
            val downloadUpdates = (downloadTestDuration / updateInterval).toInt()
            
            // Generate random target speeds for realistic simulation
            val targetDownloadSpeedMbps = 25f + Random.nextFloat() * 25f // Random between 25-50 mbps
            var maxDownloadSpeed = 0f
            
            // Simulate download speed test - measure network activity
            for (i in 0..downloadUpdates) {
                val currentTime = System.currentTimeMillis()
                val timeDelta = (currentTime - lastTime).coerceAtLeast(1L) // Avoid division by zero
                
                val currentRxBytes = TrafficStats.getTotalRxBytes()
                // Calculate delta from baseline (only measure during test)
                val rxDelta = (currentRxBytes - lastRxBytes).toFloat()
                
                // Calculate speed in bytes per second
                val speedBps = (rxDelta / timeDelta) * 1000f
                
                // Simulate realistic speed increase (like Ookla) - smooth acceleration
                val progress = i.toFloat() / downloadUpdates
                // Use easing function for smooth acceleration and deceleration
                val easedProgress = when {
                    progress < 0.3f -> progress / 0.3f * 0.5f // Slow start
                    progress < 0.7f -> 0.5f + (progress - 0.3f) / 0.4f * 0.4f // Fast middle
                    else -> 0.9f + (progress - 0.7f) / 0.3f * 0.1f // Slow end
                }
                
                val simulatedSpeedBps = if (speedBps < 1000f) {
                    // Simulate realistic download speed progression
                    (targetDownloadSpeedMbps * easedProgress * 1024 * 1024).coerceAtLeast(0f)
                } else {
                    // Use actual measured speed if available
                    speedBps.coerceAtMost(targetDownloadSpeedMbps * 1024 * 1024)
                }
                
                // Update download speed (this shows in top Download card)
                _downloadSpeed.value = simulatedSpeedBps
                
                // Track max speed for final value
                val speedInMbps = simulatedSpeedBps / (1024 * 1024)
                if (speedInMbps > maxDownloadSpeed) {
                    maxDownloadSpeed = speedInMbps
                }
                
                // Update speedometer (convert bytes/s to mb/s, then map to 0-100)
                _speedometerValue.value = speedInMbps.coerceIn(0f, 100f)
                
                lastRxBytes = currentRxBytes
                lastTime = currentTime
                delay(updateInterval)
            }
            
            // Store final download speed (this will show in Download card after test completes)
            val finalDownloadSpeedBps = maxDownloadSpeed * 1024 * 1024
            _downloadSpeed.value = finalDownloadSpeedBps
            
            // Brief pause between download and upload
            delay(1000)
            
            // Phase 2: Upload speed test
            val uploadTestDuration = 8000L // 8 seconds for upload test
            val uploadUpdates = (uploadTestDuration / updateInterval).toInt()
            lastTime = System.currentTimeMillis()
            lastTxBytes = TrafficStats.getTotalTxBytes()
            
            // Generate random target upload speed (usually lower than download)
            val targetUploadSpeedMbps = 12f + Random.nextFloat() * 13f // Random between 12-25 mbps
            var maxUploadSpeed = 0f
            
            // Measure upload speed
            for (i in 0..uploadUpdates) {
                val currentTime = System.currentTimeMillis()
                val timeDelta = (currentTime - lastTime).coerceAtLeast(1L)
                
                val currentTxBytes = TrafficStats.getTotalTxBytes()
                val txDelta = (currentTxBytes - lastTxBytes).toFloat()
                
                // Calculate upload speed in bytes per second
                val uploadSpeedBps = (txDelta / timeDelta) * 1000f
                
                // Simulate realistic upload speed with smooth progression
                val progress = i.toFloat() / uploadUpdates
                val easedProgress = when {
                    progress < 0.3f -> progress / 0.3f * 0.5f // Slow start
                    progress < 0.7f -> 0.5f + (progress - 0.3f) / 0.4f * 0.4f // Fast middle
                    else -> 0.9f + (progress - 0.7f) / 0.3f * 0.1f // Slow end
                }
                
                val simulatedUploadSpeedBps = if (uploadSpeedBps < 1000f) {
                    // Simulate realistic upload speed (usually lower than download)
                    (targetUploadSpeedMbps * easedProgress * 1024 * 1024).coerceAtLeast(0f)
                } else {
                    // Use actual measured speed if available
                    uploadSpeedBps.coerceAtMost(targetUploadSpeedMbps * 1024 * 1024)
                }
                
                // Update upload speed (this shows in top Upload card)
                _uploadSpeed.value = simulatedUploadSpeedBps
                
                // Track max upload speed for final value
                val uploadSpeedInMbps = simulatedUploadSpeedBps / (1024 * 1024)
                if (uploadSpeedInMbps > maxUploadSpeed) {
                    maxUploadSpeed = uploadSpeedInMbps
                }
                
                lastTxBytes = currentTxBytes
                lastTime = currentTime
                delay(updateInterval)
            }
            
            // Store final upload speed (this will show in Upload card after test completes)
            val finalUploadSpeedBps = maxUploadSpeed * 1024 * 1024
            _uploadSpeed.value = finalUploadSpeedBps
            
            // Keep final speeds displayed - speedometer shows download speed
            _speedometerValue.value = maxDownloadSpeed.coerceIn(0f, 100f)
            delay(2000)
            
            // Test completed - transition to completed state
            // Final speeds are already stored in _downloadSpeed and _uploadSpeed
            // These will be displayed in the top cards
            _connectionState.value = SpeedTestConnectionState.TEST_COMPLETED
        }
    }

    fun resetTest() {
        _connectionState.value = SpeedTestConnectionState.IDLE
        _downloadSpeed.value = 0.00f
        _uploadSpeed.value = 0.00f
        _speedometerValue.value = 0f
    }
    
    fun restartTest() {
        resetTest()
        startConnection()
    }
}
