package com.FusionCoreTech.myapplication.view.terms

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.FusionCoreTech.myapplication.ui.theme.*

private const val TERMS_TEXT = """
Terms of Use

Welcome to AdShield. By using this app you agree to the following:

1. Use of Service
AdShield provides DNS-based ad blocking and privacy features. You use the service at your own responsibility.

2. Acceptable Use
You agree not to use the app for any illegal or unauthorized purpose. You must comply with all applicable laws.

3. Disclaimer
The service is provided "as is". We do not guarantee uninterrupted or error-free service. We are not liable for any damages arising from your use of the app.

4. Changes
We may update these terms from time to time. Continued use of the app after changes constitutes acceptance.

5. Contact
For questions about these terms, contact us through the app or our website.
"""

private const val PRIVACY_TEXT = """
Privacy Policy

Last updated: 2025

1. Information We Collect
AdShield may collect minimal usage data to improve the service. We do not sell your personal data.

2. DNS Queries
When you use our DNS/VPN feature, DNS queries are processed according to the DNS provider you select (e.g. AdGuard DNS). Please refer to their privacy policy for how they handle data.

3. Local Data
Settings and preferences are stored locally on your device. Connection status may be shown in notifications when you are connected.

4. Permissions
We request notification permission to show connection status. We request VPN permission to apply DNS when you connect. No other sensitive data is collected without your consent.

5. Security
We aim to protect your data with industry-standard practices. No personal data is transmitted to our servers except as needed for the service.

6. Children
Our service is not directed at children under 13. We do not knowingly collect data from children.

7. Changes
We may update this policy. We will notify you of significant changes through the app or by email if provided.

8. Contact
For privacy questions, contact us through the app or our website.
"""

@Composable
fun TermsAndPrivacyScreen(
    isDarkMode: Boolean,
    onAcceptToAll: () -> Unit
) {
    val bg = if (isDarkMode) DarkBackgroundGrey else BackgroundGrey
    val textDark = if (isDarkMode) DarkTextDark else TextDark
    val textLight = if (isDarkMode) DarkTextLight else TextLight
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bg)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(24.dp)
        ) {
            Text(
                text = "Terms of Use",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textDark
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = TERMS_TEXT.trim(),
                fontSize = 14.sp,
                color = textLight,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Privacy Policy",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textDark
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = PRIVACY_TEXT.trim(),
                fontSize = 14.sp,
                color = textLight,
                lineHeight = 22.sp
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
        Button(
            onClick = onAcceptToAll,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
                .height(52.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PremiumOrange),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp)
        ) {
            Text(
                text = "Accept to all",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}
