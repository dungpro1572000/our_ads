package com.dungz.our_ads.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dungz.our_ads.remotedata.RemoteConfigData
import com.dungz.our_ads.utils.AdLogger
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

private enum class BannerAdStatus { Loading, Loaded, Failed }

@Composable
fun SmartBannerAd(adUnitId: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var adStatus by remember { mutableStateOf(BannerAdStatus.Loading) }
    var adView by remember { mutableStateOf<AdView?>(null) }

    // Check remote config then load ad; if disabled → treat as Failed to collapse
    LaunchedEffect(Unit) {
        val enableAds = RemoteConfigData.get(RemoteConfigData.ENABLE_ADS) == true
        AdLogger.debug(AdLogger.TYPE_BANNER, "check enable ads: $enableAds")
        if (!enableAds) {
            adStatus = BannerAdStatus.Failed
            return@LaunchedEffect
        }
        adView = AdView(context).apply {
            setAdSize(
                AdSize.getLandscapeAnchoredAdaptiveBannerAdSize(context, 360)
            )
            setAdUnitId(adUnitId)
            adListener = object : com.google.android.gms.ads.AdListener() {
                override fun onAdFailedToLoad(p0: LoadAdError) {
                    super.onAdFailedToLoad(p0)
                    adStatus = BannerAdStatus.Failed
                    AdLogger.error(AdLogger.TYPE_BANNER, "load banner failed: ${p0.message}")
                }

                override fun onAdLoaded() {
                    super.onAdLoaded()
                    adStatus = BannerAdStatus.Loaded
                    AdLogger.debug(AdLogger.TYPE_BANNER, "load banner successfully")
                }
            }
            loadAd(AdRequest.Builder().build())
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, adView) {
        val currentAdView = adView ?: return@DisposableEffect onDispose {}
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> currentAdView.resume()
                Lifecycle.Event.ON_PAUSE -> currentAdView.pause()
                Lifecycle.Event.ON_DESTROY -> currentAdView.destroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            currentAdView.destroy()
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize()
            .then(
                if (adStatus == BannerAdStatus.Failed) Modifier.height(0.dp)
                else Modifier
            )
    ) {
        when (adStatus) {
            BannerAdStatus.Loading -> {
                ShimmerBannerPlaceholder()
            }
            BannerAdStatus.Loaded -> {
                adView?.let { view ->
                    AndroidView(
                        factory = { view },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            BannerAdStatus.Failed -> { /* height = 0dp, collapsed */ }
        }
    }
}

@Composable
private fun ShimmerBannerPlaceholder() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer_translate"
    )
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color.LightGray.copy(alpha = 0.6f),
            Color.LightGray.copy(alpha = 0.2f),
            Color.LightGray.copy(alpha = 0.6f),
        ),
        start = Offset(translateAnim - 200f, 0f),
        end = Offset(translateAnim, 0f)
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .background(shimmerBrush)
    )
}