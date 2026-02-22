package com.dungz.our_ads.manager

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.dungz.our_ads.state.AdHolder
import com.dungz.our_ads.state.AdState
import com.dungz.our_ads.state.createAdKey
import com.dungz.our_ads.utils.AdLogger
import java.util.concurrent.ConcurrentHashMap

/**
 * Singleton Manager cho Interstitial Ads
 * - Hỗ trợ nhiều ads, lưu trong Map với key = "$adHigherId|$adNormalId"
 * - Load tuần tự: high trước, fail thì load normal
 * - Show xong tự động xóa khỏi Map
 * - Kiểm tra trạng thái trực tiếp qua adsMap[key]?.state
 */
object InterstitialAdManager {

    private lateinit var appContext: Context

    private val adsMap = ConcurrentHashMap<String, AdHolder<InterstitialAd>>()

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun isReady(adHigherId: String, adNormalId: String): Boolean {
        val key = createAdKey(adHigherId, adNormalId)
        return adsMap[key]?.state == AdState.Loaded
    }

    fun getState(adHigherId: String, adNormalId: String): AdState {
        val key = createAdKey(adHigherId, adNormalId)
        return adsMap[key]?.state ?: AdState.NotLoaded
    }

    fun loadAd(
        adHigherId: String,
        adNormalId: String,
        showHigher: Boolean = true,
        showNormal: Boolean = true,
        onLoaded: () -> Unit = {},
        onFailed: (String) -> Unit = {}
    ) {
        if (!showHigher && !showNormal) {
            AdLogger.warn(AdLogger.TYPE_INTERSTITIAL, "Both showHigher and showNormal are false, skipping load")
            onFailed("Both ads disabled")
            return
        }

        val key = createAdKey(adHigherId, adNormalId)

        if (adsMap[key]?.state == AdState.Loaded) {
            onLoaded()
            return
        }

        if (adsMap[key]?.state == AdState.Loading) {
            return
        }

        adsMap[key] = AdHolder(ad = null, state = AdState.Loading)
        AdLogger.debug(AdLogger.TYPE_INTERSTITIAL, "Starting load for key: $key")

        if (showHigher) {
            loadSingleAd(key, adHigherId, isHigher = true) { success ->
                if (success) {
                    onLoaded()
                } else if (showNormal) {
                    AdLogger.logFallbackToNormal(AdLogger.TYPE_INTERSTITIAL, adNormalId)
                    loadSingleAd(key, adNormalId, isHigher = false) { normalSuccess ->
                        if (normalSuccess) {
                            onLoaded()
                        } else {
                            AdLogger.error(AdLogger.TYPE_INTERSTITIAL, "Both ads failed for key: $key")
                            adsMap[key] = AdHolder(ad = null, state = AdState.Failed("Both ads failed"))
                            onFailed("Both ads failed to load")
                        }
                    }
                } else {
                    AdLogger.error(AdLogger.TYPE_INTERSTITIAL, "Higher ad failed, normal disabled for key: $key")
                    adsMap[key] = AdHolder(ad = null, state = AdState.Failed("Higher ad failed, normal disabled"))
                    onFailed("Higher ad failed, normal disabled")
                }
            }
        } else if (showNormal) {
            loadSingleAd(key, adNormalId, isHigher = false) { success ->
                if (success) {
                    onLoaded()
                } else {
                    AdLogger.error(AdLogger.TYPE_INTERSTITIAL, "Normal ad failed for key: $key")
                    adsMap[key] = AdHolder(ad = null, state = AdState.Failed("Normal ad failed"))
                    onFailed("Normal ad failed to load")
                }
            }
        }
    }

    private fun loadSingleAd(
        key: String,
        adUnitId: String,
        isHigher: Boolean,
        onResult: (Boolean) -> Unit
    ) {
        val startTime = System.currentTimeMillis()
        AdLogger.logLoading(AdLogger.TYPE_INTERSTITIAL, adUnitId, isHigher)

        InterstitialAd.load(
            appContext,
            adUnitId,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    val loadTime = System.currentTimeMillis() - startTime
                    adsMap[key] = AdHolder(
                        ad = ad,
                        state = AdState.Loaded,
                        isHigherAd = isHigher,
                        loadedAt = System.currentTimeMillis(),
                        adUnitId = adUnitId
                    )
                    AdLogger.logLoaded(AdLogger.TYPE_INTERSTITIAL, adUnitId, isHigher, loadTime)
                    onResult(true)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    AdLogger.logFailedToLoad(AdLogger.TYPE_INTERSTITIAL, adUnitId, isHigher, error.code, error.message)
                    onResult(false)
                }
            }
        )
    }

    fun showAd(
        activity: Activity,
        adHigherId: String,
        adNormalId: String,
        showHigher: Boolean,
        showNormal: Boolean,
        onAdDismissed: () -> Unit = {},
        onAdFailedToShow: (String) -> Unit = {}
    ) {
        val key = createAdKey(adHigherId, adNormalId)
        val holder = adsMap[key]

        if (holder?.state != AdState.Loaded || holder.ad == null) {
            onAdFailedToShow("No ad ready to show")
            return
        }

        if (holder.isHigherAd && !showHigher) {
            onAdFailedToShow("Higher ad not allowed")
            return
        }
        if (!holder.isHigherAd && !showNormal) {
            onAdFailedToShow("Normal ad not allowed")
            return
        }

        val ad = holder.ad
        val adUnitId = holder.adUnitId
        adsMap[key] = holder.copy(state = AdState.Showing)

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                AdLogger.logDismissed(AdLogger.TYPE_INTERSTITIAL, adUnitId)
                adsMap.remove(key)
                onAdDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                AdLogger.logFailedToShow(AdLogger.TYPE_INTERSTITIAL, adUnitId, error.message)
                adsMap.remove(key)
                onAdFailedToShow(error.message)
            }

            override fun onAdShowedFullScreenContent() {
                AdLogger.logShowing(AdLogger.TYPE_INTERSTITIAL, adUnitId, holder.isHigherAd)
            }

            override fun onAdClicked() {
                AdLogger.logClicked(AdLogger.TYPE_INTERSTITIAL, adUnitId)
            }

            override fun onAdImpression() {
                AdLogger.logImpression(AdLogger.TYPE_INTERSTITIAL, adUnitId)
            }
        }

        ad.show(activity)
    }

    fun loadAndShow(
        activity: Activity,
        adHigherId: String,
        adNormalId: String,
        showHigher: Boolean,
        showNormal: Boolean,
        onAdDismissed: () -> Unit = {},
        onAdFailedToShow: (String) -> Unit = {}
    ) {
        if (isReady(adHigherId, adNormalId)) {
            showAd(activity, adHigherId, adNormalId, showHigher, showNormal, onAdDismissed, onAdFailedToShow)
        } else {
            loadAd(
                adHigherId, adNormalId, showHigher, showNormal,
                onLoaded = { showAd(activity, adHigherId, adNormalId, showHigher, showNormal, onAdDismissed, onAdFailedToShow) },
                onFailed = onAdFailedToShow
            )
        }
    }

    fun removeAd(adHigherId: String, adNormalId: String) {
        val key = createAdKey(adHigherId, adNormalId)
        adsMap.remove(key)
    }

    fun clearAll() {
        adsMap.clear()
    }
}
