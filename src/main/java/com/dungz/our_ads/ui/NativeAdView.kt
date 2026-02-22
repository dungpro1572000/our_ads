package com.dungz.our_ads.ui

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.dungz.our_ads.R
import com.dungz.our_ads.manager.NativeAdManager
import com.dungz.our_ads.state.RetryConfig
import com.dungz.our_ads.utils.AdLogger

enum class NativeAdSize { SMALL, MEDIUM }

@Composable
fun NativeAdView(
    adHigherId: String,
    adNormalId: String,
    showHigher: Boolean,
    showNormal: Boolean,
    modifier: Modifier = Modifier,
    size: NativeAdSize = NativeAdSize.MEDIUM,
    retryConfig: RetryConfig? = null,
    loadingPlaceholder: (@Composable () -> Unit)? = null,
    onAdLoaded: () -> Unit = {},
    onAdFailed: (String) -> Unit = {}
) {
    // Don't show anything if both flags are false
    if (!showHigher && !showNormal) {
        AdLogger.debug(AdLogger.TYPE_NATIVE, "Skipping ad display: showHigher=$showHigher, showNormal=$showNormal")
        return
    }

    // Observe ad changes từ StateFlow để tự động cập nhật khi ad được reload
    val adFromFlow by NativeAdManager.getAdFlow(adHigherId, adNormalId).collectAsState()

    // Use key to reset states when flags change
    var isLoading by remember(showHigher, showNormal) { mutableStateOf(false) }
    var hasFailed by remember(showHigher, showNormal) { mutableStateOf(false) }

    // Key để force recompose AndroidView khi ad thay đổi
    var adVersion by remember { mutableIntStateOf(0) }

    // Cập nhật adVersion khi có ad mới từ flow
    LaunchedEffect(adFromFlow) {
        if (adFromFlow != null && (showHigher || showNormal)) {
            adVersion++
            isLoading = false
            onAdLoaded()
        }
    }

    LaunchedEffect(adHigherId, adNormalId, showHigher, showNormal) {
        // Double check flags before loading
        if (!showHigher && !showNormal) {
            AdLogger.debug(AdLogger.TYPE_NATIVE, "Skipping ad load: both flags are false")
            return@LaunchedEffect
        }

        if (NativeAdManager.isReady(adHigherId, adNormalId)) {
            // Ad đã sẵn sàng, flow sẽ emit
            onAdLoaded()
        } else {
            isLoading = true
            hasFailed = false
            NativeAdManager.loadAd(
                adHigherId, adNormalId, showHigher, showNormal, retryConfig,
                onLoaded = { _ ->
                    // Ad loaded, flow sẽ emit và trigger recompose
                    isLoading = false
                },
                onFailed = { error ->
                    isLoading = false
                    hasFailed = true
                    onAdFailed(error)
                }
            )
        }
    }

    // Don't show anything if ad failed to load
    if (hasFailed) {
        AdLogger.debug(AdLogger.TYPE_NATIVE, "Ad failed to load, hiding view")
        return
    }

    // Only show content when loading or ad is available
    if (isLoading || adFromFlow != null) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(8.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            val height = if (size == NativeAdSize.SMALL) 80.dp else 280.dp
            when {
                isLoading -> {
                    if (loadingPlaceholder != null) {
                        loadingPlaceholder()
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(height)
                                .background(Color.White),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                }
                adFromFlow != null -> {
                    // Dùng key(adVersion) để force recreate AndroidView khi ad mới được load
                    androidx.compose.runtime.key(adVersion) {
                        AndroidView(
                            modifier = Modifier.fillMaxWidth(),
                            factory = { ctx ->
                                val layout = if (size == NativeAdSize.SMALL) {
                                    R.layout.native_ad_small
                                } else {
                                    R.layout.native_ad_medium
                                }
                                (LayoutInflater.from(ctx).inflate(layout, null) as NativeAdView).also {
                                    populateNativeAdView(adFromFlow!!, it)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

private fun populateNativeAdView(nativeAd: NativeAd, adView: NativeAdView) {
    // 1. Set MediaView first (required by Google)
    adView.mediaView = adView.findViewById(R.id.ad_media)
    nativeAd.mediaContent?.let { mediaContent ->
        adView.mediaView?.mediaContent = mediaContent
        adView.mediaView?.visibility = View.VISIBLE
    }

    // 2. Set headline (required)
    adView.headlineView = adView.findViewById(R.id.ad_headline)
    (adView.headlineView as? TextView)?.text = nativeAd.headline

    // 3. Set body
    adView.bodyView = adView.findViewById(R.id.ad_body)
    (adView.bodyView as? TextView)?.apply {
        text = nativeAd.body ?: ""
        visibility = if (nativeAd.body != null) View.VISIBLE else View.GONE
    }

    // 4. Set call to action (important for clicks)
    adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
    (adView.callToActionView as? Button)?.apply {
        text = nativeAd.callToAction ?: "Install"
        visibility = View.VISIBLE
    }

    // 5. Set icon
    adView.iconView = adView.findViewById(R.id.ad_app_icon)
    (adView.iconView as? ImageView)?.apply {
        nativeAd.icon?.drawable?.let { setImageDrawable(it) }
        visibility = if (nativeAd.icon != null) View.VISIBLE else View.INVISIBLE
    }

    // 6. Set star rating
    adView.starRatingView = adView.findViewById(R.id.ad_stars)
    (adView.starRatingView as? RatingBar)?.apply {
        rating = nativeAd.starRating?.toFloat() ?: 0f
        visibility = if (nativeAd.starRating != null) View.VISIBLE else View.GONE
    }

    // 7. Set advertiser
    adView.advertiserView = adView.findViewById(R.id.ad_advertiser)
    (adView.advertiserView as? TextView)?.apply {
        text = nativeAd.advertiser ?: ""
        visibility = if (nativeAd.advertiser != null) View.VISIBLE else View.GONE
    }

    // 8. Register the native ad (must be last)
    adView.setNativeAd(nativeAd)
}

// PREVIEWS
@Preview(showBackground = true, name = "Native Small - Loading")
@Composable
private fun NativeSmallLoadingPreview() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
}

@Preview(showBackground = true, name = "Native Small - Loaded")
@Composable
private fun NativeSmallLoadedPreview() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.LightGray)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("App Headline", style = MaterialTheme.typography.titleSmall)
                Text("Advertiser", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .padding(12.dp, 6.dp)
            ) {
                Text("Install", color = Color.White, fontSize = 12.sp)
            }
        }
    }
}

@Preview(showBackground = true, name = "Native Medium - Loaded")
@Composable
private fun NativeMediumLoadedPreview() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.LightGray)
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("App Headline", style = MaterialTheme.typography.titleSmall)
                    Text("Advertiser", color = Color.Gray)
                }
                Box(
                    modifier = Modifier
                        .background(Color(0xFFFFA500))
                        .padding(4.dp, 2.dp)
                ) {
                    Text("Ad", fontSize = 10.sp, color = Color.White)
                }
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE0E0E0)),
                contentAlignment = Alignment.Center
            ) {
                Text("Media", color = Color.Gray)
            }
            Spacer(Modifier.height(8.dp))
            Text("Body text of the native ad.")
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("★★★★☆", color = Color(0xFFFFB800))
                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(16.dp, 8.dp)
                ) {
                    Text("Install", color = Color.White)
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Native - Failed")
@Composable
private fun NativeFailedPreview() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Text("Ad not available", color = Color.Gray)
        }
    }
}
