package com.FusionCoreTech.myapplication.view.preferences

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.border
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.FusionCoreTech.myapplication.R
import com.FusionCoreTech.myapplication.ui.components.AdShieldScreenInsets
import com.FusionCoreTech.myapplication.ui.components.AdShieldTopBarRow
import com.FusionCoreTech.myapplication.ui.theme.*
import com.FusionCoreTech.myapplication.viewmodel.ThemeMode

@Composable
private fun themeModeLabel(mode: ThemeMode): String = when (mode) {
    ThemeMode.System -> stringResource(R.string.preferences_theme_follow_system)
    ThemeMode.Light -> stringResource(R.string.preferences_theme_light_mode)
    ThemeMode.Dark -> stringResource(R.string.preferences_theme_dark_mode)
}

@Composable
private fun themeOptions(): List<Pair<ThemeMode, String>> = listOf(
    ThemeMode.System to stringResource(R.string.preferences_theme_follow_system),
    ThemeMode.Light to stringResource(R.string.preferences_theme_light_mode),
    ThemeMode.Dark to stringResource(R.string.preferences_theme_dark_mode)
)

@Composable
fun PreferencesScreen(
    isDarkMode: Boolean,
    themeMode: ThemeMode,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onBackClick: () -> Unit = {}
) {
    val cardBackgroundColor = if (isDarkMode) DarkBackgroundWhite else BackgroundWhite
    val textDarkColor = if (isDarkMode) DarkTextDark else TextDark
    val textLightColor = if (isDarkMode) DarkTextLight else TextLight
    val borderColor = if (isDarkMode) Color.White.copy(alpha = 0.1f) else Color.LightGray.copy(alpha = 0.3f)
    val accent = if (isDarkMode) OrangePrimary else PremiumOrange
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(adShieldScreenBackgroundBrush(isDarkMode))
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(horizontal = AdShieldScreenInsets.headerHorizontal)
                .padding(bottom = 28.dp)
        ) {
            Spacer(modifier = Modifier.height(AdShieldScreenInsets.belowStatusBar))
            AdShieldTopBarRow(
                isDarkMode = isDarkMode,
                onBackClick = onBackClick
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = AdShieldScreenInsets.backToTitleSpacing)
                ) {
                    Text(
                        text = stringResource(R.string.preferences_title),
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        color = textDarkColor
                    )
                    Text(
                        text = stringResource(R.string.settings_preferences_caption),
                        fontSize = 12.sp,
                        color = textLightColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(R.string.preferences_appearance),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = textDarkColor
            )
            Spacer(modifier = Modifier.height(12.dp))

            PreferenceRows(
                themeMode = themeMode,
                onThemeModeSelected = onThemeModeSelected,
                textDarkColor = textDarkColor,
                textLightColor = textLightColor,
                borderColor = borderColor,
                cardBackgroundColor = cardBackgroundColor,
                themeValueAccent = accent
            )
        }
    }
}

@Composable
private fun PreferenceRows(
    themeMode: ThemeMode,
    onThemeModeSelected: (ThemeMode) -> Unit,
    textDarkColor: Color,
    textLightColor: Color,
    borderColor: Color,
    cardBackgroundColor: Color,
    themeValueAccent: Color
) {
    var themeMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        ThemeRow(
            themeMode = themeMode,
            textDarkColor = textDarkColor,
            textLightColor = textLightColor,
            borderColor = borderColor,
            cardBackgroundColor = cardBackgroundColor,
            themeValueAccent = themeValueAccent,
            onRowClick = { themeMenuExpanded = true },
            dropdownExpanded = themeMenuExpanded,
            onDismiss = { themeMenuExpanded = false },
            onThemeSelected = { onThemeModeSelected(it); themeMenuExpanded = false }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Row 2: placeholder
        PreferenceRow(
            title = stringResource(R.string.preferences_notifications),
            subtitle = stringResource(R.string.preferences_notifications_subtitle),
            endLabel = stringResource(R.string.common_soon),
            textDarkColor = textDarkColor,
            textLightColor = textLightColor,
            borderColor = borderColor,
            cardBackgroundColor = cardBackgroundColor,
            onClick = { }
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Row 3: placeholder
        PreferenceRow(
            title = stringResource(R.string.preferences_data_storage),
            subtitle = stringResource(R.string.preferences_data_storage_subtitle),
            endLabel = stringResource(R.string.common_soon),
            textDarkColor = textDarkColor,
            textLightColor = textLightColor,
            borderColor = borderColor,
            cardBackgroundColor = cardBackgroundColor,
            onClick = { }
        )
    }
}

@Composable
private fun ThemeRow(
    themeMode: ThemeMode,
    textDarkColor: Color,
    textLightColor: Color,
    borderColor: Color,
    cardBackgroundColor: Color,
    themeValueAccent: Color,
    onRowClick: () -> Unit,
    dropdownExpanded: Boolean,
    onDismiss: () -> Unit,
    onThemeSelected: (ThemeMode) -> Unit
) {
    Box {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, borderColor, RoundedCornerShape(12.dp))
                .clickable { onRowClick() },
            shape = RoundedCornerShape(12.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 1.dp),
            colors = CardDefaults.cardColors(containerColor = cardBackgroundColor)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.preferences_theme),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = textDarkColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.preferences_theme_subtitle),
                        fontSize = 13.sp,
                        color = textLightColor
                    )
                }
                Text(
                    text = themeModeLabel(themeMode),
                    fontSize = 14.sp,
                    color = themeValueAccent,
                    modifier = Modifier.padding(end = 8.dp)
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = stringResource(R.string.preferences_open_options),
                    tint = textLightColor,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        DropdownMenu(
            expanded = dropdownExpanded,
            onDismissRequest = onDismiss,
            modifier = Modifier.widthIn(min = 200.dp)
        ) {
            themeOptions().forEach { (mode, label) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = label,
                            color = textDarkColor,
                            fontSize = 15.sp
                        )
                    },
                    onClick = { onThemeSelected(mode) }
                )
            }
        }
    }
}

@Composable
private fun PreferenceRow(
    title: String,
    subtitle: String,
    endLabel: String,
    textDarkColor: Color,
    textLightColor: Color,
    borderColor: Color,
    cardBackgroundColor: Color,
    onClick: () -> Unit
) {
        Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(enabled = false) { },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = textDarkColor
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = textLightColor
                )
            }
            Text(
                text = endLabel,
                fontSize = 13.sp,
                color = textLightColor,
                modifier = Modifier.padding(end = 8.dp)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = textLightColor.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
