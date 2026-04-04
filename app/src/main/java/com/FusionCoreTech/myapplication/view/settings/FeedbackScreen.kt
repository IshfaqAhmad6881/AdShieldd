package com.FusionCoreTech.myapplication.view.settings

import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.FusionCoreTech.myapplication.R
import com.FusionCoreTech.myapplication.ui.components.AdShieldScreenInsets
import com.FusionCoreTech.myapplication.ui.components.AdShieldTopBarRow
import com.FusionCoreTech.myapplication.ui.theme.BackgroundWhite
import com.FusionCoreTech.myapplication.ui.theme.DarkBackgroundWhite
import com.FusionCoreTech.myapplication.ui.theme.DarkTextDark
import com.FusionCoreTech.myapplication.ui.theme.DarkTextLight
import com.FusionCoreTech.myapplication.ui.theme.OrangePrimary
import com.FusionCoreTech.myapplication.ui.theme.PremiumOrange
import com.FusionCoreTech.myapplication.ui.theme.TextDark
import com.FusionCoreTech.myapplication.ui.theme.TextLight
import com.FusionCoreTech.myapplication.ui.theme.adShieldScreenBackgroundBrush

private const val MIN_FEEDBACK_CHARS = 100

/** Long mailto: URIs fail on some devices; fall back to ACTION_SEND. */
private const val MAILTO_URI_SAFE_MAX_LENGTH = 4000

/**
 * Prefer [Intent.ACTION_SENDTO] with a mailto: URI so the **To** field is always filled
 * (many apps ignore [Intent.EXTRA_EMAIL] on ACTION_SEND).
 */
private fun buildFeedbackEmailIntent(
    email: String,
    subject: String,
    body: String
): Intent {
    val mailtoUriString = buildString {
        append("mailto:")
        append(email)
        append("?subject=")
        append(Uri.encode(subject))
        append("&body=")
        append(Uri.encode(body))
    }
    return if (mailtoUriString.length <= MAILTO_URI_SAFE_MAX_LENGTH) {
        Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse(mailtoUriString)
        }
    } else {
        Intent(Intent.ACTION_SEND).apply {
            type = "message/rfc822"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
        }
    }
}

private fun appVersionName(context: android.content.Context): String {
    return try {
        val pm = context.packageManager
        val pkg = context.packageName
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getPackageInfo(pkg, PackageManager.PackageInfoFlags.of(0)).versionName
        } else {
            @Suppress("DEPRECATION")
            pm.getPackageInfo(pkg, 0).versionName
        }
    } catch (_: Exception) {
        null
    }.orEmpty()
}

/** Subject + preamble so your inbox can filter “AdShield” → Important; body looks like support mail. */
private fun buildFeedbackMailBody(context: android.content.Context, userMessage: String): String {
    val version = appVersionName(context)
    val device = buildString {
        append(Build.MANUFACTURER.replaceFirstChar { it.titlecase() })
        append(' ')
        append(Build.MODEL ?: "")
    }.trim()
    val androidVer = Build.VERSION.RELEASE ?: ""
    val preamble = context.getString(
        R.string.feedback_email_body_preamble,
        version.ifBlank { "—" },
        device.ifBlank { "—" },
        androidVer.ifBlank { "—" }
    )
    return preamble + "\n" + userMessage.trim()
}

