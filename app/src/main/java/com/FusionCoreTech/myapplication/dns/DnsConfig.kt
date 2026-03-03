package com.FusionCoreTech.myapplication.dns

/**
 * Private DNS hostnames for Android (Settings > Network > Private DNS).
 * Free DNS that block ads / improve privacy.
 * Ref: AdGuard https://adguard-dns.com, OpenDNS doh.opendns.com
 */
object DnsConfig {
    /** AdGuard DNS – blocks ads, trackers. Free. */
    const val ADGUARD_DNS_HOSTNAME = "dns.adguard-dns.com"

    /** Open DNS – security/filtering. */
    const val OPENDNS_HOSTNAME = "doh.opendns.com"

    /** Cloudflare 1.1.1.1 – privacy, no ad blocking. */
    const val CLOUDFLARE_DNS_HOSTNAME = "one.one.one.one"

    /** Quad9 – blocks malware. */
    const val QUAD9_DNS_HOSTNAME = "dns.quad9.net"

    /**
     * Returns the Private DNS hostname for the given server name, or null if not supported.
     * Use this when user selects a server and we want to set Android Private DNS.
     */
    fun getPrivateDnsHostname(serverName: String): String? = when (serverName) {
        "AdGuard DNS" -> ADGUARD_DNS_HOSTNAME
        "Open DNS" -> OPENDNS_HOSTNAME
        "Cloudflare DNS" -> CLOUDFLARE_DNS_HOSTNAME
        "Quad9" -> QUAD9_DNS_HOSTNAME
        else -> null
    }

    /** AdGuard DNS IPs for VPN (ad blocking). */
    val ADGUARD_DNS_IPS = listOf("94.140.14.14", "94.140.15.15")

    /** Returns DNS server IPs for VPN when user selects this server, or null. */
    fun getDnsServerIps(serverName: String): List<String>? = when (serverName) {
        "AdGuard DNS" -> ADGUARD_DNS_IPS
        "Open DNS" -> listOf("208.67.222.222", "208.67.220.220")
        "Cloudflare DNS" -> listOf("1.1.1.1", "1.0.0.1")
        "Quad9" -> listOf("9.9.9.9", "149.112.112.112")
        else -> null
    }
}
