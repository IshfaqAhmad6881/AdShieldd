package com.FusionCoreTech.myapplication.ads

import android.app.Activity
import android.os.Handler
import android.os.Looper
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * Loads and shows rewarded ads. When user earns reward, [onRewardEarned] is called (e.g. add bonus time; saved in ViewModel).
 * Uses test ad unit ID for development; replace with your own for production.
 */
class RewardedAdHelper(
    private val activity: Activity,
    private val onRewardEarned: () -> Unit
) {
    companion object {
        // Test rewarded ad unit ID – replace with your AdMob rewarded ad unit ID for production
        private const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
        private const val RETRY_LOAD_DELAY_MS = 3000L
    }

    private var rewardedAd: RewardedAd? = null
    private var isLoading = false
    private val mainHandler = Handler(Looper.getMainLooper())
    private var retryRunnable: Runnable? = null

    init {
        MobileAds.initialize(activity) {}
        loadAd()
    }

    private fun loadAd() {
        if (rewardedAd != null || isLoading) return
        isLoading = true
        RewardedAd.load(
            activity,
            REWARDED_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoading = false
                    retryRunnable?.let { mainHandler.removeCallbacks(it) }
                    retryRunnable = null
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdDismissedFullScreenContent() {
                            rewardedAd = null
                            loadAd() // Preload next
                        }
                        override fun onAdFailedToShowFullScreenContent(e: AdError) {
                            rewardedAd = null
                            loadAd()
                        }
                    }
                }
                override fun onAdFailedToLoad(e: LoadAdError) {
                    isLoading = false
                    scheduleRetry()
                }
            }
        )
    }

    private fun scheduleRetry() {
        if (retryRunnable != null) return
        retryRunnable = Runnable {
            retryRunnable = null
            loadAd()
        }.also { runnable ->
            mainHandler.postDelayed(runnable, RETRY_LOAD_DELAY_MS)
        }
    }

    /** Callback runs only if user earns reward and dismisses ad (returns to app). */
    fun show(onReward: (() -> Unit)? = null) {
        val callback = onReward ?: onRewardEarned
        val ad = rewardedAd
        if (ad != null) {
            var rewardEarned = false
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    loadAd()
                    if (rewardEarned && !activity.isFinishing && !activity.isDestroyed) {
                        activity.runOnUiThread { callback() }
                    }
                }

                override fun onAdFailedToShowFullScreenContent(e: AdError) {
                    rewardedAd = null
                    loadAd()
                }
            }

            ad.show(activity) { _ ->
                rewardEarned = true
            }
        } else {
            loadAd()
            // Ad not ready yet (first launch / slow network): still run flow so DNS connect works.
            mainHandler.post {
                if (!activity.isFinishing && !activity.isDestroyed) {
                    callback()
                }
            }
        }
    }
}
