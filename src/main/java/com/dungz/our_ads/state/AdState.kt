package com.dungz.our_ads.state

/**
 * Trạng thái của một Ad
 */
sealed interface AdState {
    data object NotLoaded : AdState
    data object Loading : AdState
    data object Loaded : AdState
    data object Showing : AdState
    data class Failed(val message: String, val retryCount: Int = 0) : AdState
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
