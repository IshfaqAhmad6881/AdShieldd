package com.FusionCoreTech.myapplication.ads

import android.app.Activity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * Loads and shows rewarded ads. When user earns reward, [onRewardEarned] is called (e.g. add 30 min).
 * Uses test ad unit ID for development; replace with your own for production.
 */
class RewardedAdHelper(
    private val activity: Activity,
    private val onRewardEarned: () -> Unit
) {
    companion object {
        // Test rewarded ad unit ID – replace with your AdMob rewarded ad unit ID for production
        private const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
    }

    private var rewardedAd: RewardedAd? = null
    private var isLoading = false

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
                }
            }
        )
    }

    /** Call when user taps "Reward". Shows ad if loaded; when user earns reward, [onRewardEarned] runs. */
    fun show() {
        val ad = rewardedAd
        if (ad != null) {
            ad.show(activity) { _ ->
                onRewardEarned()
            }
        } else {
            // Ad not ready yet – still grant reward for better UX during development
            onRewardEarned()
            loadAd()
        }
    }
}
