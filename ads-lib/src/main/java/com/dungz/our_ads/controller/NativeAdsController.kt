package com.dungz.our_ads.controller

import android.app.Activity
import android.view.LayoutInflater
import android.widget.ImageButton
import androidx.annotation.LayoutRes
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
        if (!isShow) {
            AdLogger.debug(AdLogger.TYPE_NATIVE, "preloadAds skipped (isShow=false): $adUnitId")
            return
        }
        if (!canPreloadAds(adUnitId)) {
            AdLogger.debug(
                AdLogger.TYPE_NATIVE,
                "preloadAds skipped (already loading/loaded): $adUnitId"
            )
            return
        }
        if (RemoteConfigData.get(RemoteConfigData.ENABLE_ADS) != true) {
            AdLogger.warn(
                AdLogger.TYPE_NATIVE,
                "preloadAds skipped (ENABLE_ADS remote config is off): $adUnitId"
            )
            return
        }
        AdLogger.logLoading(AdLogger.TYPE_NATIVE, adUnitId, isHigher = false)
        listAds.getOrPut(adUnitId) { MutableStateFlow(NativeAdState.Loading) }
        listAds[adUnitId]?.emit(NativeAdState.Loading)
        AppAdMob.loadSingleNativeAds(activity, adUnitId, {
            AdLogger.logClicked(AdLogger.TYPE_NATIVE, adUnitId)
        }, {
            AdLogger.logImpression(AdLogger.TYPE_NATIVE, adUnitId)
        }, {
            listAds[adUnitId]?.value = NativeAdState.Failed(it)
            AdLogger.logFailedToLoad(
                AdLogger.TYPE_NATIVE,
                adUnitId,
                isHigher = false,
                errorCode = it.code,
                errorMessage = it.message
            )
            AdLogger.error(AdLogger.TYPE_NATIVE, "onLoad ads failed by : ${it.message}")
            onLoadFailed()
        }, {
            listAds[adUnitId]?.value = NativeAdState.Loaded(it)
            AdLogger.logLoaded(AdLogger.TYPE_NATIVE, adUnitId, isHigher = false)
            AdLogger.debug(AdLogger.TYPE_NATIVE, "onLoad ads successfully: $adUnitId")
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
        val key = getHighNormalAdById(adUnitIdHigh, adUnitIdNormal)
        if (!canPreloadAds(key)) {
            AdLogger.debug(
                AdLogger.TYPE_NATIVE,
                "loadHighNormalIds skipped (already loading/loaded): $key"
            )
            return
        }
        if (showHigh) {
            AdLogger.logLoading(AdLogger.TYPE_NATIVE, adUnitIdHigh, isHigher = true)
            AppAdMob.loadSingleNativeAds(
                activity,
                adUnitIdHigh,
                onAdClick = { AdLogger.logClicked(AdLogger.TYPE_NATIVE, adUnitIdHigh) },
                onAdImpression = { AdLogger.logImpression(AdLogger.TYPE_NATIVE, adUnitIdHigh) },
                onAdFailedToLoad = {
                    AdLogger.logFailedToLoad(
                        AdLogger.TYPE_NATIVE,
                        adUnitIdHigh,
                        isHigher = true,
                        errorCode = it.code,
                        errorMessage = it.message
                    )
                    if (showNormal) {
                        AdLogger.logFallbackToNormal(AdLogger.TYPE_NATIVE, adUnitIdNormal)
                        scope.launch {
                            AdLogger.logLoading(
                                AdLogger.TYPE_NATIVE,
                                adUnitIdNormal,
                                isHigher = false
                            )
                            AppAdMob.loadSingleNativeAds(
                                activity,
                                adUnitIdNormal,
                                onAdClick = {
                                    AdLogger.logClicked(AdLogger.TYPE_NATIVE, adUnitIdNormal)
                                },
                                onAdImpression = {
                                    AdLogger.logImpression(AdLogger.TYPE_NATIVE, adUnitIdNormal)
                                },
                                onLoadSuccess = {
                                    AdLogger.logLoaded(
                                        AdLogger.TYPE_NATIVE,
                                        adUnitIdNormal,
                                        isHigher = false
                                    )
                                    listAds.getOrPut(
                                        key,
                                        { MutableStateFlow(NativeAdState.Loaded(it)) }
                                    )
                                    onLoadSuccess()
                                },
                                onAdFailedToLoad = {
                                    AdLogger.logFailedToLoad(
                                        AdLogger.TYPE_NATIVE,
                                        adUnitIdNormal,
                                        isHigher = false,
                                        errorCode = it.code,
                                        errorMessage = it.message
                                    )
                                    AdLogger.logAllRetriesExhausted(
                                        AdLogger.TYPE_NATIVE,
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
                            AdLogger.TYPE_NATIVE,
                            "Higher failed and showNormal=false, skipping fallback: $adUnitIdHigh"
                        )
                        onLoadFailed()
                    }
                },
                onLoadSuccess = {
                    AdLogger.logLoaded(AdLogger.TYPE_NATIVE, adUnitIdHigh, isHigher = true)
                    listAds.getOrPut(
                        key,
                        { MutableStateFlow(NativeAdState.Loaded(it)) }
                    )
                    onLoadSuccess()
                })
        } else if (showNormal) {
            AdLogger.logLoading(AdLogger.TYPE_NATIVE, adUnitIdNormal, isHigher = false)
            AppAdMob.loadSingleNativeAds(
                activity,
                adUnitIdNormal,
                onAdClick = { AdLogger.logClicked(AdLogger.TYPE_NATIVE, adUnitIdNormal) },
                onAdImpression = { AdLogger.logImpression(AdLogger.TYPE_NATIVE, adUnitIdNormal) },
                onLoadSuccess = {
                    AdLogger.logLoaded(AdLogger.TYPE_NATIVE, adUnitIdNormal, isHigher = false)
                    listAds.getOrPut(
                        key,
                        { MutableStateFlow(NativeAdState.Loaded(it)) }
                    )
                    onLoadSuccess()
                },
                onAdFailedToLoad = {
                    AdLogger.logFailedToLoad(
                        AdLogger.TYPE_NATIVE,
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
                AdLogger.TYPE_NATIVE,
                "loadHighNormalIds skipped (showHigh=false, showNormal=false)"
            )
            return
        }
    }

    @Composable
    fun FullScreenNativeContainerAdView(
        adId: String,
        isShow: Boolean = true,
        lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
        shimmerAds: (@Composable () -> Unit)? = null,
        onCloseClick: () -> Unit
    ) {
        if (!isShow) return

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_DESTROY) {
                    AdLogger.debug(
                        AdLogger.TYPE_NATIVE_FULLSCREEN,
                        "Lifecycle ON_DESTROY, clearing ad: $adId"
                    )
                    listAds.remove(adId)
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
        // Remember ad locally so removing from map doesn't break rendering
        var consumedAd by remember { mutableStateOf<com.google.android.gms.ads.nativead.NativeAd?>(null) }
        val state = listAds[adId]?.collectAsStateWithLifecycle()?.value

        // Consume ad: save locally and remove from map to prevent duplicate show
        LaunchedEffect(state) {
            if (state is NativeAdState.Loaded && consumedAd == null) {
                consumedAd = state.nativeAd
                listAds.remove(adId)
                AdLogger.logShowing(AdLogger.TYPE_NATIVE_FULLSCREEN, adId, isHigher = false)
            }
        }

        when {
            consumedAd != null -> {
                val nativeAd = consumedAd!!
                Dialog(
                    onDismissRequest = {
                        AdLogger.logDismissed(AdLogger.TYPE_NATIVE_FULLSCREEN, adId)
                        onCloseClick()
                    },
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
                                AdLogger.debug(
                                    AdLogger.TYPE_NATIVE_FULLSCREEN,
                                    "Close button clicked: $adId"
                                )
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

                            bindNativeAd(adView, nativeAd)
                            root
                        },
                        modifier = Modifier.fillMaxSize(),
                        update = { root ->
                            val adView = root.findViewById<NativeAdView>(R.id.native_ad_view)
                            bindNativeAd(adView, nativeAd)
                        }
                    )
                }
            }

            state is NativeAdState.Loading -> {
                AdLogger.debug(
                    AdLogger.TYPE_NATIVE_FULLSCREEN,
                    "Rendering shimmer while loading: $adId"
                )
                if (shimmerAds != null) {
                    shimmerAds()
                } else {
                    DefaultNativeAdShimmer()
                }
            }

            state is NativeAdState.Failed -> {
                AdLogger.error(
                    AdLogger.TYPE_NATIVE_FULLSCREEN,
                    "Render skipped (state=Failed): $adId | ${state.e.message}"
                )
            }

            state is NativeAdState.NotLoaded -> {
                AdLogger.debug(
                    AdLogger.TYPE_NATIVE_FULLSCREEN,
                    "Render skipped (state=NotLoaded): $adId"
                )
            }

            else -> {
                AdLogger.debug(
                    AdLogger.TYPE_NATIVE_FULLSCREEN,
                    "Render skipped (no state registered for key): $adId"
                )
            }
        }
    }

    @Composable
    fun MediumNativeContainerAdView(
        modifier: Modifier = Modifier,
        activity: WeakReference<Activity>,
        adId: String,
        isShow: Boolean = true,
        lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
        @LayoutRes nativeLayout: Int = R.layout.native_ad_medium,
        shimmerAds: (@Composable () -> Unit)? = null
    ) {
        if (!isShow) return

        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_DESTROY) {
                    AdLogger.debug(
                        AdLogger.TYPE_NATIVE,
                        "Lifecycle ON_DESTROY, clearing ad: $adId"
                    )
                    listAds.remove(adId)
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }

        // Remember ad locally so removing from map doesn't break rendering
        var consumedAd by remember { mutableStateOf<com.google.android.gms.ads.nativead.NativeAd?>(null) }
        val state = listAds[adId]?.collectAsStateWithLifecycle()?.value

        LaunchedEffect(Unit) {
            if (listAds[adId] == null) {
                AdLogger.debug(
                    AdLogger.TYPE_NATIVE,
                    "MediumNativeContainerAdView: no cached ad, triggering preload: $adId"
                )
                preloadAds(activity, adUnitId = adId)
            } else {
                AdLogger.debug(
                    AdLogger.TYPE_NATIVE,
                    "MediumNativeContainerAdView: reusing cached ad: $adId"
                )
            }
        }

        // Consume ad: save locally and remove from map to prevent duplicate show
        LaunchedEffect(state) {
            if (state is NativeAdState.Loaded && consumedAd == null) {
                consumedAd = state.nativeAd
                listAds.remove(adId)
                AdLogger.logShowing(AdLogger.TYPE_NATIVE, adId, isHigher = false)
            }
        }

        when {
            consumedAd != null -> {
                AndroidView(
                    factory = { context -> inflateNativeAdView(context, nativeLayout) },
                    modifier = modifier.fillMaxWidth(),
                    update = { adView -> bindNativeAd(adView, consumedAd!!) }
                )
            }

            state is NativeAdState.Loading -> {
                AdLogger.debug(AdLogger.TYPE_NATIVE, "Rendering shimmer while loading: $adId")
                if (shimmerAds != null) {
                    shimmerAds()
                } else {
                    DefaultNativeAdShimmer()
                }
            }

            state is NativeAdState.Failed -> {
                AdLogger.error(
                    AdLogger.TYPE_NATIVE,
                    "Render skipped (state=Failed): $adId | ${state.e.message}"
                )
            }

            state is NativeAdState.NotLoaded -> {
                AdLogger.debug(AdLogger.TYPE_NATIVE, "Render skipped (state=NotLoaded): $adId")
            }

            else -> {
                AdLogger.debug(
                    AdLogger.TYPE_NATIVE,
                    "Render skipped (no state registered for key): $adId"
                )
            }
        }
    }
}