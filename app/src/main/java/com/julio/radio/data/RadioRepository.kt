package com.julio.radio.data

import com.julio.radio.data.model.Country
import com.julio.radio.data.model.Station
import com.julio.radio.data.remote.RadioBrowserApi
import com.julio.radio.data.remote.RadioServers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton


class RadioRepository @Inject constructor(){

    private val mutex = Mutex()
    private var apis: List<RadioBrowserApi> = emptyList()

    private var countriesCache: List<Country>? = null
    private val stationsCache = mutableMapOf<String, List<Station>>()

    private suspend fun apiList(): List<RadioBrowserApi> = mutex.withLock {
        if (apis.isEmpty()) {
            apis = withContext(Dispatchers.IO) {
                RadioServers.discover().map(RadioServers::createApi)
            }
        }
        apis
    }

    private suspend fun <T> withFailover(block: suspend (RadioBrowserApi) -> T): Result<T> =
        withContext(Dispatchers.IO) {
            var last: Throwable? = null
            for (api in apiList()) {
                try {
                    return@withContext Result.success(block(api))
                } catch (e: Exception) {
                    last = e
                }
            }
            Result.failure(last ?: IllegalStateException("Sin servidores disponibles"))
        }

    suspend fun getCountries(forceRefresh: Boolean = false): Result<List<Country>> {
        countriesCache?.takeIf { !forceRefresh }?.let { return Result.success(it) }
        return withFailover { api ->
            api.countries(hideBroken = true).filter { it.code.length == 2 && it.stationCount > 0 }
        }.onSuccess { countriesCache = it }
    }

    suspend fun getStations(countryCode: String, forceRefresh: Boolean = false): Result<List<Station>> {
        val key = countryCode.uppercase()
        stationsCache[key]?.takeIf { !forceRefresh }?.let { return Result.success(it) }
        return withFailover { api ->
            api.stationsByCountry(code = key, hideBroken = true).filter { it.streamUrl.isNotBlank() }
        }.onSuccess { stationsCache[key] = it }
    }

    suspend fun search(query: String, countryCode: String?): Result<List<Station>> =
        withFailover { api ->
            api.search(name = query, countryCode = countryCode?.uppercase(), hideBroken = true).filter { it.streamUrl.isNotBlank() }
        }

    suspend fun registerClick(stationId: String) {
        withFailover { it.registerClick(stationId) }
    }
}