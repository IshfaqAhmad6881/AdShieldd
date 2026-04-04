package com.FusionCoreTech.myapplication.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * Loads and shows interstitial ads. Call [show] to display; when dismissed, next ad is preloaded.
 * [maybeShowAfterNaturalBreak] enforces a minimum interval so placements align better with
 * Google Play / AdMob guidance (avoid back-to-back or overly frequent full-screen ads).
 */
class InterstitialAdHelper(
    private val activity: Activity
) {
    companion object {
        // Test interstitial ad unit ID – replace with your real ID for production
        private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
        private const val PREFS = "adshield_interstitial_policy"
        private const val KEY_LAST_SHOWN_MS = "last_interstitial_shown_ms"
        /** Minimum time between interstitial impressions (Play “Better Ads” / AdMob frequency). */
        private const val MIN_INTERVAL_MS = 10 * 60 * 1000L
    }

    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false

    /** Called when the interstitial is closed (so caller can e.g. restart a timer). */
    var onAdDismissed: (() -> Unit)? = null

    init {
        loadAd()
    }

    private fun loadAd() {
        if (interstitialAd != null || isLoading) return
        isLoading = true
        InterstitialAd.load(
            activity,
            INTERSTITIAL_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isLoading = false
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdShowedFullScreenContent() {
                            activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                                .putLong(KEY_LAST_SHOWN_MS, System.currentTimeMillis())
                                .apply()
                        }
                        override fun onAdDismissedFullScreenContent() {
                            interstitialAd = null
                            loadAd()
                            onAdDismissed?.invoke()
                        }
                        override fun onAdFailedToShowFullScreenContent(e: AdError) {
                            interstitialAd = null
                            loadAd()
                            onAdDismissed?.invoke()
                        }
                    }
                }
                override fun onAdFailedToLoad(e: LoadAdError) {
                    isLoading = false
                }
            }
        )
    }

    /** Shows the interstitial if loaded. Safe to call from any thread; runs on main. */
    fun show() {
        activity.runOnUiThread {
            val ad = interstitialAd
            if (ad != null) {
                interstitialAd = null
                ad.show(activity)
            } else {
                loadAd()
            }
        }
    }

    /**
     * Call when the user finishes a natural screen transition (e.g. closes Settings).
     * Shows an interstitial only if [MIN_INTERVAL_MS] has passed since the last impression.
     */
    fun maybeShowAfterNaturalBreak() {
        val prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val last = prefs.getLong(KEY_LAST_SHOWN_MS, 0L)
        if (System.currentTimeMillis() - last < MIN_INTERVAL_MS) return
        show()
    }
}
