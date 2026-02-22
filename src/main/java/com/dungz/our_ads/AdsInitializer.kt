package com.dungz.our_ads

import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration

object AdsInitializer {
    private var isInitialized = false

    fun initialize(
        context: Context,
        testDeviceIds: List<String> = emptyList(),
        onInitComplete: () -> Unit = {}
    ) {
        if (isInitialized) {
            onInitComplete()
            return
        }

        if (testDeviceIds.isNotEmpty()) {
            MobileAds.setRequestConfiguration(
                RequestConfiguration.Builder()
                    .setTestDeviceIds(testDeviceIds)
                    .build()
            )
        }

        MobileAds.initialize(context) {
            isInitialized = true
            onInitComplete()
        }
    }

    fun isInitialized(): Boolean = isInitialized
}
