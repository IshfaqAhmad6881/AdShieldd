package com.digitalnestapps.adshield.view.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.res.stringResource
import com.digitalnestapps.adshield.R
import com.digitalnestapps.adshield.ui.theme.OrangePrimary
import com.digitalnestapps.adshield.ui.theme.PremiumOrange
import com.digitalnestapps.adshield.ui.theme.adShieldScreenBackgroundBrush

@Composable
fun SplashScreen(
    onNavigateToHome: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "splash")

    // Shield logo animations - smoother and more elegant
    val shieldScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shieldScale"
    )

    val shieldAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shieldAlpha"
    )

    // Inner circle pulse
    val innerCircleScale by infiniteTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "innerCircle"
    )

    val innerCircleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "innerCircleAlpha"
    )

    // Glow effect animation
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    // Content animations
    var contentAlpha by remember { mutableStateOf(0f) }
    var titleScale by remember { mutableStateOf(0.8f) }
    var taglineAlpha by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        // Staggered entrance animations
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(1000, easing = FastOutSlowInEasing)
        ) { value, _ ->
            contentAlpha = value
        }
        
        animate(
            initialValue = 0.8f,
            targetValue = 1f,
            animationSpec = tween(800, delayMillis = 200, easing = FastOutSlowInEasing)
        ) { value, _ ->
            titleScale = value
        }
        
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = tween(600, delayMillis = 600, easing = FastOutSlowInEasing)
        ) { value, _ ->
            taglineAlpha = value
        }
        
        delay(2800)
        onNavigateToHome()
    }

    // Splash screen uses system theme initially, but can accept dark mode parameter
    val isDarkMode = isSystemInDarkTheme()
    val taglineColor = if (isDarkMode) Color(0xFFB0B0B0) else Color(0xFF666666)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(adShieldScreenBackgroundBrush(isDarkMode)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(32.dp),
            modifier = Modifier.alpha(contentAlpha)
        ) {
            // Enhanced Shield Logo with glow effect
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                // Outer glow layer
                Box(
                    modifier = Modifier
                        .size((120.dp * glowScale))
                        .alpha(glowAlpha)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    OrangePrimary.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                        .blur(20.dp)
                )
                
                // Main shield circle with enhanced gradient
                Box(
                    modifier = Modifier
                        .size(100.dp * shieldScale)
                        .alpha(shieldAlpha)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    OrangePrimary,
                                    PremiumOrange,
                                    OrangePrimary.copy(alpha = 0.9f)
                                ),
                                start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                end = androidx.compose.ui.geometry.Offset(100f, 100f)
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // Animated inner pulsing circle
                    Box(
                        modifier = Modifier
                            .size(85.dp * innerCircleScale)
                            .alpha(innerCircleAlpha)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.4f),
                                        Color.White.copy(alpha = 0.15f),
                                        Color.Transparent
                                    )
                                ),
                                shape = CircleShape
                            )
                    )
                    
                    // Shield icon
                    Text(
                        text = "🛡️",
                        fontSize = 42.sp,
                        modifier = Modifier
                            .alpha(shieldAlpha)
                            .scale(shieldScale)
                    )
                }
            }
            
            // App Name with scale animation
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = OrangePrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .alpha(contentAlpha)
                        .scale(titleScale)
                )
                
                Text(
                    text = stringResource(R.string.splash_tagline),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = taglineColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.alpha(taglineAlpha)
                )
            }
            
            // Enhanced loading dots with gradient
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.alpha(contentAlpha)
            ) {
                repeat(3) { index ->
                    val dotScale by infiniteTransition.animateFloat(
                        initialValue = 0.6f,
                        targetValue = 1.1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(
                                durationMillis = 700,
                                delayMillis = index * 220,
                                easing = FastOutSlowInEasing
                            ),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dot$index"
                    )
                    
                    val dotAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.5f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(
                                durationMillis = 700,
                                delayMillis = index * 220,
                                easing = FastOutSlowInEasing
                            ),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "dotAlpha$index"
                    )
                    
                    Box(
                        modifier = Modifier
                            .size(13.dp * dotScale)
                            .alpha(dotAlpha)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        OrangePrimary,
                                        PremiumOrange
                                    )
                                ),
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}
