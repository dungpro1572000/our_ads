package com.dungz.our_ads.demo

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.dungz.our_ads.controller.InterAdsController
import com.dungz.our_ads.controller.NativeAdsController
import com.dungz.our_ads.controller.RewardAdsController
import com.dungz.our_ads.remotedata.RemoteConfigData
import com.dungz.our_ads.ui.SmartBannerAd
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "OurAdsDemo"

        // Google test ad unit IDs
        private const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/9214589741"
        private const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
        private const val TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"
        private const val TEST_NATIVE_ID = "ca-app-pub-3940256099942544/2247696110"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            RemoteConfigData.syncData()
        }

        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    DemoScreen()
                }
            }
        }
    }

    @Composable
    private fun DemoScreen() {
        val activity = this
        val context = LocalContext.current
        val scope = rememberCoroutineScope()

        var statusText by remember { mutableStateOf("Ready") }

        var showNativeFullScreen by remember { mutableStateOf(false) }

        // Full Screen Native Ad Dialog
        if (showNativeFullScreen) {
            NativeAdsController.FullScreenNativeContainerAdView(
                adId = TEST_NATIVE_ID,
                onCloseClick = { showNativeFullScreen = false }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("OurAds Demo", style = MaterialTheme.typography.headlineMedium)
            Text(statusText, style = MaterialTheme.typography.bodyMedium)

            Spacer(modifier = Modifier.height(8.dp))

            // --- Banner ---
            Text("Banner Ad", style = MaterialTheme.typography.titleMedium)
            SmartBannerAd(TEST_BANNER_ID)

            // --- Interstitial ---
            Text("Interstitial Ad", style = MaterialTheme.typography.titleMedium)
            Button(
                onClick = {
                    statusText = "Interstitial: loading..."
                    scope.launch {
                        InterAdsController.preloadAds(
                            activity = WeakReference(activity),
                            adUnitId = TEST_INTERSTITIAL_ID,
                            onLoadFailed = { statusText = "Interstitial: failed" },
                            onLoadSuccess = { statusText = "Interstitial: loaded" }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Load Interstitial") }

            Button(
                onClick = {
                    scope.launch {
                        InterAdsController.showAds(
                            activity = WeakReference(activity),
                            adUnitId = TEST_INTERSTITIAL_ID,
                            onShowFailed = { statusText = "Interstitial: show failed" },
                            onShowSuccess = { statusText = "Interstitial: dismissed" }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Show Interstitial") }

            // --- Rewarded ---
            Text("Rewarded Ad", style = MaterialTheme.typography.titleMedium)
            Button(
                onClick = {
                    statusText = "Rewarded: loading..."
                    scope.launch {
                        RewardAdsController.preloadAds(
                            activity = WeakReference(activity),
                            adUnitId = TEST_REWARDED_ID,
                            onLoadFailed = { statusText = "Rewarded: failed" },
                            onLoadSuccess = { statusText = "Rewarded: loaded" }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Load Rewarded") }

            Button(
                onClick = {
                    scope.launch {
                        RewardAdsController.showAds(
                            activity = WeakReference(activity),
                            adUnitId = TEST_REWARDED_ID,
                            onUserEarn = {
                                Toast.makeText(context, "Earned reward!", Toast.LENGTH_SHORT).show()
                            },
                            onShowFailed = { statusText = "Rewarded: show failed" },
                            onShowSuccess = { statusText = "Rewarded: dismissed" }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Show Rewarded") }

            // --- Native ---
            Text("Native Ad", style = MaterialTheme.typography.titleMedium)
            Button(
                onClick = {
                    statusText = "Native: loading..."
                    scope.launch {
                        NativeAdsController.preloadAds(
                            activity = WeakReference(activity),
                            adUnitId = TEST_NATIVE_ID
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Load Native") }

            NativeAdsController.MediumNativeContainerAdView(
                activity = WeakReference(activity),
                adId = TEST_NATIVE_ID,
                nativeLayout = com.dungz.our_ads.R.layout.native_ad_medium
            )

            // --- Native Full Screen ---
            Text("Native Full Screen", style = MaterialTheme.typography.titleMedium)
            Button(
                onClick = {
                    showNativeFullScreen = true
                    if (NativeAdsController.canPreloadAds(TEST_NATIVE_ID)) {
                        scope.launch {
                            NativeAdsController.preloadAds(
                                activity = WeakReference(activity),
                                adUnitId = TEST_NATIVE_ID
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Show Native FullScreen") }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
