package com.digitalnestapps.adshield

import android.app.Application
import com.digitalnestapps.adshield.ads.RewardedAdPool

/**
 * Starts Mobile Ads rewarded preload at process start so the first tap can show an ad without cold-load delay.
 */
class AdShieldApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RewardedAdPool.warm(this)
    }
}
