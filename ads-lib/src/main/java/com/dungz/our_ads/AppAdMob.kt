package com.dungz.our_ads

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dungz.our_ads.state.NativeAdState
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

object AppAdMob {
    suspend fun loadSingleNativeAds(
        context: WeakReference<Activity>,
        id: String,
        onAdClick: () -> Unit,
        onAdImpression: () -> Unit,
        onAdFailedToLoad: (adError: AdError) -> Unit,
        onLoadSuccess: (nativeAd: NativeAd) -> Unit,
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            val activity = context.get()
            if (activity == null) {
                onAdFailedToLoad(AdError(0, "Context is null", "AppAdMob"))
                return@launch
            }

            val adLoader = AdLoader.Builder(activity, id)
                .forNativeAd { nativeAd ->
                    onLoadSuccess(nativeAd)
                }
                .withAdListener(
                    object : AdListener() {
                        override fun onAdFailedToLoad(adError: LoadAdError) {
                            onAdFailedToLoad(adError)
                        }

                        override fun onAdClicked() {
                            super.onAdClicked()
                            onAdClick()
                        }

                        override fun onAdImpression() {
                            super.onAdImpression()
                            onAdImpression()
                        }
                    }
                )
                .withNativeAdOptions(NativeAdOptions.Builder().build())
                .build()
            adLoader.loadAd(AdRequest.Builder().build())
        }
    }

    suspend fun loadInterstitialAds(
        context: WeakReference<Activity>,
        id: String,
        onAdFailedToLoad: (adError: AdError) -> Unit,
        onLoadSuccess: (interstitialAd: InterstitialAd) -> Unit,
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            val activity = context.get()
            if (activity == null) {
                onAdFailedToLoad(AdError(0, "Context is null", "AppAdMob"))
                return@launch
            }

            val adRequest = AdRequest.Builder().build()
            InterstitialAd.load(activity, id, adRequest, object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    onLoadSuccess(interstitialAd)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    onAdFailedToLoad(loadAdError)
                }
            })
        }
    }

    fun showInterstitialAd(
        activity: WeakReference<Activity>,
        interstitialAd: InterstitialAd,
        onAdDismissed: () -> Unit,
        onAdFailedToShow: (adError: AdError) -> Unit
    ) {
        interstitialAd.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                onAdDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                onAdFailedToShow(adError)
            }
        }
        activity.get()?.let { interstitialAd.show(it) }
    }

    suspend fun loadRewardAds(
        context: WeakReference<Activity>,
        id: String,
        onAdFailedToLoad: (adError: AdError) -> Unit,
        onLoadSuccess: (rewardedAd: RewardedAd) -> Unit,
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            val activity = context.get()
            if (activity == null) {
                onAdFailedToLoad(AdError(0, "Context is null", "AppAdMob"))
                return@launch
            }

            val adRequest = AdRequest.Builder().build()
            RewardedAd.load(activity, id, adRequest, object : RewardedAdLoadCallback() {
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    onLoadSuccess(rewardedAd)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    onAdFailedToLoad(loadAdError)
                }
            })
        }
    }

    fun showRewardAds(
        activity: WeakReference<Activity>,
        rewardedAd: RewardedAd,
        onUserEarnedReward: (reward: RewardItem) -> Unit,
        onAdDismissed: () -> Unit,
        onAdFailedToShow: (adError: AdError) -> Unit
    ) {
        rewardedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                onAdDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                onAdFailedToShow(adError)
            }
        }
        activity.get()?.let {
            rewardedAd.show(it) { rewardItem ->
                onUserEarnedReward(rewardItem)
            }
        }
    }

    suspend fun loadBanner(
        context: WeakReference<Activity>,
        id: String,
        onAdFailedToLoad: (adError: AdError) -> Unit,
        onLoadSuccess: (adView: AdView) -> Unit,
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            val activity = context.get()
            if (activity == null) {
                onAdFailedToLoad(AdError(0, "Context is null", "AppAdMob"))
                return@launch
            }

            val adView = AdView(activity)
            adView.adUnitId = id
            adView.setAdSize(AdSize.BANNER)
            adView.adListener = object : AdListener() {
                override fun onAdLoaded() {
                    super.onAdLoaded()
                    onLoadSuccess(adView)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    super.onAdFailedToLoad(error)
                    onAdFailedToLoad(error)
                }
            }
            adView.loadAd(AdRequest.Builder().build())
        }
    }
}

// [START display_native_ad]
// [END display_native_ad]
