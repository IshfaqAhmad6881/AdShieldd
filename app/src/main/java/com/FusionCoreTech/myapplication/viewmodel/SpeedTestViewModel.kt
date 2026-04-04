package com.FusionCoreTech.myapplication.viewmodel

import android.app.Application
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.FusionCoreTech.myapplication.dns.SelectedDnsPrefs
import com.FusionCoreTech.myapplication.model.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress

enum class SpeedTestConnectionState {
    IDLE,
    CONNECTING,
    TEST_RUNNING,
    TEST_COMPLETED
}

data class DnsLatencyResult(
    val name: String,
    val primaryIp: String,
    val secondaryIp: String? = null,
    val latencyMs: Long? = null
)

private const val DNS_QUERY_TIMEOUT_MS = 2200
private const val DNS_ATTEMPTS = 2
private const val DNS_TEST_DOMAIN = "example.com"

/**
 * Speed Test screen: real DNS resolver benchmark only (UDP port 53), no HTTP throughput test.
 * Server list uses well-known public DNS IPs (same class of resolvers as typical DNS benchmarks).
 */
class SpeedTestViewModel(application: Application) : AndroidViewModel(application) {

    private val _connectionState = mutableStateOf(SpeedTestConnectionState.IDLE)
    val connectionState: State<SpeedTestConnectionState> = _connectionState

    private val _selectedLocation = mutableStateOf(
        Location(name = SelectedDnsPrefs.getSelectedName(getApplication()))
    )
    val selectedLocation: State<Location> = _selectedLocation

    private val _dnsResults = mutableStateOf<List<DnsLatencyResult>>(emptyList())
    val dnsResults: State<List<DnsLatencyResult>> = _dnsResults

    fun selectLocation(location: Location) {
        _selectedLocation.value = location
        SelectedDnsPrefs.saveSelectedName(getApplication(), location.name)
    }

    /** Keep speed-test “active DNS” in sync after home/changed server picker (prefs updated elsewhere). */
    fun reloadPersistedSelection() {
        _selectedLocation.value = Location(name = SelectedDnsPrefs.getSelectedName(getApplication()))
    }

    fun startConnection() {
        _connectionState.value = SpeedTestConnectionState.CONNECTING
        _dnsResults.value = emptyList()

        viewModelScope.launch {
            try {
                delay(600)
                if (_connectionState.value != SpeedTestConnectionState.CONNECTING) return@launch
                _connectionState.value = SpeedTestConnectionState.TEST_RUNNING
                runDnsBenchmarkOnly()
            } catch (_: Throwable) { }
            withContext(Dispatchers.Main) {
                _connectionState.value = SpeedTestConnectionState.TEST_COMPLETED
            }
        }
    }

    private suspend fun runDnsBenchmarkOnly() {
        try {
            runDnsLatencyTest()
        } catch (_: Exception) { }
        delay(400)
    }

    private suspend fun runDnsLatencyTest() = withContext(Dispatchers.IO) {
        val targets = listOf(
            DnsLatencyResult("Cloudflare DNS", "1.1.1.1", "1.0.0.1"),
            DnsLatencyResult("Google DNS", "8.8.8.8", "8.8.4.4"),
            DnsLatencyResult("Alternate DNS", "76.76.2.0", "76.76.10.0"),
            DnsLatencyResult("Quad9", "9.9.9.9", "149.112.112.112"),
            DnsLatencyResult("Level3 DNS", "4.2.2.1", "4.2.2.2"),
            DnsLatencyResult("SafeDNS", "195.46.39.39", "195.46.39.40"),
            DnsLatencyResult("Open DNS", "208.67.222.222", "208.67.220.220"),
            DnsLatencyResult("Yandex DNS", "77.88.8.8", "77.88.8.1"),
            DnsLatencyResult("Comodo Secure DNS", "8.26.56.26", "8.20.247.20"),
            DnsLatencyResult("DNS.Watch", "84.200.69.80", "84.200.70.40"),
            DnsLatencyResult("UncensoredDNS", "91.239.100.100", "89.233.43.71")
        )
        val results = mutableListOf<DnsLatencyResult>()
        for (target in targets) {
            try {
                val latency = measureDnsLatency(target.primaryIp, target.secondaryIp)
                results.add(target.copy(latencyMs = latency))
            } catch (_: Exception) {
                results.add(target.copy(latencyMs = null))
            }
            withContext(Dispatchers.Main) {
                _dnsResults.value = results.sortedBy { it.latencyMs ?: Long.MAX_VALUE }
            }
        }
        delay(200)
    }

