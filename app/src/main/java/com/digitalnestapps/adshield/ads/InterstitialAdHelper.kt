package com.digitalnestapps.adshield.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
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
        private const val TAG = "InterstitialAdHelper"
        // Test interstitial ad unit ID – replace with your real ID for production
        private const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-7882326227440171/3821142420"
        private const val PREFS = "adshield_interstitial_policy"
        private const val KEY_LAST_SHOWN_MS = "last_interstitial_shown_ms"
        private const val LOAD_RETRY_MS = 3_000L
        /**
         * Minimum time between interstitial impressions (40s). Same value is used for the home-screen
         * periodic check — pair with frequency caps in AdMob console.
         * Policy: https://support.google.com/admob/answer/6066980 — natural breaks; avoid disallowed patterns:
         * https://support.google.com/admob/answer/6201362
         */
        const val MIN_IMPRESSION_INTERVAL_MS = 40_000L
        private val MIN_INTERVAL_MS = MIN_IMPRESSION_INTERVAL_MS

        /**
         * After a rewarded fullscreen closes, the home periodic [maybeShowAfterNaturalBreak] can think
         * 40s elapsed while the user was in the ad — call this so the interstitial cooldown restarts
         * from now; normal 40s behavior continues afterward.
         */
        fun deferCooldownAfterRewarded(activity: Activity) {
            activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                .putLong(KEY_LAST_SHOWN_MS, System.currentTimeMillis())
                .apply()
        }
    }

    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var retryRunnable: Runnable? = null

    /**
     * True when [show] ran but no creative was ready yet (still loading or load failed).
     * When the next load succeeds, we show immediately so the user does not need another back navigation.
     */
    private var pendingShowAfterLoad = false

    /** Called when the interstitial is closed (so caller can e.g. restart a timer). */
    var onAdDismissed: (() -> Unit)? = null

    init {
        MobileAds.initialize(activity) {
            mainHandler.post { loadAd("init") }
        }
    }

    private fun attachFullScreenCallback(ad: InterstitialAd) {
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                    .putLong(KEY_LAST_SHOWN_MS, System.currentTimeMillis())
                    .apply()
            }

            override fun onAdDismissedFullScreenContent() {
                interstitialAd = null
                loadAd("after-dismiss")
                onAdDismissed?.invoke()
            }

            override fun onAdFailedToShowFullScreenContent(e: AdError) {
                interstitialAd = null
                loadAd("after-show-fail")
                onAdDismissed?.invoke()
            }
        }
    }

    private fun loadAd(reason: String) {
        if (interstitialAd != null || isLoading) return
        isLoading = true
        Log.d(TAG, "Loading interstitial ($reason)")
        InterstitialAd.load(
            activity,
            INTERSTITIAL_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    isLoading = false
                    attachFullScreenCallback(ad)
                    if (
                        pendingShowAfterLoad &&
                        !activity.isFinishing &&
                        !activity.isDestroyed
                    ) {
                        pendingShowAfterLoad = false
                        interstitialAd = null
                        ad.show(activity)
                    } else {
                        interstitialAd = ad
                    }
                }

                override fun onAdFailedToLoad(e: LoadAdError) {
                    isLoading = false
                    Log.w(TAG, "Interstitial load failed: ${e.message} code=${e.code}")
                    scheduleRetryLoad()
                }
            }
        )
    }

    private fun scheduleRetryLoad() {
        if (retryRunnable != null) return
        retryRunnable = Runnable {
            retryRunnable = null
            loadAd("retry")
        }.also { mainHandler.postDelayed(it, LOAD_RETRY_MS) }
    }

    /** Shows the interstitial if loaded. Safe to call from any thread; runs on main. */
    fun show() {
        activity.runOnUiThread {
            val ad = interstitialAd
            if (ad != null) {
                pendingShowAfterLoad = false
                interstitialAd = null
                ad.show(activity)
            } else {
                pendingShowAfterLoad = true
                loadAd("show-request")
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
