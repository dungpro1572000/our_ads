package com.dungz.our_ads.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.dungz.our_ads.manager.BannerAdManager
import com.dungz.our_ads.utils.AdLogger

@Composable
fun BannerAdView(
    adHigherId: String,
    adNormalId: String,
    showHigher: Boolean,
    showNormal: Boolean,
    modifier: Modifier = Modifier,
    adSize: AdSize = AdSize.BANNER,
    loadingPlaceholder: (@Composable () -> Unit)? = null,
    onAdLoaded: () -> Unit = {},
    onAdFailed: (String) -> Unit = {}
) {
    // Don't show anything if both flags are false
    if (!showHigher && !showNormal) {
        AdLogger.debug(AdLogger.TYPE_BANNER, "Skipping ad display: showHigher=$showHigher, showNormal=$showNormal")
        return
    }

    // Use key to reset states when flags change
    var currentAdView by remember(showHigher, showNormal) { mutableStateOf<AdView?>(null) }
    var isLoading by remember(showHigher, showNormal) { mutableStateOf(false) }
    var hasFailed by remember(showHigher, showNormal) { mutableStateOf(false) }

    LaunchedEffect(adHigherId, adNormalId, showHigher, showNormal) {
        // Double check flags before loading
        if (!showHigher && !showNormal) {
            AdLogger.debug(AdLogger.TYPE_BANNER, "Skipping ad load: both flags are false")
            return@LaunchedEffect
        }

        if (BannerAdManager.isReady(adHigherId, adNormalId)) {
            currentAdView = BannerAdManager.getAdView(adHigherId, adNormalId)
            if (currentAdView != null) onAdLoaded()
        } else {
            isLoading = true
            BannerAdManager.loadAd(
                adHigherId, adNormalId, showHigher, showNormal, adSize,
                onLoaded = { adView ->
                    currentAdView = adView
                    isLoading = false
                    onAdLoaded()
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
        AdLogger.debug(AdLogger.TYPE_BANNER, "Ad failed to load, hiding view")
        return
    }

    // Only show content when loading or ad is available
    if (isLoading || currentAdView != null) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(adSize.height.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    if (loadingPlaceholder != null) {
                        loadingPlaceholder()
                    } else {
                        CircularProgressIndicator()
                    }
                }
                currentAdView != null -> AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { currentAdView!! }
                )
            }
        }
    }
}

// PREVIEWS
@Preview(showBackground = true, name = "Banner - Loading")
@Composable
private fun BannerLoadingPreview() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color(0xFFF5F5F5)),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Preview(showBackground = true, name = "Banner - Loaded")
@Composable
private fun BannerLoadedPreview() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color(0xFFE8E8E8)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "Banner Ad (320x50)",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
        )
    }
}
