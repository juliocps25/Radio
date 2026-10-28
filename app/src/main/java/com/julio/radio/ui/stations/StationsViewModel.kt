package com.julio.radio.ui.stations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.julio.radio.data.RadioRepository
import com.julio.radio.data.model.Station
import com.julio.radio.data.prefs.FavoritesRepository
import com.julio.radio.data.prefs.SettingsRepository
import com.julio.radio.domain.AppLanguage
import com.julio.radio.playback.PlayerController
import com.julio.radio.playback.PlayerUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class StationsTab { ALL, FAVORITES }

data class StationsUiState(
    val countryCode: String? = null,
    val language: AppLanguage = AppLanguage.EN,
    val languageOverride: AppLanguage? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val stations: List<Station> = emptyList(),
    val favorites: List<Station> = emptyList(),
    val tab: StationsTab = StationsTab.ALL,
    val query: String = "",
    val player: PlayerUiState = PlayerUiState()
) {
    val favoriteIds: Set<String> = favorites.mapTo(HashSet()) { it.id }

    // La pestaña "Favoritas" no depende de la carga del país actual.
    val showLoading: Boolean get() = tab == StationsTab.ALL && loading
    val showError: Boolean get() = tab == StationsTab.ALL && error != null

    val visible: List<Station>
        get() {
            val source = if (tab == StationsTab.ALL) stations else favorites
            if (query.isBlank()) return source
            val q = query.trim()
            return source.filter {
                it.name.contains(q, ignoreCase = true) || it.tags.contains(q, ignoreCase = true)
            }
        }
}

@HiltViewModel
class StationsViewModel @Inject constructor(
    private val repository: RadioRepository,
    private val settings: SettingsRepository,
    private val favoritesRepo: FavoritesRepository,
    private val player: PlayerController
) : ViewModel() {

    private val _state = MutableStateFlow(StationsUiState())
    val state: StateFlow<StationsUiState> = _state.asStateFlow()

    init {

        viewModelScope.launch {
            settings.settings.collectLatest { s ->
                _state.update {
                    it.copy(language = s.effectiveLanguage, languageOverride = s.languageOverride)
                }
            }
        }

        viewModelScope.launch {
            settings.settings.map { it.countryCode }.distinctUntilChanged().collectLatest { code ->
                _state.update { it.copy(countryCode = code, query = "", tab = StationsTab.ALL) }
                if (code != null) loadStations(code)
                else _state.update { it.copy(loading = false, stations = emptyList(), error = null) }
            }
        }

        viewModelScope.launch {
            favoritesRepo.favorites.collectLatest { list ->
                _state.update { it.copy(favorites = list) }
            }
        }
        viewModelScope.launch {
            player.state.collect { p -> _state.update { it.copy(player = p) } }
        }
    }

    private suspend fun loadStations(code: String) {
        _state.update { it.copy(loading = true, error = null, stations = emptyList()) }
        repository.getStations(code)
            .onSuccess { list -> _state.update { it.copy(loading = false, stations = list) } }
            .onFailure { e ->
                _state.update { it.copy(loading = false, error = e.message ?: "Error de conexión") }
            }
    }

    fun retry() {
        val code = _state.value.countryCode ?: return
        viewModelScope.launch { loadStations(code) }
    }

    fun onQueryChange(value: String) = _state.update { it.copy(query = value) }

    fun setTab(tab: StationsTab) = _state.update { it.copy(tab = tab) }

    fun toggleFavorite(station: Station) {
        viewModelScope.launch { favoritesRepo.toggle(station) }
    }

    fun onStationClick(station: Station) {
        if (_state.value.player.station?.id == station.id) player.togglePlayPause()
        else player.play(station)
    }

    fun togglePlayPause() = player.togglePlayPause()

    fun stopPlayback() = player.stop()

    fun setSleepTimer(minutes: Int?) = player.setSleepTimer(minutes)

    fun changeCountry() {
        viewModelScope.launch { settings.clearCountry() }
    }

    fun setLanguageOverride(language: AppLanguage?) {
        viewModelScope.launch { settings.setLanguageOverride(language) }
    }
}