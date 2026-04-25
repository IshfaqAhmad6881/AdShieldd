package com.digitalnestapps.adshield.view.premium

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalnestapps.adshield.R
import com.digitalnestapps.adshield.billing.BillingManager
import com.digitalnestapps.adshield.billing.PremiumProductIds
import com.digitalnestapps.adshield.ui.components.AdShieldScreenInsets
import com.digitalnestapps.adshield.ui.components.AdShieldTopBarRow
import com.digitalnestapps.adshield.ui.theme.BackgroundGrey
import com.digitalnestapps.adshield.ui.theme.DarkBackgroundGrey
import com.digitalnestapps.adshield.ui.theme.adShieldScreenBackgroundBrush
import com.digitalnestapps.adshield.ui.theme.DarkBackgroundWhite
import com.digitalnestapps.adshield.ui.theme.DarkTextDark
import com.digitalnestapps.adshield.ui.theme.DarkTextLight
import com.digitalnestapps.adshield.ui.theme.OrangePrimary
import com.digitalnestapps.adshield.ui.theme.PremiumOrange
import com.digitalnestapps.adshield.ui.theme.TextDark
import com.digitalnestapps.adshield.ui.theme.TextLight

private const val TAG = "PremiumScreen"

@Composable
private fun premiumBillingPeriodSuffix(iso: String?): String {
    if (iso.isNullOrBlank()) return ""
    return when (iso) {
        "P1M", "P1m" -> stringResource(R.string.premium_price_suffix_month)
        "P1Y", "P1y" -> stringResource(R.string.premium_period_suffix_year)
        "P1W", "P1w" -> stringResource(R.string.premium_period_suffix_week)
        "P3M", "P3m", "P6M", "P6m" -> stringResource(R.string.premium_period_suffix_quarter)
        else -> ""
    }
}

@Composable
private fun premiumLocalizedPrice(billingManager: BillingManager, productId: String): String {
    val base = billingManager.getFormattedPrice(productId).trim()
    if (base.isEmpty()) return "—"
    val iso = billingManager.getRecurringBillingPeriodIso(productId)
    return base + premiumBillingPeriodSuffix(iso)
}

