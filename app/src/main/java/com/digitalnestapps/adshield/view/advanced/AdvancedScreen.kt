package com.digitalnestapps.adshield.view.advanced

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.digitalnestapps.adshield.R
import com.digitalnestapps.adshield.ui.components.AdShieldScreenInsets
import com.digitalnestapps.adshield.ui.components.AdShieldTopBarRow
import com.digitalnestapps.adshield.dns.CustomDnsPrefs
import com.digitalnestapps.adshield.ui.theme.BackgroundWhite
import com.digitalnestapps.adshield.ui.theme.DarkBackgroundWhite
import com.digitalnestapps.adshield.ui.theme.DarkTextDark
import com.digitalnestapps.adshield.ui.theme.DarkTextLight
import com.digitalnestapps.adshield.ui.theme.OrangePrimary
import com.digitalnestapps.adshield.ui.theme.PremiumOrange
import com.digitalnestapps.adshield.ui.theme.TextDark
import com.digitalnestapps.adshield.ui.theme.TextLight
import com.digitalnestapps.adshield.ui.theme.adShieldScreenBackgroundBrush

private data class AdvancedNetworkInfo(
    val isConnected: Boolean = false,
    val isWifi: Boolean = false,
    val connectionType: String = "",
    val connectionName: String = ""
)

private fun hasFineLocationPermission(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED
}

private fun readNetworkInfo(
    context: Context,
    hasLocationPermission: Boolean
): AdvancedNetworkInfo {
    val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        ?: return AdvancedNetworkInfo()
    val network = cm.activeNetwork ?: return AdvancedNetworkInfo()
    val caps = cm.getNetworkCapabilities(network) ?: return AdvancedNetworkInfo()

    val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    val validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    val connected = hasInternet && validated
    if (!connected) return AdvancedNetworkInfo()

    val transports = mutableListOf<String>()
    if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
        transports += context.getString(R.string.network_transport_wifi)
    }
    if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
        transports += context.getString(R.string.network_transport_mobile_data)
    }
    if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
        transports += context.getString(R.string.network_transport_ethernet)
    }
    if (caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) {
        transports += context.getString(R.string.network_transport_vpn)
    }
    val type = if (transports.isEmpty()) {
        context.getString(R.string.common_connected)
    } else {
        transports.joinToString(" + ")
    }
    val isWifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)

    val connectionName = when {
        isWifi -> readWifiName(context, hasLocationPermission)
        caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> context.getString(R.string.network_carrier)
        caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> context.getString(R.string.network_ethernet_link)
        caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN) -> context.getString(R.string.network_secure_tunnel)
        else -> context.getString(R.string.network_active_network)
    }

    return AdvancedNetworkInfo(
        isConnected = true,
        isWifi = isWifi,
        connectionType = type,
        connectionName = connectionName
    )
}

@Suppress("DEPRECATION")
private fun readWifiSsidLegacy(context: Context): String {
    val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
    return wifi?.connectionInfo?.ssid?.replace("\"", "")?.trim().orEmpty()
}

private fun readWifiSsid(context: Context): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val cm = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val network = cm?.activeNetwork ?: return readWifiSsidLegacy(context)
        val caps = cm.getNetworkCapabilities(network) ?: return readWifiSsidLegacy(context)
        if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return ""
        val transport = caps.transportInfo
        if (transport is WifiInfo) {
            return transport.ssid?.replace("\"", "")?.trim().orEmpty()
        }
    }
    return readWifiSsidLegacy(context)
}

private fun readWifiName(context: Context, hasLocationPermission: Boolean): String {
    if (!hasLocationPermission) {
        return context.getString(R.string.network_wifi_permission_required)
    }
    return try {
        val ssid = readWifiSsid(context)
        if (ssid.isBlank() || ssid.equals("<unknown ssid>", ignoreCase = true)) {
            context.getString(R.string.network_wifi_connected)
        } else {
            ssid
        }
    } catch (_: Exception) {
        context.getString(R.string.network_wifi_connected)
    }
}

