package com.dungz.our_ads.manager

import android.Manifest
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.dungz.our_ads.state.AdState
import com.dungz.our_ads.state.RetryConfig
import com.dungz.our_ads.state.createAdKey
import com.dungz.our_ads.utils.AdLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * Data class chứa Native Ad và metadata
 */
data class NativeAdHolder(
    val ad: NativeAd?,
    val state: AdState = AdState.NotLoaded,
    val isHigherAd: Boolean = false,
    val loadedAt: Long = 0L,
    val retryCount: Int = 0,
    val reloadCount: Int = 0,
    val adUnitId: String = ""
)

/**
 * Singleton Manager cho Native Ads
 * - Load tuần tự: high trước, fail thì load normal
 * - Hỗ trợ retry khi fail
 * - Hỗ trợ auto-reload theo thời gian
 * - Kiểm tra trạng thái trực tiếp qua adsMap[key]?.state
 */
object NativeAdManager {

    private lateinit var appContext: Context
    private val handler = Handler(Looper.getMainLooper())

    private val adsMap = ConcurrentHashMap<String, NativeAdHolder>()
    private val retryConfigMap = ConcurrentHashMap<String, RetryConfig>()
    private val reloadRunnables = ConcurrentHashMap<String, Runnable>()

    // StateFlow để notify UI khi ad được reload
    private val adFlowMap = ConcurrentHashMap<String, MutableStateFlow<NativeAd?>>()

    fun getAdFlow(adHigherId: String, adNormalId: String): StateFlow<NativeAd?> {
        val key = createAdKey(adHigherId, adNormalId)
        return adFlowMap.getOrPut(key) { MutableStateFlow(null) }.asStateFlow()
    }

    private fun emitAdChange(key: String, ad: NativeAd?) {
        adFlowMap.getOrPut(key) { MutableStateFlow(null) }.value = ad
    }