@Composable
fun PremiumScreen(
    isDarkMode: Boolean,
    billingManager: BillingManager,
    onBackClick: () -> Unit,
    onPurchaseSuccess: () -> Unit = {},
    onPurchaseError: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val accent = if (isDarkMode) OrangePrimary else PremiumOrange
    val heroFadeBottom = if (isDarkMode) DarkBackgroundGrey else BackgroundGrey
    val cardBg = if (isDarkMode) DarkBackgroundWhite else Color.White
    val textDark = if (isDarkMode) DarkTextDark else TextDark
    val textLight = if (isDarkMode) DarkTextLight else TextLight
    val borderColor = if (isDarkMode) Color.White.copy(alpha = 0.12f) else Color(0xFFE0E0E0)

    val connected by billingManager.connectionState.collectAsState(initial = false)
    val productMap by billingManager.productDetails.collectAsState(initial = emptyMap())
    val isPremium by billingManager.isPremium.collectAsState(initial = false)

    var selectedPlan by remember { mutableStateOf(PremiumProductIds.ANNUAL) }
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        try {
            billingManager.startConnection()
        } catch (e: Exception) {
            Log.e(TAG, "Billing startConnection failed", e)
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            try {
                billingManager.endConnection()
            } catch (e: Exception) {
                Log.e(TAG, "Billing endConnection failed", e)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(adShieldScreenBackgroundBrush(isDarkMode))
            .statusBarsPadding()
            .verticalScroll(scrollState)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accent,
                            accent.copy(alpha = 0.85f),
                            heroFadeBottom
                        )
                    )
                )
                .padding(horizontal = AdShieldScreenInsets.headerHorizontal)
                .padding(top = 0.dp, bottom = 18.dp)
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
                        text = stringResource(R.string.home_go_premium),
                        color = Color.White,
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.premium_subtitle),
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(14.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.premium),
                        contentDescription = null,
                        modifier = Modifier.size(44.dp)
                    )
                }
                if (!isPremium && connected && productMap.isNotEmpty()) {
                    val annualLine = premiumLocalizedPrice(billingManager, PremiumProductIds.ANNUAL)
                    val monthlyLine = premiumLocalizedPrice(billingManager, PremiumProductIds.MONTHLY)
                    if (annualLine != "—" || monthlyLine != "—") {
                        Spacer(modifier = Modifier.height(14.dp))
                        if (annualLine != "—") {
                            Text(
                                text = stringResource(R.string.premium_hero_line_yearly, annualLine),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                        if (monthlyLine != "—") {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = stringResource(R.string.premium_hero_line_monthly, monthlyLine),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White.copy(alpha = 0.92f),
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.premium_play_ready),
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.82f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        ) {
            if (!connected) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = accent
                    )
                    Spacer(modifier = Modifier.size(10.dp))
                    Text(
                        text = stringResource(R.string.premium_connecting_play),
                        fontSize = 13.sp,
                        color = textLight
                    )
                }
            } else if (productMap.isEmpty()) {
                Text(
                    text = stringResource(R.string.premium_play_ready),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = accent,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            if (isPremium) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.12f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, accent.copy(alpha = 0.4f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.premium_already_title),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = textDark
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.premium_already_subtitle),
                            fontSize = 14.sp,
                            color = textLight,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onBackClick,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = accent)
                        ) {
                            Text(stringResource(R.string.common_ok), color = Color.White)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = stringResource(R.string.premium_console_hint),
                    fontSize = 11.sp,
                    color = textLight.copy(alpha = 0.75f)
                )
                Spacer(modifier = Modifier.height(32.dp))
            } else {
                if (productMap.isEmpty() && connected) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = accent,
                        trackColor = borderColor
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.premium_console_hint),
                        fontSize = 12.sp,
                        color = textLight
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text(
                    text = stringResource(R.string.premium_select_plan),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = textDark,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                PlanOptionRow(
                    selected = selectedPlan == PremiumProductIds.ANNUAL,
                    title = stringResource(R.string.premium_plan_yearly),
                    subtitle = stringResource(R.string.premium_billed_yearly),
                    price = premiumLocalizedPrice(billingManager, PremiumProductIds.ANNUAL),
                    badge = stringResource(R.string.premium_best_value),
                    accent = accent,
                    cardBg = cardBg,
                    textDark = textDark,
                    textLight = textLight,
                    borderColor = borderColor,
                    onSelect = { selectedPlan = PremiumProductIds.ANNUAL }
                )
                Spacer(modifier = Modifier.height(12.dp))
                PlanOptionRow(
                    selected = selectedPlan == PremiumProductIds.MONTHLY,
                    title = stringResource(R.string.premium_plan_monthly),
                    subtitle = stringResource(R.string.premium_billed_monthly),
                    price = premiumLocalizedPrice(billingManager, PremiumProductIds.MONTHLY),
                    badge = null,
                    accent = accent,
                    cardBg = cardBg,
                    textDark = textDark,
                    textLight = textLight,
                    borderColor = borderColor,
                    onSelect = { selectedPlan = PremiumProductIds.MONTHLY }
                )

                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        try {
                            billingManager.launchPurchaseFlow(
                                selectedPlan,
                                onSuccess = { onPurchaseSuccess() },
                                onError = { msg -> onPurchaseError(msg) }
                            )
                        } catch (e: Exception) {
                            Log.e(TAG, "launchPurchaseFlow", e)
                            onPurchaseError(e.message ?: "Purchase failed")
                        }
                    },
                    enabled = connected && productMap.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text = stringResource(R.string.premium_subscribe_now),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    color = cardBg,
                    tonalElevation = 0.dp,
                    shadowElevation = 2.dp,
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.premium_features),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = accent
                        )
                        FeatureRow(
                            icon = Icons.Filled.Star,
                            title = stringResource(R.string.premium_no_ads),
                            subtitle = stringResource(R.string.premium_no_ads_subtitle),
                            isDarkMode = isDarkMode
                        )
                        FeatureRow(
                            icon = Icons.Filled.Person,
                            title = stringResource(R.string.premium_vip_support),
                            subtitle = stringResource(R.string.premium_vip_support_subtitle),
                            isDarkMode = isDarkMode
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                TextButton(
                    onClick = {
                        try {
                            billingManager.restorePurchases { ok, err ->
                                when {
                                    ok -> onPurchaseSuccess()
                                    err.isNullOrBlank() -> Toast.makeText(
                                        context,
                                        context.getString(R.string.premium_restore_none),
                                        Toast.LENGTH_LONG
                                    ).show()

                                    else -> Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "restorePurchases", e)
                            Toast.makeText(context, e.message ?: "Error", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.premium_restore_purchase),
                        color = accent,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.premium_legal),
                    fontSize = 11.sp,
                    color = textLight.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    lineHeight = 15.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 32.dp)
                )
            }
        }
    }
}

@Composable
private fun PlanOptionRow(
    selected: Boolean,
    title: String,
    subtitle: String,
    price: String,
    badge: String?,
    accent: Color,
    cardBg: Color,
    textDark: Color,
    textLight: Color,
    borderColor: Color,
    onSelect: () -> Unit
) {
    val borderW = if (selected) 2.dp else 1.dp
    val borderC = if (selected) accent else borderColor
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(borderW, borderC, RoundedCornerShape(18.dp))
            .clickable { onSelect() },
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) accent.copy(alpha = 0.08f) else cardBg
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 3.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(selectedColor = accent, unselectedColor = textLight)
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = textDark
                    )
                    if (badge != null) {
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = badge,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier
                                .background(accent, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Text(text = subtitle, fontSize = 12.sp, color = textLight)
            }
            Text(
                text = price,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = textDark
            )
        }
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isDarkMode: Boolean
) {
    val textDark = if (isDarkMode) DarkTextDark else TextDark
    val textLight = if (isDarkMode) DarkTextLight else TextLight
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = PremiumOrange,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.size(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = textDark
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = textLight
            )
        }
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = null,
            tint = Color(0xFF2E7D32),
            modifier = Modifier.size(22.dp)
        )
    }
}