    private fun measureDnsLatency(primaryIp: String, secondaryIp: String?): Long? {
        val latencies = listOfNotNull(primaryIp, secondaryIp)
            .mapNotNull { measureSingleDnsServerLatency(it) }
        if (latencies.isEmpty()) return null
        return latencies.minOrNull()
    }

    private fun measureSingleDnsServerLatency(ip: String): Long? {
        val socket = DatagramSocket()
        return try {
            socket.soTimeout = DNS_QUERY_TIMEOUT_MS
            val samples = mutableListOf<Long>()
            repeat(DNS_ATTEMPTS) { attempt ->
                val txId = ((System.nanoTime() + attempt) and 0xFFFF).toInt()
                val query = buildDnsQuery(txId, DNS_TEST_DOMAIN)
                val request = DatagramPacket(query, query.size).apply {
                    socketAddress = InetSocketAddress(ip, 53)
                }
                val responseBytes = ByteArray(512)
                val response = DatagramPacket(responseBytes, responseBytes.size)
                val start = System.nanoTime()
                socket.send(request)
                socket.receive(response)
                val elapsedMs = ((System.nanoTime() - start) / 1_000_000).coerceAtLeast(0L)
                if (isValidDnsResponse(responseBytes, response.length, txId)) {
                    samples.add(elapsedMs)
                }
            }
            if (samples.isEmpty()) null else (samples.sum() / samples.size)
        } catch (_: Exception) {
            null
        } finally {
            runCatching { socket.close() }
        }
    }

    private fun buildDnsQuery(transactionId: Int, domain: String): ByteArray {
        val labels = domain.split('.').filter { it.isNotBlank() }
        val qnameSize = labels.sumOf { it.length + 1 } + 1
        val packet = ByteArray(12 + qnameSize + 4)
        packet[0] = ((transactionId shr 8) and 0xFF).toByte()
        packet[1] = (transactionId and 0xFF).toByte()
        packet[2] = 0x01
        packet[3] = 0x00
        packet[4] = 0x00
        packet[5] = 0x01
        packet[6] = 0x00
        packet[7] = 0x00
        packet[8] = 0x00
        packet[9] = 0x00
        packet[10] = 0x00
        packet[11] = 0x00
        var offset = 12
        for (label in labels) {
            val bytes = label.toByteArray(Charsets.US_ASCII)
            packet[offset++] = bytes.size.toByte()
            for (b in bytes) packet[offset++] = b
        }
        packet[offset++] = 0x00
        packet[offset++] = 0x00
        packet[offset++] = 0x01
        packet[offset++] = 0x00
        packet[offset] = 0x01
        return packet
    }

    private fun isValidDnsResponse(data: ByteArray, length: Int, txId: Int): Boolean {
        if (length < 12) return false
        val respId = ((data[0].toInt() and 0xFF) shl 8) or (data[1].toInt() and 0xFF)
        val flags = ((data[2].toInt() and 0xFF) shl 8) or (data[3].toInt() and 0xFF)
        val isResponse = (flags and 0x8000) != 0
        val responseCode = flags and 0x000F
        return respId == txId && isResponse && responseCode == 0
    }

    fun resetTest() {
        _connectionState.value = SpeedTestConnectionState.IDLE
        _dnsResults.value = emptyList()
    }

    fun restartTest() {
        resetTest()
        startConnection()
    }
}
