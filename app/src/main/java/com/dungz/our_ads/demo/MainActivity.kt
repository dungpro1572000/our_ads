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
import com.dungz.our_ads.AdsInitializer
import com.dungz.our_ads.AppAdMob
import com.dungz.our_ads.FullScreenNativeContainerAdView
import com.dungz.our_ads.MediumNativeContainerAdView
import com.dungz.our_ads.remotedata.RemoteConfigData
import com.dungz.our_ads.state.NativeAdState
import com.dungz.our_ads.ui.SmartBannerAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.rewarded.RewardedAd
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "OurAdsDemo"

        // Google test ad unit IDs
        private const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"
        private const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
        private const val TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"
        private const val TEST_NATIVE_ID = "ca-app-pub-3940256099942544/2247696110"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        lifecycleScope.launch {
            RemoteConfigData.syncData()
            Log.d("DungNT2", "${RemoteConfigData.get(RemoteConfigData.ENABLE_ADS)}")
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

        // State variables for ads
        var interstitialAd by remember { mutableStateOf<InterstitialAd?>(null) }
        var rewardedAd by remember { mutableStateOf<RewardedAd?>(null) }
        var nativeAdState by remember { mutableStateOf<NativeAdState>(NativeAdState.NotLoaded) }
        var showNativeFullScreen by remember { mutableStateOf(false) }

        // Full Screen Native Ad Dialog
        if (showNativeFullScreen) {
            FullScreenNativeContainerAdView(
                nativeAdState = nativeAdState,
                onCloseClick = { showNativeFullScreen = false }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
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
                        AppAdMob.loadInterstitialAds(
                            context = WeakReference(activity),
                            id = TEST_INTERSTITIAL_ID,
                            onLoadSuccess = { ad ->
                                statusText = "Interstitial: loaded"
                                interstitialAd = ad
                            },
                            onAdFailedToLoad = { error ->
                                statusText = "Interstitial: failed ${error.message}"
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Load Interstitial") }

            Button(
                onClick = {
                    interstitialAd?.let { ad ->
                        AppAdMob.showInterstitialAd(
                            activity = activity,
                            interstitialAd = ad,
                            onAdDismissed = {
                                statusText = "Interstitial: dismissed"
                                interstitialAd = null
                            },
                            onAdFailedToShow = { error ->
                                statusText = "Interstitial: show failed ${error.message}"
                            }
                        )
                    } ?: run {
                        statusText = "Load Interstitial first"
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
                        AppAdMob.loadRewardAds(
                            context = WeakReference(activity),
                            id = TEST_REWARDED_ID,
                            onLoadSuccess = { ad ->
                                statusText = "Rewarded: loaded"
                                rewardedAd = ad
                            },
                            onAdFailedToLoad = { error ->
                                statusText = "Rewarded: failed ${error.message}"
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Load Rewarded") }

            Button(
                onClick = {
                    rewardedAd?.let { ad ->
                        AppAdMob.showRewardAds(
                            activity = activity,
                            rewardedAd = ad,
                            onUserEarnedReward = { reward ->
                                Toast.makeText(context, "Earned: ${reward.amount} ${reward.type}", Toast.LENGTH_SHORT).show()
                            },
                            onAdDismissed = {
                                statusText = "Rewarded: dismissed"
                                rewardedAd = null
                            },
                            onAdFailedToShow = { error ->
                                statusText = "Rewarded: show failed ${error.message}"
                            }
                        )
                    } ?: run {
                        statusText = "Load Rewarded first"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Show Rewarded") }

            // --- Native ---
            Text("Native Ad", style = MaterialTheme.typography.titleMedium)
            Button(
                onClick = {
                    statusText = "Native: loading..."
                    nativeAdState = NativeAdState.Loading
                    scope.launch {
                        AppAdMob.loadSingleNativeAds(
                            context = WeakReference(activity),
                            id = TEST_NATIVE_ID,
                            onAdClick = { Log.d(TAG, "Native clicked") },
                            onAdImpression = { Log.d(TAG, "Native impression") },
                            onLoadSuccess = { ad ->
                                statusText = "Native: loaded"
                                nativeAdState = NativeAdState.Loaded(ad)
                            },
                            onAdFailedToLoad = { error ->
                                statusText = "Native: failed ${error.message}"
                                nativeAdState = NativeAdState.Failed(error)
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Load Native") }

            MediumNativeContainerAdView(
                nativeAdState = nativeAdState,
                nativeLayout = com.dungz.our_ads.R.layout.native_ad_medium
            )

            // --- Native Full Screen ---
            Text("Native Full Screen", style = MaterialTheme.typography.titleMedium)
            Button(
                onClick = {
                    if (nativeAdState is NativeAdState.Loaded) {
                        showNativeFullScreen = true
                    } else {
                        statusText = "Load Native first"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Show Native FullScreen") }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
