package com.dungz.our_ads.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import com.dungz.our_ads.R
import com.google.android.gms.ads.nativead.NativeAd

@Composable
fun NativeFullScreenAdView(
    nativeAd: NativeAd,
    onCloseClick: () -> Unit
) {
    Dialog(
        onDismissRequest = onCloseClick,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            NativeAdView(
                nativeAd = nativeAd,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 16.dp, end = 16.dp, top = 56.dp, bottom = 16.dp)
                ) {
                    // Header: Icon + Title + Advertiser + Ad Badge
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        nativeAd.icon?.let { icon ->
                            NativeAdIconView(modifier = Modifier.size(48.dp)) {
                                icon.drawable?.toBitmap()?.let { bitmap ->
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = "Icon",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            nativeAd.headline?.let { headline ->
                                NativeAdHeadlineView {
                                    Text(
                                        text = headline,
                                        style = MaterialTheme.typography.titleLarge,
                                        maxLines = 2
                                    )
                                }
                            }
                            nativeAd.advertiser?.let { advertiser ->
                                NativeAdAdvertiserView {
                                    Text(
                                        text = advertiser,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.Gray
                                    )
                                }
                            }
                        }

                        NativeAdAttribution(
                            containerColor = Color(0xFFFFA500),
                            contentColor = Color.White
                        )
                    }

                    // Media Content
                    NativeAdMediaView(
                        nativeAd,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                    )

                    // Body
                    nativeAd.body?.let { body ->
                        NativeAdBodyView(modifier = Modifier.padding(bottom = 12.dp)) {
                            Text(
                                text = body,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 3
                            )
                        }
                    }

                    // Footer: Rating + CTA
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        nativeAd.starRating?.let { rating ->
                            NativeAdStarRatingView {
                                Text(
                                    text = "★ ".repeat(rating.toInt()),
                                    color = Color(0xFFFFD700),
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        nativeAd.callToAction?.let { cta ->
                            NativeAdCallToActionView {
                                NativeAdButton(
                                    text = cta,
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                    }
                }
            }

            // Close Button
            IconButton(
                onClick = onCloseClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .size(36.dp)
            ) {
                Icon(
                    painterResource(R.drawable.our_ads_ic_close),
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
