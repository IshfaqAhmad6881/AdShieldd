package com.digitalnestapps.adshield.dns

import android.content.Context

private const val PREFS_NAME = "adshield_prefs"
private const val KEY_SELECTED_DNS_NAME = "selected_dns_server_name"

object SelectedDnsPrefs {
    const val DEFAULT_SERVER_NAME = "AdGuard DNS"

    fun getSelectedName(context: Context): String {
        val name = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_SELECTED_DNS_NAME, DEFAULT_SERVER_NAME)
            .orEmpty()
            .trim()
        return name.ifBlank { DEFAULT_SERVER_NAME }
    }

    fun saveSelectedName(context: Context, serverName: String) {
        val clean = serverName.trim().ifBlank { DEFAULT_SERVER_NAME }
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
            .putString(KEY_SELECTED_DNS_NAME, clean)
            .apply()
    }
}
