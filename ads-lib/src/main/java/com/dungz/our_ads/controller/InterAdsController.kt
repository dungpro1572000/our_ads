package com.dungz.our_ads.controller

import android.app.Activity
import android.util.Log
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
        if (!isShow || !canPreloadAds(adUnitId)) return
        listAds[adUnitId]?.value = InterAdState.Loading
        AppAdMob.loadInterstitialAds(activity, adUnitId, {
            listAds[adUnitId]?.value = InterAdState.Failed(it)
            onLoadFailed()
        }, {
            listAds[adUnitId]?.value = InterAdState.Loaded(it)
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
            onShowFailed()
            return
        }
        listAds[adUnitId]?.let {
            if (it.value is InterAdState.Loaded) {
                AppAdMob.showInterstitialAd(
                    activity,
                    (it.value as InterAdState.Loaded).interstitialAd,
                    {
                        listAds.remove(adUnitId)
                        AdLogger.debug(AdLogger.TYPE_INTERSTITIAL, "onShow ads successfully")
                        onShowSuccess()
                    },
                    {
                        listAds.remove(adUnitId)
                        AdLogger.error(
                            AdLogger.TYPE_INTERSTITIAL,
                            "onShow ads failed by : ${it.message}"
                        )
                        onShowFailed()
                    })
            }
        }
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
        if (!canPreloadAds(getHighNormalAdById(adUnitIdHigh, adUnitIdNormal))) return
        if (showHigh) {
            AppAdMob.loadInterstitialAds(
                activity,
                adUnitIdHigh,
                onAdFailedToLoad = {
                    if (showNormal) {
                        scope.launch {
                            AppAdMob.loadInterstitialAds(
                                activity,
                                adUnitIdNormal,
                                onLoadSuccess = {
                                    listAds.getOrPut(
                                        getHighNormalAdById(adUnitIdHigh, adUnitIdNormal),
                                        { MutableStateFlow(InterAdState.Loaded(it)) }
                                    )
                                    onLoadSuccess()
                                },
                                onAdFailedToLoad = {
                                    onLoadFailed()
                                    listAds.remove(getHighNormalAdById(adUnitIdHigh, adUnitIdNormal))
                                })
                        }
                    } else {
                        onLoadFailed()
                    }
                },
                onLoadSuccess = {
                    listAds.getOrPut(
                        getHighNormalAdById(adUnitIdHigh, adUnitIdNormal),
                        { MutableStateFlow(InterAdState.Loaded(it)) }
                    )
                    onLoadSuccess()
                })
        } else if (showNormal) {
            AppAdMob.loadInterstitialAds(
                activity,
                adUnitIdNormal,
                onLoadSuccess = {
                    listAds.getOrPut(
                        getHighNormalAdById(adUnitIdHigh, adUnitIdNormal),
                        { MutableStateFlow(InterAdState.Loaded(it)) }
                    )
                    onLoadSuccess()
                },
                onAdFailedToLoad = {
                    onLoadFailed()
                    listAds.remove(getHighNormalAdById(adUnitIdHigh, adUnitIdNormal))
                })
        } else {
            Log.d(TAG, "dont preload for any ad")
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
        if (listAds[getHighNormalAdById(
                adUnitIdHigh,
                adUnitIdNormal
            )]?.value !is InterAdState.Loaded || listAds[getHighNormalAdById(
                adUnitIdHigh,
                adUnitIdNormal
            )] == null
        ) {
            onShowFailed()
            return
        }
        showAds(
            activity,
            showHigh || showNormal,
            getHighNormalAdById(adUnitIdHigh, adUnitIdNormal),
            onShowFailed,
            onShowSuccess
        )
    }
}