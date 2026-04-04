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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.FusionCoreTech.myapplication.R
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
Settings and preferences are stored locally on your device. Before the first VPN-style DNS connection, the app shows a separate in-app notice explaining why Android’s VPN permission is used (this is required by Google Play for VpnService). While you are connected, Android requires us to show an ongoing notification so the DNS/VPN protection service can keep running in the background. That notice means you are still protected; you should disconnect inside the app before expecting it to go away—on many devices it cannot be dismissed safely until then.

4. Permissions
We request: VPN (system approval when you connect) to route DNS; notification permission (Android 13+) so you can see that required connection status while the service runs in the background; approximate or precise location only in the foreground where the system requires it to show Wi‑Fi network name in Advanced diagnostics; network and Wi‑Fi state to display connectivity; optional Advertising ID related use per Google Play / AdMob policies for ads; and Play Billing to process purchases. Core connection and speed test are not gated behind watching ads. Sensitive data is not sold. Optional Private DNS automation may describe an ADB developer grant that is not available from the Play install flow.

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
    val textDark = if (isDarkMode) DarkTextDark else TextDark
    val textLight = if (isDarkMode) DarkTextLight else TextLight
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(adShieldScreenBackgroundBrush(isDarkMode))
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(24.dp)
        ) {
            Text(
                text = stringResource(R.string.terms_of_use_title),
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
                text = stringResource(R.string.privacy_policy_title),
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
                text = stringResource(R.string.terms_accept_all),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
        }
    }
}
