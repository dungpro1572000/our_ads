package com.dungz.our_ads.controller

import android.app.Activity
import android.util.Log
import com.dungz.our_ads.AppAdMob
import com.dungz.our_ads.state.RewardAdState
import com.dungz.our_ads.utils.AdLogger
import com.dungz.our_ads.utils.getHighNormalAdById
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

object RewardAdsController {
    val listAds = hashMapOf<String, MutableStateFlow<RewardAdState>>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    const val TAG = "ControllerRewardAds"

    fun isAdsLoading(key: String) = listAds[key]?.value is RewardAdState.Loading
    fun canPreloadAds(key: String): Boolean {
        return listAds[key]?.value !is RewardAdState.Loaded &&
                listAds[key]?.value !is RewardAdState.Loading
    }

    suspend fun preloadAds(
        activity: WeakReference<Activity>,
        isShow: Boolean = true,
        adUnitId: String,
        onLoadFailed: () -> Unit = {},
        onLoadSuccess: () -> Unit = {},
    ) {
        if (!isShow || !canPreloadAds(adUnitId)) return
        listAds[adUnitId]?.value = RewardAdState.Loading
        AppAdMob.loadRewardAds(activity, adUnitId, {
            listAds[adUnitId]?.value = RewardAdState.Failed(it)
            AdLogger.error(AdLogger.TYPE_REWARDED, "onLoad ads failed by : ${it.message}")
            onLoadFailed()
        }, {
            listAds[adUnitId]?.value = RewardAdState.Loaded(it)
            AdLogger.debug(AdLogger.TYPE_REWARDED, "onLoad ads successfully")
            onLoadSuccess()
        })
    }

    fun showAds(
        activity: WeakReference<Activity>,
        isShow: Boolean = true,
        adUnitId: String,
        onUserEarn: () -> Unit,
        onShowFailed: () -> Unit,
        onShowSuccess: () -> Unit,
    ) {
        if (!isShow || listAds[adUnitId]?.value !is RewardAdState.Loaded || listAds[adUnitId] == null) {
            onShowFailed()
            return
        }
        listAds[adUnitId]?.let {
            if (it.value is RewardAdState.Loaded) {
                AppAdMob.showRewardAds(
                    activity,
                    (it.value as RewardAdState.Loaded).rewardedAd,
                    {
                        onUserEarn()
                    },
                    {
                        listAds.remove(adUnitId)
                        AdLogger.debug(AdLogger.TYPE_REWARDED, "onShow ads successfully")
                        onShowSuccess()
                    },
                    {
                        listAds.remove(adUnitId)
                        AdLogger.error(
                            AdLogger.TYPE_REWARDED,
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
            AppAdMob.loadRewardAds(
                activity,
                adUnitIdHigh,
                onAdFailedToLoad = {
                    if (showNormal) {
                        scope.launch {
                            AppAdMob.loadRewardAds(
                                activity,
                                adUnitIdNormal,
                                onLoadSuccess = {
                                    listAds.getOrPut(
                                        getHighNormalAdById(adUnitIdHigh, adUnitIdNormal),
                                        { MutableStateFlow(RewardAdState.Loaded(it)) }
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
                        { MutableStateFlow(RewardAdState.Loaded(it)) }
                    )
                    onLoadSuccess()
                })
        } else if (showNormal) {
            AppAdMob.loadRewardAds(
                activity,
                adUnitIdNormal,
                onLoadSuccess = {
                    listAds.getOrPut(
                        getHighNormalAdById(adUnitIdHigh, adUnitIdNormal),
                        { MutableStateFlow(RewardAdState.Loaded(it)) }
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
        onUserEarn: () -> Unit,
        onShowFailed: () -> Unit,
        onShowSuccess: () -> Unit,
    ) {
        if (listAds[getHighNormalAdById(
                adUnitIdHigh,
                adUnitIdNormal
            )]?.value !is RewardAdState.Loaded || listAds[getHighNormalAdById(
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
            onUserEarn,
            onShowFailed,
            onShowSuccess
        )
    }
}
