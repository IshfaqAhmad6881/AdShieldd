package com.FusionCoreTech.myapplication.view.speedtest

import androidx.compose.animation.core.*
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.FusionCoreTech.myapplication.R
import com.FusionCoreTech.myapplication.ui.theme.*
import com.FusionCoreTech.myapplication.viewmodel.SpeedTestConnectionState
import java.lang.Math.toRadians
import kotlin.math.cos
import kotlin.math.sin

/**
 * Speed Test screen — matches Figma frame 106:716 (VPN App UI Kit - Community Copy).
 * Specs from get_design_context + get_screenshot.
 * Tokens: Neutral/100 #F5F5F5, Neutral/200 #EEEEEE, Neutral/500 #AAAAAA, Neutral/600 #777777,
 * Primary/100 #FEF1EB, Primary/500 #F06A30.
 */
@Composable
fun SpeedTestScreen(
    downloadSpeed: Float,
    uploadSpeed: Float,
    speedometerValue: Float,
    connectionState: SpeedTestConnectionState,
    isDarkMode: Boolean,
    onBackClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onStartTestClick: () -> Unit = {}
) {
    // Figma 106-716: VPN App UI Kit (Community Copy) - get_design_context
    val backgroundColor = if (isDarkMode) DarkBackgroundGrey else Color(0xFFF5F5F5)   // Neutral/100
    val contentBg = if (isDarkMode) DarkBackgroundWhite else Color.White
    val textDarkColor = if (isDarkMode) DarkTextDark else Color.Black
    val textLightColor = if (isDarkMode) DarkTextLight else Color(0xFFAAAAAA)          // Neutral/500
    val borderColor = if (isDarkMode) Color.White.copy(alpha = 0.1f) else Color(0xFFE8E8E8)
    val primaryButtonColor = if (isDarkMode) OrangePrimary else PremiumOrange          // Primary/500 #F06A30

    // Figma 106:716 — Frame is #F5F5F5 only; white is ONLY on buttons + cards (no full-width white sheet)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .padding(bottom = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Figma 106:716 — Header has ONLY Back (left) + Right button (Book/History). No center title.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Card(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { onBackClick() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Color(0xFF2C2C2C) else Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 1.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(id = R.drawable.leftbtn),
                            contentDescription = "Back",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Card(
                    modifier = Modifier
                        .size(48.dp)
                        .clickable { onHistoryClick() },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = if (isDarkMode) Color(0xFF2C2C2C) else Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 1.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "History",
                            tint = textLightColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Figma: back at 68px, card at 140px → 24px between header (48px) and card
            Spacer(modifier = Modifier.height(24.dp))

            // CONNECTING state - frame style
            if (connectionState == SpeedTestConnectionState.CONNECTING) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(56.dp),
                            color = primaryButtonColor,
                            strokeWidth = 3.dp
                        )
                        Text(
                            text = "Connecting",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textDarkColor
                        )
                        Text(
                            text = "Testing your connection...",
                            fontSize = 13.sp,
                            color = textLightColor
                        )
                    }
                }
            } else {
            // Figma Traffic: single white card with Download | Divider | Upload
            TrafficCard(
                downloadSpeed = downloadSpeed,
                uploadSpeed = uploadSpeed,
                isDarkMode = isDarkMode,
                textDarkColor = textDarkColor,
                textLightColor = textLightColor
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Speedometer - center
            SpeedometerGauge(
                value = speedometerValue,
                isDarkMode = isDarkMode,
                textDarkColor = textDarkColor
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Test running - live speed below gauge
            if (connectionState == SpeedTestConnectionState.TEST_RUNNING) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val (speedStr, unit) = formatSpeed(downloadSpeed)
                    Text(
                        text = speedStr,
                        fontSize = 34.sp,
                        fontWeight = FontWeight.Bold,
                        color = textDarkColor
                    )
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Text(
                            text = unit.uppercase(),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = textLightColor,
                            modifier = Modifier.padding(end = 6.dp)
                        )
                        Image(
                            painter = painterResource(id = R.drawable.arrowdownward),
                            contentDescription = "Download",
                            modifier = Modifier.size(18.dp),
                            colorFilter = ColorFilter.tint(OrangePrimary)
                        )
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.height(48.dp))
            } else {
            Spacer(modifier = Modifier.weight(1f))

            if (connectionState == SpeedTestConnectionState.IDLE) {
                Button(
                    onClick = { onStartTestClick() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryButtonColor),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp
                    )
                ) {
                    Text(
                        text = "Start Test",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            } else if (connectionState == SpeedTestConnectionState.TEST_COMPLETED) {
                Button(
                    onClick = { onStartTestClick() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryButtonColor),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp
                    )
                ) {
                    Text(
                        text = "Test Again",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
            } // end else (TEST_RUNNING -> show speed; else -> server + button)
            } // end else (not CONNECTING)
        }
    }
}

