package com.dungz.our_ads.demo

import android.app.Application
import android.util.Log
import com.dungz.our_ads.AdsInitializer

class AppApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        // Ads SDK init moved to MainActivity to enable GDPR consent flow (requires Activity)
    }
}