package com.digitalnestapps.adshield.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.digitalnestapps.adshield.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/** Minimum time offline before showing “no internet” — filters VPN/handover flicker. */
private const val OFFLINE_DEBOUNCE_MS = 900L

class NetworkStatusObserver(context: Context) {
    private val appContext = context.applicationContext
    private val connectivityManager =
        appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    fun observe(): Flow<Boolean> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) = sendNow()
            override fun onLost(network: Network) = sendNow()
            override fun onUnavailable() {
                trySend(false)
            }

            // Do not use onLosing → offline: it fires during VPN attach / handover and causes
            // false "Internet restored" snackbars when the user connects protection.

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                trySend(isValidated(networkCapabilities))
            }

            private fun sendNow() {
                trySend(isInternetAvailable())
            }
        }

        trySend(isInternetAvailable())
        connectivityManager.registerDefaultNetworkCallback(callback)
        awaitClose { runCatching { connectivityManager.unregisterNetworkCallback(callback) } }
    }.distinctUntilChanged()

    private fun isInternetAvailable(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val caps = connectivityManager.getNetworkCapabilities(network) ?: return false
        return isValidated(caps)
    }

    private fun isValidated(caps: NetworkCapabilities): Boolean {
        val hasInternet = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isValidated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        val hasKnownTransport = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
        return hasInternet && isValidated && hasKnownTransport
    }
}

private data class NetworkSnackbarVisuals(
    override val message: String,
    val restored: Boolean,
    override val actionLabel: String? = null,
    override val withDismissAction: Boolean = false,
    override val duration: SnackbarDuration = SnackbarDuration.Indefinite
) : SnackbarVisuals

@Composable
fun InternetStatusSnackbar(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    // Re-read when app language changes (AppCompat per-app locales update configuration).
    val localeTag = configuration.locales[0]?.toLanguageTag().orEmpty()
    val observer = remember { NetworkStatusObserver(context) }
    val disconnectedText = stringResource(R.string.network_unavailable)
    val restoredText = stringResource(R.string.network_restored)
    val hostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    /** Latest connectivity from the observer (may flip quickly during VPN / handover). */
    var latestAvailable by remember(localeTag) { mutableStateOf(true) }
    /** True only after we showed the offline snackbar (user actually saw no internet). */
    var userWasShownOffline by remember(localeTag) { mutableStateOf(false) }
    var pendingDisconnectJob by remember { mutableStateOf<Job?>(null) }
    var snackbarJob by remember { mutableStateOf<Job?>(null) }

    DisposableEffect(localeTag) {
        snackbarJob?.cancel()
        pendingDisconnectJob?.cancel()
        snackbarJob = null
        pendingDisconnectJob = null
        hostState.currentSnackbarData?.dismiss()
        onDispose {
            snackbarJob?.cancel()
            pendingDisconnectJob?.cancel()
        }
    }

    // Resolve messages at show-time + restart collector when locale/strings change so text matches selected language.
    LaunchedEffect(observer, localeTag, disconnectedText, restoredText) {
        observer.observe().collect { available ->
            latestAvailable = available

            if (!available) {
                // Wait — brief false is common when enabling VPN; don't flash offline/restored.
                pendingDisconnectJob?.cancel()
                pendingDisconnectJob = scope.launch {
                    delay(OFFLINE_DEBOUNCE_MS)
                    if (!latestAvailable) {
                        snackbarJob?.cancel()
                        hostState.currentSnackbarData?.dismiss()
                        val msg = context.getString(R.string.network_unavailable)
                        snackbarJob = scope.launch {
                            launch {
                                delay(2000)
                                hostState.currentSnackbarData?.dismiss()
                            }
                            hostState.showSnackbar(
                                NetworkSnackbarVisuals(
                                    message = msg,
                                    restored = false
                                )
                            )
                            userWasShownOffline = true
                        }
                    }
                }
            } else {
                pendingDisconnectJob?.cancel()
                pendingDisconnectJob = null
                if (userWasShownOffline) {
                    userWasShownOffline = false
                    snackbarJob?.cancel()
                    hostState.currentSnackbarData?.dismiss()
                    val msg = context.getString(R.string.network_restored)
                    snackbarJob = scope.launch {
                        launch {
                            delay(2000)
                            hostState.currentSnackbarData?.dismiss()
                        }
                        hostState.showSnackbar(
                            NetworkSnackbarVisuals(
                                message = msg,
                                restored = true
                            )
                        )
                    }
                }
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        SnackbarHost(
            hostState = hostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) { data ->
            val restored = (data.visuals as? NetworkSnackbarVisuals)?.restored == true
            Snackbar(
                snackbarData = data,
                containerColor = if (restored) Color(0xFF2E7D32) else Color(0xFFC62828),
                contentColor = Color.White
            )
        }
    }
}