/** Figma 106-716: Traffic = single white card with Download | Divider | Upload */
@Composable
fun TrafficCard(
    downloadSpeed: Float,
    uploadSpeed: Float,
    isDarkMode: Boolean,
    textDarkColor: Color,
    textLightColor: Color
) {
    val (downStr, downUnit) = formatSpeed(downloadSpeed)
    val (upStr, upUnit) = formatSpeed(uploadSpeed)
    val cardBg = if (isDarkMode) DarkBackgroundWhite else Color.White
    val iconBg = if (isDarkMode) OrangePrimary.copy(alpha = 0.2f) else Color(0xFFFEF1EB)  // Figma Primary/100
    val iconTint = if (isDarkMode) OrangePrimary else PremiumOrange                       // Figma Primary/500 #F06A30
    val dividerColor = if (isDarkMode) Color.White.copy(alpha = 0.15f) else Color(0xFFEEEEEE) // Figma Neutral/200

    // Figma: shadow 0px 8px 12px rgba(0,0,0,0.05), rounded 16dp, 16dp padding, 16dp gap
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp, pressedElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Download
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Download",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textDarkColor
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(iconBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_download),
                            contentDescription = "Download",
                            modifier = Modifier.size(16.dp),
                            colorFilter = ColorFilter.tint(iconTint)
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = downStr,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textDarkColor
                    )
                    Text(
                        text = downUnit,
                        fontSize = 14.sp,
                        color = textLightColor,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .height(104.dp)
                    .background(dividerColor)
            )
            // Upload
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Upload",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textDarkColor
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(iconBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_upload),
                            contentDescription = "Upload",
                            modifier = Modifier.size(16.dp),
                            colorFilter = ColorFilter.tint(iconTint)
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = upStr,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textDarkColor
                    )
                    Text(
                        text = upUnit,
                        fontSize = 14.sp,
                        color = textLightColor,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SpeedMetricCard(
    label: String,
    speed: Float,
    iconResId: Int,
    isDarkMode: Boolean,
    textDarkColor: Color,
    textLightColor: Color,
    cardBackgroundColor: Color
) {
    val (speedStr, unit) = formatSpeed(speed)
    val cardSurface = if (isDarkMode) DarkBackgroundWhite else Color.White
    val cardBorder = if (isDarkMode) Color.White.copy(alpha = 0.08f) else Color(0xFFE8E8E8)
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(108.dp)
            .border(1.dp, cardBorder, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        ),
        colors = CardDefaults.cardColors(containerColor = cardSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (isDarkMode) OrangePrimary.copy(alpha = 0.2f)
                            else OrangeLight
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = iconResId),
                        contentDescription = label,
                        modifier = Modifier.size(16.dp),
                        colorFilter = ColorFilter.tint(OrangePrimary)
                    )
                }
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = textDarkColor
                )
            }
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = speedStr,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = textDarkColor
                )
                Text(
                    text = unit,
                    fontSize = 12.sp,
                    color = textLightColor,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }
}

