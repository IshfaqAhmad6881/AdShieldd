package com.digitalnestapps.adshield.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.ServiceCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import com.digitalnestapps.adshield.MainActivity
import com.digitalnestapps.adshield.R
import com.digitalnestapps.adshield.dns.SelectedDnsPrefs
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/**
 * VPN that only routes DNS through selected servers (e.g. AdGuard).
 * User sees system "Would you like to add VPN configuration?" → Allow → DNS auto-applied.
 */
class DnsVpnService : VpnService() {

    private var tunnel: ParcelFileDescriptor? = null
    private var running = false
    /** True only after [startForeground] for this run — avoids [stopForeground] if never made foreground. */
    private var foregroundActive = false

    private val notificationManager: NotificationManager
        get() = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

    private fun clearVpnForegroundState() {
        if (foregroundActive) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
            foregroundActive = false
        }
        // Ensure ongoing notification and status shade entry are gone; system key icon clears when tunnel is closed
        notificationManager.cancel(NOTIFICATION_ID)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            running = false
            try { tunnel?.close() } catch (_: Exception) {}
            tunnel = null
            clearVpnForegroundState()
            stopSelf()
            return START_NOT_STICKY
        }

        val serverName = intent?.getStringExtra(EXTRA_SERVER_NAME)
            ?: SelectedDnsPrefs.DEFAULT_SERVER_NAME
        val dnsIps = loadDnsIpsForServer(serverName)
        if (dnsIps.isEmpty()) {
            Log.e(TAG, "No DNS IPs for server: $serverName")
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = createNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        foregroundActive = true
        runVpn(dnsIps)
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        try { tunnel?.close() } catch (_: Exception) {}
        tunnel = null
        clearVpnForegroundState()
        super.onDestroy()
    }

    override fun onRevoke() {
        running = false
        try { tunnel?.close() } catch (_: Exception) {}
        tunnel = null
        clearVpnForegroundState()
        stopSelf()
    }

    private fun createNotification(): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // DEFAULT keeps the VPN status visible (LOW often sits in “silent/minimized” and feels removed).
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.vpn_notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                setShowBadge(false)
                description = getString(R.string.vpn_notification_channel_description)
                setSound(null, null)
                enableVibration(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.vpn_notification_title))
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(open)
            .setOngoing(true)
            .setAutoCancel(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setShowWhen(false)
            .setOnlyAlertOnce(true)
        val subText = getString(R.string.vpn_notification_text)
        if (subText.isNotEmpty()) {
            builder.setContentText(subText)
        }
        // Android 14+: keep FGS notification visible and non-deferred when user switches apps / recents.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            builder.setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
        }
        return builder.build()
    }

    private fun loadDnsIpsForServer(serverName: String): List<String> {
        val staticOrCustom = if (serverName == "Custom DNS") {
            com.digitalnestapps.adshield.dns.CustomDnsPrefs.getDnsServerIps(this)
        } else {
            com.digitalnestapps.adshield.dns.DnsConfig.getDnsServerIps(serverName)
        }
        if (!staticOrCustom.isNullOrEmpty()) return staticOrCustom

        val host = com.digitalnestapps.adshield.dns.DnsConfig.getDnsResolverHostForVpn(serverName)
        if (!host.isNullOrBlank()) {
            val resolved = resolveHostToIpv4(host)
            if (resolved.isNotEmpty()) return resolved
        }

        val fallback = com.digitalnestapps.adshield.dns.DnsConfig.getDnsIpv4Fallback(serverName)
        return fallback.orEmpty()
    }

    private fun resolveHostToIpv4(host: String): List<String> = try {
        InetAddress.getAllByName(host)
            .mapNotNull { it.hostAddress }
            .filter { it.indexOf(':') < 0 }
            .distinct()
            .take(6)
    } catch (e: Exception) {
        Log.w(TAG, "Could not resolve $host", e)
        emptyList()
    }

    private fun runVpn(dnsIps: List<String>) {
        if (dnsIps.isEmpty()) return
        val primaryDns = dnsIps.first()
        val primaryAddr = try { InetAddress.getByName(primaryDns) } catch (_: Exception) { return }

        val builder = Builder()
            .setSession(getString(R.string.vpn_session_name))
            .setMtu(1500)
            .addAddress("10.0.0.2", 32)

        // So that when user connects, only this app is unaffected: its ads keep loading
        try {
            builder.addDisallowedApplication(packageName)
        } catch (_: Exception) { /* ignore if not supported */ }

        dnsIps.forEach {
            builder.addDnsServer(it)
            builder.addRoute(it, 32)
        }

        try {
            tunnel = builder.establish()
        } catch (e: Exception) {
            Log.e(TAG, "VPN establish failed", e)
            clearVpnForegroundState()
            stopSelf()
            return
        } ?: run {
            clearVpnForegroundState()
            stopSelf()
            return
        }

        running = true
        Thread {
            val pfd = tunnel!!
            val input = FileInputStream(pfd.fileDescriptor)
            val output = FileOutputStream(pfd.fileDescriptor)
            val readBuf = ByteArray(32767)
            val socket = DatagramSocket()

            while (running) {
                try {
                    val read = input.read(readBuf)
                    if (read <= 0) break
                    if (read < 20) continue
                    if ((readBuf[0].toInt() shr 4) != 4) continue
                    if ((readBuf[9].toInt() and 0xFF) != 17) continue // UDP
                    val headerLen = (readBuf[0].toInt() and 0x0F) * 4
                    if (read < headerLen + 8) continue
                    val srcPort = ((readBuf[headerLen].toInt() and 0xFF) shl 8) or (readBuf[headerLen + 1].toInt() and 0xFF)
                    val dstPort = ((readBuf[headerLen + 2].toInt() and 0xFF) shl 8) or (readBuf[headerLen + 3].toInt() and 0xFF)
                    if (dstPort != 53) continue
                    val payloadLen = read - headerLen - 8
                    if (payloadLen <= 0) continue
                    val payload = readBuf.copyOfRange(headerLen + 8, read)
                    val response = ByteArray(4096)
                    val recv = DatagramPacket(response, response.size)
                    val send = DatagramPacket(payload, payload.size, primaryAddr, 53)
                    socket.send(send)
                    socket.soTimeout = 5000
                    socket.receive(recv)
                    val replyLen = recv.length
                    if (replyLen <= 0) continue
                    val replyPacket = buildUdpIpPacket(
                        primaryDns, 53,
                        "10.0.0.2", srcPort,
                        response, replyLen
                    )
                    if (replyPacket != null) output.write(replyPacket)
                } catch (e: Exception) {
                    if (running) Log.w(TAG, "Tunnel", e)
                }
            }
            socket.close()
        }.start()
    }

    private fun buildUdpIpPacket(
        srcIp: String, srcPort: Int,
        dstIp: String, dstPort: Int,
        payload: ByteArray, payloadLen: Int
    ): ByteArray? {
        return try {
            val ipLen = 20
            val udpLen = 8 + payloadLen
            val totalLen = ipLen + udpLen
            val out = ByteArray(totalLen)
            var i = 0
            out[i++] = 0x45
            out[i++] = 0
            out[i++] = (totalLen shr 8).toByte()
            out[i++] = totalLen.toByte()
            out[i++] = 0
            out[i++] = 0
            out[i++] = 0x40
            out[i++] = 0
            out[i++] = 0
            out[i++] = 17  // UDP
            out[i++] = 0
            out[i++] = 0
            System.arraycopy(InetAddress.getByName(srcIp).address, 0, out, i, 4)
            i += 4
            System.arraycopy(InetAddress.getByName(dstIp).address, 0, out, i, 4)
            i += 4
            out[i++] = (udpLen shr 8).toByte()
            out[i++] = udpLen.toByte()
            out[i++] = (srcPort shr 8).toByte()
            out[i++] = srcPort.toByte()
            out[i++] = (dstPort shr 8).toByte()
            out[i++] = dstPort.toByte()
            out[i++] = (udpLen shr 8).toByte()
            out[i++] = udpLen.toByte()
            out[i++] = 0
            out[i++] = 0
            System.arraycopy(payload, 0, out, i, payloadLen)
            out
        } catch (_: Exception) { null }
    }

    companion object {
        const val ACTION_STOP = "com.digitalnestapps.adshield.vpn.STOP"
        const val EXTRA_SERVER_NAME = "server_name"
        private const val TAG = "DnsVpnService"
        /** v2: IMPORTANCE_DEFAULT so ongoing VPN status stays clearly visible until disconnect. */
        private const val CHANNEL_ID = "adshield_dns_vpn_v2"
        private const val NOTIFICATION_ID = 9001
    }
}
