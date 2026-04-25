package com.digitalnestapps.adshield.dns

import android.util.Patterns

object DnsInputValidation {

    fun isValidIpv4(s: String): Boolean {
        val p = s.trim()
        if (p.isEmpty()) return false
        val parts = p.split(".")
        if (parts.size != 4) return false
        for (part in parts) {
            val n = part.toIntOrNull() ?: return false
            if (n !in 0..255) return false
        }
        return true
    }

    /** Empty string is valid (optional field). Non-empty must look like a hostname. */
    fun isValidHostname(s: String): Boolean {
        val t = s.trim()
        if (t.isEmpty()) return true
        return Patterns.DOMAIN_NAME.matcher(t).matches()
    }

    /**
     * At least one of hostname or primary IP is required (non-empty and valid).
     * Secondary IP optional; if present must be valid IPv4.
     */
    fun validateProfile(hostname: String, primaryIp: String, secondaryIp: String): DnsFormErrors {
        val h = hostname.trim()
        val p = primaryIp.trim()
        val s = secondaryIp.trim()

        var hostnameErr = false
        var primaryErr = false
        var secondaryErr = false
        var needOneOf = false

        if (h.isNotEmpty() && !isValidHostname(h)) hostnameErr = true
        if (p.isNotEmpty() && !isValidIpv4(p)) primaryErr = true
        if (s.isNotEmpty() && !isValidIpv4(s)) secondaryErr = true

        val hasValidH = h.isNotEmpty() && !hostnameErr
        val hasValidP = p.isNotEmpty() && !primaryErr
        if (!hasValidH && !hasValidP) needOneOf = true

        val ok = !hostnameErr && !primaryErr && !secondaryErr && !needOneOf
        return DnsFormErrors(ok, hostnameErr, primaryErr, secondaryErr, needOneOf)
    }
}

data class DnsFormErrors(
    val ok: Boolean,
    val hostnameInvalid: Boolean = false,
    val primaryInvalid: Boolean = false,
    val secondaryInvalid: Boolean = false,
    val needHostnameOrPrimary: Boolean = false
)
