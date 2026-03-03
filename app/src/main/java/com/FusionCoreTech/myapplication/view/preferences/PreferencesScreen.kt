package com.FusionCoreTech.myapplication.view.preferences

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.FusionCoreTech.myapplication.ui.theme.*
import com.FusionCoreTech.myapplication.viewmodel.ThemeMode

@Composable
fun PreferencesScreen(
    isDarkMode: Boolean,
    themeMode: ThemeMode,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onBackClick: () -> Unit = {}
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
        Spacer(modifier = Modifier.height(32.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(cardBackgroundColor)
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                Spacer(modifier = Modifier.height(20.dp))

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
                        colors = CardDefaults.cardColors(containerColor = cardBackgroundColor)
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

                Spacer(modifier = Modifier.height(40.dp))

                Text(
                    text = "Preferences",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = textDarkColor,
                    modifier = Modifier.padding(start = 10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Appearance",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = textDarkColor,
                    modifier = Modifier.padding(start = 10.dp, top = 24.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))
            }

            ThemeModeSelector(
                themeMode = themeMode,
                onThemeModeSelected = onThemeModeSelected,
                textDarkColor = textDarkColor,
                textLightColor = textLightColor,
                borderColor = borderColor,
                cardBackgroundColor = cardBackgroundColor
            )
        }
    }
}

@Composable
fun ThemeModeSelector(
    themeMode: ThemeMode,
    onThemeModeSelected: (ThemeMode) -> Unit,
    textDarkColor: Color,
    textLightColor: Color,
    borderColor: Color,
    cardBackgroundColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        listOf(
            ThemeMode.System to "Follow system",
            ThemeMode.Light to "Light mode",
            ThemeMode.Dark to "Dark mode"
        ).forEach { (mode, label) ->
            val selected = themeMode == mode
            val subtitle = when (mode) {
                ThemeMode.System -> "Use device theme (light or dark)"
                ThemeMode.Light -> "Always use light theme"
                ThemeMode.Dark -> "Always use dark theme"
            }
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .then(
                        if (selected) Modifier.border(2.dp, OrangePrimary, RoundedCornerShape(12.dp))
                        else Modifier.border(1.dp, borderColor, RoundedCornerShape(12.dp))
                    )
                    .clickable { onThemeModeSelected(mode) },
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
                    RadioButton(
                        selected = selected,
                        onClick = { onThemeModeSelected(mode) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = OrangePrimary,
                            unselectedColor = borderColor
                        )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = label,
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
                }
            }
        }
    }
}
