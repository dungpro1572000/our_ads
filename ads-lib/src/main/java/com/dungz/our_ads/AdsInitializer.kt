package com.dungz.our_ads

import android.app.Activity
import android.content.Context
import com.dungz.our_ads.remotedata.RemoteConfigData
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration

object AdsInitializer {
    private var isInitialized = false

    /**
     * Initialize ads SDK with GDPR consent flow.
     * Consent form shows if needed, ads SDK inits regardless of consent result.
     */
    fun initialize(
        activity: Activity,
        testDeviceIds: List<String> = emptyList(),
        testDeviceHashedId: String = "",
        onInitComplete: () -> Unit = {}
    ) {
        if (isInitialized) {
            onInitComplete()
            return
        }
        RemoteConfigData.init(activity)

        // Gather GDPR consent (non-blocking, ads still work regardless)
        GoogleMobileAdsConsentManager.getInstance(activity)
            .gatherConsent(activity, { _ -> }, testDeviceHashedId)

        if (testDeviceIds.isNotEmpty()) {
            MobileAds.setRequestConfiguration(
                RequestConfiguration.Builder()
                    .setTestDeviceIds(testDeviceIds)
                    .build()
            )
        }

        MobileAds.initialize(activity) {
            isInitialized = true
            onInitComplete()
        }
    }

    /**
     * Initialize without consent flow (use when Activity not available).
     */
    fun initialize(
        context: Context,
        testDeviceIds: List<String> = emptyList(),
        onInitComplete: () -> Unit = {}
    ) {
        if (isInitialized) {
            onInitComplete()
            return
        }
        RemoteConfigData.init(context)

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
