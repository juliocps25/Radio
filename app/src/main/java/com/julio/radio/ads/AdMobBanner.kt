package com.julio.radio.ads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/** Banner adaptativo de AdMob listo para colocarse en cualquier pantalla Compose. */
@Composable
fun AdMobBanner(
    modifier: Modifier = Modifier,
    adUnitId: String = AdMobAdUnitIds.BANNER
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val adSize = remember(configuration.screenWidthDp, context) {
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(
            context,
            configuration.screenWidthDp
        )
    }
    val adView = remember(context, adUnitId, adSize) {
        AdView(context).apply {
            setAdSize(adSize)
            this.adUnitId = adUnitId
            loadAd(AdRequest.Builder().build())
        }
    }

    DisposableEffect(adView) {
        onDispose(adView::destroy)
    }

    AndroidView(
        factory = { adView },
        modifier = modifier
    )
}
