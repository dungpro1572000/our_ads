package com.dungz.our_ads.controller

import android.app.Activity
import com.dungz.our_ads.AppAdMob
import com.dungz.our_ads.state.InterAdState
import com.dungz.our_ads.utils.AdLogger
import com.dungz.our_ads.utils.getHighNormalAdById
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

object InterAdsController {
    val listAds = hashMapOf<String, MutableStateFlow<InterAdState>>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    const val TAG = "ControllerInterAds"

    fun isAdsLoading(key: String) = listAds[key]?.value is InterAdState.Loading
    fun canPreloadAds(key: String): Boolean {
        return listAds[key]?.value !is InterAdState.Loaded &&
                listAds[key]?.value !is InterAdState.Loading
    }

    suspend fun preloadAds(
        activity: WeakReference<Activity>,
        isShow: Boolean = true,
        adUnitId: String,
        onLoadFailed: () -> Unit,
        onLoadSuccess: () -> Unit,
    ) {
        if (!isShow) {
            AdLogger.debug(AdLogger.TYPE_INTERSTITIAL, "preloadAds skipped (isShow=false): $adUnitId")
            return
        }
        if (!canPreloadAds(adUnitId)) {
            AdLogger.debug(AdLogger.TYPE_INTERSTITIAL, "preloadAds skipped (already loading/loaded): $adUnitId")
            return
        }
        AdLogger.logLoading(AdLogger.TYPE_INTERSTITIAL, adUnitId, isHigher = false)
        listAds.getOrPut(adUnitId) { MutableStateFlow(InterAdState.Loading) }.value = InterAdState.Loading
        AppAdMob.loadInterstitialAds(activity, adUnitId, {
            listAds[adUnitId]?.value = InterAdState.Failed(it)
            AdLogger.error(AdLogger.TYPE_INTERSTITIAL, "onLoad ads failed by : ${it.message}")
            onLoadFailed()
        }, {
            listAds[adUnitId]?.value = InterAdState.Loaded(it)
            AdLogger.debug(AdLogger.TYPE_INTERSTITIAL, "onLoad ads successfully: $adUnitId")
            onLoadSuccess()
        })
    }

    fun showAds(
        activity: WeakReference<Activity>,
        isShow: Boolean = true,
        adUnitId: String,
        onShowFailed: () -> Unit,
        onShowSuccess: () -> Unit,
    ) {
        if (!isShow || listAds[adUnitId]?.value !is InterAdState.Loaded || listAds[adUnitId] == null) {
            AdLogger.warn(
                AdLogger.TYPE_INTERSTITIAL,
                "showAds skipped: $adUnitId (isShow=$isShow, state=${listAds[adUnitId]?.value})"
            )
            onShowFailed()
            return
        }
        val ad = (listAds[adUnitId]?.value as? InterAdState.Loaded)?.interstitialAd ?: return
        // Remove immediately to prevent duplicate show from another screen
        listAds.remove(adUnitId)
        AdLogger.logShowing(AdLogger.TYPE_INTERSTITIAL, adUnitId, isHigher = false)
        AppAdMob.showInterstitialAd(
            activity,
            ad,
            onAdDismissed = {
                AdLogger.logDismissed(AdLogger.TYPE_INTERSTITIAL, adUnitId)
                AdLogger.debug(AdLogger.TYPE_INTERSTITIAL, "onShow ads successfully")
                onShowSuccess()
            },
            onAdFailedToShow = {
                AdLogger.logFailedToShow(
                    AdLogger.TYPE_INTERSTITIAL,
                    adUnitId,
                    it.message
                )
                onShowFailed()
            },
            onAdClicked = {
                AdLogger.logClicked(AdLogger.TYPE_INTERSTITIAL, adUnitId)
            },
            onAdImpression = {
                AdLogger.logImpression(AdLogger.TYPE_INTERSTITIAL, adUnitId)
            })
    }

