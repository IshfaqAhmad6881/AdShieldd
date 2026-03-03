package com.FusionCoreTech.myapplication.view.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.FusionCoreTech.myapplication.R
import com.FusionCoreTech.myapplication.model.SettingsState
import com.FusionCoreTech.myapplication.ui.theme.*

@Composable
fun SettingsScreen(
    settingsState: SettingsState,
    progressPercentage: Float,
    isDarkMode: Boolean,
    onBackClick: () -> Unit = {},
    onPreferencesClick: () -> Unit = {},
    onSpeedTestClick: () -> Unit = {},
    onReferFriendsClick: () -> Unit = {}
) {
    val backgroundColor = if (isDarkMode) DarkBackgroundGrey else BackgroundGrey
    val cardBackgroundColor = if (isDarkMode) DarkBackgroundWhite else BackgroundWhite
    val textDarkColor = if (isDarkMode) DarkTextDark else TextDark
    val textLightColor = if (isDarkMode) DarkTextLight else TextLight
    val borderColor = if (isDarkMode) Color.White.copy(alpha = 0.1f) else Color.LightGray.copy(alpha = 0.3f)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        // Status bar spacer
        Spacer(modifier = Modifier.height(32.dp))

        // Main Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(cardBackgroundColor)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
        ) {
            // Content wrapper with horizontal padding
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Back Button - White rounded rectangle with leftbtn image and shadow
            // Positioned slightly right and down
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, top = 20.dp)
            ) {
                Card(
                    modifier = Modifier
                        .width(48.dp)
                        .height(48.dp)
                        .clickable { onBackClick() },
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 4.dp,
                        pressedElevation = 2.dp
                    ),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.leftbtn),
                            contentDescription = "Back",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Circular Progress Indicator with Timer
            CircularProgressSection(
                timer = settingsState.timer,
                progressPercentage = progressPercentage,
                textDarkColor = textDarkColor,
                textLightColor = textLightColor,
                cardBackgroundColor = cardBackgroundColor
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Timer Status Text
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Your are secured Until",
                    fontSize = 14.sp,
                    color = textLightColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Connection expires in ${settingsState.timer}",
                    fontSize = 14.sp,
                    color = textLightColor
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
            }

            // Menu Items List with Full-Width Separators
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                MenuItem(
                    iconResId = R.drawable.preferences,
                    title = "Preferences",
                    caption = "Customize your settings",
                    onClick = onPreferencesClick,
                    cardBackgroundColor = cardBackgroundColor,
                    textDarkColor = textDarkColor,
                    textLightColor = textLightColor,
                    borderColor = borderColor
                )
                CustomSeparator()
                MenuItem(
                    iconResId = R.drawable.speed,
                    title = "Speed Test",
                    caption = "Check your connection speed",
                    onClick = onSpeedTestClick,
                    cardBackgroundColor = cardBackgroundColor,
                    textDarkColor = textDarkColor,
                    textLightColor = textLightColor,
                    borderColor = borderColor
                )
                CustomSeparator()
                MenuItem(
                    iconResId = R.drawable.gift,
                    title = "Refer Friends",
                    caption = "Share with your friend",
                    onClick = onReferFriendsClick,
                    cardBackgroundColor = cardBackgroundColor,
                    textDarkColor = textDarkColor,
                    textLightColor = textLightColor,
                    borderColor = borderColor
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // App Version Footer - Centered
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "App version ${settingsState.appVersion}",
                    fontSize = 12.sp,
                    color = textLightColor,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
fun CustomSeparator() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .padding(start = 50.dp) // 30dp + 20dp = 50dp left margin, right side attached to parent
            .background(Color(0xFF757575)) // Dark grey color
    )
}

@Composable
fun CircularProgressSection(
    timer: String,
    progressPercentage: Float,
    textDarkColor: Color,
    textLightColor: Color,
    cardBackgroundColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp),
        contentAlignment = Alignment.Center
    ) {
        // Outer Circle with Shadow/Popup Effect and Ripple (like power button)
        Card(
            modifier = Modifier.size(200.dp),
            shape = CircleShape,
            elevation = CardDefaults.cardElevation(
                defaultElevation = 12.dp,
                pressedElevation = 8.dp
            ),
            colors = CardDefaults.cardColors(containerColor = cardBackgroundColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .clickable { },
                contentAlignment = Alignment.Center
            ) {
                // Progress Arc using CircularProgressIndicator
                CircularProgressIndicator(
                    progress = progressPercentage,
                    modifier = Modifier.size(200.dp),
                    color = OrangePrimary,
                    strokeWidth = 8.dp,
                    trackColor = OuterRingGrey
                )

                // Inner Content - Timer Display
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Expire",
                        fontSize = 14.sp,
                        color = textLightColor,
                        fontWeight = FontWeight.Normal
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = timer,
                        fontSize = 32.sp,
                        color = textDarkColor,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Remaining",
                        fontSize = 14.sp,
                        color = textLightColor,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun MenuItem(
    iconResId: Int,
    title: String,
    caption: String,
    onClick: () -> Unit,
    cardBackgroundColor: Color,
    textDarkColor: Color,
    textLightColor: Color,
    borderColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 18.dp, horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Left side: Icon + Text
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Icon - Orange colored
            Image(
                painter = painterResource(id = iconResId),
                contentDescription = title,
                modifier = Modifier.size(24.dp)
            )

            // Text Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textDarkColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = caption,
                    fontSize = 12.sp,
                    color = textLightColor
                )
            }
        }

        // Right side: Arrow Icon
        Image(
            painter = painterResource(id = R.drawable.forward),
            contentDescription = "Navigate",
            modifier = Modifier.size(40.dp)
        )
    }
}
