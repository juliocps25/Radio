package com.julio.radio.ui.countries

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.julio.radio.data.RadioRepository
import com.julio.radio.data.model.Country
import com.julio.radio.data.prefs.SettingsRepository
import com.julio.radio.domain.localizedCountryName
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CountriesUiState(
    val loading: Boolean = true,
    val error: String? = null,
    val countries: List<Country> = emptyList(),
    val query: String = ""
) {
    val filtered: List<Country>
        get() = if (query.isBlank()) countries else {
            val q = query.trim()
            countries.filter {
                localizedCountryName(it.code, it.name).contains(q, ignoreCase = true) ||
                        it.name.contains(q, ignoreCase = true)
            }
        }
}

@HiltViewModel
class CountriesViewModel @Inject constructor(
    private val repository: RadioRepository,
    private val settings: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CountriesUiState())
    val state: StateFlow<CountriesUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            repository.getCountries()
                .onSuccess { list ->
                    _state.update { it.copy(loading = false, countries = list) }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(loading = false, error = e.message ?: "Error de conexión")
                    }
                }
        }
    }

    fun onQueryChange(value: String) = _state.update { it.copy(query = value) }

    fun selectCountry(code: String) {
        viewModelScope.launch { settings.setCountry(code) }
    }
}