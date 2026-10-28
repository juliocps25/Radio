package com.julio.radio.data.remote

import com.julio.radio.data.model.Country
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import okhttp3.MediaType.Companion.toMediaType
import java.util.concurrent.TimeUnit
import com.julio.radio.BuildConfig

internal val appJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    isLenient = true
}

internal val httpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", "RadioWorld/1.0")
                    .build()
            )
        }
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
            else HttpLoggingInterceptor.Level.NONE
        })
        .build()
}

@Serializable
private data class ServerDto(val name: String)

object RadioServers {

    private const val DISCOVER_URL = "https://all.api.radio-browser.info/json/servers"

    private val fallback = listOf(
        "https://de1.api.radio-browser.info/",
        "https://de2.api.radio-browser.info/",
        "https://nl1.api.radio-browser.info/"
    )

    fun discover (): List<String> = try {
        val request = Request.Builder().url(DISCOVER_URL).build()
        httpClient.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            val names = appJson.decodeFromString<List<ServerDto>>(body)
                .map { it.name }
                .distinct()
                .filter { it.isNotBlank() }
            names.map { "https://$it/" }.shuffled().ifEmpty { fallback }
        }
    } catch (e: Exception) {
        fallback
    }

    fun createApi(baseUrl: String): RadioBrowserApi =
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(httpClient)
            .addConverterFactory(appJson.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(RadioBrowserApi::class.java)
}
