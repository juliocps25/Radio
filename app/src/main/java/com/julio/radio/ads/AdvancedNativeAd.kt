package com.julio.radio.ads

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView

/**
 * Carga y muestra un anuncio nativo avanzado. El SDK gestiona sus clics,
 * impresiones y el indicador AdChoices mediante [NativeAdView].
 */
@Composable
fun AdvancedNativeAd(
    modifier: Modifier = Modifier,
    adUnitId: String = AdMobAdUnitIds.ADVANCED_NATIVE,
    onLoadFailed: (LoadAdError) -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var nativeAd by remember(adUnitId) { mutableStateOf<NativeAd?>(null) }
    val currentOnLoadFailed by rememberUpdatedState(onLoadFailed)

    DisposableEffect(context, adUnitId) {
        var disposed = false
        val adLoader = AdLoader.Builder(context, adUnitId)
            .forNativeAd { loadedAd ->
                if (disposed) {
                    loadedAd.destroy()
                } else {
                    nativeAd?.destroy()
                    nativeAd = loadedAd
                }
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    currentOnLoadFailed(error)
                }
            })
            .build()

        adLoader.loadAd(AdRequest.Builder().build())

        onDispose {
            disposed = true
            nativeAd?.destroy()
            nativeAd = null
        }
    }

    nativeAd?.let { ad ->
        val nativeAdLayout = remember(ad) { RadioNativeAdLayout(context) }
        AndroidView(
            factory = { nativeAdLayout.root },
            modifier = modifier,
            update = { nativeAdLayout.bind(ad) }
        )
    }
}

private class RadioNativeAdLayout(context: Context) {
    val root = NativeAdView(context)
    private val icon = ImageView(context)
    private val headline = TextView(context)
    private val body = TextView(context)
    private val advertiser = TextView(context)
    private val media = MediaView(context)
    private val callToAction = Button(context)

    init {
        root.background = GradientDrawable().apply {
            setColor(Color.WHITE)
            cornerRadius = 20f
        }
        root.setPadding(24, 24, 24, 24)

        val content = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }
        val heading = LinearLayout(context).apply {
            gravity = Gravity.CENTER_VERTICAL
            orientation = LinearLayout.HORIZONTAL
        }
        icon.layoutParams = LinearLayout.LayoutParams(96, 96).apply { marginEnd = 20 }
        headline.setTextColor(Color.BLACK)
        headline.textSize = 18f
        headline.maxLines = 2
        headline.layoutParams = LinearLayout.LayoutParams(
            0,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            1f
        )
        body.setTextColor(Color.DKGRAY)
        body.textSize = 14f
        advertiser.setTextColor(Color.GRAY)
        advertiser.textSize = 12f
        val mediaHeight = (180 * context.resources.displayMetrics.density).toInt()
        media.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            mediaHeight
        ).apply { topMargin = 16 }
        callToAction.isAllCaps = false
        callToAction.isClickable = false
        callToAction.layoutParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = 12 }

        heading.addView(icon)
        heading.addView(headline)
        content.addView(heading)
        content.addView(advertiser)
        content.addView(media)
        content.addView(body)
        content.addView(callToAction)
        root.addView(content)

        root.iconView = icon
        root.headlineView = headline
        root.bodyView = body
        root.advertiserView = advertiser
        root.mediaView = media
        root.callToActionView = callToAction
    }

    fun bind(ad: NativeAd) {
        headline.text = ad.headline
        bindOptional(ad.icon?.drawable, icon) { drawable -> icon.setImageDrawable(drawable) }
        bindOptional(ad.advertiser, advertiser) { value -> advertiser.text = value }
        bindOptional(ad.body, body) { value -> body.text = value }
        bindOptional(ad.callToAction, callToAction) { value -> callToAction.text = value }
        root.setNativeAd(ad)
    }

    private fun <T> bindOptional(value: T?, view: View, render: (T) -> Unit) {
        if (value == null) {
            view.visibility = View.GONE
        } else {
            render(value)
            view.visibility = View.VISIBLE
        }
    }
}