@Composable
fun AdvancedScreen(
    isDarkMode: Boolean,
    isConnected: Boolean,
    providerName: String,
    onBackClick: () -> Unit,
    onCustomDnsClick: () -> Unit
) {
    val context = LocalContext.current
    val cardBg = if (isDarkMode) DarkBackgroundWhite else BackgroundWhite
    val textDark = if (isDarkMode) DarkTextDark else TextDark
    val textLight = if (isDarkMode) DarkTextLight else TextLight
    val themeAccent = if (isDarkMode) OrangePrimary else PremiumOrange
    val borderColor = if (isDarkMode) Color.White.copy(alpha = 0.10f) else Color(0xFFE6EAF0)

    var contentVisible by remember { mutableStateOf(false) }
    var hasLocationPermission by remember { mutableStateOf(hasFineLocationPermission(context)) }
    var permissionRequested by rememberSaveable { mutableStateOf(false) }
    var netInfo by remember { mutableStateOf(readNetworkInfo(context, hasLocationPermission)) }
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
        netInfo = readNetworkInfo(context, granted)
    }

    LaunchedEffect(Unit) {
        contentVisible = true
        if (!hasLocationPermission && !permissionRequested) {
            permissionRequested = true
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    DisposableEffect(context, hasLocationPermission) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        if (cm == null) return@DisposableEffect onDispose { }

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                netInfo = readNetworkInfo(context, hasLocationPermission)
            }

            override fun onLost(network: Network) {
                netInfo = readNetworkInfo(context, hasLocationPermission)
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                netInfo = readNetworkInfo(context, hasLocationPermission)
            }
        }
        runCatching { cm.registerDefaultNetworkCallback(callback) }
        netInfo = readNetworkInfo(context, hasLocationPermission)
        onDispose { runCatching { cm.unregisterNetworkCallback(callback) } }
    }

    val activeCustom = CustomDnsPrefs.getActiveProfile(context)
    val customDnsSummary = if (activeCustom != null) {
        buildString {
            append(activeCustom.label)
            activeCustom.hostname?.let { h ->
                if (h.isNotBlank()) {
                    append(" · ")
                    append(h)
                }
            }
            activeCustom.primaryIp?.let { ip ->
                if (ip.isNotBlank()) {
                    if (isNotEmpty()) append(" · ")
                    append(ip)
                }
            }
            activeCustom.secondaryIp?.let { ip2 ->
                if (ip2.isNotBlank()) {
                    append(", ")
                    append(ip2)
                }
            }
        }
    } else {
        stringResource(R.string.advanced_custom_dns_empty)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(adShieldScreenBackgroundBrush(isDarkMode))
            .statusBarsPadding()
            .padding(horizontal = AdShieldScreenInsets.headerHorizontal)
    ) {
        Spacer(modifier = Modifier.height(AdShieldScreenInsets.belowStatusBar))

        AdShieldTopBarRow(
            isDarkMode = isDarkMode,
            onBackClick = onBackClick,
            trailing = {
                StatusPill(
                    text = if (isConnected) stringResource(R.string.common_connected) else stringResource(R.string.common_not_connected),
                    isConnected = isConnected,
                    isDarkMode = isDarkMode
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = AdShieldScreenInsets.backToTitleSpacing)
            ) {
                Text(
                    text = stringResource(R.string.advanced_title),
                    color = textDark,
                    fontSize = 25.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = stringResource(R.string.advanced_subtitle),
                    color = textLight,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp, pressedElevation = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.linearGradient(
                            colors = if (isDarkMode) {
                                listOf(themeAccent.copy(alpha = 0.18f), cardBg)
                            } else {
                                listOf(themeAccent.copy(alpha = 0.10f), Color.White)
                            }
                        )
                    )
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.advanced_connection_name),
                            color = textLight,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isConnected) netInfo.connectionName else stringResource(R.string.common_not_connected),
                            color = textDark,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.advanced_provider),
                            color = textLight,
                            fontSize = 12.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isConnected) providerName else stringResource(R.string.common_not_connected),
                            color = themeAccent,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(themeAccent.copy(alpha = if (isDarkMode) 0.24f else 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_globe),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            colorFilter = ColorFilter.tint(themeAccent)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        AnimatedVisibility(
            visible = contentVisible,
            enter = fadeIn(animationSpec = tween(350)) + slideInVertically(animationSpec = tween(350, easing = FastOutSlowInEasing)) { it / 4 }
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AdvancedInfoCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.advanced_status),
                        value = if (isConnected) {
                            stringResource(R.string.common_connected)
                        } else {
                            stringResource(R.string.common_not_connected)
                        },
                        iconRes = R.drawable.power,
                        isDarkMode = isDarkMode,
                        iconTint = themeAccent
                    )
                    AdvancedInfoCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.advanced_provider),
                        value = if (isConnected) providerName else stringResource(R.string.common_not_connected),
                        iconRes = R.drawable.ic_globe,
                        isDarkMode = isDarkMode,
                        iconTint = themeAccent
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AdvancedInfoCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.advanced_connection_type),
                        value = if (isConnected) netInfo.connectionType else stringResource(R.string.common_not_connected),
                        iconRes = R.drawable.menu,
                        isDarkMode = isDarkMode,
                        iconTint = themeAccent
                    )
                    AdvancedInfoCard(
                        modifier = Modifier.weight(1f),
                        title = stringResource(R.string.advanced_connection_name),
                        value = if (isConnected) netInfo.connectionName else stringResource(R.string.common_not_connected),
                        iconRes = R.drawable.speed,
                        isDarkMode = isDarkMode,
                        iconTint = themeAccent
                    )
                }
            }
        }
        if (netInfo.isConnected && netInfo.isWifi && !hasLocationPermission) {
            Card(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .fillMaxWidth()
                    .clickable { locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp, pressedElevation = 3.dp)
            ) {
                Text(
                    text = stringResource(R.string.network_wifi_permission_cta),
                    color = themeAccent,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = stringResource(R.string.advanced_custom_dns_list),
            color = textDark,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))

        CustomDnsActionCard(
            text = customDnsSummary,
            isDarkMode = isDarkMode,
            themeAccent = themeAccent,
            onClick = onCustomDnsClick
        )
    }
}

