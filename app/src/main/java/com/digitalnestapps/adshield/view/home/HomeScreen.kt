package com.digitalnestapps.adshield.view.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalnestapps.adshield.R
import com.digitalnestapps.adshield.model.ConnectionState
import com.digitalnestapps.adshield.model.Location
import com.digitalnestapps.adshield.ui.theme.OrangePrimary
import com.digitalnestapps.adshield.ui.theme.PremiumOrange
import com.digitalnestapps.adshield.ui.theme.OrangeLight
import com.digitalnestapps.adshield.ui.theme.BackgroundWhite
import com.digitalnestapps.adshield.ui.theme.OuterRingGrey
import com.digitalnestapps.adshield.ui.theme.TextDark
import com.digitalnestapps.adshield.ui.theme.TextLight
import com.digitalnestapps.adshield.ui.theme.DarkBackgroundWhite
import com.digitalnestapps.adshield.ui.theme.DarkOuterRingGrey
import com.digitalnestapps.adshield.ui.theme.DarkTextDark
import com.digitalnestapps.adshield.ui.theme.DarkTextLight
import com.digitalnestapps.adshield.ads.BannerAdView
import androidx.compose.ui.platform.LocalConfiguration

private const val HOME_COMPACT_HEIGHT_BREAKPOINT_DP = 520
private const val HOME_WIDE_WIDTH_BREAKPOINT_DP = 600
private val HomeContentMaxWidthTablet = 520.dp

