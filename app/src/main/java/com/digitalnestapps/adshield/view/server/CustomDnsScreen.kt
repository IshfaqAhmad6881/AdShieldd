package com.digitalnestapps.adshield.view.server

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.digitalnestapps.adshield.R
import com.digitalnestapps.adshield.billing.isPremiumUnlocked
import com.digitalnestapps.adshield.dns.CustomDnsLimits
import com.digitalnestapps.adshield.dns.CustomDnsPrefs
import com.digitalnestapps.adshield.dns.CustomDnsProfile
import com.digitalnestapps.adshield.dns.DnsFormErrors
import com.digitalnestapps.adshield.dns.DnsInputValidation
import com.digitalnestapps.adshield.ui.components.AdShieldScreenInsets
import com.digitalnestapps.adshield.ui.components.AdShieldTopBarRow
import com.digitalnestapps.adshield.ui.theme.DarkBackgroundWhite
import com.digitalnestapps.adshield.ui.theme.DarkTextDark
import com.digitalnestapps.adshield.ui.theme.DarkTextLight
import com.digitalnestapps.adshield.ui.theme.BackgroundWhite
import com.digitalnestapps.adshield.ui.theme.OrangePrimary
import com.digitalnestapps.adshield.ui.theme.PremiumOrange
import com.digitalnestapps.adshield.ui.theme.TextDark
import com.digitalnestapps.adshield.ui.theme.TextLight
import android.widget.Toast
import java.util.UUID

