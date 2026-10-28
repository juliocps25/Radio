package com.julio.radio.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.julio.radio.data.model.Station
import com.julio.radio.data.remote.appJson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import javax.inject.Inject
import javax.inject.Singleton

private val Context.favoritesStore: DataStore<Preferences> by preferencesDataStore(name = "favorites")

@Singleton
class FavoritesRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val key = stringPreferencesKey("stations_json")

    private fun decode(raw: String?): List<Station> =
        raw?.let { runCatching { appJson.decodeFromString<List<Station>>(it) }.getOrNull() }
            ?: emptyList()

    val favorites: Flow<List<Station>> = context.favoritesStore.data.map { decode(it[key]) }

    suspend fun toggle(station: Station) {
        context.favoritesStore.edit { prefs ->
            val current = decode(prefs[key])
            val updated = if (current.any { it.id == station.id }) {
                current.filterNot { it.id == station.id }
            } else {
                listOf(station) + current
            }
            prefs[key] = appJson.encodeToString(updated)
        }
    }
}