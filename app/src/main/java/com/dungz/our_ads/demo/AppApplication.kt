package com.dungz.our_ads.demo

import android.app.Application
import android.util.Log
import com.dungz.our_ads.AdsInitializer

class AppApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        AdsInitializer.initialize(this) {
            Log.d("TAG", "Ads SDK initialized")
        }
    }
}