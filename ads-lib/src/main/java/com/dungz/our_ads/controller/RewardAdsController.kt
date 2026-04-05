package com.dungz.our_ads.controller

import android.app.Activity
import com.dungz.our_ads.AppAdMob
import com.dungz.our_ads.state.RewardAdState
import java.lang.ref.WeakReference

object RewardAdsController {
    val listAds = hashMapOf<String, RewardAdState>()

    fun isAdsLoading(key: String) = listAds[key] is RewardAdState.Loading
    fun canPreloadAds(key: String): Boolean {
        return listAds[key] !is RewardAdState.Loaded ||
                listAds[key] !is RewardAdState.Loading
    }

    suspend fun preloadAds(
        activity: WeakReference<Activity>,
        adUnitId: String,
        preloadKey: String
    ) {
        if (!canPreloadAds(preloadKey)) return
        listAds[preloadKey] = RewardAdState.Loading
        AppAdMob.loadRewardAds(activity, adUnitId, {
            listAds[preloadKey] = RewardAdState.Failed(it)
        }, {
            listAds[preloadKey] = RewardAdState.Loaded(it)
        })
    }
}