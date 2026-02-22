package com.dungz.our_ads.manager

import android.content.Context
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.dungz.our_ads.state.AdHolder
import com.dungz.our_ads.state.AdState
import com.dungz.our_ads.state.createAdKey
import com.dungz.our_ads.utils.AdLogger
import java.util.concurrent.ConcurrentHashMap

/**
 * Singleton Manager cho Banner Ads
 * - Load tuần tự: high trước, fail thì load normal
 * - Kiểm tra trạng thái trực tiếp qua adsMap[key]?.state
 */
object BannerAdManager {

    private lateinit var appContext: Context

    private val adsMap = ConcurrentHashMap<String, AdHolder<AdView>>()

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
        adSize: AdSize = AdSize.BANNER,
        onLoaded: (AdView) -> Unit = {},
        onFailed: (String) -> Unit = {}
    ) {
        if (!showHigher && !showNormal) {
            AdLogger.warn(AdLogger.TYPE_BANNER, "Both showHigher and showNormal are false, skipping load")
            onFailed("Both ads disabled")
            return
        }

        val key = createAdKey(adHigherId, adNormalId)

        if (adsMap[key]?.state == AdState.Loaded && adsMap[key]?.ad != null) {
            onLoaded(adsMap[key]!!.ad!!)
            return
        }

        if (adsMap[key]?.state == AdState.Loading) {
            return
        }

        adsMap[key] = AdHolder(ad = null, state = AdState.Loading)
        AdLogger.debug(AdLogger.TYPE_BANNER, "Starting load for key: $key")

        if (showHigher) {
            loadSingleBanner(key, adHigherId, adSize, isHigher = true) { adView ->
                if (adView != null) {
                    onLoaded(adView)
                } else if (showNormal) {
                    AdLogger.logFallbackToNormal(AdLogger.TYPE_BANNER, adNormalId)
                    loadSingleBanner(key, adNormalId, adSize, isHigher = false) { normalAdView ->
                        if (normalAdView != null) {
                            onLoaded(normalAdView)
                        } else {
                            AdLogger.error(AdLogger.TYPE_BANNER, "Both ads failed for key: $key")
                            adsMap[key] = AdHolder(ad = null, state = AdState.Failed("Both ads failed"))
                            onFailed("Both ads failed to load")
                        }
                    }
                } else {
                    AdLogger.error(AdLogger.TYPE_BANNER, "Higher ad failed, normal disabled for key: $key")
                    adsMap[key] = AdHolder(ad = null, state = AdState.Failed("Higher ad failed"))
                    onFailed("Higher ad failed, normal disabled")
                }
            }
        } else if (showNormal) {
            loadSingleBanner(key, adNormalId, adSize, isHigher = false) { adView ->
                if (adView != null) {
                    onLoaded(adView)
                } else {
                    AdLogger.error(AdLogger.TYPE_BANNER, "Normal ad failed for key: $key")
                    adsMap[key] = AdHolder(ad = null, state = AdState.Failed("Normal ad failed"))
                    onFailed("Normal ad failed to load")
                }
            }
        }
    }

    private fun loadSingleBanner(
        key: String,
        adUnitId: String,
        adSize: AdSize,
        isHigher: Boolean,
        onResult: (AdView?) -> Unit
    ) {
        val startTime = System.currentTimeMillis()
        AdLogger.logLoading(AdLogger.TYPE_BANNER, adUnitId, isHigher)

        val adView = AdView(appContext).apply {
            this.adUnitId = adUnitId
            setAdSize(adSize)
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    val loadTime = System.currentTimeMillis() - startTime
                    adsMap[key] = AdHolder(
                        ad = this@apply,
                        state = AdState.Loaded,
                        isHigherAd = isHigher,
                        loadedAt = System.currentTimeMillis(),
                        adUnitId = adUnitId
                    )
                    AdLogger.logLoaded(AdLogger.TYPE_BANNER, adUnitId, isHigher, loadTime)
                    onResult(this@apply)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    AdLogger.logFailedToLoad(AdLogger.TYPE_BANNER, adUnitId, isHigher, error.code, error.message)
                    onResult(null)
                }

                override fun onAdClicked() {
                    AdLogger.logClicked(AdLogger.TYPE_BANNER, adUnitId)
                }

                override fun onAdImpression() {
                    AdLogger.logImpression(AdLogger.TYPE_BANNER, adUnitId)
                }
            }
            loadAd(AdRequest.Builder().build())
        }
    }

    fun getAdView(adHigherId: String, adNormalId: String): AdView? {
        val key = createAdKey(adHigherId, adNormalId)
        return adsMap[key]?.ad
    }

    fun removeAd(adHigherId: String, adNormalId: String) {
        val key = createAdKey(adHigherId, adNormalId)
        adsMap[key]?.ad?.destroy()
        adsMap.remove(key)
    }

    fun clearAll() {
        adsMap.values.forEach { it.ad?.destroy() }
        adsMap.clear()
    }
}
