package com.FusionCoreTech.myapplication.dns

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private const val PREFS_NAME = "adshield_custom_dns"
private const val KEY_PROFILES = "custom_dns_profiles_json"
private const val KEY_ACTIVE_ID = "active_custom_dns_id"
// Legacy keys (migrated once)
private const val LEGACY_HOSTNAME = "custom_dns_hostname"
private const val LEGACY_PRIMARY = "custom_dns_primary_ip"
private const val LEGACY_SECONDARY = "custom_dns_secondary_ip"

data class CustomDnsProfile(
    val id: String,
    val label: String,
    val hostname: String?,
    val primaryIp: String?,
    val secondaryIp: String?
)

object CustomDnsPrefs {

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Call before reading profiles; migrates old single-entry prefs to list. */
    private fun ensureMigrated(context: Context) {
        val p = prefs(context)
        if (p.contains(KEY_PROFILES)) return

        val legacyH = p.getString(LEGACY_HOSTNAME, null)?.trim()?.takeIf { it.isNotEmpty() }
        val legacyP = p.getString(LEGACY_PRIMARY, null)?.trim()?.takeIf { it.isNotEmpty() }
        val legacyS = p.getString(LEGACY_SECONDARY, null)?.trim()?.takeIf { it.isNotEmpty() }

        val list = if (legacyH == null && legacyP == null && legacyS == null) {
            emptyList()
        } else {
            val id = UUID.randomUUID().toString()
            val label = when {
                !legacyH.isNullOrEmpty() -> legacyH
                !legacyP.isNullOrEmpty() -> legacyP
                else -> "Custom DNS"
            }
            listOf(CustomDnsProfile(id, label, legacyH, legacyP, legacyS))
        }

        persistProfiles(context, list)
        val ed = p.edit()
        if (list.isNotEmpty()) ed.putString(KEY_ACTIVE_ID, list.first().id)
        ed.remove(LEGACY_HOSTNAME).remove(LEGACY_PRIMARY).remove(LEGACY_SECONDARY).apply()
    }

    fun getProfiles(context: Context): List<CustomDnsProfile> {
        ensureMigrated(context)
        val json = prefs(context).getString(KEY_PROFILES, null) ?: return emptyList()
        return parseProfiles(json)
    }

    fun getActiveProfileId(context: Context): String? {
        ensureMigrated(context)
        return prefs(context).getString(KEY_ACTIVE_ID, null)
    }

    fun getActiveProfile(context: Context): CustomDnsProfile? {
        val profiles = getProfiles(context)
        if (profiles.isEmpty()) return null
        val id = getActiveProfileId(context)
        return profiles.find { it.id == id } ?: profiles.first()
    }

    fun setActiveProfile(context: Context, profileId: String) {
        ensureMigrated(context)
        if (getProfiles(context).any { it.id == profileId }) {
            prefs(context).edit().putString(KEY_ACTIVE_ID, profileId).apply()
        }
    }

    fun addProfile(context: Context, profile: CustomDnsProfile) {
        val list = getProfiles(context).toMutableList()
        list.add(profile)
        persistProfiles(context, list)
        if (list.size == 1) {
            prefs(context).edit().putString(KEY_ACTIVE_ID, profile.id).apply()
        }
    }

    fun deleteProfile(context: Context, profileId: String) {
        val list = getProfiles(context).toMutableList()
        list.removeAll { it.id == profileId }
        persistProfiles(context, list)
        val p = prefs(context)
        val active = p.getString(KEY_ACTIVE_ID, null)
        val ed = p.edit()
        when {
            list.isEmpty() -> ed.remove(KEY_ACTIVE_ID)
            active == profileId -> ed.putString(KEY_ACTIVE_ID, list.first().id)
        }
        ed.apply()
    }

    private fun persistProfiles(context: Context, profiles: List<CustomDnsProfile>) {
        val arr = JSONArray()
        for (pr in profiles) {
            val o = JSONObject()
            o.put("id", pr.id)
            o.put("label", pr.label)
            o.put("hostname", pr.hostname.orEmpty())
            o.put("primaryIp", pr.primaryIp.orEmpty())
            o.put("secondaryIp", pr.secondaryIp.orEmpty())
            arr.put(o)
        }
        prefs(context).edit().putString(KEY_PROFILES, arr.toString()).apply()
    }

    private fun parseProfiles(json: String): List<CustomDnsProfile> {
        return try {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        CustomDnsProfile(
                            id = o.getString("id"),
                            label = o.optString("label", "").ifBlank { "Custom DNS" },
                            hostname = o.optString("hostname").trim().takeIf { it.isNotEmpty() },
                            primaryIp = o.optString("primaryIp").trim().takeIf { it.isNotEmpty() },
                            secondaryIp = o.optString("secondaryIp").trim().takeIf { it.isNotEmpty() }
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun getHostname(context: Context): String? =
        getActiveProfile(context)?.hostname?.trim()?.takeIf { it.isNotEmpty() }

    fun getPrimaryIp(context: Context): String? =
        getActiveProfile(context)?.primaryIp?.trim()?.takeIf { it.isNotEmpty() }

    fun getSecondaryIp(context: Context): String? =
        getActiveProfile(context)?.secondaryIp?.trim()?.takeIf { it.isNotEmpty() }

    fun getDnsServerIps(context: Context): List<String>? {
        val active = getActiveProfile(context) ?: return null
        val primary = active.primaryIp?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (!DnsInputValidation.isValidIpv4(primary)) return null
        val secondary = active.secondaryIp?.trim()?.takeIf { it.isNotEmpty() }
        return if (secondary != null && DnsInputValidation.isValidIpv4(secondary)) {
            listOf(primary, secondary)
        } else {
            listOf(primary)
        }
    }

    fun hasCustomDns(context: Context): Boolean = getProfiles(context).isNotEmpty()
}
