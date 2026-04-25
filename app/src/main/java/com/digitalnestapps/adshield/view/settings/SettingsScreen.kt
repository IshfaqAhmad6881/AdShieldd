package com.digitalnestapps.adshield.view.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalnestapps.adshield.R
import com.digitalnestapps.adshield.model.SettingsState
import com.digitalnestapps.adshield.ui.components.AdShieldScreenInsets
import com.digitalnestapps.adshield.ui.components.AdShieldTopBarRow
import com.digitalnestapps.adshield.ui.theme.*

@Composable
fun SettingsScreen(
    settingsState: SettingsState,
    progressPercentage: Float,
    isDarkMode: Boolean,
    onBackClick: () -> Unit = {},
    onPreferencesClick: () -> Unit = {},
    onSpeedTestClick: () -> Unit = {},
    onReferFriendsClick: () -> Unit = {},
    onLanguageClick: () -> Unit = {},
    onAdvancedClick: () -> Unit = {},
    onFeedbackClick: () -> Unit = {}
) {
    val cardBackgroundColor = if (isDarkMode) DarkBackgroundWhite else BackgroundWhite
    val textDarkColor = if (isDarkMode) DarkTextDark else TextDark
    val textLightColor = if (isDarkMode) DarkTextLight else TextLight
    val borderColor = if (isDarkMode) Color.White.copy(alpha = 0.1f) else Color.LightGray.copy(alpha = 0.3f)
    val menuIconTint = if (isDarkMode) OrangePrimary else PremiumOrange

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(adShieldScreenBackgroundBrush(isDarkMode))
            .statusBarsPadding()
    ) {
        val scrollState = rememberScrollState()
        Column(
            modifier = Modifier
                .weight(1f, fill = true)
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AdShieldScreenInsets.headerHorizontal)
            ) {
                Spacer(modifier = Modifier.height(AdShieldScreenInsets.belowStatusBar))

                AdShieldTopBarRow(
                    isDarkMode = isDarkMode,
                    onBackClick = onBackClick
                )

                Spacer(modifier = Modifier.height(24.dp))

                CircularProgressSection(
                    timer = settingsState.timer,
                    progressPercentage = progressPercentage,
                    textDarkColor = textDarkColor,
                    textLightColor = textLightColor,
                    cardBackgroundColor = cardBackgroundColor
                )

                Spacer(modifier = Modifier.height(16.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.settings_secured_until),
                        fontSize = 14.sp,
                        color = textLightColor
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.settings_connection_expires_in, settingsState.timer),
                        fontSize = 14.sp,
                        color = textLightColor
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                MenuItem(
                    iconResId = R.drawable.preferences,
                    title = stringResource(R.string.settings_preferences),
                    caption = stringResource(R.string.settings_preferences_caption),
                    onClick = onPreferencesClick,
                    cardBackgroundColor = cardBackgroundColor,
                    textDarkColor = textDarkColor,
                    textLightColor = textLightColor,
                    borderColor = borderColor,
                    iconTint = menuIconTint
                )
                CustomSeparator()
                MenuItem(
                    iconResId = R.drawable.speed,
                    title = stringResource(R.string.settings_speed_test),
                    caption = stringResource(R.string.settings_speed_test_caption),
                    onClick = onSpeedTestClick,
                    cardBackgroundColor = cardBackgroundColor,
                    textDarkColor = textDarkColor,
                    textLightColor = textLightColor,
                    borderColor = borderColor,
                    iconTint = menuIconTint
                )
                CustomSeparator()
                MenuItem(
                    iconResId = R.drawable.ic_globe,
                    title = stringResource(R.string.settings_language),
                    caption = stringResource(R.string.settings_language_caption),
                    onClick = onLanguageClick,
                    cardBackgroundColor = cardBackgroundColor,
                    textDarkColor = textDarkColor,
                    textLightColor = textLightColor,
                    borderColor = borderColor,
                    iconTint = menuIconTint
                )
                CustomSeparator()
                MenuItem(
                    iconResId = R.drawable.menu,
                    title = stringResource(R.string.settings_advanced),
                    caption = stringResource(R.string.settings_advanced_caption),
                    onClick = onAdvancedClick,
                    cardBackgroundColor = cardBackgroundColor,
                    textDarkColor = textDarkColor,
                    textLightColor = textLightColor,
                    borderColor = borderColor,
                    iconTint = menuIconTint
                )
                CustomSeparator()
                MenuItem(
                    iconResId = R.drawable.gift,
                    title = stringResource(R.string.settings_refer_friends),
                    caption = stringResource(R.string.settings_refer_friends_caption),
                    onClick = onReferFriendsClick,
                    cardBackgroundColor = cardBackgroundColor,
                    textDarkColor = textDarkColor,
                    textLightColor = textLightColor,
                    borderColor = borderColor,
                    iconTint = menuIconTint
                )
                CustomSeparator()
                MenuItem(
                    iconResId = R.drawable.ic_feedback,
                    title = stringResource(R.string.settings_feedback),
                    caption = stringResource(R.string.settings_feedback_caption),
                    onClick = onFeedbackClick,
                    cardBackgroundColor = cardBackgroundColor,
                    textDarkColor = textDarkColor,
                    textLightColor = textLightColor,
                    borderColor = borderColor,
                    iconTint = menuIconTint
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.settings_app_version, settingsState.appVersion),
                fontSize = 12.sp,
                color = textLightColor,
                style = MaterialTheme.typography.bodySmall
            )
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
                    progress = { progressPercentage },
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
                        text = stringResource(R.string.settings_expire),
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
                        text = stringResource(R.string.settings_remaining),
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
    borderColor: Color,
    iconTint: Color
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
            // Icon — theme accent (matches Advanced / Home)
            Image(
                painter = painterResource(id = iconResId),
                contentDescription = title,
                modifier = Modifier.size(24.dp),
                colorFilter = ColorFilter.tint(iconTint)
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
            contentDescription = stringResource(R.string.common_navigate),
            modifier = Modifier.size(40.dp),
            colorFilter = ColorFilter.tint(iconTint.copy(alpha = 0.85f))
        )
    }
}
