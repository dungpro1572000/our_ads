package com.dungz.our_ads.controller

import android.app.Activity
import com.dungz.our_ads.AppAdMob
import com.dungz.our_ads.utils.AdLogger
import com.dungz.our_ads.state.RewardAdState
import java.lang.ref.WeakReference

object RewardAdsController {
    val listAds = hashMapOf<String, RewardAdState>()
    const val TAG = "ControllerRewardAds"

    fun isAdsLoading(key: String) = listAds[key] is RewardAdState.Loading
    fun canPreloadAds(key: String): Boolean {
        return listAds[key] !is RewardAdState.Loaded &&
                listAds[key] !is RewardAdState.Loading
    }

    suspend fun preloadAds(
        activity: WeakReference<Activity>,
        adUnitId: String,
        onLoadFailed: () -> Unit = {},
        onLoadSuccess: () -> Unit = {},
    ) {
        if (!canPreloadAds(adUnitId)) return
        listAds[adUnitId] = RewardAdState.Loading
        AppAdMob.loadRewardAds(activity, adUnitId, {
            listAds[adUnitId] = RewardAdState.Failed(it)
            AdLogger.error(AdLogger.TYPE_REWARDED, "onLoad ads failed by : ${it.message}")
            onLoadFailed()
        }, {
            listAds[adUnitId] = RewardAdState.Loaded(it)
            AdLogger.debug(AdLogger.TYPE_REWARDED, "onLoad ads successfully")
            onLoadSuccess()
        })
    }

    suspend fun showAds(
        activity: WeakReference<Activity>,
        adUnitId: String,
        onUserEarn: () -> Unit,
        onShowFailed: () -> Unit,
        onShowSuccess: () -> Unit
    ) {
        if (listAds[adUnitId] !is RewardAdState.Loaded || listAds[adUnitId] == null) {
            return
        }
        listAds[adUnitId]?.let {
            if (it is RewardAdState.Loaded) {
                AppAdMob.showRewardAds(activity, it.rewardedAd, {
                    onUserEarn()
                }, {
                    onShowSuccess()
                    listAds.remove(adUnitId)
                    AdLogger.debug(AdLogger.TYPE_REWARDED, "onShow ads successfully")
                }, {
                    listAds.remove(adUnitId)
                    onShowFailed()
                    AdLogger.error(AdLogger.TYPE_REWARDED, "onShow ads failed by : ${it.message}")
                })
            }
        }
    }
}