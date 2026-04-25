package com.digitalnestapps.adshield

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.VpnService
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.digitalnestapps.adshield.dns.DnsConfig
import com.digitalnestapps.adshield.dns.copyHostnameToClipboard
import com.digitalnestapps.adshield.dns.tryClearPrivateDns
import com.digitalnestapps.adshield.dns.trySetPrivateDns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.digitalnestapps.adshield.ui.theme.AdShieldTheme
import com.digitalnestapps.adshield.ui.theme.adShieldScreenBackgroundBrush
import com.digitalnestapps.adshield.view.home.HomeScreen
import com.digitalnestapps.adshield.view.premium.PremiumScreen
import com.digitalnestapps.adshield.view.preferences.PreferencesScreen
import com.digitalnestapps.adshield.view.advanced.AdvancedScreen
import com.digitalnestapps.adshield.view.settings.SettingsScreen
import com.digitalnestapps.adshield.view.settings.LanguageScreen
import com.digitalnestapps.adshield.view.settings.FeedbackScreen
import com.digitalnestapps.adshield.billing.BillingManager
import com.digitalnestapps.adshield.billing.isPremiumUnlocked
import com.digitalnestapps.adshield.view.speedtest.SpeedTestScreen
import com.digitalnestapps.adshield.view.server.ChooseServerSheet
import com.digitalnestapps.adshield.view.server.CustomDnsScreen
import com.digitalnestapps.adshield.dns.CustomDnsPrefs
import com.digitalnestapps.adshield.dns.SelectedDnsPrefs
import com.digitalnestapps.adshield.localization.AppLanguageManager
import com.digitalnestapps.adshield.model.Location
import com.digitalnestapps.adshield.view.splash.SplashScreen
import com.digitalnestapps.adshield.view.terms.TermsAndPrivacyScreen
import com.digitalnestapps.adshield.view.terms.TermsIntroScreen
import com.digitalnestapps.adshield.view.vpn.VpnProminentDisclosureScreen
import com.digitalnestapps.adshield.viewmodel.HomeViewModel
import com.digitalnestapps.adshield.viewmodel.PreferencesViewModel
import com.digitalnestapps.adshield.viewmodel.ThemeMode
import com.digitalnestapps.adshield.viewmodel.SettingsViewModel
import com.digitalnestapps.adshield.ads.InterstitialAdHelper
import com.digitalnestapps.adshield.ads.RewardedAdHelper
import com.digitalnestapps.adshield.viewmodel.DnsLatencyResult
import com.digitalnestapps.adshield.viewmodel.SpeedTestConnectionState
import com.digitalnestapps.adshield.viewmodel.SpeedTestViewModel
import com.digitalnestapps.adshield.vpn.DnsVpnService
import com.digitalnestapps.adshield.network.InternetStatusSnackbar
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.delay

private const val APP_PREFS_NAME = "adshield_prefs"
private const val APP_LANGUAGE_TAG_KEY = "app_language_tag"
/** Play VpnService policy: separate in-app consent, persisted after first accept. */
private const val KEY_VPN_PROMINENT_DISCLOSURE_ACCEPTED = "vpn_prominent_disclosure_accepted"

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

private fun shareReferralLink(activity: Activity) {
    val referralLink = "https://adshield.app/refer?code=YOUR_REFERRAL_CODE"
    val shareMessage = activity.getString(
        R.string.share_referral_message,
        activity.getString(R.string.app_name),
        referralLink
    )
    
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, shareMessage)
        type = "text/plain"
    }
    
    val chooser = Intent.createChooser(
        shareIntent,
        activity.getString(R.string.share_referral_chooser_title)
    )
    activity.startActivity(chooser)
}

class MainActivity : AppCompatActivity() {
    