    suspend fun loadHighNormalIds(
        activity: WeakReference<Activity>,
        showHigh: Boolean = true,
        showNormal: Boolean = true,
        adUnitIdHigh: String,
        adUnitIdNormal: String,
        onLoadFailed: () -> Unit,
        onLoadSuccess: () -> Unit,
    ) {
        val key = getHighNormalAdById(adUnitIdHigh, adUnitIdNormal)
        if (!canPreloadAds(key)) {
            AdLogger.debug(
                AdLogger.TYPE_INTERSTITIAL,
                "loadHighNormalIds skipped (already loading/loaded): $key"
            )
            return
        }
        if (showHigh) {
            AdLogger.logLoading(AdLogger.TYPE_INTERSTITIAL, adUnitIdHigh, isHigher = true)
            AppAdMob.loadInterstitialAds(
                activity,
                adUnitIdHigh,
                onAdFailedToLoad = {
                    AdLogger.logFailedToLoad(
                        AdLogger.TYPE_INTERSTITIAL,
                        adUnitIdHigh,
                        isHigher = true,
                        errorCode = it.code,
                        errorMessage = it.message
                    )
                    if (showNormal) {
                        AdLogger.logFallbackToNormal(AdLogger.TYPE_INTERSTITIAL, adUnitIdNormal)
                        scope.launch {
                            AdLogger.logLoading(
                                AdLogger.TYPE_INTERSTITIAL,
                                adUnitIdNormal,
                                isHigher = false
                            )
                            AppAdMob.loadInterstitialAds(
                                activity,
                                adUnitIdNormal,
                                onLoadSuccess = {
                                    AdLogger.logLoaded(
                                        AdLogger.TYPE_INTERSTITIAL,
                                        adUnitIdNormal,
                                        isHigher = false
                                    )
                                    listAds.getOrPut(
                                        key,
                                        { MutableStateFlow(InterAdState.Loaded(it)) }
                                    )
                                    onLoadSuccess()
                                },
                                onAdFailedToLoad = {
                                    AdLogger.logFailedToLoad(
                                        AdLogger.TYPE_INTERSTITIAL,
                                        adUnitIdNormal,
                                        isHigher = false,
                                        errorCode = it.code,
                                        errorMessage = it.message
                                    )
                                    AdLogger.logAllRetriesExhausted(
                                        AdLogger.TYPE_INTERSTITIAL,
                                        adUnitIdHigh,
                                        adUnitIdNormal,
                                        totalRetries = 2
                                    )
                                    onLoadFailed()
                                    listAds.remove(key)
                                })
                        }
                    } else {
                        AdLogger.warn(
                            AdLogger.TYPE_INTERSTITIAL,
                            "Higher failed and showNormal=false, skipping fallback: $adUnitIdHigh"
                        )
                        onLoadFailed()
                    }
                },
                onLoadSuccess = {
                    AdLogger.logLoaded(
                        AdLogger.TYPE_INTERSTITIAL,
                        adUnitIdHigh,
                        isHigher = true
                    )
                    listAds.getOrPut(
                        key,
                        { MutableStateFlow(InterAdState.Loaded(it)) }
                    )
                    onLoadSuccess()
                })
        } else if (showNormal) {
            AdLogger.logLoading(AdLogger.TYPE_INTERSTITIAL, adUnitIdNormal, isHigher = false)
            AppAdMob.loadInterstitialAds(
                activity,
                adUnitIdNormal,
                onLoadSuccess = {
                    AdLogger.logLoaded(
                        AdLogger.TYPE_INTERSTITIAL,
                        adUnitIdNormal,
                        isHigher = false
                    )
                    listAds.getOrPut(
                        key,
                        { MutableStateFlow(InterAdState.Loaded(it)) }
                    )
                    onLoadSuccess()
                },
                onAdFailedToLoad = {
                    AdLogger.logFailedToLoad(
                        AdLogger.TYPE_INTERSTITIAL,
                        adUnitIdNormal,
                        isHigher = false,
                        errorCode = it.code,
                        errorMessage = it.message
                    )
                    onLoadFailed()
                    listAds.remove(key)
                })
        } else {
            AdLogger.debug(
                AdLogger.TYPE_INTERSTITIAL,
                "loadHighNormalIds skipped (showHigh=false, showNormal=false)"
            )
            return
        }
    }

    fun showHighNormalIds(
        activity: WeakReference<Activity>,
        showHigh: Boolean = true,
        showNormal: Boolean = true,
        adUnitIdHigh: String,
        adUnitIdNormal: String,
        onShowFailed: () -> Unit,
        onShowSuccess: () -> Unit,
    ) {
        val key = getHighNormalAdById(adUnitIdHigh, adUnitIdNormal)
        if (listAds[key]?.value !is InterAdState.Loaded || listAds[key] == null) {
            AdLogger.warn(
                AdLogger.TYPE_INTERSTITIAL,
                "showHighNormalIds skipped: $key (state=${listAds[key]?.value})"
            )
            onShowFailed()
            return
        }
        AdLogger.debug(AdLogger.TYPE_INTERSTITIAL, "showHighNormalIds dispatching to showAds: $key")
        showAds(
            activity,
            showHigh || showNormal,
            key,
            onShowFailed,
            onShowSuccess
        )
    }
}