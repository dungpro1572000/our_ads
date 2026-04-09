# Our Ads

Android Ads library tích hợp AdMob, Firebase Remote Config, và DataStore. Hỗ trợ Jetpack Compose.

[![](https://jitpack.io/v/dungpro1572000/our_ads.svg)](https://jitpack.io/#dungpro1572000/our_ads)

## Cài đặt

**1. Thêm JitPack vào `settings.gradle.kts`:**

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

**2. Thêm dependency vào `build.gradle.kts` (app module):**

```kotlin
dependencies {
    implementation("com.github.dungpro1572000:our_ads:1.1.0")
}
```

**3. Yêu cầu:**
- `minSdk = 24`, `compileSdk = 35`
- Firebase Remote Config đã setup trong project (google-services.json)
- AdMob App ID trong `AndroidManifest.xml`:

```xml
<meta-data
    android:name="com.google.android.gms.ads.APPLICATION_ID"
    android:value="ca-app-pub-xxxxxxxx~xxxxxxxx" />
```

---

## Khởi tạo

Gọi trong `Application.onCreate()` hoặc `MainActivity`:

```kotlin
// 1. Xin consent GDPR (bắt buộc cho EU)
val consentManager = GoogleMobileAdsConsentManager.getInstance(this)
consentManager.gatherConsent(this, { error ->
    if (error != null) Log.e("Ads", "Consent error: ${error.message}")

    if (consentManager.canRequestAds) {
        // 2. Khởi tạo AdMob
        AdsInitializer.initialize(
            context = this,
            testDeviceIds = listOf("YOUR_TEST_DEVICE_ID"), // bỏ trống khi release
            onInitComplete = {
                Log.d("Ads", "AdMob initialized")
            }
        )
    }
})

// 3. Sync Remote Config (gọi trong coroutine)
lifecycleScope.launch {
    RemoteConfigData.syncData()
}
```

---

## Remote Config

Library dùng Firebase Remote Config để bật/tắt ads từ xa qua key `enable_all_ads`.

- `true` = bật ads
- `false` = tắt ads (default)

Tạo key `enable_all_ads` trên Firebase Console > Remote Config, set giá trị `true` để bật.

Thêm key tùy chỉnh bằng cách sửa `RemoteConfigData.localSyncRemoteConfigListKey`.

---

## Các loại Ads

### 1. Banner Ad

```kotlin
@Composable
fun MyScreen() {
    // Banner tự động adaptive, quản lý lifecycle
    SmartBannerAd(adUnitId = "ca-app-pub-xxx/xxx")
}
```

### 2. Native Ad - Preload

```kotlin
// Preload (gọi sớm, VD: khi mở app)
lifecycleScope.launch {
    NativeAdsController.preloadAds(
        activity = WeakReference(this@MainActivity),
        adUnitId = "ca-app-pub-xxx/xxx"
    )
}
```

### Native Ad (Compose thuần)

Dùng Compose wrapper để tự thiết kế layout:

```kotlin
@Composable
fun MyNativeAd(nativeAd: NativeAd) {
    NativeAdView(nativeAd = nativeAd) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Badge "Ad"
            NativeAdAttribution()

            Row {
                // Icon app
                NativeAdIconView(modifier = Modifier.size(40.dp)) {
                    nativeAd.icon?.drawable?.toBitmap()?.let {
                        Image(bitmap = it.asImageBitmap(), contentDescription = null)
                    }
                }

                Column {
                    // Tiêu đề
                    NativeAdHeadlineView {
                        Text(text = nativeAd.headline ?: "")
                    }
                    // Nhà quảng cáo
                    NativeAdAdvertiserView {
                        Text(text = nativeAd.advertiser ?: "")
                    }
                }
            }

            // Media (hình/video)
            NativeAdMediaView(nativeAd, modifier = Modifier.fillMaxWidth().height(200.dp))

            // Nội dung
            NativeAdBodyView {
                Text(text = nativeAd.body ?: "")
            }

            // Nút CTA
            NativeAdCallToActionView {
                NativeAdButton(text = nativeAd.callToAction ?: "Install")
            }
        }
    }
}
```

**Compose wrapper có sẵn:** `NativeAdHeadlineView`, `NativeAdBodyView`, `NativeAdMediaView`, `NativeAdIconView`, `NativeAdCallToActionView`, `NativeAdAdvertiserView`, `NativeAdStarRatingView`, `NativeAdPriceView`, `NativeAdStoreView`, `NativeAdChoicesView`, `NativeAdAttribution`, `NativeAdButton`.

### Native Ad (XML layout) - Medium

Dùng `NativeAdsController.MediumNativeContainerAdView` với XML layout truyền thống:

```kotlin
@Composable
fun MyScreen() {
    val adUnitId = "ca-app-pub-xxx/xxx"
    val nativeAdState = NativeAdsController.listAds[adUnitId]

    if (nativeAdState != null) {
        NativeAdsController.MediumNativeContainerAdView(
            nativeAdState = nativeAdState,
            nativeLayout = R.layout.your_native_ad_layout, // hoặc dùng default R.layout.native_ad_medium
            shimmerAds = { /* Custom shimmer hoặc null để dùng default */ }
        )
    }
}
```

XML layout cần có các view ID: `ad_headline`, `ad_call_to_action`, `ad_media`, `ad_body`, `ad_app_icon`, `ad_advertiser`, `ad_stars`.

### Native Ad Full Screen

Dùng `NativeAdsController.FullScreenNativeContainerAdView`, layout content được `<include>` từ `native_ad_fullscreen_content.xml`:

```kotlin
@Composable
fun MyScreen() {
    val adUnitId = "ca-app-pub-xxx/xxx"
    val nativeAdState = NativeAdsController.listAds[adUnitId]

    if (nativeAdState != null) {
        NativeAdsController.FullScreenNativeContainerAdView(
            nativeAdState = nativeAdState,
            onCloseClick = { /* đóng ad */ }
        )
    }
}
```

### 3. Interstitial Ad

```kotlin
// Preload (gọi sớm, VD: khi mở app)
lifecycleScope.launch {
    InterAdsController.preloadAds(
        activity = WeakReference(this@MainActivity),
        adUnitId = "ca-app-pub-xxx/xxx",
        onLoadFailed = { /* xử lý lỗi */ },
        onLoadSuccess = { /* load thành công */ }
    )
}

// Hiển thị khi đã load xong
lifecycleScope.launch {
    InterAdsController.showAds(
        activity = WeakReference(this@MainActivity),
        adUnitId = "ca-app-pub-xxx/xxx",
        onShowFailed = { /* show thất bại */ },
        onShowSuccess = { /* user đóng ad */ }
    )
}
```

### 4. Rewarded Ad

```kotlin
// Preload
lifecycleScope.launch {
    RewardAdsController.preloadAds(
        activity = WeakReference(this@MainActivity),
        adUnitId = "ca-app-pub-xxx/xxx",
        onLoadFailed = { /* xử lý lỗi */ },
        onLoadSuccess = { /* load thành công */ }
    )
}

// Hiển thị khi đã load xong
lifecycleScope.launch {
    RewardAdsController.showAds(
        activity = WeakReference(this@MainActivity),
        adUnitId = "ca-app-pub-xxx/xxx",
        onUserEarn = { /* user nhận thưởng */ },
        onShowFailed = { /* show thất bại */ },
        onShowSuccess = { /* user đóng ad */ }
    )
}
```

---

## Preload Ads với Controller

Các controller giúp preload ads trước khi hiển thị, tránh delay:

| Controller | Ad Type | State Class |
|---|---|---|
| `NativeAdsController` | Native | `NativeAdState` |
| `InterAdsController` | Interstitial | `InterAdState` |
| `RewardAdsController` | Rewarded | `RewardAdState` |

**Pattern chung:**

```kotlin
// 1. Preload (gọi sớm, VD: khi mở app) - dùng adUnitId làm key
XxxController.preloadAds(WeakReference(activity), adUnitId)

// 2. Kiểm tra state bằng adUnitId
val state = XxxController.listAds[adUnitId]

// 3. Dùng khi state là Loaded
when (state) {
    is XxxAdState.Loading -> { /* đang load, show shimmer */ }
    is XxxAdState.Loaded -> { /* sẵn sàng hiển thị */ }
    is XxxAdState.Failed -> { /* load thất bại: state.e */ }
    else -> { /* chưa load */ }
}
```

---

## Cấu trúc thư viện

```
com.dungz.our_ads/
├── AdsInitializer          # Khởi tạo AdMob SDK
├── AppAdMob                # Load/show ads (native, inter, reward, banner)
├── GoogleConsent            # GDPR consent manager
├── controller/
│   ├── NativeAdsController # Preload native ads
│   ├── InterAdsController  # Preload interstitial ads
│   └── RewardAdsController # Preload rewarded ads
├── state/
│   └── AdState             # Sealed classes cho trạng thái ads
├── model/
│   └── DataKey             # Key-value model cho Remote Config
├── remotedata/
│   └── RemoteConfigData    # Firebase Remote Config + DataStore sync
└── ui/
    ├── BannerAdView        # SmartBannerAd composable
    ├── NativeAdView        # Compose wrappers cho native ad
    └── NativeFullScreenAdView # Full-screen native ad composable
```

## License

Apache License 2.0