@Composable
fun CustomDnsScreen(
    isDarkMode: Boolean,
    onBackClick: () -> Unit,
    onSaved: () -> Unit,
    onGoPremium: () -> Unit = {},
) {
    val context = LocalContext.current

    val accent = if (isDarkMode) OrangePrimary else PremiumOrange
    val cardBackgroundColor = if (isDarkMode) DarkBackgroundWhite else BackgroundWhite
    val textDarkColor = if (isDarkMode) DarkTextDark else TextDark
    val textLightColor = if (isDarkMode) DarkTextLight else TextLight
    val borderSubtle = if (isDarkMode) Color.White.copy(alpha = 0.12f) else Color(0xFFE0E0E0)

    var listVersion by remember { mutableIntStateOf(0) }
    val profiles = remember(listVersion) { CustomDnsPrefs.getProfiles(context) }
    val activeId = remember(listVersion) { CustomDnsPrefs.getActiveProfileId(context) }

    var profileLabel by remember { mutableStateOf("") }
    var hostname by remember { mutableStateOf("") }
    var primaryIp by remember { mutableStateOf("") }
    var secondaryIp by remember { mutableStateOf("") }
    var formErrors by remember { mutableStateOf<DnsFormErrors?>(null) }

    var deleteTarget by remember { mutableStateOf<CustomDnsProfile?>(null) }
    var showSaveConfirm by remember { mutableStateOf(false) }
    var showPremiumLimit by remember { mutableStateOf(false) }

    @Composable
    fun fieldColors() = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = accent,
        focusedLabelColor = accent,
        cursorColor = accent,
        focusedTextColor = textDarkColor,
        unfocusedTextColor = textDarkColor,
        unfocusedBorderColor = textLightColor.copy(alpha = 0.45f),
        errorBorderColor = MaterialTheme.colorScheme.error,
        errorLabelColor = MaterialTheme.colorScheme.error,
        errorSupportingTextColor = MaterialTheme.colorScheme.error
    )

    fun reloadList() {
        listVersion++
    }

    fun canAddAnotherProfile(): Boolean =
        isPremiumUnlocked(context) || profiles.size < CustomDnsLimits.FREE_MAX_PROFILES

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.custom_dns_delete_confirm_title)) },
            text = { Text(stringResource(R.string.custom_dns_delete_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        CustomDnsPrefs.deleteProfile(context, target.id)
                        reloadList()
                        deleteTarget = null
                    }
                ) {
                    Text(stringResource(R.string.custom_dns_delete), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) {
                    Text(stringResource(R.string.common_no))
                }
            }
        )
    }

    if (showPremiumLimit) {
        AlertDialog(
            onDismissRequest = { showPremiumLimit = false },
            title = { Text(stringResource(R.string.custom_dns_premium_limit_title)) },
            text = { Text(stringResource(R.string.custom_dns_premium_limit_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showPremiumLimit = false
                        onGoPremium()
                    }
                ) {
                    Text(stringResource(R.string.home_go_premium), color = accent, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showPremiumLimit = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    if (showSaveConfirm) {
        AlertDialog(
            onDismissRequest = { showSaveConfirm = false },
            title = { Text(stringResource(R.string.custom_dns_save_confirm_title)) },
            text = { Text(stringResource(R.string.custom_dns_save_confirm_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showSaveConfirm = false
                        if (!canAddAnotherProfile()) {
                            showPremiumLimit = true
                            return@TextButton
                        }
                        val v = DnsInputValidation.validateProfile(hostname, primaryIp, secondaryIp)
                        formErrors = if (v.ok) null else v
                        if (!v.ok) return@TextButton
                        val label = profileLabel.trim().ifBlank {
                            hostname.trim().ifBlank { primaryIp.trim().ifBlank { "Custom DNS" } }
                        }
                        val profile = CustomDnsProfile(
                            id = UUID.randomUUID().toString(),
                            label = label,
                            hostname = hostname.trim().takeIf { it.isNotEmpty() },
                            primaryIp = primaryIp.trim().takeIf { it.isNotEmpty() },
                            secondaryIp = secondaryIp.trim().takeIf { it.isNotEmpty() }
                        )
                        CustomDnsPrefs.addProfile(context, profile)
                        profileLabel = ""
                        hostname = ""
                        primaryIp = ""
                        secondaryIp = ""
                        reloadList()
                        Toast.makeText(
                            context,
                            context.getString(R.string.custom_dns_saved_toast),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                ) {
                    Text(stringResource(R.string.common_confirm), color = accent, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveConfirm = false }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(cardBackgroundColor)
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    accent.copy(alpha = if (isDarkMode) 0.14f else 0.10f),
                                    Color.Transparent
                                )
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .padding(horizontal = AdShieldScreenInsets.headerHorizontal)
                            .padding(top = AdShieldScreenInsets.belowStatusBar, bottom = 8.dp)
                    ) {
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
                                    text = stringResource(R.string.custom_dns_title),
                                    fontSize = 25.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textDarkColor
                                )
                                Text(
                                    text = stringResource(R.string.custom_dns_subtitle),
                                    fontSize = 12.sp,
                                    color = textLightColor,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = AdShieldScreenInsets.headerHorizontal)
                        .padding(bottom = 24.dp)
                ) {
                    OutlinedTextField(
                        value = profileLabel,
                        onValueChange = { profileLabel = it; formErrors = null },
                        label = { Text(stringResource(R.string.custom_dns_profile_label)) },
                        placeholder = { Text(stringResource(R.string.custom_dns_profile_label_placeholder)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = hostname,
                        onValueChange = { hostname = it; formErrors = null },
                        label = { Text(stringResource(R.string.custom_dns_hostname_label)) },
                        placeholder = { Text(stringResource(R.string.custom_dns_hostname_placeholder)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = formErrors?.hostnameInvalid == true || formErrors?.needHostnameOrPrimary == true,
                        supportingText = {
                            when {
                                formErrors?.hostnameInvalid == true ->
                                    Text(stringResource(R.string.custom_dns_validation_hostname))
                                formErrors?.needHostnameOrPrimary == true ->
                                    Text(stringResource(R.string.custom_dns_validation_need_one))
                                else -> {}
                            }
                        },
                        colors = fieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = primaryIp,
                        onValueChange = { primaryIp = it; formErrors = null },
                        label = { Text(stringResource(R.string.custom_dns_primary_ip_label)) },
                        placeholder = { Text(stringResource(R.string.custom_dns_primary_ip_placeholder)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = formErrors?.primaryInvalid == true || formErrors?.needHostnameOrPrimary == true,
                        supportingText = {
                            when {
                                formErrors?.primaryInvalid == true ->
                                    Text(stringResource(R.string.custom_dns_validation_ip))
                                else -> {}
                            }
                        },
                        colors = fieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = secondaryIp,
                        onValueChange = { secondaryIp = it; formErrors = null },
                        label = { Text(stringResource(R.string.custom_dns_secondary_ip_label)) },
                        placeholder = { Text(stringResource(R.string.custom_dns_secondary_ip_placeholder)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        isError = formErrors?.secondaryInvalid == true,
                        supportingText = {
                            if (formErrors?.secondaryInvalid == true) {
                                Text(stringResource(R.string.custom_dns_validation_ip))
                            }
                        },
                        colors = fieldColors(),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = {
                            val v = DnsInputValidation.validateProfile(hostname, primaryIp, secondaryIp)
                            formErrors = if (v.ok) null else v
                            if (!v.ok) return@Button
                            if (!canAddAnotherProfile()) {
                                showPremiumLimit = true
                                return@Button
                            }
                            showSaveConfirm = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accent)
                    ) {
                        Text(
                            stringResource(R.string.common_save),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }

                    if (profiles.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(28.dp))
                        Text(
                            text = stringResource(R.string.custom_dns_saved_profiles),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = textDarkColor
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        profiles.forEach { pr ->
                            val isActive = pr.id == activeId || (activeId == null && pr == profiles.first())
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp)
                                    .then(
                                        if (isActive) Modifier.border(2.dp, accent, RoundedCornerShape(16.dp))
                                        else Modifier.border(1.dp, borderSubtle, RoundedCornerShape(16.dp))
                                    ),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDarkMode) Color(0xFF252525) else Color(0xFFF8F8F8)
                                ),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = pr.label,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = textDarkColor
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = buildString {
                                                    pr.hostname?.let { append(it) }
                                                    pr.primaryIp?.let {
                                                        if (isNotEmpty()) append(" · ")
                                                        append(it)
                                                    }
                                                    pr.secondaryIp?.let {
                                                        if (isNotEmpty()) append(", ")
                                                        append(it)
                                                    }
                                                }.ifBlank { "—" },
                                                fontSize = 12.sp,
                                                color = textLightColor
                                            )
                                        }
                                        if (isActive) {
                                            Text(
                                                text = stringResource(R.string.custom_dns_active_badge),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = accent,
                                                modifier = Modifier
                                                    .background(accent.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                        IconButton(onClick = { deleteTarget = pr }) {
                                            Icon(
                                                imageVector = Icons.Filled.Delete,
                                                contentDescription = stringResource(R.string.custom_dns_delete),
                                                tint = textLightColor
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    HorizontalDivider(color = borderSubtle)
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedButton(
                                        onClick = {
                                            CustomDnsPrefs.setActiveProfile(context, pr.id)
                                            reloadList()
                                            onSaved()
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, accent),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = accent)
                                    ) {
                                        Text(
                                            stringResource(R.string.custom_dns_use_this),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
    }
}