@Composable
fun FeedbackScreen(
    isDarkMode: Boolean,
    onBackClick: () -> Unit,
) {
    val context = LocalContext.current
    var message by remember { mutableStateOf("") }
    var showThankYouDialog by remember { mutableStateOf(false) }
    var validationAlertText: String? by remember { mutableStateOf(null) }

    val accent = if (isDarkMode) OrangePrimary else PremiumOrange
    val cardBg = if (isDarkMode) DarkBackgroundWhite else BackgroundWhite
    val textDark = if (isDarkMode) DarkTextDark else TextDark
    val textLight = if (isDarkMode) DarkTextLight else TextLight
    val borderSubtle = if (isDarkMode) Color.White.copy(alpha = 0.12f) else Color(0xFFE0E0E0)

    fun sendFeedback() {
        val body = message.trim()
        if (body.isEmpty()) {
            validationAlertText = context.getString(R.string.feedback_empty)
            return
        }
        val chars = body.length
        if (chars < MIN_FEEDBACK_CHARS) {
            validationAlertText = context.getString(
                R.string.feedback_min_chars_message,
                MIN_FEEDBACK_CHARS,
                chars
            )
            return
        }
        val email = context.getString(R.string.feedback_support_email)
        val subject = context.getString(R.string.feedback_email_subject)
        val fullBody = buildFeedbackMailBody(context, body)
        val intent = buildFeedbackEmailIntent(email, subject, fullBody)
        val chooser = Intent.createChooser(
            intent,
            context.getString(R.string.feedback_chooser_title)
        )
        try {
            // Chooser needs explicit flags when context is not an Activity.
            if (context !is android.app.Activity) {
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
            message = ""
            Toast.makeText(
                context,
                context.getString(R.string.feedback_opened_mail_app, email),
                Toast.LENGTH_LONG
            ).show()
            showThankYouDialog = true
        } catch (_: ActivityNotFoundException) {
            Toast.makeText(
                context,
                context.getString(R.string.feedback_no_email_app),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    validationAlertText?.let { alertBody ->
        AlertDialog(
            onDismissRequest = { validationAlertText = null },
            title = {
                Text(
                    text = stringResource(R.string.feedback_send_validation_title),
                    fontWeight = FontWeight.Bold
                )
            },
            text = { Text(alertBody) },
            confirmButton = {
                TextButton(onClick = { validationAlertText = null }) {
                    Text(stringResource(R.string.common_ok))
                }
            }
        )
    }

    if (showThankYouDialog) {
        FeedbackThankYouDialog(
            accent = accent,
            cardBg = cardBg,
            textDark = textDark,
            textLight = textLight,
            supportEmail = context.getString(R.string.feedback_support_email),
            onDismiss = { showThankYouDialog = false }
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
                .verticalScroll(rememberScrollState())
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
                        text = stringResource(R.string.feedback_title),
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        color = textDark
                    )
                    Text(
                        text = stringResource(R.string.feedback_subtitle),
                        fontSize = 12.sp,
                        color = textLight
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                placeholder = {
                    Text(
                        stringResource(R.string.feedback_hint),
                        color = textLight.copy(alpha = 0.7f)
                    )
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent,
                    unfocusedBorderColor = borderSubtle,
                    focusedLabelColor = accent,
                    cursorColor = accent,
                    focusedTextColor = textDark,
                    unfocusedTextColor = textDark,
                    focusedContainerColor = cardBg,
                    unfocusedContainerColor = cardBg
                ),
                maxLines = 9
            )

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = { sendFeedback() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = accent),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    text = stringResource(R.string.feedback_send),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun FeedbackThankYouDialog(
    accent: Color,
    cardBg: Color,
    textDark: Color,
    textLight: Color,
    supportEmail: String,
    onDismiss: () -> Unit,
) {
    var animateIn by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { animateIn = true }

    val cardScale by animateFloatAsState(
        targetValue = if (animateIn) 1f else 0.72f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "thanks_card_scale"
    )

    val infinite = rememberInfiniteTransition(label = "thanks_pulse")
    val glowPulse by infinite.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer(scaleX = cardScale, scaleY = cardScale),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ThankYouCheckBadge(accent = accent, glowPulse = glowPulse)
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.feedback_thank_you_title),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = textDark,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.feedback_thank_you_message, supportEmail),
                    fontSize = 14.sp,
                    color = textLight,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = stringResource(R.string.feedback_thank_you_ok),
                        color = accent,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ThankYouCheckBadge(accent: Color, glowPulse: Float) {
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size((76 * glowPulse).dp)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.18f))
        )
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(accent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}
