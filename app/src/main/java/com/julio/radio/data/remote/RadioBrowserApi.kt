package com.julio.radio.data.remote

import com.julio.radio.data.model.Country
import com.julio.radio.data.model.Station
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface RadioBrowserApi {

    @GET("json/countries")
    suspend fun countries(
        @Query("order") order: String = "stationcount",
        @Query("reverse") reverse: Boolean = true,
        @Query("hidebroken") hideBroken: Boolean = true
    ): List<Country>

    @GET("json/stations/bycountrycodeexact/{code}")
    suspend fun stationsByCountry(
        @Path("code") code: String,
        @Query("hidebroken") hideBroken: Boolean = true,
        @Query("order") order: String = "clickcount",
        @Query("reverse") reverse: Boolean = true,
        @Query("limit") limit: Int = 500,
        @Query("offset") offset: Int = 0
    ): List<Station>

    @GET("json/stations/search")
    suspend fun search(
        @Query("name") name: String,
        @Query("countrycode") countryCode: String? = null,
        @Query("hidebroken") hideBroken: Boolean = true,
        @Query("order") order: String = "clickcount",
        @Query("reverse") reverse: Boolean = true,
        @Query("limit") limit: Int = 100
    ): List<Station>

    @GET("json/url/{uuid}")
    suspend fun registerClick(
        @Path("uuid") uuid: String
    )
}