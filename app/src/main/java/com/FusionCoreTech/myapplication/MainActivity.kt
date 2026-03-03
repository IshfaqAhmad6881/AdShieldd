package com.FusionCoreTech.myapplication

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.FusionCoreTech.myapplication.dns.DnsConfig
import com.FusionCoreTech.myapplication.dns.copyHostnameToClipboard
import com.FusionCoreTech.myapplication.dns.tryClearPrivateDns
import com.FusionCoreTech.myapplication.dns.trySetPrivateDns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.FusionCoreTech.myapplication.ui.theme.AdShieldTheme
import com.FusionCoreTech.myapplication.view.home.HomeScreen
import com.FusionCoreTech.myapplication.view.preferences.PreferencesScreen
import com.FusionCoreTech.myapplication.view.settings.SettingsScreen
import com.FusionCoreTech.myapplication.view.speedtest.SpeedTestScreen
import com.FusionCoreTech.myapplication.view.server.ChooseServerSheet
import com.FusionCoreTech.myapplication.view.splash.SplashScreen
import com.FusionCoreTech.myapplication.view.terms.TermsAndPrivacyScreen
import com.FusionCoreTech.myapplication.view.terms.TermsIntroScreen
import com.FusionCoreTech.myapplication.viewmodel.HomeViewModel
import com.FusionCoreTech.myapplication.viewmodel.PreferencesViewModel
import com.FusionCoreTech.myapplication.viewmodel.ThemeMode
import com.FusionCoreTech.myapplication.viewmodel.SettingsViewModel
import com.FusionCoreTech.myapplication.ads.RewardedAdHelper
import com.FusionCoreTech.myapplication.viewmodel.SpeedTestConnectionState
import com.FusionCoreTech.myapplication.viewmodel.SpeedTestViewModel
import com.FusionCoreTech.myapplication.vpn.DnsVpnService

private fun parseTimerToSeconds(timer: String): Int {
    // Parse "HH:MM:SS" format to seconds
    val parts = timer.split(":")
    if (parts.size == 3) {
        val hours = parts[0].toIntOrNull() ?: 0
        val minutes = parts[1].toIntOrNull() ?: 0
        val seconds = parts[2].toIntOrNull() ?: 0
        return hours * 3600 + minutes * 60 + seconds
    }
    return 0
}

private fun shareReferralLink(activity: ComponentActivity) {
    val referralLink = "https://adshield.app/refer?code=YOUR_REFERRAL_CODE"
    val shareMessage = "Check out AdShield - Secure your online privacy!\n\n$referralLink"
    
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, shareMessage)
        type = "text/plain"
    }
    
    val chooser = Intent.createChooser(shareIntent, "Share AdShield with friends")
    activity.startActivity(chooser)
}

class MainActivity : ComponentActivity() {
    
