package com.dungz.our_ads.state

import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.rewarded.RewardedAd

/**
 * Trạng thái của một Ad
 */
sealed interface AdState {
    data object NotLoaded : AdState
    data object Loading : AdState
    data object Loaded : AdState
    data object Showing : AdState
    data class Failed(val e: AdError) : AdState
}

/**
 * Wrapper chứa ad và metadata
 */
data class AdHolder<T>(
    val ad: T?,
    val state: AdState = AdState.NotLoaded,
    val isHigherAd: Boolean = false,
    val loadedAt: Long = 0L,
    val adUnitId: String = ""
)

/**
 * Config cho việc retry load ad
 */
data class RetryConfig(
    val maxRetryCount: Int = 3,
    val reloadDuration: Long = 0L,
    val reloadTriggerCount: Int = 0
)

/**
 * Callback cho Rewarded Ads
 */
data class RewardItem(val type: String, val amount: Int)

/**
 * Tạo key unique cho mỗi cặp ad IDs
 */
fun createAdKey(adHigherId: String, adNormalId: String): String = "$adHigherId|$adNormalId"


sealed class NativeAdState{
    data object NotLoaded : NativeAdState()
    data object Loading : NativeAdState()
    data class Loaded(val nativeAd: NativeAd) : NativeAdState()
    data class Failed(val e: AdError) : NativeAdState()
}

sealed class InterAdState {
    data object NotLoaded : InterAdState()
    data object Loading : InterAdState()
    data class Loaded(val interstitialAd: InterstitialAd) : InterAdState()
    data class Failed(val e: AdError) : InterAdState()
}

sealed class RewardAdState {
    data object NotLoaded : RewardAdState()
    data object Loading : RewardAdState()
    data class Loaded(val rewardedAd: RewardedAd) : RewardAdState()
    data class Failed(val e: AdError) : RewardAdState()
}