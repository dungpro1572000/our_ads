package com.dungz.our_ads.controller

import android.app.Activity
import com.dungz.our_ads.AppAdMob
import com.dungz.our_ads.state.InterAdState
import java.lang.ref.WeakReference

object InterAdsController {
    val listAds = hashMapOf<String, InterAdState>()

    fun isAdsLoading(key: String) = listAds[key] is InterAdState.Loading
    fun canPreloadAds(key: String): Boolean {
        return listAds[key] !is InterAdState.Loaded ||
                listAds[key] !is InterAdState.Loading
    }

    suspend fun preloadAds(
        activity: WeakReference<Activity>,
        adUnitId: String,
        preloadKey: String
    ) {
        if (!canPreloadAds(preloadKey)) return
        listAds[preloadKey] = InterAdState.Loading
        AppAdMob.loadInterstitialAds(activity, adUnitId, {
            listAds[preloadKey] = InterAdState.Failed(it)
        }, {
            listAds[preloadKey] = InterAdState.Loaded(it)
        })
    }
}