package com.digitalnestapps.adshield.ads

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * Preloads rewarded ads at app start ([warm]) so the user often gets an immediate show on first tap.
 * After each impression, [requestLoad] fills the pool again in the background.
 */
object RewardedAdPool {
    private const val TAG = "RewardedAdPool"
    private const val REWARDED_AD_UNIT_ID = "ca-app-pub-7882326227440171/4488666344"
    private const val RETRY_MS = 3000L

    private val mainHandler = Handler(Looper.getMainLooper())
    private var app: Application? = null

    private val lock = Any()

    @Volatile
    private var readyAd: RewardedAd? = null

    @Volatile
    private var loading = false

    private var retryRunnable: Runnable? = null

    /** One-shot callbacks when an ad becomes available (user tapped before preload finished). */
    private val whenReady = mutableListOf<() -> Unit>()

    /** Call once from [com.digitalnestapps.adshield.AdShieldApplication]. */
    fun warm(application: Application) {
        synchronized(lock) {
            if (app != null) return
            app = application.applicationContext as Application
        }
        MobileAds.initialize(application) {
            mainHandler.post { loadNext("warm") }
        }
    }

    /**
     * Take the preloaded ad for display. Returns null if not ready yet.
     */
    fun takePreloadedAd(): RewardedAd? {
        synchronized(lock) {
            val ad = readyAd
            readyAd = null
            return ad
        }
    }

    fun hasReadyAd(): Boolean = synchronized(lock) { readyAd != null }

    /**
     * If an ad is already in the pool, [block] runs on the main thread immediately.
     * Otherwise it runs once when the next load succeeds (or immediately if load completes later).
     */
    fun runWhenAdReady(block: () -> Unit) {
        synchronized(lock) {
            if (readyAd != null) {
                mainHandler.post(block)
            } else {
                whenReady.add(block)
            }
        }
    }

    private fun notifyWhenReady() {
        val copy: List<() -> Unit>
        synchronized(lock) {
            if (whenReady.isEmpty()) return
            copy = whenReady.toList()
            whenReady.clear()
        }
        copy.forEach { mainHandler.post(it) }
    }

    /**
     * Start a load if idle. Runs [loadNext] immediately when already on the main thread so the
     * network request starts without an extra frame of delay (e.g. Start → rewarded connect).
     */
    fun requestLoad(reason: String) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            loadNext(reason)
        } else {
            mainHandler.post { loadNext(reason) }
        }
    }

    private fun loadNext(reason: String) {
        val ctx = synchronized(lock) {
            if (app == null) {
                Log.w(TAG, "loadNext skipped (no app): $reason")
                return
            }
            if (readyAd != null || loading) return
            loading = true
            app!!
        }
        Log.d(TAG, "Loading rewarded ($reason)")
        RewardedAd.load(
            ctx,
            REWARDED_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    synchronized(lock) {
                        loading = false
                        readyAd = ad
                    }
                    Log.d(TAG, "Rewarded loaded ($reason)")
                    notifyWhenReady()
                }

                override fun onAdFailedToLoad(e: LoadAdError) {
                    synchronized(lock) {
                        loading = false
                    }
                    Log.w(TAG, "Rewarded load failed: ${e.message} code=${e.code}")
                    scheduleRetry()
                }
            }
        )
    }

    private fun scheduleRetry() {
        if (retryRunnable != null) return
        retryRunnable = Runnable {
            retryRunnable = null
            loadNext("retry")
        }.also { mainHandler.postDelayed(it, RETRY_MS) }
    }
}
