package com.dungz.our_ads.utils

import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.dungz.our_ads.R
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

fun bindNativeAd(adView: NativeAdView, nativeAd: NativeAd) {
    (adView.headlineView as? TextView)?.text = nativeAd.headline

    (adView.bodyView as? TextView)?.apply {
        text = nativeAd.body
        visibility = if (nativeAd.body != null) View.VISIBLE else View.GONE
    }

    (adView.callToActionView as? Button)?.apply {
        text = nativeAd.callToAction
        visibility = if (nativeAd.callToAction != null) View.VISIBLE else View.GONE
    }

    (adView.iconView as? ImageView)?.apply {
        val icon = nativeAd.icon
        if (icon != null) {
            setImageDrawable(icon.drawable)
            visibility = View.VISIBLE
        } else {
            visibility = View.GONE
        }
    }

    (adView.advertiserView as? TextView)?.apply {
        text = nativeAd.advertiser
        visibility = if (nativeAd.advertiser != null) View.VISIBLE else View.GONE
    }

    (adView.starRatingView as? RatingBar)?.apply {
        val stars = nativeAd.starRating
        if (stars != null) {
            rating = stars.toFloat()
            visibility = View.VISIBLE
        } else {
            visibility = View.GONE
        }
    }

    (adView.mediaView as? MediaView)?.apply {
        nativeAd.mediaContent?.let { mediaContent = it }
    }

    adView.setNativeAd(nativeAd)
}

fun inflateNativeAdView(
    context: android.content.Context,
    @LayoutRes layout: Int
): NativeAdView {
    val adView = LayoutInflater.from(context).inflate(layout, null) as NativeAdView
    adView.headlineView = adView.findViewById(R.id.ad_headline)
    adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
    adView.mediaView = adView.findViewById(R.id.ad_media)
    adView.bodyView = adView.findViewById(R.id.ad_body)
    adView.iconView = adView.findViewById(R.id.ad_app_icon)
    adView.advertiserView = adView.findViewById(R.id.ad_advertiser)
    adView.starRatingView = adView.findViewById(R.id.ad_stars)
    return adView
}

@Composable
fun DefaultNativeAdShimmer() {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(
            Color.LightGray.copy(alpha = 0.6f),
            Color.LightGray.copy(alpha = 0.2f),
            Color.LightGray.copy(alpha = 0.6f)
        ),
        start = Offset(translateAnim - 200f, 0f),
        end = Offset(translateAnim, 0f)
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        // Header: icon + title lines
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(shimmerBrush)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(14.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.4f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(shimmerBrush)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Media placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(shimmerBrush)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Body text lines
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(shimmerBrush)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(shimmerBrush)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // CTA button placeholder
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(shimmerBrush)
        )
    }
}