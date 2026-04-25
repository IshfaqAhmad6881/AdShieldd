package com.digitalnestapps.adshield.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.digitalnestapps.adshield.R
import com.digitalnestapps.adshield.ui.theme.BackgroundWhite
import com.digitalnestapps.adshield.ui.theme.DarkBackgroundWhite
import com.digitalnestapps.adshield.ui.theme.OrangePrimary
import com.digitalnestapps.adshield.ui.theme.PremiumOrange

/**
 * Shared back control: light/dark sheet card background + theme accent icon (Premium orange / dark orange).
 */
@Composable
fun ThemeBackButton(
    isDarkMode: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBg = if (isDarkMode) DarkBackgroundWhite else BackgroundWhite
    val accent = if (isDarkMode) OrangePrimary else PremiumOrange
    val shape = RoundedCornerShape(12.dp)
    Card(
        modifier = modifier
            .size(48.dp)
            .shadow(
                elevation = 5.dp,
                shape = shape,
                spotColor = accent.copy(alpha = 0.32f),
                ambientColor = Color.Black.copy(alpha = if (isDarkMode) 0.35f else 0.12f)
            )
            .clickable { onClick() },
        shape = shape,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(R.drawable.leftbtn),
                contentDescription = stringResource(R.string.common_back),
                modifier = Modifier.size(24.dp),
                colorFilter = ColorFilter.tint(accent)
            )
        }
    }
}