@Composable
private fun AdvancedInfoCard(
    modifier: Modifier,
    title: String,
    value: String,
    iconRes: Int,
    isDarkMode: Boolean,
    iconTint: Color
) {
    val cardBg = if (isDarkMode) Color(0xFF141C25) else Color.White
    val textDark = if (isDarkMode) DarkTextDark else TextDark
    val textLight = if (isDarkMode) DarkTextLight else TextLight
    val borderColor = if (isDarkMode) Color.White.copy(alpha = 0.10f) else Color(0xFFE8ECF2)
    val iconBg = iconTint.copy(alpha = if (isDarkMode) 0.22f else 0.12f)
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                .padding(horizontal = 12.dp, vertical = 14.dp)
                .heightIn(min = 96.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        colorFilter = ColorFilter.tint(iconTint)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = textLight
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = textDark
            )
        }
    }
}

@Composable
private fun StatusPill(
    text: String,
    isConnected: Boolean,
    isDarkMode: Boolean
) {
    val pillBg = if (isConnected) {
        Color(0x1A2E7D32)
    } else if (isDarkMode) {
        Color.White.copy(alpha = 0.10f)
    } else {
        Color(0xFFF0F2F6)
    }
    val textColor = if (isConnected) Color(0xFF2E7D32) else if (isDarkMode) DarkTextLight else TextLight
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(pillBg)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun CustomDnsActionCard(
    text: String,
    isDarkMode: Boolean,
    themeAccent: Color,
    onClick: () -> Unit
) {
    val cardBg = if (isDarkMode) Color(0xFF141C25) else Color.White
    val textLight = if (isDarkMode) DarkTextLight else TextLight
    val textDark = if (isDarkMode) DarkTextDark else TextDark
    val borderColor = if (isDarkMode) Color.White.copy(alpha = 0.10f) else Color(0xFFE8ECF2)
    val pulse = rememberInfiniteTransition(label = "dns_plus_pulse")
    val plusScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.16f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "plus_scale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp, pressedElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = if (isDarkMode) {
                            listOf(themeAccent.copy(alpha = 0.10f), cardBg)
                        } else {
                            listOf(themeAccent.copy(alpha = 0.08f), Color.White)
                        }
                    )
                )
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.advanced_add_dns),
                    color = textDark,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = text,
                    color = textLight,
                    fontSize = 13.sp
                )
            }
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(themeAccent.copy(alpha = if (isDarkMode) 0.26f else 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = stringResource(R.string.advanced_add_dns),
                    tint = themeAccent,
                    modifier = Modifier
                        .size(22.dp)
                        .scale(plusScale)
                )
            }
        }
    }
}
