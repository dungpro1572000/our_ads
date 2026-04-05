package com.dungz.our_ads.controller

import android.app.Activity
import com.dungz.our_ads.AppAdMob
import com.dungz.our_ads.remotedata.RemoteConfigData
import com.dungz.our_ads.state.NativeAdState
import java.lang.ref.WeakReference

object NativeAdsController {
    val listAds = hashMapOf<String, NativeAdState>()

    fun isAdsLoading(key: String) = listAds[key] is NativeAdState.Loading
    fun canPreloadAds(key: String): Boolean {
        return listAds[key] !is NativeAdState.Loaded ||
                listAds[key] !is NativeAdState.Loading
    }

    suspend fun preloadAds(
        activity: WeakReference<Activity>,
        adUnitId: String,
        preloadKey: String
    ) {
        if (!canPreloadAds(preloadKey) || RemoteConfigData.get(RemoteConfigData.ENABLE_ADS) == true) return
        listAds[preloadKey] = NativeAdState.Loading
        AppAdMob.loadSingleNativeAds(activity, adUnitId, {}, {
        }, {
            listAds[preloadKey] = NativeAdState.Failed(it)
        }, {
            listAds[preloadKey] = NativeAdState.Loaded(it)
        })
    }

}