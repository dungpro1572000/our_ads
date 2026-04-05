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
        activity: Activity,
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
        interstitialAd.show(activity)
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
        activity: Activity,
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
        rewardedAd.show(activity) { rewardItem ->
            onUserEarnedReward(rewardItem)
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

/** Inflate XML layout and bind view references for a NativeAdView. */
private fun inflateNativeAdView(
    context: android.content.Context,
    @LayoutRes layout: Int
): NativeAdView {
    val adView = LayoutInflater.from(context).inflate(layout, null) as NativeAdView
    adView.headlineView = adView.findViewById(R.id.ad_headline)
    adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
    adView.mediaView = adView.findViewById(R.id.ad_media)
    adView.bodyView = adView.findViewById(R.id.ad_body)
    adView.iconView = adView.findViewById(R.id.ad_app_icon)
    adView.advertiserView = adView.findViewById(R.id.ad_advertiser)
    adView.starRatingView = adView.findViewById(R.id.ad_stars)
    return adView
}

/** Populate NativeAdView with NativeAd data. */
private fun bindNativeAd(adView: NativeAdView, nativeAd: com.google.android.gms.ads.nativead.NativeAd) {
    (adView.headlineView as? TextView)?.text = nativeAd.headline

    (adView.bodyView as? TextView)?.apply {
        text = nativeAd.body
        visibility = if (nativeAd.body != null) View.VISIBLE else View.GONE
    }

    (adView.callToActionView as? Button)?.apply {
        text = nativeAd.callToAction
        visibility = if (nativeAd.callToAction != null) View.VISIBLE else View.GONE
    }

    (adView.iconView as? ImageView)?.apply {
        val icon = nativeAd.icon
        if (icon != null) {
            setImageDrawable(icon.drawable)
            visibility = View.VISIBLE
        } else {
            visibility = View.GONE
        }
    }

    (adView.advertiserView as? TextView)?.apply {
        text = nativeAd.advertiser
        visibility = if (nativeAd.advertiser != null) View.VISIBLE else View.GONE
    }

    (adView.starRatingView as? RatingBar)?.apply {
        val stars = nativeAd.starRating
        if (stars != null) {
            rating = stars.toFloat()
            visibility = View.VISIBLE
        } else {
            visibility = View.GONE
        }
    }

    (adView.mediaView as? MediaView)?.apply {
        nativeAd.mediaContent?.let { mediaContent = it }
    }

    adView.setNativeAd(nativeAd)
}

/** Display a native ad by inflating a XML layout and binding NativeAd data into it. */
@Composable
fun MediumNativeContainerAdView(
    nativeAdState: NativeAdState,
    @LayoutRes nativeLayout: Int,
    shimmerAds: (@Composable () -> Unit)? = null
) {
    when (nativeAdState) {
        is NativeAdState.Loading -> {
            if (shimmerAds != null) {
                shimmerAds()
            } else {
                DefaultNativeAdShimmer()
            }
        }

        is NativeAdState.Loaded -> {
            val nativeAd = nativeAdState.nativeAd
            AndroidView(
                factory = { context -> inflateNativeAdView(context, nativeLayout) },
                modifier = Modifier.fillMaxWidth(),
                update = { adView -> bindNativeAd(adView, nativeAd) }
            )
        }

        else -> {}
    }
}

/** Full-screen native ad dialog using XML layout. */
@Composable
fun FullScreenNativeContainerAdView(
    nativeAdState: NativeAdState,
    @LayoutRes nativeLayout: Int = R.layout.native_ad_fullscreen_content,
    onCloseClick: () -> Unit
) {
    if (nativeAdState !is NativeAdState.Loaded) return

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

                // Bind close button
                root.findViewById<ImageButton>(R.id.btn_close)?.setOnClickListener {
                    onCloseClick()
                }

                // Bind native ad view inside the fullscreen layout
                val adView = root.findViewById<NativeAdView>(R.id.native_ad_view)
                adView.headlineView = adView.findViewById(R.id.ad_headline)
                adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
                adView.mediaView = adView.findViewById(R.id.ad_media)
                adView.bodyView = adView.findViewById(R.id.ad_body)
                adView.iconView = adView.findViewById(R.id.ad_app_icon)
                adView.advertiserView = adView.findViewById(R.id.ad_advertiser)
                adView.starRatingView = adView.findViewById(R.id.ad_stars)

                bindNativeAd(adView, nativeAdState.nativeAd)
                root
            },
            modifier = Modifier.fillMaxSize(),
            update = { root ->
                val adView = root.findViewById<NativeAdView>(R.id.native_ad_view)
                bindNativeAd(adView, nativeAdState.nativeAd)
            }
        )
    }
}

/** Default shimmer placeholder when no custom shimmer is provided. */
@Composable
private fun DefaultNativeAdShimmer() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color.LightGray.copy(alpha = 0.6f),
            Color.LightGray.copy(alpha = 0.2f),
            Color.LightGray.copy(alpha = 0.6f)
        ),
        start = Offset(translateAnim - 200f, 0f),
        end = Offset(translateAnim, 0f)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Header: icon + title lines
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(shimmerBrush)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Media placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(shimmerBrush)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Body text lines
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(shimmerBrush)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(shimmerBrush)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // CTA button placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(shimmerBrush)
        )
    }
}
// [END display_native_ad]
