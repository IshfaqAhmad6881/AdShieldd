package com.digitalnestapps.adshield.view.vpn

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalnestapps.adshield.R
import com.digitalnestapps.adshield.ui.theme.DarkTextDark
import com.digitalnestapps.adshield.ui.theme.DarkTextLight
import com.digitalnestapps.adshield.ui.theme.PremiumOrange
import com.digitalnestapps.adshield.ui.theme.TextDark
import com.digitalnestapps.adshield.ui.theme.TextLight
import com.digitalnestapps.adshield.ui.theme.adShieldScreenBackgroundBrush

/**
 * Google Play requires a prominent, in-app disclosure for [android.net.VpnService] that is separate
 * from generic terms — see Play Console “VpnService policy” and User Data policy.
 */
@Composable
fun VpnProminentDisclosureScreen(
    isDarkMode: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit
) {
    val textDark = if (isDarkMode) DarkTextDark else TextDark
    val textLight = if (isDarkMode) DarkTextLight else TextLight
    val scroll = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(adShieldScreenBackgroundBrush(isDarkMode))
            .padding(24.dp)
    ) {
        Text(
            text = stringResource(R.string.vpn_prominent_title),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = textDark,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.vpn_prominent_body),
            fontSize = 15.sp,
            color = textLight,
            lineHeight = 22.sp,
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scroll)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onAccept,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PremiumOrange),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
        ) {
            Text(
                text = stringResource(R.string.vpn_prominent_accept),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        TextButton(
            onClick = onDecline,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.vpn_prominent_decline),
                color = textLight,
                fontSize = 15.sp
            )
        }
    }
}
