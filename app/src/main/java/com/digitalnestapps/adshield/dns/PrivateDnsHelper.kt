package com.digitalnestapps.adshield.dns

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import com.digitalnestapps.adshield.R

private const val TAG = "PrivateDnsHelper"

/**
 * Tries to set Android Private DNS to the given hostname (e.g. dns.adguard-dns.com).
 * Requires WRITE_SECURE_SETTINGS (grant via: adb shell pm grant <package> android.permission.WRITE_SECURE_SETTINGS).
 * Returns true if set successfully, false otherwise (then show dialog so user can set manually).
 */
fun trySetPrivateDns(context: Context, hostname: String): Boolean {
    return try {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        context.contentResolver?.let { resolver ->
            Settings.Global.putString(resolver, "private_dns_mode", "hostname")
            Settings.Global.putString(resolver, "private_dns_specifier", hostname)
            true
        } ?: false
    } catch (e: Throwable) {
        Log.w(TAG, "Could not set Private DNS (WRITE_SECURE_SETTINGS not granted or other error)", e)
        false
    }
}

/**
 * Resets Private DNS to automatic (opportunistic) so DNS is no longer forced to our hostname.
 * Call this when user disconnects so DNS goes back to normal.
 */
fun tryClearPrivateDns(context: Context): Boolean {
    return try {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        context.contentResolver?.let { resolver ->
            Settings.Global.putString(resolver, "private_dns_mode", "opportunistic")
            Settings.Global.putString(resolver, "private_dns_specifier", "")
            true
        } ?: false
    } catch (e: Throwable) {
        Log.w(TAG, "Could not clear Private DNS (WRITE_SECURE_SETTINGS not granted or other error)", e)
        false
    }
}

fun copyHostnameToClipboard(context: Context, hostname: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
    cm?.setPrimaryClip(
        ClipData.newPlainText(context.getString(R.string.clipboard_label_dns), hostname)
    )
}

/** Opens system Settings so user can set Private DNS manually (path varies by device). */
fun openNetworkOrPrivateDnsSettings(context: Context) {
    val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
        context.startActivity(Intent(Settings.ACTION_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
    }
}
