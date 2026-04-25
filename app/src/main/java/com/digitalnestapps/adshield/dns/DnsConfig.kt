package com.digitalnestapps.adshield.dns

import android.content.Context

/**
 * Resolver catalog for the server picker + [com.digitalnestapps.adshield.vpn.DnsVpnService].
 *
 * **IPv4 lists** — from each provider’s public documentation (operational resolvers).
 * **NextDNS** — anycast can differ by region; [NEXTDNS_RESOLVER_HOST] is resolved at connect time,
 * with [NEXTDNS_IPV4_FALLBACK] if lookup fails (see NextDNS / steering network).
 *
 * **CleanBrowsing** — Family filter IPs and Private DNS host per
 * [cleanbrowsing.org](https://cleanbrowsing.org/filters/).
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

    /** Hostname used by [DnsVpnService] to resolve IPv4 when no static list exists. */
    const val NEXTDNS_RESOLVER_HOST = "dns.nextdns.io"

    /** Fallback if DNS lookup fails (NextDNS anycast; widely documented). */
    val NEXTDNS_IPV4_FALLBACK = listOf("45.90.28.193", "45.90.30.193")

    /**
     * Returns the Private DNS hostname for the given server name, or null if not supported.
     * Use this when user selects a server and we want to set Android Private DNS.
     */
    fun getPrivateDnsHostname(serverName: String): String? = when (serverName) {
        "AdGuard DNS" -> ADGUARD_DNS_HOSTNAME
        "Open DNS" -> OPENDNS_HOSTNAME
        "Cloudflare DNS" -> CLOUDFLARE_DNS_HOSTNAME
        "Quad9" -> QUAD9_DNS_HOSTNAME
        "Google DNS" -> "dns.google"
        "NextDNS" -> NEXTDNS_RESOLVER_HOST
        "CleanBrowsing" -> "family-filter-dns.cleanbrowsing.org"
        else -> null
    }

    /** AdGuard DNS IPs for VPN (ad blocking). */
    val ADGUARD_DNS_IPS = listOf("94.140.14.14", "94.140.15.15")

    /**
     * Host [DnsVpnService] resolves to IPv4 when [getDnsServerIps] is null (regional anycast).
     */
    fun getDnsResolverHostForVpn(serverName: String): String? = when (serverName) {
        "NextDNS" -> NEXTDNS_RESOLVER_HOST
        else -> null
    }

    fun getDnsIpv4Fallback(serverName: String): List<String>? = when (serverName) {
        "NextDNS" -> NEXTDNS_IPV4_FALLBACK
        else -> null
    }

    /**
     * Use DNS-only VPN tunnel: static IPs, or a hostname we can resolve to IPv4, or custom profile with IPs.
     */
    fun usesDnsVpnTunnel(serverName: String, context: Context): Boolean {
        if (serverName == "Custom DNS") {
            return CustomDnsPrefs.getDnsServerIps(context)?.isNotEmpty() == true
        }
        return getDnsServerIps(serverName) != null || getDnsResolverHostForVpn(serverName) != null
    }

    /** Returns DNS server IPs for VPN when user selects this server, or null if resolved at runtime. */
    fun getDnsServerIps(serverName: String): List<String>? = when (serverName) {
        "AdGuard DNS" -> ADGUARD_DNS_IPS
        "Open DNS" -> listOf("208.67.222.222", "208.67.220.220")
        "Cloudflare DNS" -> listOf("1.1.1.1", "1.0.0.1")
        "Quad9" -> listOf("9.9.9.9", "149.112.112.112")
        "Google DNS" -> listOf("8.8.8.8", "8.8.4.4")
        "Alternate DNS" -> listOf("76.76.2.0", "76.76.10.0")
        "Level3 DNS" -> listOf("4.2.2.1", "4.2.2.2")
        "SafeDNS" -> listOf("195.46.39.39", "195.46.39.40")
        "Yandex DNS" -> listOf("77.88.8.8", "77.88.8.1")
        "Comodo Secure DNS" -> listOf("8.26.56.26", "8.20.247.20")
        "DNS.Watch" -> listOf("84.200.69.80", "84.200.70.40")
        "UncensoredDNS" -> listOf("91.239.100.100", "89.233.43.71")
        // CleanBrowsing Family filter (blocks malware, adult, etc.) — published IPv4.
        "CleanBrowsing" -> listOf("185.228.168.168", "185.228.169.168")
        "NextDNS" -> null
        else -> null
    }
}

