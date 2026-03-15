package com.dungz.our_ads.utils

import android.util.Log

/**
 * Centralized logging utility for ads
 * Provides consistent log format across all ad types
 */
object AdLogger {

    private const val TAG = "OurAds"

    var isEnabled: Boolean = true
    var showToasts: Boolean = false
    var onLogListener: ((String) -> Unit)? = null

    // Ad Types
    const val TYPE_NATIVE = "Native"
    const val TYPE_BANNER = "Banner"
    const val TYPE_INTERSTITIAL = "Interstitial"
    const val TYPE_REWARDED = "Rewarded"

    /**
     * Log when ad starts loading
     */
    fun logLoading(adType: String, adUnitId: String, isHigher: Boolean) {
        if (!isEnabled) return
        val priority = if (isHigher) "Higher" else "Normal"
        Log.d(TAG, "[$adType] Loading $priority ad: $adUnitId")
    }

    /**
     * Log when ad is loaded successfully
     */
    fun logLoaded(adType: String, adUnitId: String, isHigher: Boolean, loadTimeMs: Long = 0) {
        if (!isEnabled) return
        val priority = if (isHigher) "Higher" else "Normal"
        val timeInfo = if (loadTimeMs > 0) " (${loadTimeMs}ms)" else ""
        Log.i(TAG, "[$adType] Loaded $priority ad: $adUnitId$timeInfo")
    }

    /**
     * Log when ad fails to load
     */
    fun logFailedToLoad(adType: String, adUnitId: String, isHigher: Boolean, errorCode: Int, errorMessage: String) {
        if (!isEnabled) return
        val priority = if (isHigher) "Higher" else "Normal"
        Log.e(TAG, "[$adType] Failed to load $priority ad: $adUnitId | Error[$errorCode]: $errorMessage")
    }

    /**
     * Log when ad is shown
     */
    fun logShowing(adType: String, adUnitId: String, isHigher: Boolean) {
        if (!isEnabled) return
        val priority = if (isHigher) "Higher" else "Normal"
        Log.i(TAG, "[$adType] Showing $priority ad: $adUnitId")
    }

    /**
     * Log when ad is dismissed
     */
    fun logDismissed(adType: String, adUnitId: String) {
        if (!isEnabled) return
        Log.d(TAG, "[$adType] Dismissed: $adUnitId")
    }

    /**
     * Log when ad fails to show
     */
    fun logFailedToShow(adType: String, adUnitId: String, errorMessage: String) {
        if (!isEnabled) return
        Log.e(TAG, "[$adType] Failed to show: $adUnitId | Error: $errorMessage")
    }

    /**
     * Log when ad is clicked
     */
    fun logClicked(adType: String, adUnitId: String) {
        if (!isEnabled) return
        Log.d(TAG, "[$adType] Clicked: $adUnitId")
    }

    /**
     * Log when ad impression is recorded
     */
    fun logImpression(adType: String, adUnitId: String) {
        if (!isEnabled) return
        Log.d(TAG, "[$adType] Impression: $adUnitId")
    }

    /**
     * Log retry attempt
     */
    fun logRetry(adType: String, adUnitId: String, retryCount: Int, maxRetry: Int) {
        if (!isEnabled) return
        Log.w(TAG, "[$adType] Retrying ($retryCount/$maxRetry): $adUnitId")
    }

    /**
     * Log when all retries exhausted
     */
    fun logAllRetriesExhausted(adType: String, adHigherId: String, adNormalId: String, totalRetries: Int) {
        if (!isEnabled) return
        Log.e(TAG, "[$adType] All retries exhausted ($totalRetries) for: Higher=$adHigherId, Normal=$adNormalId")
    }

    /**
     * Log when user earns reward (for rewarded ads)
     */
    fun logRewardEarned(adType: String, rewardType: String, rewardAmount: Int) {
        if (!isEnabled) return
        Log.i(TAG, "[$adType] Reward earned: $rewardAmount $rewardType")
    }

    /**
     * Log auto-reload scheduled
     */
    fun logAutoReloadScheduled(adType: String, adUnitId: String, delayMs: Long) {
        if (!isEnabled) return
        Log.d(TAG, "[$adType] Auto-reload scheduled in ${delayMs}ms: $adUnitId")
    }

    /**
     * Log fallback to normal ad
     */
    fun logFallbackToNormal(adType: String, normalAdId: String) {
        if (!isEnabled) return
        Log.w(TAG, "[$adType] Higher ad failed, falling back to Normal: $normalAdId")
    }

    /**
     * General debug log
     */
    fun debug(adType: String, message: String) {
        if (!isEnabled) return
        Log.d(TAG, "[$adType] $message")
    }

    /**
     * General warning log
     */
    fun warn(adType: String, message: String) {
        if (!isEnabled) return
        Log.w(TAG, "[$adType] $message")
    }

    /**
     * General error log
     */
    fun error(adType: String, message: String) {
        if (!isEnabled) return
        Log.e(TAG, "[$adType] $message")
    }

    /**
     * Get a summary of current ad status
     */
    fun getStatusSummary(): String {
        return "AdLogger enabled=$isEnabled, toasts=$showToasts"
    }
}
