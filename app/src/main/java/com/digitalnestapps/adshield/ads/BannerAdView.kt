package com.digitalnestapps.adshield.ads

import android.util.Log
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

// Test banner ID (Google sample) – replace with your real ID for production
private const val BANNER_AD_UNIT_ID = "ca-app-pub-7882326227440171/7485055263"
private const val TAG = "BannerAdView"

/** Banner height – slightly smaller than standard 50dp. */
private val BANNER_HEIGHT_DP = 40.dp
private val BANNER_CORNER_RADIUS = 12.dp

/**
 * Banner ad shown at the bottom of the screen. Uses your AdMob banner ad unit.
 * Loads ad after view is attached (view.post) so the banner has layout dimensions.
 */
@Composable
fun BannerAdView(
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    val context = LocalContext.current

    Box(
        modifier = modifier
            .height(BANNER_HEIGHT_DP)
            .clip(RoundedCornerShape(BANNER_CORNER_RADIUS))
            .background(Color.Black.copy(alpha = 0.05f))
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(BANNER_HEIGHT_DP)
                .clip(RoundedCornerShape(BANNER_CORNER_RADIUS)),
            factory = { ctx ->
                AdView(ctx).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        (40 * context.resources.displayMetrics.density).toInt()
                    )
                    setAdSize(AdSize.BANNER)
                    adUnitId = BANNER_AD_UNIT_ID
                    setAdListener(object : com.google.android.gms.ads.AdListener() {
                        override fun onAdLoaded() {
                            Log.d(TAG, "Banner ad loaded")
                        }
                        override fun onAdFailedToLoad(e: LoadAdError) {
                            Log.w(TAG, "Banner ad failed: code=${e.code} ${e.message}")
                        }
                    })
                    // Load after view is attached and laid out (needed for correct ad size)
                    post {
                        loadAd(AdRequest.Builder().build())
                    }
                }
            }
        )
    }
}
