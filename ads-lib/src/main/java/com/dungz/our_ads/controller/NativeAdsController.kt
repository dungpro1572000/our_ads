package com.dungz.our_ads.controller

import android.app.Activity
import android.util.Log
import android.view.LayoutInflater
import android.widget.ImageButton
import androidx.annotation.LayoutRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dungz.our_ads.AppAdMob
import com.dungz.our_ads.R
import com.dungz.our_ads.utils.AdLogger
import com.dungz.our_ads.remotedata.RemoteConfigData
import com.dungz.our_ads.state.NativeAdState
import com.dungz.our_ads.utils.DefaultNativeAdShimmer
import com.dungz.our_ads.utils.bindNativeAd
import com.dungz.our_ads.utils.getHighNormalAdById
import com.dungz.our_ads.utils.inflateNativeAdView
import com.google.android.gms.ads.nativead.NativeAdView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

object NativeAdsController {
    val listAds = mutableStateMapOf<String, MutableStateFlow<NativeAdState>>()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    const val TAG = "ControllerNativeAds"

    fun isAdsLoading(key: String) = listAds[key]?.value is NativeAdState.Loading
    fun canPreloadAds(key: String): Boolean {
        return listAds[key]?.value !is NativeAdState.Loaded &&
                listAds[key]?.value !is NativeAdState.Loading
    }

    suspend fun preloadAds(
        activity: WeakReference<Activity>,
        isShow: Boolean = true,
        adUnitId: String,
        onLoadFailed: () -> Unit = {},
        onLoadSuccess: () -> Unit = {},
    ) {
        if (!isShow || !canPreloadAds(adUnitId) || RemoteConfigData.get(RemoteConfigData.ENABLE_ADS) != true) return
        listAds.getOrPut(adUnitId) { MutableStateFlow(NativeAdState.Loading) }
        listAds[adUnitId]?.emit(NativeAdState.Loading)
        AppAdMob.loadSingleNativeAds(activity, adUnitId, {}, {
        }, {
            listAds[adUnitId]?.value = NativeAdState.Failed(it)
            AdLogger.error(AdLogger.TYPE_NATIVE, "onLoad ads failed by : ${it.message}")
            onLoadFailed()
        }, {
            listAds[adUnitId]?.value = NativeAdState.Loaded(it)
            AdLogger.debug(AdLogger.TYPE_NATIVE, "onLoad ads successfully")
            onLoadSuccess()
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
        if (!canPreloadAds(getHighNormalAdById(adUnitIdHigh, adUnitIdNormal))) return
        if (showHigh) {
            AppAdMob.loadSingleNativeAds(
                activity,
                adUnitIdHigh,
                onAdClick = {},
                onAdImpression = {},
                onAdFailedToLoad = {
                    if (showNormal) {
                        scope.launch {
                            AppAdMob.loadSingleNativeAds(
                                activity,
                                adUnitIdNormal,
                                onAdClick = {},
                                onAdImpression = {},
                                onLoadSuccess = {
                                    listAds.getOrPut(
                                        getHighNormalAdById(adUnitIdHigh, adUnitIdNormal),
                                        { MutableStateFlow(NativeAdState.Loaded(it)) }
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
                        { MutableStateFlow(NativeAdState.Loaded(it)) }
                    )
                    onLoadSuccess()
                })
        } else if (showNormal) {
            AppAdMob.loadSingleNativeAds(
                activity,
                adUnitIdNormal,
                onAdClick = {},
                onAdImpression = {},
                onLoadSuccess = {
                    listAds.getOrPut(
                        getHighNormalAdById(adUnitIdHigh, adUnitIdNormal),
                        { MutableStateFlow(NativeAdState.Loaded(it)) }
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

    @Composable
    fun FullScreenNativeContainerAdView(
        adId: String,
        lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
        shimmerAds: (@Composable () -> Unit)? = null,
        onCloseClick: () -> Unit
    ) {
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_DESTROY) {
                    listAds.remove(adId)
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
        when (val state = listAds[adId]?.collectAsStateWithLifecycle()?.value) {
            is NativeAdState.Loading -> {
                if (shimmerAds != null) {
                    shimmerAds()
                } else {
                    DefaultNativeAdShimmer()
                }
            }

            is NativeAdState.Loaded -> {
                Dialog(
                    onDismissRequest = onCloseClick,
                    properties = DialogProperties(
                        usePlatformDefaultWidth = false,
                        dismissOnBackPress = true,
                        dismissOnClickOutside = false
                    )
                ) {
                    AndroidView(
                        factory = { context ->
                            val root = LayoutInflater.from(context)
                                .inflate(R.layout.native_ad_fullscreen, null) as android.widget.FrameLayout

                            root.findViewById<ImageButton>(R.id.btn_close)?.setOnClickListener {
                                onCloseClick()
                            }

                            val adView = root.findViewById<NativeAdView>(R.id.native_ad_view)
                            adView.headlineView = adView.findViewById(R.id.ad_headline)
                            adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
                            adView.mediaView = adView.findViewById(R.id.ad_media)
                            adView.bodyView = adView.findViewById(R.id.ad_body)
                            adView.iconView = adView.findViewById(R.id.ad_app_icon)
                            adView.advertiserView = adView.findViewById(R.id.ad_advertiser)
                            adView.starRatingView = adView.findViewById(R.id.ad_stars)

                            bindNativeAd(adView, state.nativeAd)
                            root
                        },
                        modifier = Modifier.fillMaxSize(),
                        update = { root ->
                            val adView = root.findViewById<NativeAdView>(R.id.native_ad_view)
                            bindNativeAd(adView, state.nativeAd)
                        }
                    )
                }
            }

            else -> {}
        }
    }

    @Composable
    fun MediumNativeContainerAdView(
        modifier: Modifier = Modifier,
        activity: WeakReference<Activity>,
        adId: String,
        lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
        @LayoutRes nativeLayout: Int = R.layout.native_ad_medium,
        shimmerAds: (@Composable () -> Unit)? = null
    ) {
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_DESTROY) {
                    listAds.remove(adId)
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        LaunchedEffect(Unit) {
            if (listAds[adId] == null){
                preloadAds(activity, adUnitId = adId)
            }
        }

        when (val state = listAds[adId]?.collectAsStateWithLifecycle()?.value) {
            is NativeAdState.Loading -> {
                if (shimmerAds != null) {
                    shimmerAds()
                } else {
                    DefaultNativeAdShimmer()
                }
            }

            is NativeAdState.Loaded -> {
                AndroidView(
                    factory = { context -> inflateNativeAdView(context, nativeLayout) },
                    modifier = modifier.fillMaxWidth(),
                    update = { adView -> bindNativeAd(adView, state.nativeAd) }
                )
            }

            else -> {}
        }
    }
}