@Composable
fun HomeScreen(
    connectionState: ConnectionState,
    selectedLocation: Location,
    remainingSeconds: Int,
    isDarkMode: Boolean,
    onStartClick: () -> Unit = {},
    onRewardClick: () -> Unit = {},
    onPremiumClick: () -> Unit = {},
    onLocationClick: () -> Unit = {},
    onMenuClick: () -> Unit = {}
) {
    val scrollState = rememberScrollState()
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val isCompactHeight = maxHeight.value.toInt() < HOME_COMPACT_HEIGHT_BREAKPOINT_DP
        val isWideLayout = maxWidth.value.toInt() >= HOME_WIDE_WIDTH_BREAKPOINT_DP
        // Short/narrow: smaller dial so status + “not connected” stay on screen; wide: cap width for tablets.
        val horizontalPadding = 20.dp
        val contentWidth = maxWidth - horizontalPadding * 2
        val controlOuter = (minOf(contentWidth, maxHeight * 0.36f)).coerceIn(176.dp, 280.dp)

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .then(
                        if (isWideLayout) {
                            Modifier
                                .widthIn(max = HomeContentMaxWidthTablet)
                                .fillMaxHeight()
                        } else {
                            Modifier.fillMaxSize()
                        }
                    )
                    .padding(horizontal = horizontalPadding)
            ) {
                if (isCompactHeight) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(scrollState)
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HeaderSection(
                            onMenuClick = onMenuClick,
                            onPremiumClick = onPremiumClick,
                            isDarkMode = isDarkMode,
                            compactHeader = screenWidthDp < 400
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        SpeedMetricsCard(
                            downloadSpeed = connectionState.downloadSpeed,
                            uploadSpeed = connectionState.uploadSpeed,
                            isDarkMode = isDarkMode
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        MainControlSection(
                            isConnected = connectionState.isConnected,
                            timer = connectionState.timer,
                            remainingSeconds = remainingSeconds,
                            isDarkMode = isDarkMode,
                            onStartClick = onStartClick,
                            onRewardClick = onRewardClick,
                            controlOuterDp = controlOuter
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                } else {
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        HeaderSection(
                            onMenuClick = onMenuClick,
                            onPremiumClick = onPremiumClick,
                            isDarkMode = isDarkMode,
                            compactHeader = screenWidthDp < 400
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        SpeedMetricsCard(
                            downloadSpeed = connectionState.downloadSpeed,
                            uploadSpeed = connectionState.uploadSpeed,
                            isDarkMode = isDarkMode
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        MainControlSection(
                            isConnected = connectionState.isConnected,
                            timer = connectionState.timer,
                            remainingSeconds = remainingSeconds,
                            isDarkMode = isDarkMode,
                            onStartClick = onStartClick,
                            onRewardClick = onRewardClick,
                            controlOuterDp = controlOuter
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                LocationSelector(
                    location = selectedLocation,
                    isDarkMode = isDarkMode,
                    onLocationClick = onLocationClick
                )
                Spacer(modifier = Modifier.height(12.dp))
                BannerAdView(modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
fun HeaderSection(
    onMenuClick: () -> Unit,
    onPremiumClick: () -> Unit,
    isDarkMode: Boolean,
    compactHeader: Boolean = false
) {
    val cardBg = if (isDarkMode) DarkBackgroundWhite else BackgroundWhite
    val borderColor = if (isDarkMode) Color.White.copy(alpha = 0.2f) else Color.LightGray.copy(alpha = 0.5f)
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Menu Icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(cardBg)
                .border(1.dp, borderColor, RoundedCornerShape(16.dp))
                .clickable { onMenuClick() },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.menu),
                contentDescription = stringResource(R.string.home_menu),
                modifier = Modifier.size(20.dp)
            )
        }
        
        // Go Premium Button with Shadow/Popup Effect
        Card(
            modifier = Modifier
                .height(44.dp)
                .clickable { onPremiumClick() },
            shape = RoundedCornerShape(22.dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp,
                pressedElevation = 6.dp
            ),
            colors = CardDefaults.cardColors(containerColor = PremiumOrange)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(horizontal = if (compactHeader) 10.dp else 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Premium icon
                    Image(
                        painter = painterResource(id = R.drawable.premium),
                        contentDescription = stringResource(R.string.home_premium),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.home_go_premium),
                        fontSize = if (compactHeader) 14.sp else 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Formats speed in bytes per second to display value and unit.
 * Unit is chosen by speed: B/s, KB/s, MB/s, GB/s with appropriate decimals.
 */
private fun formatSpeed(bytesPerSecond: Float): Pair<String, String> {
    if (bytesPerSecond <= 0f) return "0.00" to "B/s"
    return when {
        bytesPerSecond >= 1e9f -> "%.2f".format(bytesPerSecond / 1e9f) to "GB/s"
        bytesPerSecond >= 1e6f -> "%.2f".format(bytesPerSecond / 1e6f) to "MB/s"
        bytesPerSecond >= 1e3f -> "%.2f".format(bytesPerSecond / 1e3f) to "KB/s"
        else -> "%.2f".format(bytesPerSecond) to "B/s"
    }
}

@Composable
fun SpeedMetricsCard(
    downloadSpeed: Float,
    uploadSpeed: Float,
    isDarkMode: Boolean
) {
    val cardBg = if (isDarkMode) DarkBackgroundWhite else BackgroundWhite
    val textColor = if (isDarkMode) DarkTextDark else TextDark
    val textLightColor = if (isDarkMode) DarkTextLight else TextLight
    val borderColor = if (isDarkMode) Color.White.copy(alpha = 0.1f) else Color.LightGray.copy(alpha = 0.2f)
    val (downloadStr, downloadUnit) = formatSpeed(downloadSpeed)
    val (uploadStr, uploadUnit) = formatSpeed(uploadSpeed)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 6.dp,
            pressedElevation = 4.dp
        ),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .border(1.dp, borderColor, RoundedCornerShape(16.dp))
        ) {
            Row(
                modifier = Modifier.fillMaxSize()
            ) {
                // Download Section
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(OrangeLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.arrowdownward),
                                contentDescription = stringResource(R.string.home_download),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = stringResource(R.string.home_download),
                            fontSize = 16.sp,
                            color = textColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = downloadStr,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = downloadUnit,
                            fontSize = 12.sp,
                            color = textLightColor,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
                
                // Divider
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .fillMaxHeight()
                        .background(
                            if (isDarkMode) Color.White.copy(alpha = 0.15f) 
                            else Color.LightGray.copy(alpha = 0.3f)
                        )
                )
                
                // Upload Section
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(OrangeLight),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.arrowupward),
                                contentDescription = stringResource(R.string.home_upload),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Text(
                            text = stringResource(R.string.home_upload),
                            fontSize = 16.sp,
                            color = textColor,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(
                            text = uploadStr,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Text(
                            text = uploadUnit,
                            fontSize = 12.sp,
                            color = textLightColor,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun formatTimeLabel(seconds: Int): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return "%02d:%02d:%02d".format(h, m, s)
}

@Composable
fun MainControlSection(
    isConnected: Boolean,
    timer: String,
    remainingSeconds: Int,
    isDarkMode: Boolean,
    onStartClick: () -> Unit,
    onRewardClick: () -> Unit,
    controlOuterDp: Dp = 250.dp
) {
    val outerRingColor = if (isConnected) OrangeLight else if (isDarkMode) DarkOuterRingGrey else OuterRingGrey
    val cardBg = if (isConnected) OrangePrimary else if (isDarkMode) DarkBackgroundWhite else BackgroundWhite
    val textDarkColor = if (isDarkMode) DarkTextDark else TextDark
    val textLightColor = if (isDarkMode) DarkTextLight else TextLight
    val innerRingDp = controlOuterDp * (170f / 250f)
    val centerCardDp = controlOuterDp * (150f / 250f)
    val powerIconDp = (centerCardDp.value * 0.27f).coerceIn(32f, 40f).dp
    val startLabelSp = (14f * (centerCardDp.value / 150f).coerceIn(0.9f, 1.1f)).coerceIn(13f, 16f)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Reward button: watch ad → +30 min
        Button(
            onClick = onRewardClick,
            modifier = Modifier.height(38.dp),
            shape = RoundedCornerShape(19.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PremiumOrange),
            contentPadding = PaddingValues(horizontal = 18.dp, vertical = 6.dp)
        ) {
            Text(
                text = stringResource(R.string.home_reward_watch_ad),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
        // Small label: remaining / total time
        Text(
            text = stringResource(R.string.home_time_left, formatTimeLabel(remainingSeconds)),
            fontSize = 11.sp,
            color = textLightColor,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        )
        // Animated concentric circles + main dial (sizes follow controlOuterDp)
        Box(
            modifier = Modifier.size(controlOuterDp),
            contentAlignment = Alignment.Center
        ) {
            if (!isConnected) {
                AnimatedCircles(outerDiameter = controlOuterDp)
            }

            Box(
                modifier = Modifier
                    .size(innerRingDp)
                    .clip(CircleShape)
                    .background(outerRingColor),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.size(centerCardDp),
                    shape = CircleShape,
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 12.dp,
                        pressedElevation = 8.dp
                    ),
                    colors = CardDefaults.cardColors(containerColor = cardBg)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .clickable(onClick = { onStartClick() }),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!isConnected) {
                            Image(
                                painter = painterResource(id = R.drawable.stroke),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.FillBounds
                            )
                        }

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.power),
                                contentDescription = if (isConnected) {
                                    stringResource(R.string.home_stop)
                                } else {
                                    stringResource(R.string.home_start)
                                },
                                modifier = Modifier.size(powerIconDp),
                                colorFilter = if (isConnected) ColorFilter.tint(Color.White) else null
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isConnected) {
                                    stringResource(R.string.home_stop)
                                } else {
                                    stringResource(R.string.home_start)
                                },
                                fontSize = startLabelSp.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isConnected) Color.White else OrangePrimary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Text(
                text = if (isConnected) {
                    stringResource(R.string.home_safely_connected)
                } else {
                    stringResource(R.string.common_not_connected)
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = textDarkColor,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = timer,
                fontSize = 14.sp,
                color = textLightColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun AnimatedCircles(outerDiameter: Dp = 250.dp) {
    val innerBase = outerDiameter * (170f / 250f)
    // Animation for first circle
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    
    val scale1 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "circle1"
    )
    
    val alpha1 by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha1"
    )
    
    // Animation for second circle
    val scale2 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = 500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "circle2"
    )
    
    val alpha2 by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = 500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha2"
    )
    
    // Animation for third circle
    val scale3 by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "circle3"
    )
    
    val alpha3 by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, delayMillis = 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha3"
    )
    
    Box(
        modifier = Modifier.size(outerDiameter),
        contentAlignment = Alignment.Center
    ) {
        // First animated circle
        Box(
            modifier = Modifier
                .size(innerBase * scale1)
                .alpha(alpha1)
                .clip(CircleShape)
                .background(OrangePrimary.copy(alpha = 0.3f))
        )
        
        // Second animated circle
        Box(
            modifier = Modifier
                .size(innerBase * scale2)
                .alpha(alpha2)
                .clip(CircleShape)
                .background(OrangePrimary.copy(alpha = 0.25f))
        )
        
        // Third animated circle
        Box(
            modifier = Modifier
                .size(innerBase * scale3)
                .alpha(alpha3)
                .clip(CircleShape)
                .background(OrangePrimary.copy(alpha = 0.2f))
        )
    }
}

@Composable
fun LocationSelector(
    location: Location,
    isDarkMode: Boolean,
    onLocationClick: () -> Unit
) {
    val cardBg = if (isDarkMode) DarkBackgroundWhite else BackgroundWhite
    val textColor = if (isDarkMode) DarkTextDark else TextDark
    val captionColor = if (isDarkMode) DarkTextLight else TextLight
    val accent = if (isDarkMode) OrangePrimary else PremiumOrange
    val chevronColor = captionColor.copy(alpha = 0.85f)
    val borderColor = if (isDarkMode) Color.White.copy(alpha = 0.15f) else Color.LightGray.copy(alpha = 0.3f)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .clickable { onLocationClick() },
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 12.dp,
            pressedElevation = 8.dp
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .clip(RoundedCornerShape(28.dp))
                .background(cardBg)
                .border(
                    width = 1.dp,
                    color = borderColor,
                    shape = RoundedCornerShape(28.dp)
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_dns),
                        contentDescription = stringResource(R.string.home_choose_dns_server),
                        modifier = Modifier.size(26.dp),
                        colorFilter = ColorFilter.tint(accent)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = stringResource(R.string.home_choose_dns_server),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = captionColor
                        )
                        Text(
                            text = location.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textColor
                        )
                    }
                }
                
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = stringResource(R.string.home_select_location),
                    tint = chevronColor,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
