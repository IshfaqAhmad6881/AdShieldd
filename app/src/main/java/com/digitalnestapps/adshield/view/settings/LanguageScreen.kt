package com.digitalnestapps.adshield.view.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.ripple
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalnestapps.adshield.R
import com.digitalnestapps.adshield.ui.components.AdShieldScreenInsets
import com.digitalnestapps.adshield.ui.components.AdShieldTopBarRow
import com.digitalnestapps.adshield.localization.AppLanguageManager
import com.digitalnestapps.adshield.ui.theme.DarkTextDark
import com.digitalnestapps.adshield.ui.theme.DarkTextLight
import com.digitalnestapps.adshield.ui.theme.TextDark
import com.digitalnestapps.adshield.ui.theme.OrangePrimary
import com.digitalnestapps.adshield.ui.theme.PremiumOrange
import com.digitalnestapps.adshield.ui.theme.TextLight
import com.digitalnestapps.adshield.ui.theme.adShieldScreenBackgroundBrush

@Composable
fun LanguageScreen(
    isDarkMode: Boolean,
    selectedLanguageTag: String,
    onLanguageSelected: (String) -> Unit,
    onBackClick: () -> Unit
) {
    val textDark = if (isDarkMode) DarkTextDark else TextDark
    val textLight = if (isDarkMode) DarkTextLight else TextLight
    val themeAccent = if (isDarkMode) OrangePrimary else PremiumOrange
    val scrollState = rememberScrollState()

    var pendingLanguage by remember { mutableStateOf<Pair<String, String>?>(null) }

    pendingLanguage?.let { (tag, displayName) ->
        AlertDialog(
            onDismissRequest = { pendingLanguage = null },
            title = { Text(stringResource(R.string.language_change_confirm_title)) },
            text = {
                Text(stringResource(R.string.language_change_confirm_message, displayName))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onLanguageSelected(tag)
                        pendingLanguage = null
                    }
                ) {
                    Text(stringResource(R.string.common_yes), color = themeAccent, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingLanguage = null }) {
                    Text(stringResource(R.string.common_no), color = textLight)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(adShieldScreenBackgroundBrush(isDarkMode))
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
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
                        text = stringResource(R.string.language_screen_title),
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        color = textDark
                    )
                    Text(
                        text = stringResource(R.string.language_screen_subtitle),
                        fontSize = 12.sp,
                        color = textLight
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            val rowShape = RoundedCornerShape(18.dp)
            AppLanguageManager.supportedLanguages.forEach { language ->
                val selected = language.tag == selectedLanguageTag
                val elev = if (selected) 16.dp else 11.dp
                val spot = if (selected) themeAccent.copy(alpha = 0.65f) else themeAccent.copy(alpha = 0.35f)
                val ambient = Color.Black.copy(alpha = if (isDarkMode) 0.55f else 0.12f)
                val rowBg = if (isDarkMode) Color.Black else Color(0xFFF7F5F2)
                val nameColor = if (isDarkMode) Color.White else textDark
                val borderColor = when {
                    selected -> themeAccent
                    isDarkMode -> Color.White.copy(alpha = 0.2f)
                    else -> Color(0xFFE8E4E0)
                }
                val radioUnselected = if (isDarkMode) {
                    Color.White.copy(alpha = 0.45f)
                } else {
                    textLight.copy(alpha = 0.85f)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .shadow(
                            elevation = elev,
                            shape = rowShape,
                            clip = false,
                            spotColor = spot,
                            ambientColor = ambient
                        )
                        .clip(rowShape)
                        .background(rowBg)
                        .border(
                            width = if (selected) 2.dp else 1.dp,
                            color = borderColor,
                            shape = rowShape
                        )
                        .clickable(
                            interactionSource = remember(language.tag) { MutableInteractionSource() },
                            indication = ripple(color = themeAccent.copy(alpha = 0.3f)),
                            onClick = { pendingLanguage = language.tag to language.name }
                        )
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = language.name,
                        modifier = Modifier.weight(1f),
                        fontSize = 16.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = nameColor
                    )
                    RadioButton(
                        selected = selected,
                        onClick = { pendingLanguage = language.tag to language.name },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = themeAccent,
                            unselectedColor = radioUnselected
                        )
                    )
                }
            }
        }
    }
}
