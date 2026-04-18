package com.dungz.our_ads.controller

import android.app.Activity
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
        if (!isShow) {
            AdLogger.debug(AdLogger.TYPE_REWARDED, "preloadAds skipped (isShow=false): $adUnitId")
            return
        }
        if (!canPreloadAds(adUnitId)) {
            AdLogger.debug(
                AdLogger.TYPE_REWARDED,
                "preloadAds skipped (already loading/loaded): $adUnitId"
            )
            return
        }
        AdLogger.logLoading(AdLogger.TYPE_REWARDED, adUnitId, isHigher = false)
        listAds[adUnitId]?.value = RewardAdState.Loading
        AppAdMob.loadRewardAds(activity, adUnitId, {
            listAds[adUnitId]?.value = RewardAdState.Failed(it)
            AdLogger.logFailedToLoad(
                AdLogger.TYPE_REWARDED,
                adUnitId,
                isHigher = false,
                errorCode = it.code,
                errorMessage = it.message
            )
            AdLogger.error(AdLogger.TYPE_REWARDED, "onLoad ads failed by : ${it.message}")
            onLoadFailed()
        }, {
            listAds[adUnitId]?.value = RewardAdState.Loaded(it)
            AdLogger.logLoaded(AdLogger.TYPE_REWARDED, adUnitId, isHigher = false)
            AdLogger.debug(AdLogger.TYPE_REWARDED, "onLoad ads successfully: $adUnitId")
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
            AdLogger.warn(
                AdLogger.TYPE_REWARDED,
                "showAds skipped: $adUnitId (isShow=$isShow, state=${listAds[adUnitId]?.value})"
            )
            onShowFailed()
            return
        }
        listAds[adUnitId]?.let {
            if (it.value is RewardAdState.Loaded) {
                AdLogger.logShowing(AdLogger.TYPE_REWARDED, adUnitId, isHigher = false)
                AppAdMob.showRewardAds(
                    activity,
                    (it.value as RewardAdState.Loaded).rewardedAd,
                    { rewardItem ->
                        AdLogger.logRewardEarned(
                            AdLogger.TYPE_REWARDED,
                            rewardType = rewardItem.type,
                            rewardAmount = rewardItem.amount
                        )
                        onUserEarn()
                    },
                    {
                        listAds.remove(adUnitId)
                        AdLogger.logDismissed(AdLogger.TYPE_REWARDED, adUnitId)
                        AdLogger.debug(AdLogger.TYPE_REWARDED, "onShow ads successfully")
                        onShowSuccess()
                    },
                    {
                        listAds.remove(adUnitId)
                        AdLogger.logFailedToShow(
                            AdLogger.TYPE_REWARDED,
                            adUnitId,
                            it.message
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
        val key = getHighNormalAdById(adUnitIdHigh, adUnitIdNormal)
        if (!canPreloadAds(key)) {
            AdLogger.debug(
                AdLogger.TYPE_REWARDED,
                "loadHighNormalIds skipped (already loading/loaded): $key"
            )
            return
        }
        if (showHigh) {
            AdLogger.logLoading(AdLogger.TYPE_REWARDED, adUnitIdHigh, isHigher = true)
            AppAdMob.loadRewardAds(
                activity,
                adUnitIdHigh,
                onAdFailedToLoad = {
                    AdLogger.logFailedToLoad(
                        AdLogger.TYPE_REWARDED,
                        adUnitIdHigh,
                        isHigher = true,
                        errorCode = it.code,
                        errorMessage = it.message
                    )
                    if (showNormal) {
                        AdLogger.logFallbackToNormal(AdLogger.TYPE_REWARDED, adUnitIdNormal)
                        scope.launch {
                            AdLogger.logLoading(
                                AdLogger.TYPE_REWARDED,
                                adUnitIdNormal,
                                isHigher = false
                            )
                            AppAdMob.loadRewardAds(
                                activity,
                                adUnitIdNormal,
                                onLoadSuccess = {
                                    AdLogger.logLoaded(
                                        AdLogger.TYPE_REWARDED,
                                        adUnitIdNormal,
                                        isHigher = false
                                    )
                                    listAds.getOrPut(
                                        key,
                                        { MutableStateFlow(RewardAdState.Loaded(it)) }
                                    )
                                    onLoadSuccess()
                                },
                                onAdFailedToLoad = {
                                    AdLogger.logFailedToLoad(
                                        AdLogger.TYPE_REWARDED,
                                        adUnitIdNormal,
                                        isHigher = false,
                                        errorCode = it.code,
                                        errorMessage = it.message
                                    )
                                    AdLogger.logAllRetriesExhausted(
                                        AdLogger.TYPE_REWARDED,
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
                            AdLogger.TYPE_REWARDED,
                            "Higher failed and showNormal=false, skipping fallback: $adUnitIdHigh"
                        )
                        onLoadFailed()
                    }
                },
                onLoadSuccess = {
                    AdLogger.logLoaded(AdLogger.TYPE_REWARDED, adUnitIdHigh, isHigher = true)
                    listAds.getOrPut(
                        key,
                        { MutableStateFlow(RewardAdState.Loaded(it)) }
                    )
                    onLoadSuccess()
                })
        } else if (showNormal) {
            AdLogger.logLoading(AdLogger.TYPE_REWARDED, adUnitIdNormal, isHigher = false)
            AppAdMob.loadRewardAds(
                activity,
                adUnitIdNormal,
                onLoadSuccess = {
                    AdLogger.logLoaded(AdLogger.TYPE_REWARDED, adUnitIdNormal, isHigher = false)
                    listAds.getOrPut(
                        key,
                        { MutableStateFlow(RewardAdState.Loaded(it)) }
                    )
                    onLoadSuccess()
                },
                onAdFailedToLoad = {
                    AdLogger.logFailedToLoad(
                        AdLogger.TYPE_REWARDED,
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
                AdLogger.TYPE_REWARDED,
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
        onUserEarn: () -> Unit,
        onShowFailed: () -> Unit,
        onShowSuccess: () -> Unit,
    ) {
        val key = getHighNormalAdById(adUnitIdHigh, adUnitIdNormal)
        if (listAds[key]?.value !is RewardAdState.Loaded || listAds[key] == null) {
            AdLogger.warn(
                AdLogger.TYPE_REWARDED,
                "showHighNormalIds skipped: $key (state=${listAds[key]?.value})"
            )
            onShowFailed()
            return
        }
        AdLogger.debug(AdLogger.TYPE_REWARDED, "showHighNormalIds dispatching to showAds: $key")
        showAds(
            activity,
            showHigh || showNormal,
            key,
            onUserEarn,
            onShowFailed,
            onShowSuccess
        )
    }
}
