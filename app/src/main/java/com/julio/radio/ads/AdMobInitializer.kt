package com.julio.radio.ads

import android.content.Context
import com.google.android.gms.ads.MobileAds

/** Inicializa el SDK una sola vez al arrancar la aplicación. */
object AdMobInitializer {
    fun initialize(context: Context) {
        MobileAds.initialize(context.applicationContext) {}
    }
}