    private fun getAppVersion(): String {
        return try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            packageInfo.versionName ?: "1.0"
        } catch (e: PackageManager.NameNotFoundException) {
            "1.0"
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        val launchPrefs = getSharedPreferences(APP_PREFS_NAME, MODE_PRIVATE)
        AppLanguageManager.applyLanguage(
            launchPrefs.getString(APP_LANGUAGE_TAG_KEY, AppLanguageManager.SYSTEM_LANGUAGE_TAG)
                ?: AppLanguageManager.SYSTEM_LANGUAGE_TAG
        )
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this) {}
        val appVersion = getAppVersion()
        // Show splash screen as default launch screen
        setContent {
            var showSplash by rememberSaveable { mutableStateOf(true) }
            var showSettings by rememberSaveable { mutableStateOf(false) }
            var showAdvanced by rememberSaveable { mutableStateOf(false) }
            var showLanguage by rememberSaveable { mutableStateOf(false) }
            var showPreferences by rememberSaveable { mutableStateOf(false) }
            var showSpeedTest by rememberSaveable { mutableStateOf(false) }
            var showPremium by rememberSaveable { mutableStateOf(false) }
            var showServerPicker by rememberSaveable { mutableStateOf(false) }
            var showFeedback by rememberSaveable { mutableStateOf(false) }
            var showCustomDns by rememberSaveable { mutableStateOf(false) }
            var customDnsFromAdvanced by rememberSaveable { mutableStateOf(false) }
            var serverPickerFromSpeedTest by rememberSaveable { mutableStateOf(true) }
            var showDnsPrompt by remember { mutableStateOf<Pair<String, String>?>(null) }
            var showTermsIntro by rememberSaveable { mutableStateOf(false) }
            var showTermsFull by rememberSaveable { mutableStateOf(false) }
            var showNotificationPrimer by rememberSaveable { mutableStateOf(false) }
            var showVpnProminentDisclosure by rememberSaveable { mutableStateOf(false) }
            /** Full-screen “Preparing ad…” during rewarded load/show (Start connect or +30 min button). */
            var showRewardPreparingGate by remember { mutableStateOf(false) }
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
                    val server = pendingVpnServerName!!
                    pendingVpnServerName = null
                    startService(
                        Intent(this@MainActivity, DnsVpnService::class.java)
                            .putExtra(DnsVpnService.EXTRA_SERVER_NAME, server)
                    )
                    // Only start session timer if not already connected (e.g. changing server while on stays on).
                    if (!homeViewModel.connectionState.value.isConnected) {
                        homeViewModel.toggleConnection()
                    }
                }
            }
            val rewardedAdHelper = remember { RewardedAdHelper(this@MainActivity) { homeViewModel.addTimeFromReward() } }
            val interstitialAdHelper = remember { InterstitialAdHelper(this@MainActivity) }
            val billingManager = remember { BillingManager(this@MainActivity) }
            val settingsViewModel: SettingsViewModel = viewModel()
            val preferencesViewModel: PreferencesViewModel = viewModel()
            val speedTestViewModel: SpeedTestViewModel = viewModel()

            /** DNS / VPN connect — call after reward ad (or Premium bypass). */
            fun connectHomeFlow() {
                val serverName = SelectedDnsPrefs.getSelectedName(this@MainActivity)
                if (homeViewModel.selectedLocation.value.name != serverName) {
                    homeViewModel.selectLocation(Location(serverName))
                }
                if (homeViewModel.connectionState.value.isConnected) return
                val hostname = if (serverName == "Custom DNS") {
                    CustomDnsPrefs.getHostname(this@MainActivity)
                } else {
                    DnsConfig.getPrivateDnsHostname(serverName)
                }
                val useVpnTunnel = DnsConfig.usesDnsVpnTunnel(serverName, this@MainActivity)
                if (useVpnTunnel) {
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
                } else if (hostname != null) {
                    homeViewModel.toggleConnection()
                    if (!trySetPrivateDns(this@MainActivity, hostname)) {
                        showDnsPrompt = hostname to packageName
                    }
                } else {
                    homeViewModel.toggleConnection()
                }
                speedTestViewModel.reloadPersistedSelection()
            }

            /** Free users must complete a rewarded ad before connect; Premium skips the ad. */
            fun runConnectAfterReward() {
                if (isPremiumUnlocked(this@MainActivity)) {
                    connectHomeFlow()
                } else {
                    showRewardPreparingGate = true
                    rewardedAdHelper.showRequiredForConnect(
                        onRewarded = { connectHomeFlow() },
                        onDismissGate = { showRewardPreparingGate = false }
                    )
                }
            }

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
                    modifier = Modifier
                        .fillMaxSize()
                        .background(adShieldScreenBackgroundBrush(isDarkMode)),
                    color = Color.Transparent
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                    if (showNotificationPrimer) {
                        AlertDialog(
                            onDismissRequest = {
                                showNotificationPrimer = false
                                getSharedPreferences(APP_PREFS_NAME, MODE_PRIVATE).edit()
                                    .putBoolean("notification_permission_asked", true)
                                    .apply()
                            },
                            title = {
                                Text(stringResource(R.string.notification_permission_rationale_title))
                            },
                            text = {
                                Text(stringResource(R.string.notification_permission_rationale_message))
                            },
                            confirmButton = {
                                TextButton(
                                    onClick = {
                                        showNotificationPrimer = false
                                        getSharedPreferences(APP_PREFS_NAME, MODE_PRIVATE).edit()
                                            .putBoolean("notification_permission_asked", true)
                                            .apply()
                                        notificationPermissionLauncher.launch(
                                            "android.permission.POST_NOTIFICATIONS"
                                        )
                                    }
                                ) {
                                    Text(stringResource(R.string.common_continue))
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = {
                                        showNotificationPrimer = false
                                        getSharedPreferences(APP_PREFS_NAME, MODE_PRIVATE).edit()
                                            .putBoolean("notification_permission_asked", true)
                                            .apply()
                                    }
                                ) {
                                    Text(stringResource(R.string.common_not_now))
                                }
                            }
                        )
                    }
                    // System back / gesture: go back one screen only, don't close app
                    BackHandler {
                        when {
                            showRewardPreparingGate -> {
                                showRewardPreparingGate = false
                                rewardedAdHelper.cancelConnectRewardFlow()
                                rewardedAdHelper.cancelBonusRewardFlow()
                            }
                            showVpnProminentDisclosure -> showVpnProminentDisclosure = false
                            showPreferences -> showPreferences = false
                            showLanguage -> showLanguage = false
                            showFeedback -> showFeedback = false
                            showAdvanced -> { showAdvanced = false; showSettings = true }
                            showSettings -> { showFeedback = false; showSettings = false }
                            showCustomDns -> showCustomDns = false
                            showServerPicker -> showServerPicker = false
                            showPremium -> showPremium = false
                            showSpeedTest -> {
                                speedTestViewModel.resetTest()
                                showSpeedTest = false
                            }
                            showDnsPrompt != null -> showDnsPrompt = null
                            showTermsFull -> { showTermsFull = false; showTermsIntro = true }
                            showTermsIntro -> { showTermsIntro = false; showSplash = true }
                            else -> { /* On Home or Splash: consume back so app doesn't close */ }
                        }
                    }

                    // Splash screen shows immediately as default launch screen
                    when {
                        showSplash -> {
                            SplashScreen(
                                onNavigateToHome = {
                                    showSplash = false
                                    val prefs = getSharedPreferences(APP_PREFS_NAME, MODE_PRIVATE)
                                    val termsAccepted = prefs.getBoolean("terms_accepted", false)
                                    if (!termsAccepted) {
                                        showTermsIntro = true
                                    } else {
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                            val asked = prefs.getBoolean("notification_permission_asked", false)
                                            val hasPermission = ContextCompat.checkSelfPermission(
                                                this@MainActivity,
                                                "android.permission.POST_NOTIFICATIONS"
                                            ) == PackageManager.PERMISSION_GRANTED
                                            if (!asked && !hasPermission) {
                                                showNotificationPrimer = true
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
                                    getSharedPreferences(APP_PREFS_NAME, MODE_PRIVATE).edit()
                                        .putBoolean("terms_accepted", true).apply()
                                    showTermsFull = false
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        val prefs = getSharedPreferences(APP_PREFS_NAME, MODE_PRIVATE)
                                        val asked = prefs.getBoolean("notification_permission_asked", false)
                                        val hasPermission = ContextCompat.checkSelfPermission(
                                            this@MainActivity,
                                            "android.permission.POST_NOTIFICATIONS"
                                        ) == PackageManager.PERMISSION_GRANTED
                                        if (!asked && !hasPermission) {
                                            showNotificationPrimer = true
                                        }
                                    }
                                }
                            )
                        }
                        showSpeedTest -> {
                            BackHandler {
                                speedTestViewModel.resetTest()
                                showSpeedTest = false
                            }
                            val connectionState by speedTestViewModel.connectionState
                            val dnsResults by speedTestViewModel.dnsResults
                            val selectedSpeedLocation by speedTestViewModel.selectedLocation
                            val applyDnsFromSpeedTest: (DnsLatencyResult) -> Unit = { dns ->
                                val loc = Location(dns.name)
                                speedTestViewModel.selectLocation(loc)
                                homeViewModel.selectLocation(loc)
                            }
                            SpeedTestScreen(
                                connectionState = connectionState,
                                isDarkMode = isDarkMode,
                                dnsResults = dnsResults,
                                activeDnsName = selectedSpeedLocation.name,
                                onDnsRowClick = applyDnsFromSpeedTest,
                                onUseDnsClick = { dns ->
                                    applyDnsFromSpeedTest(dns)
                                    showSpeedTest = false
                                },
                                onBackClick = {
                                    speedTestViewModel.resetTest()
                                    showSpeedTest = false
                                    interstitialAdHelper.maybeShowAfterNaturalBreak()
                                },
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
                                onBackClick = {
                                    showPreferences = false
                                    interstitialAdHelper.maybeShowAfterNaturalBreak()
                                }
                            )
                        }
                        showLanguage -> {
                            val selectedLanguageTag by preferencesViewModel.languageTag.collectAsState()
                            LanguageScreen(
                                isDarkMode = isDarkMode,
                                selectedLanguageTag = selectedLanguageTag,
                                onLanguageSelected = { tag ->
                                    preferencesViewModel.setLanguageTag(tag)
                                    showLanguage = false
                                    recreate()
                                },
                                onBackClick = { showLanguage = false }
                            )
                        }
                        showPremium -> {
                            PremiumScreen(
                                isDarkMode = isDarkMode,
                                billingManager = billingManager,
                                onBackClick = {
                                    showPremium = false
                                    interstitialAdHelper.maybeShowAfterNaturalBreak()
                                },
                                onPurchaseSuccess = { showPremium = false },
                                onPurchaseError = { msg -> android.util.Log.e("Premium", msg) }
                            )
                        }
                        showAdvanced -> {
                            val connectionState by homeViewModel.connectionState
                            val selectedLocation by homeViewModel.selectedLocation
                            AdvancedScreen(
                                isDarkMode = isDarkMode,
                                isConnected = connectionState.isConnected,
                                providerName = selectedLocation.name,
                                onBackClick = {
                                    showAdvanced = false
                                    showFeedback = false
                                    showSettings = true
                                },
                                onCustomDnsClick = {
                                    customDnsFromAdvanced = true
                                    showAdvanced = false
                                    showCustomDns = true
                                }
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
                            if (showFeedback) {
                                FeedbackScreen(
                                    isDarkMode = isDarkMode,
                                    onBackClick = { showFeedback = false }
                                )
                            } else {
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
                                    onBackClick = {
                                        showFeedback = false
                                        showSettings = false
                                        interstitialAdHelper.maybeShowAfterNaturalBreak()
                                    },
                                    onPreferencesClick = { showPreferences = true },
                                    onSpeedTestClick = {
                                        speedTestViewModel.reloadPersistedSelection()
                                        showSpeedTest = true
                                    },
                                    onReferFriendsClick = { shareReferralLink(this@MainActivity) },
                                    onLanguageClick = { showLanguage = true },
                                    onAdvancedClick = {
                                        showFeedback = false
                                        showSettings = false
                                        showAdvanced = true
                                    },
                                    onFeedbackClick = { showFeedback = true }
                                )
                            }
                        }
                        else -> {
                            val connectionState by homeViewModel.connectionState
                            val selectedLocation by homeViewModel.selectedLocation
                            val remainingSeconds by homeViewModel.remainingSeconds
                            // While on home: try interstitial every [MIN_IMPRESSION_INTERVAL_MS] (skipped until
                            // enough time passed since last impression — same rule as back-from-settings).
                            LaunchedEffect(Unit) {
                                while (true) {
                                    delay(InterstitialAdHelper.MIN_IMPRESSION_INTERVAL_MS)
                                    interstitialAdHelper.maybeShowAfterNaturalBreak()
                                }
                            }
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
                                        val wasConnected = connectionState.isConnected

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

                                        val serverName = SelectedDnsPrefs.getSelectedName(this@MainActivity)
                                        if (homeViewModel.selectedLocation.value.name != serverName) {
                                            homeViewModel.selectLocation(Location(serverName))
                                        }
                                        if (homeViewModel.connectionState.value.isConnected) {
                                            return@HomeScreen
                                        }
                                        val useVpnTunnel = DnsConfig.usesDnsVpnTunnel(serverName, this@MainActivity)
                                        val disclosureOk = getSharedPreferences(APP_PREFS_NAME, MODE_PRIVATE)
                                            .getBoolean(KEY_VPN_PROMINENT_DISCLOSURE_ACCEPTED, false)
                                        if (useVpnTunnel && !disclosureOk) {
                                            showVpnProminentDisclosure = true
                                            return@HomeScreen
                                        }
                                        runConnectAfterReward()
                                    } catch (e: Exception) {
                                        android.util.Log.e("MainActivity", "Connect button error", e)
                                    }
                                },
                                onRewardClick = {
                                    showRewardPreparingGate = true
                                    rewardedAdHelper.show(
                                        onReward = null,
                                        onDismissGate = { showRewardPreparingGate = false }
                                    )
                                },
                                onPremiumClick = { showPremium = true },
                                onLocationClick = {
                                    serverPickerFromSpeedTest = false
                                    showServerPicker = true
                                },
                                onMenuClick = {
                                    showFeedback = false
                                    showSettings = true
                                }
                            )
                        }
                    }
                    if (showCustomDns && !showPremium) {
                        CustomDnsScreen(
                            isDarkMode = isDarkMode,
                            onBackClick = {
                                showCustomDns = false
                                if (customDnsFromAdvanced) {
                                    showAdvanced = true
                                    customDnsFromAdvanced = false
                                }
                            },
                            onSaved = {
                                showCustomDns = false
                                homeViewModel.selectLocation(Location("Custom DNS"))
                                if (customDnsFromAdvanced) {
                                    showAdvanced = true
                                    customDnsFromAdvanced = false
                                }
                            },
                            onGoPremium = { showPremium = true }
                        )
                    }
                    if (showServerPicker) {
                                val speedSelected by speedTestViewModel.selectedLocation
                                val homeSelected by homeViewModel.selectedLocation
                                ChooseServerSheet(
                                    selectedLocation = if (serverPickerFromSpeedTest) speedSelected else homeSelected,
                                    isDarkMode = isDarkMode,
                                    onSelect = {
                                        val picked = it
                                        if (serverPickerFromSpeedTest) {
                                            speedTestViewModel.selectLocation(picked)
                                        } else {
                                            val newServer = picked.name
                                            val wasConnected = homeViewModel.connectionState.value.isConnected
                                            homeViewModel.selectLocation(picked)
                                            if (wasConnected) {
                                                startService(
                                                    Intent(this@MainActivity, DnsVpnService::class.java).apply {
                                                        action = DnsVpnService.ACTION_STOP
                                                    }
                                                )
                                                try {
                                                    tryClearPrivateDns(this@MainActivity)
                                                } catch (_: Throwable) { }
                                                val useVpnTunnel = DnsConfig.usesDnsVpnTunnel(newServer, this@MainActivity)
                                                val host = if (newServer == "Custom DNS") {
                                                    CustomDnsPrefs.getHostname(this@MainActivity)
                                                } else {
                                                    DnsConfig.getPrivateDnsHostname(newServer)
                                                }
                                                if (useVpnTunnel) {
                                                    val prep = VpnService.prepare(this@MainActivity)
                                                    if (prep != null) {
                                                        pendingVpnServerName = newServer
                                                        vpnPrepareLauncher.launch(prep)
                                                    } else {
                                                        startService(
                                                            Intent(this@MainActivity, DnsVpnService::class.java)
                                                                .putExtra(DnsVpnService.EXTRA_SERVER_NAME, newServer)
                                                        )
                                                    }
                                                } else if (host != null) {
                                                    if (!trySetPrivateDns(this@MainActivity, host)) {
                                                        showDnsPrompt = host to packageName
                                                    }
                                                }
                                            }
                                        }
                                        showServerPicker = false
                                    },
                                    onDismiss = { showServerPicker = false },
                                    onCustomDnsClick = {
                                        customDnsFromAdvanced = false
                                        showServerPicker = false
                                        showCustomDns = true
                                    }
                                )
                    }
                    if (showVpnProminentDisclosure) {
                        VpnProminentDisclosureScreen(
                            isDarkMode = isDarkMode,
                            onAccept = {
                                getSharedPreferences(APP_PREFS_NAME, MODE_PRIVATE).edit()
                                    .putBoolean(KEY_VPN_PROMINENT_DISCLOSURE_ACCEPTED, true)
                                    .apply()
                                showVpnProminentDisclosure = false
                                runConnectAfterReward()
                            },
                            onDecline = { showVpnProminentDisclosure = false }
                        )
                    }
                    showDnsPrompt?.let { (hostname, packageName) ->
                        val ctx = LocalContext.current
                        val adbCommand = "adb shell pm grant $packageName android.permission.WRITE_SECURE_SETTINGS"
                        AlertDialog(
                            onDismissRequest = { showDnsPrompt = null },
                            title = { Text(stringResource(R.string.dns_prompt_title)) },
                            text = {
                                Text(
                                    stringResource(R.string.dns_prompt_message, adbCommand),
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    copyHostnameToClipboard(ctx, adbCommand)
                                    showDnsPrompt = null
                                }) { Text(stringResource(R.string.dns_prompt_copy_command)) }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDnsPrompt = null }) { Text(stringResource(R.string.common_ok)) }
                            }
                        )
                    }
                    InternetStatusSnackbar()
                    if (showRewardPreparingGate) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.78f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                CircularProgressIndicator()
                                Text(
                                    text = stringResource(R.string.connect_reward_gate_message),
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                    }
                }
            }
        }
    }
}