@Composable
fun SpeedometerGauge(
    value: Float,
    isDarkMode: Boolean,
    textDarkColor: Color
) {
    // Ensure initial value is exactly 0 (needle starts at label 0 - leftmost position)
    // Label 0 is at 180 degrees (leftmost), so needle must start at 180 degrees rotation
    val initialValue = if (value < 0f) 0f else value.coerceIn(0f, 100f)
    
    // Animate the value like a real car speedometer
    // Real speedometers move proportionally with speed - direct, smooth, no overshoot
    // The needle position directly corresponds to the speed value
    val animatedValue by animateFloatAsState(
        targetValue = initialValue,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy, // No bounce - needle moves directly to position like real speedometer
            stiffness = Spring.StiffnessMedium, // Medium stiffness for responsive but smooth movement
            visibilityThreshold = 0.01f // Very small threshold for precise positioning
        ),
        label = "speedometer_value"
    )
    
    // Ensure needle starts at label 0 when value is 0
    // When animatedValue = 0, rotation = 180° which aligns with label 0 position
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp),
        contentAlignment = Alignment.Center
    ) {
        // Speedometer Container
        Box(
            modifier = Modifier.size(280.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Draw arc and labels together in Canvas for perfect alignment
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val centerX = size.width / 2
                val centerY = size.height
                val outerRadius = size.width / 2 - 10.dp.toPx()
                val innerRadius = outerRadius - 20.dp.toPx()
                
                // Figma: Neutral/200 #EEEEEE for track
                drawArc(
                    color = if (isDarkMode) DarkOuterRingGrey else Color(0xFFEEEEEE),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(centerX - outerRadius, centerY - outerRadius),
                    size = androidx.compose.ui.geometry.Size(outerRadius * 2, outerRadius * 2),
                    style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
                )
                
                // Draw fill arc (OrangePrimary) that moves with the animated value
                // Fill arc must align perfectly with needle tip position
                // Calculate sweep angle: 0-100 maps to 0-180 degrees
                // Fill arc starts at 180° (label 0) and sweeps clockwise
                // Fill arc end position = needle tip position
                val fillSweepAngle = (animatedValue / 100f * 180f)
                if (fillSweepAngle > 0f) {
                    // Figma: Primary/500 #F06A30 (light), keep OrangePrimary for dark
                    drawArc(
                        color = if (isDarkMode) OrangePrimary else PremiumOrange,
                        startAngle = 180f, // Start at label 0 position
                        sweepAngle = fillSweepAngle, // Sweep angle - end matches needle tip
                        useCenter = false,
                        topLeft = Offset(centerX - outerRadius, centerY - outerRadius),
                        size = androidx.compose.ui.geometry.Size(outerRadius * 2, outerRadius * 2),
                        style = Stroke(width = 20.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                
                // Draw labels along the inner edge of arc (car speedometer style)
                val labels = listOf(0, 1, 5, 10, 20, 30, 50, 75, 100)
                val totalLabels = labels.size
                val angleStep = 180f / (totalLabels - 1)
                
                labels.forEachIndexed { index, labelValue ->
                    val angle = 180f - (index * angleStep)
                    val angleRad = toRadians(angle.toDouble())
                    
                    // Position label at inner edge of arc track
                    val labelRadius = innerRadius - 5.dp.toPx() // Just inside the arc track
                    val labelX = centerX + (labelRadius * cos(angleRad).toFloat())
                    val labelY = centerY - (labelRadius * sin(angleRad).toFloat())
                    
                    // Figma: labels 16px SemiBold #AAAAAA (Neutral/500)
                    drawContext.canvas.nativeCanvas.apply {
                        save()
                        val paint = android.graphics.Paint().apply {
                            color = android.graphics.Color.parseColor(if (isDarkMode) "#B0B0B0" else "#AAAAAA")
                            textSize = 48f // ~16sp
                            textAlign = android.graphics.Paint.Align.CENTER
                            isAntiAlias = true
                            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                        }
                        translate(labelX, labelY)
                        drawText(labelValue.toString(), 0f, paint.textSize / 3, paint)
                        restore()
                    }
                }
            }
            
            // Needle with pivot point - using speedarrow.png image
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                // Needle rotation calculation - tip must align exactly with labels:
                // - Label 0 is at 180 degrees (leftmost position)
                // - Label 100 is at 0 degrees (rightmost position)
                // - When value = 0: needle angle = 180° (tip at label 0)
                // - When value = 100: needle angle = 0° (tip at label 100)
                // - Needle rotates around fixed center pivot, tip follows arc curve
                val needleAngle = 180f - (animatedValue / 100f * 180f) // Angle along arc: 180° to 0°
                
                // Needle size - properly sized to reach labels
                // Arrow should extend from center to label position
                val needleWidth = 140.dp
                val needleHeight = 70.dp
                
                // Rotate the arrow image around center pivot
                // Arrow image pivot point is at bottom center, rotates around that point
                Image(
                    painter = painterResource(id = R.drawable.speedarrow),
                    contentDescription = "Speed Needle",
                    modifier = Modifier
                        .size(width = needleWidth, height = needleHeight)
                        .rotate(needleAngle),
                    alignment = Alignment.BottomCenter,
                    contentScale = ContentScale.FillBounds
                )
                
                // Figma: white pivot 24px, shadow 0px 0px 12px rgba(0,0,0,0.05)
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .offset(y = (-12).dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(2.dp, PremiumOrange, CircleShape)
                )
            }
        }
    }
}

private fun formatSpeed(bytesPerSecond: Float): Pair<String, String> {
    val mbps = bytesPerSecond / (1024 * 1024) // Convert to MB/s
    return if (mbps <= 0f) {
        "0.00" to "mb/s"
    } else {
        String.format("%.2f", mbps) to "mb/s"
    }
}
