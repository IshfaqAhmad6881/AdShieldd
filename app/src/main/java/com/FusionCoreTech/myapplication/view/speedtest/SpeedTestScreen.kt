package com.FusionCoreTech.myapplication.view.speedtest

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.FusionCoreTech.myapplication.R
import com.FusionCoreTech.myapplication.ui.components.AdShieldScreenInsets
import com.FusionCoreTech.myapplication.ui.components.AdShieldTopBarRow
import com.FusionCoreTech.myapplication.ui.theme.*
import com.FusionCoreTech.myapplication.viewmodel.SpeedTestConnectionState
import com.airbnb.lottie.compose.*
import kotlinx.coroutines.delay

/**
 * Speed Test screen — matches Figma frame 106:716 (VPN App UI Kit - Community Copy).
 * Specs from get_design_context + get_screenshot.
 * Tokens: Neutral/100 #F5F5F5, Neutral/200 #EEEEEE, Neutral/500 #AAAAAA, Neutral/600 #777777,
 * Primary/100 #FEF1EB, Primary/500 #F06A30.
 */
@Composable
fun SpeedTestScreen(
    connectionState: SpeedTestConnectionState,
    isDarkMode: Boolean,
    dnsResults: List<com.FusionCoreTech.myapplication.viewmodel.DnsLatencyResult>,
    activeDnsName: String? = null,
    onDnsRowClick: (com.FusionCoreTech.myapplication.viewmodel.DnsLatencyResult) -> Unit = {},
    onUseDnsClick: (com.FusionCoreTech.myapplication.viewmodel.DnsLatencyResult) -> Unit = {},
    onBackClick: () -> Unit = {},
    onStartTestClick: () -> Unit = {}
) {
    /** After tap: show Lottie + loading briefly, then parent shows rewarded ad. */
    var preparingForRewardAd by remember { mutableStateOf(false) }
    val startTestCallback by rememberUpdatedState(onStartTestClick)
    LaunchedEffect(preparingForRewardAd) {
        if (!preparingForRewardAd) return@LaunchedEffect
        delay(2000)
        startTestCallback()
        preparingForRewardAd = false
    }
    LaunchedEffect(connectionState) {
        if (connectionState == SpeedTestConnectionState.CONNECTING ||
            connectionState == SpeedTestConnectionState.TEST_RUNNING
        ) {
            preparingForRewardAd = false
        }
    }

    // Same app scaffold background as Advanced / Home
    val textDarkColor = if (isDarkMode) DarkTextDark else Color.Black
    val textLightColor = if (isDarkMode) DarkTextLight else Color(0xFFAAAAAA)          // Neutral/500
    val primaryButtonColor = if (isDarkMode) OrangePrimary else PremiumOrange          // Primary/500 #F06A30

    // Figma 106:716 — Frame is #F5F5F5 only; white is ONLY on buttons + cards (no full-width white sheet)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(adShieldScreenBackgroundBrush(isDarkMode))
            .statusBarsPadding()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = AdShieldScreenInsets.headerHorizontal)
                .padding(bottom = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(AdShieldScreenInsets.belowStatusBar))

            AdShieldTopBarRow(
                isDarkMode = isDarkMode,
                onBackClick = onBackClick,
                trailing = { Spacer(modifier = Modifier.size(48.dp)) }
            ) {
                Text(
                    text = stringResource(R.string.speed_test_title),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 34.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textDarkColor
                )
            }

            // Figma: back at 68px, card at 140px → 24px between header (48px) and card
            Spacer(modifier = Modifier.height(24.dp))

            // CONNECTING state - frame style
            if (connectionState == SpeedTestConnectionState.CONNECTING) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(56.dp),
                            color = primaryButtonColor,
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = stringResource(R.string.speed_test_connecting),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textDarkColor
                        )
                        Text(
                            text = stringResource(R.string.speed_test_testing_dns),
                            fontSize = 13.sp,
                            color = textLightColor
                        )
                    }
                }
            } else {
            // Lottie animation - center (rocket speed test). No key() to avoid reloading composition on state change (prevents OOM/crash).
            val isRunning = connectionState == SpeedTestConnectionState.TEST_RUNNING ||
                    connectionState == SpeedTestConnectionState.CONNECTING ||
                    preparingForRewardAd
            if (connectionState != SpeedTestConnectionState.TEST_COMPLETED || preparingForRewardAd) {
                val composition by rememberLottieComposition(
                    LottieCompositionSpec.RawRes(R.raw.businessman_rocket)
                )
                val progress by animateLottieCompositionAsState(
                    composition = composition,
                    isPlaying = isRunning,
                    iterations = if (isRunning) LottieConstants.IterateForever else 1
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    contentAlignment = Alignment.Center
                ) {
                        composition?.let {
                            LottieAnimation(
                                composition = it,
                                progress = { progress },
                                modifier = Modifier.size(230.dp)
                            )
                        }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }

            // Preparing (before ad) + DNS testing progress line
            if (connectionState == SpeedTestConnectionState.CONNECTING ||
                connectionState == SpeedTestConnectionState.TEST_RUNNING ||
                preparingForRewardAd
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (preparingForRewardAd &&
                            connectionState != SpeedTestConnectionState.CONNECTING &&
                            connectionState != SpeedTestConnectionState.TEST_RUNNING
                        ) {
                            stringResource(R.string.speed_test_preparing)
                        } else {
                            stringResource(R.string.speed_test_testing_dns)
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textDarkColor
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(4.dp),
                        color = primaryButtonColor,
                        trackColor = if (isDarkMode) Color(0xFF2C2C2C) else Color(0xFFE0E0E0)
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
            } else {
                if (connectionState == SpeedTestConnectionState.TEST_COMPLETED && !preparingForRewardAd) {
                    Text(
                        text = stringResource(R.string.speed_test_dns_benchmark),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textLightColor
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    DnsResultsList(
                        results = dnsResults,
                        isDarkMode = isDarkMode,
                        activeDnsName = activeDnsName,
                        modifier = Modifier.weight(1f),
                        onDnsRowClick = onDnsRowClick,
                        onUseDnsClick = onUseDnsClick
                    )
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(14.dp))
                Button(
                    onClick = { preparingForRewardAd = true },
                    enabled = !preparingForRewardAd,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryButtonColor),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp
                    )
                ) {
                    Text(
                        text = if (connectionState == SpeedTestConnectionState.TEST_COMPLETED) {
                            stringResource(R.string.speed_test_test_again)
                        } else {
                            stringResource(R.string.speed_test_start)
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
            } // end else (TEST_RUNNING -> show speed; else -> server + button)
        }
    }
}

@Composable
private fun DnsResultsList(
    results: List<com.FusionCoreTech.myapplication.viewmodel.DnsLatencyResult>,
    isDarkMode: Boolean,
    activeDnsName: String?,
    modifier: Modifier = Modifier,
    onDnsRowClick: (com.FusionCoreTech.myapplication.viewmodel.DnsLatencyResult) -> Unit,
    onUseDnsClick: (com.FusionCoreTech.myapplication.viewmodel.DnsLatencyResult) -> Unit
) {
    val cardBackground = if (isDarkMode) DarkBackgroundWhite else Color.White
    val textDarkColor = if (isDarkMode) DarkTextDark else TextDark
    val textLightColor = if (isDarkMode) DarkTextLight else TextLight
    val accent = if (isDarkMode) OrangePrimary else PremiumOrange

    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(scrollState)
            .padding(horizontal = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        results.forEach { item ->
            val latency = item.latencyMs
            val latencyText = latency?.let { String.format("%.2f ms", it.toDouble()) } ?: "-"
            val isActive = item.name.equals(activeDnsName, ignoreCase = true)
            val latencyColor = when {
                latency == null -> textLightColor
                latency <= 80 -> Color(0xFF2E7D32) // green
                latency <= 180 -> Color(0xFFF9A825) // yellow
                else -> Color(0xFFC62828) // red
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDnsRowClick(item) },
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = cardBackground),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 3.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Latency left
                    Text(
                        text = latencyText,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = latencyColor,
                        modifier = Modifier.widthIn(min = 96.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // DNS name + IPs
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = item.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textDarkColor
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = buildString {
                                append(item.primaryIp)
                                item.secondaryIp?.let {
                                    append("\n")
                                    append(it)
                                }
                            },
                            fontSize = 12.sp,
                            color = textLightColor
                        )
                    }

                    Text(
                        text = if (isActive) {
                            stringResource(R.string.speed_test_active)
                        } else {
                            stringResource(R.string.speed_test_use_it)
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isActive) textLightColor.copy(alpha = 0.75f) else accent,
                        modifier = if (isActive) {
                            Modifier
                        } else {
                            Modifier.clickable { onUseDnsClick(item) }
                        }
                    )
                }
            }
        }
    }
}
