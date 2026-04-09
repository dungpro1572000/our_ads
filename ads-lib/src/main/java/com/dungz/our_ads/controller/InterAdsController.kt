package com.dungz.our_ads.controller

import android.app.Activity
import com.dungz.our_ads.AppAdMob
import com.dungz.our_ads.utils.AdLogger
import com.dungz.our_ads.state.InterAdState
import java.lang.ref.WeakReference

object InterAdsController {
    val listAds = hashMapOf<String, InterAdState>()
    const val TAG = "ControllerInterAds"

    fun isAdsLoading(key: String) = listAds[key] is InterAdState.Loading
    fun canPreloadAds(key: String): Boolean {
        return listAds[key] !is InterAdState.Loaded &&
                listAds[key] !is InterAdState.Loading
    }

    suspend fun preloadAds(
        activity: WeakReference<Activity>,
        adUnitId: String,
        onLoadFailed: () -> Unit,
        onLoadSuccess: () -> Unit,
    ) {
        if (!canPreloadAds(adUnitId)) return
        listAds[adUnitId] = InterAdState.Loading
        AppAdMob.loadInterstitialAds(activity, adUnitId, {
            listAds[adUnitId] = InterAdState.Failed(it)
            onLoadFailed()
        }, {
            listAds[adUnitId] = InterAdState.Loaded(it)
            onLoadSuccess()
        })
    }

    suspend fun showAds(
        activity: WeakReference<Activity>,
        adUnitId: String,
        onShowFailed: () -> Unit,
        onShowSuccess: () -> Unit,
    ) {
        if (listAds[adUnitId] !is InterAdState.Loaded || listAds[adUnitId] == null) {
            onShowFailed()
            return
        }
        listAds[adUnitId]?.let {
            if (it is InterAdState.Loaded) {
                AppAdMob.showInterstitialAd(activity, it.interstitialAd, {
                    listAds.remove(adUnitId)
                    AdLogger.debug(AdLogger.TYPE_INTERSTITIAL, "onShow ads successfully")
                    onShowSuccess()
                }, {
                    listAds.remove(adUnitId)
                    AdLogger.error(AdLogger.TYPE_INTERSTITIAL, "onShow ads failed by : ${it.message}")
                    onShowFailed()
                })
            }
        }
    }
}