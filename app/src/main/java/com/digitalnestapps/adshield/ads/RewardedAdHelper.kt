package com.digitalnestapps.adshield.ads

import android.app.Activity
import android.util.Log
import android.widget.Toast
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.digitalnestapps.adshield.R

/**
 * Shows rewarded ads using [RewardedAdPool] (preloaded at app start). After each full watch,
 * the pool preloads the next ad in the background.
 */
class RewardedAdHelper(
    private val activity: Activity,
    private val onRewardEarned: () -> Unit
) {
    companion object {
        private const val TAG = "RewardedAdHelper"
    }

    private var pendingShowBonusAfterLoad = false
    private var pendingBonusCallback: (() -> Unit)? = null
    private var pendingBonusDismissGate: (() -> Unit)? = null

    private var pendingShowConnectAfterLoad = false
    private var pendingConnectCallback: (() -> Unit)? = null

    init {
        // Pool already warmed in Application — first frame may already have an ad.
        RewardedAdPool.requestLoad("helper-init")
    }

    private fun toastIfAlive(stringRes: Int) {
        if (!activity.isFinishing && !activity.isDestroyed) {
            Toast.makeText(activity, activity.getString(stringRes), Toast.LENGTH_LONG).show()
        }
    }

    /**
     * Home “+30 min”: same as connect — load starts immediately; [onDismissGate] when the preparing
     * overlay should hide (ad done, failed, or could not load).
     */
    fun show(
        onReward: (() -> Unit)? = null,
        onDismissGate: () -> Unit = {}
    ) {
        val callback = onReward ?: onRewardEarned
        RewardedAdPool.requestLoad("bonus-tap")
        val ad = RewardedAdPool.takePreloadedAd()
        if (ad != null) {
            presentRewardedAd(ad, callback, onDismissGate)
            return
        }
        pendingShowBonusAfterLoad = true
        pendingBonusCallback = callback
        pendingBonusDismissGate = onDismissGate
        RewardedAdPool.runWhenAdReady {
            tryFlushBonusPending()
        }
    }

    private fun tryFlushBonusPending() {
        if (!pendingShowBonusAfterLoad) return
        val ad = RewardedAdPool.takePreloadedAd() ?: run {
            pendingShowBonusAfterLoad = false
            pendingBonusCallback = null
            pendingBonusDismissGate?.invoke()
            pendingBonusDismissGate = null
            toastIfAlive(R.string.rewarded_bonus_not_ready_toast)
            return
        }
        pendingShowBonusAfterLoad = false
        val cb = pendingBonusCallback ?: onRewardEarned
        val dismissGate = pendingBonusDismissGate
        pendingBonusCallback = null
        pendingBonusDismissGate = null
        presentRewardedAd(ad, cb, dismissGate ?: {})
    }

    private fun presentRewardedAd(
        ad: RewardedAd,
        callback: () -> Unit,
        onDismissGate: () -> Unit
    ) {
        if (activity.isFinishing || activity.isDestroyed) {
            onDismissGate()
            return
        }
        var rewardEarned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                RewardedAdPool.requestLoad("after-bonus-dismiss")
                activity.runOnUiThread {
                    InterstitialAdHelper.deferCooldownAfterRewarded(activity)
                    onDismissGate()
                    if (rewardEarned && !activity.isFinishing && !activity.isDestroyed) {
                        callback()
                    }
                }
            }

            override fun onAdFailedToShowFullScreenContent(e: AdError) {
                Log.w(TAG, "show failed: ${e.message}")
                RewardedAdPool.requestLoad("after-bonus-fail")
                activity.runOnUiThread {
                    onDismissGate()
                    if (!activity.isFinishing && !activity.isDestroyed) {
                        Toast.makeText(
                            activity,
                            activity.getString(R.string.rewarded_ad_show_failed_toast),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
        ad.show(activity) { _ ->
            rewardEarned = true
        }
    }

    private var pendingConnectDismissGate: (() -> Unit)? = null

    /**
     * Starts loading the rewarded ad immediately (no extra frame delay), then shows it when ready.
     * [onDismissGate] runs when the full-screen gate should hide: ad finished, failed to show, or
     * load could not start (call from UI first to show the gate, then pass the same lambda).
     */
    fun showRequiredForConnect(
        onRewarded: () -> Unit,
        onDismissGate: () -> Unit = {}
    ) {
        // Kick load first so the SDK request starts in the same frame as Start tap.
        RewardedAdPool.requestLoad("connect-tap")
        val ad = RewardedAdPool.takePreloadedAd()
        if (ad != null) {
            presentRewardedAdForConnect(ad, onRewarded, onDismissGate)
            return
        }
        pendingShowConnectAfterLoad = true
        pendingConnectCallback = onRewarded
        pendingConnectDismissGate = onDismissGate
        RewardedAdPool.runWhenAdReady {
            tryFlushConnectPending()
        }
    }

    private fun tryFlushConnectPending() {
        if (!pendingShowConnectAfterLoad) return
        val ad = RewardedAdPool.takePreloadedAd() ?: run {
            pendingShowConnectAfterLoad = false
            pendingConnectCallback = null
            pendingConnectDismissGate?.invoke()
            pendingConnectDismissGate = null
            toastIfAlive(R.string.rewarded_ad_not_ready_toast)
            return
        }
        pendingShowConnectAfterLoad = false
        val cb = pendingConnectCallback
        val dismissGate = pendingConnectDismissGate
        pendingConnectCallback = null
        pendingConnectDismissGate = null
        if (cb != null) presentRewardedAdForConnect(ad, cb, dismissGate ?: {})
    }

    private fun presentRewardedAdForConnect(
        ad: RewardedAd,
        onRewarded: () -> Unit,
        onDismissGate: () -> Unit
    ) {
        if (activity.isFinishing || activity.isDestroyed) {
            onDismissGate()
            return
        }
        var rewardEarned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                RewardedAdPool.requestLoad("after-connect-dismiss")
                activity.runOnUiThread {
                    InterstitialAdHelper.deferCooldownAfterRewarded(activity)
                    onDismissGate()
                    if (rewardEarned && !activity.isFinishing && !activity.isDestroyed) {
                        onRewarded()
                    }
                }
            }

            override fun onAdFailedToShowFullScreenContent(e: AdError) {
                RewardedAdPool.requestLoad("after-connect-fail")
                activity.runOnUiThread {
                    onDismissGate()
                    if (!activity.isFinishing && !activity.isDestroyed) {
                        Toast.makeText(
                            activity,
                            activity.getString(R.string.rewarded_ad_show_failed_toast),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
        ad.show(activity) { _ ->
            rewardEarned = true
        }
    }

    /** User dismissed the connect gate (e.g. back) before the ad appeared — stop waiting for load. */
    fun cancelConnectRewardFlow() {
        pendingShowConnectAfterLoad = false
        pendingConnectCallback = null
        pendingConnectDismissGate = null
    }

    /** Same for the “+30 min” bonus button flow. */
    fun cancelBonusRewardFlow() {
        pendingShowBonusAfterLoad = false
        pendingBonusCallback = null
        pendingBonusDismissGate = null
    }
}
