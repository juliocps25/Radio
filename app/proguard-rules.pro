# --- Retrofit / OkHttp ---
-keepattributes Signature, InnerClasses, EnclosingMethod, RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations, AnnotationDefault
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# --- API y modelos (kotlinx.serialization) ---
-keep interface com.julio.radio.data.remote.RadioBrowserApi { *; }
-keep class com.julio.radio.data.model.** { *; }
-keepclassmembers class com.julio.radio.data.model.** {
    *** Companion;
}
-keepclasseswithmembers class com.julio.radio.data.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# --- Servicio de reproducción ---
-keep class com.julio.radio.playback.PlaybackService

# --- Stack traces legibles en Play Console ---
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile