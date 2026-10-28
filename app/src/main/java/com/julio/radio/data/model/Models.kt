package com.julio.radio.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Country(
    val name: String,
    @SerialName("iso_3166_1") val code: String = "",
    @SerialName("stationcount") val stationCount: Int = 0
)
@Serializable
data class Station(
    @SerialName("stationuuid") val id: String,
    val name: String,
    @SerialName("url_resolved") val streamUrl: String = "",
    val homepage: String = "",
    val favicon: String = "",
    val tags: String = "",
    val codec: String = "",
    val bitrate: Int = 0,
    val hls: Int = 0,
    @SerialName("countrycode") val countryCode: String = "",
    val language: String = "",
    val votes: Int = 0,
    @SerialName("clickcount") val clickCount: Int = 0
) {
    val tagList: List<String>
        get() = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}


