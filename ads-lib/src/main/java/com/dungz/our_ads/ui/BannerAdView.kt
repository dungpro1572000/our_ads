package com.dungz.our_ads.ui

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.dungz.our_ads.remotedata.RemoteConfigData
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun SmartBannerAd(adUnitId: String) {
    var enableAds by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        enableAds = RemoteConfigData.get(RemoteConfigData.ENABLE_ADS) == true
    }
    if (!enableAds) return

    val context = LocalContext.current

    // Tạo và nhớ AdView để không bị khởi tạo lại khi recompose
    val adView = remember {
        AdView(context).apply {
            setAdSize(
                AdSize.getLandscapeAnchoredAdaptiveBannerAdSize(
                    context,
                    360
                )
            ) // Hoặc AdSize.BANNER
            setAdUnitId(adUnitId)
            loadAd(AdRequest.Builder().build())
        }
    }

    // Quản lý Lifecycle (Resume/Pause/Destroy)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> adView.resume()
                Lifecycle.Event.ON_PAUSE -> adView.pause()
                Lifecycle.Event.ON_DESTROY -> adView.destroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            adView.destroy()
        }
    }

    // Hiển thị lên Compose UI
    AndroidView(
        factory = { adView },
        modifier = Modifier.fillMaxWidth()
    )
}