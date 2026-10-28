package com.julio.radio.data.prefs

import android.content.Context
import androidx.compose.ui.input.key.Key
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.julio.radio.domain.AppLanguage
import com.julio.radio.domain.CountryLanguages
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class UserSettings(
    val countryCode: String? = null,
    val languageOverride: AppLanguage? = null
) {
    val effectiveLanguage: AppLanguage
        get() = languageOverride ?: CountryLanguages.forCountry(countryCode)
}

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val COUNTRY = stringPreferencesKey("country:code")
        val LANGUAGE_OVERRIDE = stringPreferencesKey("language_override")
    }

    val settings: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            countryCode = prefs[Keys.COUNTRY],
            languageOverride = AppLanguage.fromTag(prefs[Keys.LANGUAGE_OVERRIDE])
        )
    }

    suspend fun setCountry(code: String) {
        context.dataStore.edit { it[Keys.COUNTRY] = code.uppercase() }
    }

    suspend fun clearCountry() {
        context.dataStore.edit { it.remove(Keys.COUNTRY) }
    }

    suspend fun setLanguageOverride(language: AppLanguage?) {
        context.dataStore.edit {
            if (language == null) it.remove(Keys.LANGUAGE_OVERRIDE)
            else it[Keys.LANGUAGE_OVERRIDE] = language.tag
        }
    }
}