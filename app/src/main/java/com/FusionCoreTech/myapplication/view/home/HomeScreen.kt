package com.FusionCoreTech.myapplication.view.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.FusionCoreTech.myapplication.R
import com.FusionCoreTech.myapplication.model.ConnectionState
import com.FusionCoreTech.myapplication.model.Location
import com.FusionCoreTech.myapplication.ui.theme.OrangePrimary
import com.FusionCoreTech.myapplication.ui.theme.PremiumOrange
import com.FusionCoreTech.myapplication.ui.theme.OrangeLight
import com.FusionCoreTech.myapplication.ui.theme.BackgroundWhite
import com.FusionCoreTech.myapplication.ui.theme.OuterRingGrey
import com.FusionCoreTech.myapplication.ui.theme.TextDark
import com.FusionCoreTech.myapplication.ui.theme.TextLight
import com.FusionCoreTech.myapplication.ui.theme.DarkBackgroundWhite
import com.FusionCoreTech.myapplication.ui.theme.DarkOuterRingGrey
import com.FusionCoreTech.myapplication.ui.theme.DarkTextDark
import com.FusionCoreTech.myapplication.ui.theme.DarkTextLight
import com.FusionCoreTech.myapplication.ads.BannerAdView

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
    val cardBackgroundColor = if (isDarkMode) DarkBackgroundWhite else BackgroundWhite
    val outerRingColor = if (isDarkMode) DarkOuterRingGrey else OuterRingGrey
    val textDarkColor = if (isDarkMode) DarkTextDark else TextDark
    val textLightColor = if (isDarkMode) DarkTextLight else TextLight
    
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Status bar spacer - space for system status bar
        Spacer(modifier = Modifier.height(32.dp))
        
        // Main Content: scrollable top + fixed bottom (DNS server row always visible)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Spacer(modifier = Modifier.height(25.dp))
                
                HeaderSection(
                    onMenuClick = onMenuClick,
                    onPremiumClick = onPremiumClick,
                    isDarkMode = isDarkMode
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
                    onRewardClick = onRewardClick
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            // DNS server row — opens bottom sheet (label + selection inside row)
            LocationSelector(
                location = selectedLocation,
                isDarkMode = isDarkMode,
                onLocationClick = onLocationClick
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Banner ad - only on Home screen
            BannerAdView(modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun HeaderSection(
    onMenuClick: () -> Unit,
    onPremiumClick: () -> Unit,
    isDarkMode: Boolean
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
                    .padding(horizontal = 20.dp),
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
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
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

@Composable
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
    onRewardClick: () -> Unit
) {
    val outerRingColor = if (isConnected) OrangeLight else if (isDarkMode) DarkOuterRingGrey else OuterRingGrey
    val cardBg = if (isConnected) OrangePrimary else if (isDarkMode) DarkBackgroundWhite else BackgroundWhite
    val textDarkColor = if (isDarkMode) DarkTextDark else TextDark
    val textLightColor = if (isDarkMode) DarkTextLight else TextLight
    
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
            color = textLightColor
        )
        // Animated Concentric Circles Container
        Box(
            modifier = Modifier
                .size(250.dp),
            contentAlignment = Alignment.Center
        ) {
            // Animated concentric circles (only when disconnected)
            if (!isConnected) AnimatedCircles()
            
            // Outer Round Ring (lighter orange when connected)
            Box(
                modifier = Modifier
                    .size(170.dp)
                    .clip(CircleShape)
                    .background(outerRingColor),
                contentAlignment = Alignment.Center
            ) {
                // Circular Start/Stop Button with Shadow/Popup Effect and Ripple
                Card(
                    modifier = Modifier.size(150.dp),
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
                            // Stroke background image (disconnected only)
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
                                modifier = Modifier.size(40.dp),
                                colorFilter = if (isConnected) ColorFilter.tint(Color.White) else null
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (isConnected) {
                                    stringResource(R.string.home_stop)
                                } else {
                                    stringResource(R.string.home_start)
                                },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isConnected) Color.White else OrangePrimary
                            )
                        }
                    }
                }
            }
        }
        
        // Status Text – 10dp up, connect btn ke sath attach
        Column(
            modifier = Modifier.offset(y = (-10).dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (isConnected) {
                    stringResource(R.string.home_safely_connected)
                } else {
                    stringResource(R.string.common_not_connected)
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = textDarkColor
            )
            Text(
                text = timer,
                fontSize = 14.sp,
                color = textLightColor
            )
        }
    }
}

@Composable
fun AnimatedCircles() {
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
        modifier = Modifier.size(250.dp),
        contentAlignment = Alignment.Center
    ) {
        // First animated circle
        Box(
            modifier = Modifier
                .size((170.dp * scale1))
                .alpha(alpha1)
                .clip(CircleShape)
                .background(OrangePrimary.copy(alpha = 0.3f))
        )
        
        // Second animated circle
        Box(
            modifier = Modifier
                .size((170.dp * scale2))
                .alpha(alpha2)
                .clip(CircleShape)
                .background(OrangePrimary.copy(alpha = 0.25f))
        )
        
        // Third animated circle
        Box(
            modifier = Modifier
                .size((170.dp * scale3))
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
