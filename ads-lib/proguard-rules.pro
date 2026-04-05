# Google Mobile Ads SDK ProGuard Rules
-keep public class com.google.android.gms.ads.** {
   public *;
}

-keep public class com.google.ads.mediation.** {
   public *;
}

-keep class com.google.ads.mediation.admob.AdMobAdapter {
    public *;
}

# For Native Ads
-keep class com.google.android.gms.ads.nativead.** {
    public *;
}

# For Rewarded Ads
-keep class com.google.android.gms.ads.rewarded.** {
    public *;
}

# For Interstitial Ads
-keep class com.google.android.gms.ads.interstitial.** {
    public *;
}

# For Banner Ads
-keep class com.google.android.gms.ads.AdView {
    public *;
}
-keep class com.google.android.gms.ads.AdSize {
    public *;
}

# Preserve line numbers for better crash reports
-keepattributes SourceFile,LineNumberTable

# Handle JavaScript interfaces if you use them in ads
-keepattributes *Annotation*
-keepattributes JavascriptInterface

# Compose specific rules (usually handled by the compiler, but good to have if issues arise)
-keepclassmembers class * extends androidx.compose.runtime.RecomposeScope { *; }
-keep class androidx.compose.runtime.CompositionLocal { *; }

# Prevent obfuscation of AdMob internal classes that might be accessed via reflection
-dontwarn com.google.android.gms.ads.**
-dontwarn com.google.ads.mediation.**