    var defaultRetryConfig = RetryConfig(
        maxRetryCount = 3,
        reloadDuration = 0L,
        reloadTriggerCount = 0
    )

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
        showHigher: Boolean,
        showNormal: Boolean,
        retryConfig: RetryConfig? = null,
        onLoaded: (NativeAd) -> Unit = {},
        onFailed: (String) -> Unit = {}
    ) {
        if (!showHigher && !showNormal) {
            AdLogger.warn(AdLogger.TYPE_NATIVE, "Both showHigher and showNormal are false, skipping load")
            onFailed("Both ads disabled")
            return
        }

        val key = createAdKey(adHigherId, adNormalId)
        val config = retryConfig ?: defaultRetryConfig
        retryConfigMap[key] = config

        if (adsMap[key]?.state == AdState.Loaded && adsMap[key]?.ad != null) {
            onLoaded(adsMap[key]!!.ad!!)
            return
        }

        if (adsMap[key]?.state == AdState.Loading) {
            return
        }

        adsMap[key] = NativeAdHolder(ad = null, state = AdState.Loading)
        AdLogger.debug(AdLogger.TYPE_NATIVE, "Starting load for key: $key")

        loadWithRetry(
            key = key,
            adHigherId = adHigherId,
            adNormalId = adNormalId,
            showHigher = showHigher,
            showNormal = showNormal,
            currentRetry = 0,
            config = config,
            onLoaded = onLoaded,
            onFailed = onFailed
        )
    }

    @RequiresPermission(Manifest.permission.INTERNET)
    private fun loadWithRetry(
        key: String,
        adHigherId: String,
        adNormalId: String,
        showHigher: Boolean,
        showNormal: Boolean,
        currentRetry: Int,
        config: RetryConfig,
        onLoaded: (NativeAd) -> Unit,
        onFailed: (String) -> Unit
    ) {
        if (showHigher) {
            loadSingleNativeAd(key, adHigherId, isHigher = true) { nativeAd ->
                if (nativeAd != null) {
                    setupAutoReload(key, adHigherId, adNormalId, showHigher, showNormal, config)
                    onLoaded(nativeAd)
                } else if (showNormal) {
                    AdLogger.logFallbackToNormal(AdLogger.TYPE_NATIVE, adNormalId)
                    loadSingleNativeAd(key, adNormalId, isHigher = false) { normalAd ->
                        if (normalAd != null) {
                            setupAutoReload(key, adHigherId, adNormalId, showHigher, showNormal, config)
                            onLoaded(normalAd)
                        } else {
                            handleRetry(key, adHigherId, adNormalId, showHigher, showNormal, currentRetry, config, onLoaded, onFailed)
                        }
                    }
                } else {
                    handleRetry(key, adHigherId, adNormalId, showHigher, showNormal, currentRetry, config, onLoaded, onFailed)
                }
            }
        } else if (showNormal) {
            loadSingleNativeAd(key, adNormalId, isHigher = false) { nativeAd ->
                if (nativeAd != null) {
                    setupAutoReload(key, adHigherId, adNormalId, showHigher, showNormal, config)
                    onLoaded(nativeAd)
                } else {
                    handleRetry(key, adHigherId, adNormalId, showHigher, showNormal, currentRetry, config, onLoaded, onFailed)
                }
            }
        }
    }

    private fun handleRetry(
        key: String,
        adHigherId: String,
        adNormalId: String,
        showHigher: Boolean,
        showNormal: Boolean,
        currentRetry: Int,
        config: RetryConfig,
        onLoaded: (NativeAd) -> Unit,
        onFailed: (String) -> Unit
    ) {
        if (currentRetry < config.maxRetryCount) {
            val delayMs = (1000L * (currentRetry + 1)).coerceAtMost(5000L)
            AdLogger.logRetry(AdLogger.TYPE_NATIVE, key, currentRetry + 1, config.maxRetryCount)
            handler.postDelayed(@androidx.annotation.RequiresPermission(android.Manifest.permission.INTERNET) {
                adsMap[key] = (adsMap[key] ?: NativeAdHolder(null)).copy(
                    retryCount = currentRetry + 1
                )
                loadWithRetry(
                    key, adHigherId, adNormalId, showHigher, showNormal,
                    currentRetry + 1, config, onLoaded, onFailed
                )
            }, delayMs)
        } else {
            AdLogger.logAllRetriesExhausted(AdLogger.TYPE_NATIVE, adHigherId, adNormalId, currentRetry)
            adsMap[key] = NativeAdHolder(ad = null, state = AdState.Failed("All retries exhausted", currentRetry))
            onFailed("Failed after $currentRetry retries")
        }
    }

    private fun setupAutoReload(
        key: String,
        adHigherId: String,
        adNormalId: String,
        showHigher: Boolean,
        showNormal: Boolean,
        config: RetryConfig
    ) {
        reloadRunnables[key]?.let { handler.removeCallbacks(it) }

        if (config.reloadDuration > 0) {
            val currentReloadCount = adsMap[key]?.reloadCount ?: 0

            if (config.reloadTriggerCount > 0 && currentReloadCount >= config.reloadTriggerCount) {
                return
            }

            val reloadRunnable = Runnable {
                AdLogger.debug(AdLogger.TYPE_NATIVE, "Auto-reloading ad: $key")
                adsMap[key]?.ad?.destroy()
                adsMap[key] = (adsMap[key] ?: NativeAdHolder(null)).copy(
                    ad = null,
                    state = AdState.NotLoaded,
                    reloadCount = currentReloadCount + 1
                )
                loadAd(adHigherId, adNormalId, showHigher, showNormal, config)
            }

            reloadRunnables[key] = reloadRunnable
            AdLogger.logAutoReloadScheduled(AdLogger.TYPE_NATIVE, key, config.reloadDuration)
            handler.postDelayed(reloadRunnable, config.reloadDuration)
        }
    }

    private fun loadSingleNativeAd(
        key: String,
        adUnitId: String,
        isHigher: Boolean,
        onResult: (NativeAd?) -> Unit
    ) {
        val startTime = System.currentTimeMillis()
        AdLogger.logLoading(AdLogger.TYPE_NATIVE, adUnitId, isHigher)

        AdLoader.Builder(appContext, adUnitId)
            .forNativeAd { nativeAd ->
                val loadTime = System.currentTimeMillis() - startTime
                adsMap[key]?.ad?.destroy()

                adsMap[key] = NativeAdHolder(
                    ad = nativeAd,
                    state = AdState.Loaded,
                    isHigherAd = isHigher,
                    loadedAt = System.currentTimeMillis(),
                    retryCount = adsMap[key]?.retryCount ?: 0,
                    reloadCount = adsMap[key]?.reloadCount ?: 0,
                    adUnitId = adUnitId
                )
                AdLogger.logLoaded(AdLogger.TYPE_NATIVE, adUnitId, isHigher, loadTime)
                emitAdChange(key, nativeAd)
                onResult(nativeAd)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    AdLogger.logFailedToLoad(AdLogger.TYPE_NATIVE, adUnitId, isHigher, error.code, error.message)
                    onResult(null)
                }

                override fun onAdClicked() {
                    AdLogger.logClicked(AdLogger.TYPE_NATIVE, adUnitId)
                }

                override fun onAdImpression() {
                    AdLogger.logImpression(AdLogger.TYPE_NATIVE, adUnitId)
                }
            })
            .withNativeAdOptions(
                NativeAdOptions.Builder()
                    .setAdChoicesPlacement(NativeAdOptions.ADCHOICES_TOP_RIGHT)
                    .build()
            )
            .build()
            .loadAd(AdRequest.Builder().build())
    }

    fun getNativeAd(adHigherId: String, adNormalId: String): NativeAd? {
        val key = createAdKey(adHigherId, adNormalId)
        return adsMap[key]?.ad
    }

    fun getAdUnitId(adHigherId: String, adNormalId: String): String {
        val key = createAdKey(adHigherId, adNormalId)
        return adsMap[key]?.adUnitId ?: ""
    }

    fun removeAd(adHigherId: String, adNormalId: String) {
        val key = createAdKey(adHigherId, adNormalId)

        reloadRunnables[key]?.let { handler.removeCallbacks(it) }
        reloadRunnables.remove(key)

        adsMap[key]?.ad?.destroy()
        adsMap.remove(key)
        retryConfigMap.remove(key)
        adFlowMap.remove(key)
    }

    fun clearAll() {
        reloadRunnables.values.forEach { handler.removeCallbacks(it) }
        reloadRunnables.clear()

        adsMap.values.forEach { it.ad?.destroy() }
        adsMap.clear()
        retryConfigMap.clear()
        adFlowMap.clear()
    }

    fun forceReload(
        adHigherId: String,
        adNormalId: String,
        showHigher: Boolean,
        showNormal: Boolean,
        retryConfig: RetryConfig? = null,
        onLoaded: (NativeAd) -> Unit = {},
        onFailed: (String) -> Unit = {}
    ) {
        removeAd(adHigherId, adNormalId)
        loadAd(adHigherId, adNormalId, showHigher, showNormal, retryConfig, onLoaded, onFailed)
    }
}