    private fun getAppVersion(): String {
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            packageInfo.versionName ?: "1.0"
        } catch (e: PackageManager.NameNotFoundException) {
            "1.0"
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Set theme before super.onCreate to prevent black screen
        // Theme is already set in manifest, but ensure it's applied early
        super.onCreate(savedInstanceState)
        
        val appVersion = getAppVersion()
        
        // Show splash screen as default launch screen
        setContent {
            var showSplash by remember { mutableStateOf(true) }
            var showSettings by remember { mutableStateOf(false) }
            var showPreferences by remember { mutableStateOf(false) }
            var showSpeedTest by remember { mutableStateOf(false) }
            var showServerPicker by remember { mutableStateOf(false) }
            var serverPickerFromSpeedTest by remember { mutableStateOf(true) }
            var showDnsPrompt by remember { mutableStateOf<Pair<String, String>?>(null) }
            var showTermsIntro by remember { mutableStateOf(false) }
            var showTermsFull by remember { mutableStateOf(false) }
            var pendingVpnServerName by remember { mutableStateOf<String?>(null) }
            val notificationPermissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestPermission()
            ) { _ -> /* Android default dialog result – no custom follow-up */ }
            val homeViewModel: HomeViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                            @Suppress("UNCHECKED_CAST")
                            return HomeViewModel(this@MainActivity.application) as T
                        }
                        throw IllegalArgumentException("Unknown ViewModel")
                    }
                }
            )
            val vpnPrepareLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.StartActivityForResult()
            ) { result ->
                if (result.resultCode == Activity.RESULT_OK && pendingVpnServerName != null) {
                    startService(
                        Intent(this@MainActivity, DnsVpnService::class.java)
                            .putExtra(DnsVpnService.EXTRA_SERVER_NAME, pendingVpnServerName)
                    )
                    homeViewModel.toggleConnection()
                    pendingVpnServerName = null
                }
            }
            val rewardedAdHelper = remember { RewardedAdHelper(this@MainActivity) { homeViewModel.addTimeFromReward() } }
            val settingsViewModel: SettingsViewModel = viewModel()
            val preferencesViewModel: PreferencesViewModel = viewModel()
            val speedTestViewModel: SpeedTestViewModel = viewModel()
            
            // Theme: System / Light / Dark from preferences
            val systemDark = isSystemInDarkTheme()
            val themeMode by preferencesViewModel.themeMode.collectAsState(initial = ThemeMode.System)
            val isDarkMode = when (themeMode) {
                ThemeMode.System -> systemDark
                ThemeMode.Light -> false
                ThemeMode.Dark -> true
            }

            AdShieldTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    
                    // Splash screen shows immediately as default launch screen
                    when {
                        showSplash -> {
                            SplashScreen(
                                onNavigateToHome = {
                                    showSplash = false
                                    val prefs = getSharedPreferences("adshield_prefs", MODE_PRIVATE)
                                    val termsAccepted = prefs.getBoolean("terms_accepted", false)
                                    if (!termsAccepted) {
                                        showTermsIntro = true
                                    } else {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            val asked = prefs.getBoolean("notification_permission_asked", false)
                                            val hasPermission = ContextCompat.checkSelfPermission(
                                                this@MainActivity, Manifest.permission.POST_NOTIFICATIONS
                                            ) == PackageManager.PERMISSION_GRANTED
                                            if (!asked && !hasPermission) {
                                                prefs.edit().putBoolean("notification_permission_asked", true).apply()
                                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            }
                                        }
                                    }
                                }
                            )
                        }
                        showTermsIntro -> {
                            TermsIntroScreen(
                                isDarkMode = isDarkMode,
                                onAccept = { showTermsIntro = false; showTermsFull = true }
                            )
                        }
                        showTermsFull -> {
                            TermsAndPrivacyScreen(
                                isDarkMode = isDarkMode,
                                onAcceptToAll = {
                                    getSharedPreferences("adshield_prefs", MODE_PRIVATE).edit()
                                        .putBoolean("terms_accepted", true).apply()
                                    showTermsFull = false
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        val prefs = getSharedPreferences("adshield_prefs", MODE_PRIVATE)
                                        val asked = prefs.getBoolean("notification_permission_asked", false)
                                        val hasPermission = ContextCompat.checkSelfPermission(
                                            this@MainActivity, Manifest.permission.POST_NOTIFICATIONS
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (!asked && !hasPermission) {
                                            prefs.edit().putBoolean("notification_permission_asked", true).apply()
                                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                        }
                                    }
                                }
                            )
                        }
                        showSpeedTest -> {
                            val downloadSpeed by speedTestViewModel.downloadSpeed
                            val uploadSpeed by speedTestViewModel.uploadSpeed
                            val speedometerValue by speedTestViewModel.speedometerValue
                            val connectionState by speedTestViewModel.connectionState
                            SpeedTestScreen(
                                downloadSpeed = downloadSpeed,
                                uploadSpeed = uploadSpeed,
                                speedometerValue = speedometerValue,
                                connectionState = connectionState,
                                isDarkMode = isDarkMode,
                                onBackClick = { 
                                    speedTestViewModel.resetTest()
                                    showSpeedTest = false 
                                },
                                onHistoryClick = { /* TODO: Show history */ },
                                onStartTestClick = { 
                                    if (connectionState == SpeedTestConnectionState.TEST_COMPLETED) {
                                        speedTestViewModel.restartTest()
                                    } else {
                                        speedTestViewModel.startConnection()
                                    }
                                }
                            )
                        }
                        showPreferences -> {
                            PreferencesScreen(
                                isDarkMode = isDarkMode,
                                themeMode = themeMode,
                                onThemeModeSelected = { preferencesViewModel.setThemeMode(it) },
                                onBackClick = { showPreferences = false }
                            )
                        }
                        showSettings -> {
                            val connectionState by homeViewModel.connectionState
                            // Update settings timer from home connection state in real-time
                            LaunchedEffect(connectionState.timer) {
                                settingsViewModel.updateTimer(
                                    timer = connectionState.timer,
                                    remainingSeconds = parseTimerToSeconds(connectionState.timer)
                                )
                            }
                            val settingsState = settingsViewModel.settingsState.value
                            SettingsScreen(
                                settingsState = settingsState.copy(
                                    timer = connectionState.timer,
                                    remainingSeconds = parseTimerToSeconds(connectionState.timer),
                                    appVersion = appVersion
                                ),
                                progressPercentage = settingsViewModel.getProgressPercentage().let {
                                    val totalSeconds = 1800f
                                    val remaining = parseTimerToSeconds(connectionState.timer).toFloat()
                                    (remaining / totalSeconds).coerceIn(0f, 1f)
                                },
                                isDarkMode = isDarkMode,
                                onBackClick = { showSettings = false },
                                onPreferencesClick = { showPreferences = true },
                                onSpeedTestClick = { showSpeedTest = true },
                                onReferFriendsClick = { shareReferralLink(this@MainActivity) }
                            )
                        }
                        else -> {
                            val connectionState by homeViewModel.connectionState
                            val selectedLocation by homeViewModel.selectedLocation
                            val remainingSeconds by homeViewModel.remainingSeconds
                            // When connection becomes disconnected (e.g. timer expired), stop VPN and clear Private DNS
                            var wasConnected by remember { mutableStateOf(connectionState.isConnected) }
                            LaunchedEffect(connectionState.isConnected) {
                                if (wasConnected && !connectionState.isConnected) {
                                    startService(
                                        Intent(this@MainActivity, DnsVpnService::class.java).apply {
                                            action = DnsVpnService.ACTION_STOP
                                        }
                                    )
                                    try {
                                        tryClearPrivateDns(this@MainActivity)
                                    } catch (_: Throwable) { /* WRITE_SECURE_SETTINGS not granted - ignore */ }
                                }
                                wasConnected = connectionState.isConnected
                            }
                            HomeScreen(
                                connectionState = connectionState,
                                selectedLocation = selectedLocation,
                                remainingSeconds = remainingSeconds,
                                isDarkMode = isDarkMode,
                                onStartClick = {
                                    try {
                                        val wasDisconnected = !connectionState.isConnected
                                        val wasConnected = connectionState.isConnected
                                        val serverName = selectedLocation.name
                                        val dnsIps = DnsConfig.getDnsServerIps(serverName)
                                        val hostname = DnsConfig.getPrivateDnsHostname(serverName)

                                        if (wasConnected) {
                                            startService(
                                                Intent(this@MainActivity, DnsVpnService::class.java).apply {
                                                    action = DnsVpnService.ACTION_STOP
                                                }
                                            )
                                            tryClearPrivateDns(this@MainActivity)
                                            homeViewModel.toggleConnection()
                                            return@HomeScreen
                                        }
                                        if (wasDisconnected && dnsIps != null) {
                                            val prepareIntent = VpnService.prepare(this@MainActivity)
                                            if (prepareIntent != null) {
                                                pendingVpnServerName = serverName
                                                vpnPrepareLauncher.launch(prepareIntent)
                                            } else {
                                                startService(
                                                    Intent(this@MainActivity, DnsVpnService::class.java)
                                                        .putExtra(DnsVpnService.EXTRA_SERVER_NAME, serverName)
                                                )
                                                homeViewModel.toggleConnection()
                                            }
                                        } else if (wasDisconnected && hostname != null) {
                                            homeViewModel.toggleConnection()
                                            if (!trySetPrivateDns(this@MainActivity, hostname)) {
                                                showDnsPrompt = hostname to packageName
                                            }
                                        } else {
                                            homeViewModel.toggleConnection()
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("MainActivity", "Connect button error", e)
                                    }
                                },
                                onRewardClick = { rewardedAdHelper.show() },
                                onLocationClick = {
                                    serverPickerFromSpeedTest = false
                                    showServerPicker = true
                                },
                                onMenuClick = { showSettings = true }
                            )
                        }
                    }
                    if (showServerPicker) {
                        val speedSelected by speedTestViewModel.selectedLocation
                        val homeSelected by homeViewModel.selectedLocation
                        ChooseServerSheet(
                            selectedLocation = if (serverPickerFromSpeedTest) speedSelected else homeSelected,
                            isDarkMode = isDarkMode,
                            onSelect = {
                                if (serverPickerFromSpeedTest) speedTestViewModel.selectLocation(it)
                                else homeViewModel.selectLocation(it)
                                showServerPicker = false
                            },
                            onDismiss = { showServerPicker = false }
                        )
                    }
                    showDnsPrompt?.let { (hostname, packageName) ->
                        val ctx = LocalContext.current
                        val adbCommand = "adb shell pm grant $packageName android.permission.WRITE_SECURE_SETTINGS"
                        AlertDialog(
                            onDismissRequest = { showDnsPrompt = null },
                            title = { Text("Allow app to set DNS automatically") },
                            text = {
                                Text(
                                    "To set DNS from the app (no manual step), allow permission once:\n\n" +
                                    "1. Connect phone to PC with USB\n" +
                                    "2. Enable USB debugging (Settings → Developer options)\n" +
                                    "3. On PC, run this command:\n\n$adbCommand\n\n" +
                                    "After this, app will set DNS automatically when you Connect.",
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    copyHostnameToClipboard(ctx, adbCommand)
                                    showDnsPrompt = null
                                }) { Text("Copy command") }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDnsPrompt = null }) { Text("OK") }
                            }
                        )
                    }
                }
            }
        }
    }
}
