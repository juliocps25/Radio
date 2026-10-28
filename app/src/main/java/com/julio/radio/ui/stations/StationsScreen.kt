@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.julio.radio.ui.stations

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.julio.radio.R
import com.julio.radio.data.model.Station
import com.julio.radio.domain.countryFlag
import com.julio.radio.domain.localizedCountryName
import com.julio.radio.playback.PlayerUiState
import com.julio.radio.ui.components.EmptyState
import com.julio.radio.ui.components.ErrorState
import com.julio.radio.ui.components.LoadingState
import com.julio.radio.ui.components.SearchField
import com.julio.radio.ui.components.SegmentedItem
import com.julio.radio.ui.components.StationLogo
import com.julio.radio.ui.player.MiniPlayer
import com.julio.radio.ui.settings.LanguageDialog
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.rounded.Place
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.text.font.FontWeight
import com.julio.radio.ui.player.NowPlayingScreen

@Composable
fun StationsScreen(
    modifier: Modifier = Modifier,
    vm: StationsViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val code = state.countryCode.orEmpty()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showPlayer by remember { mutableStateOf(false) }

    LaunchedEffect(state.player.station) {
        if (state.player.station == null) showPlayer = false
    }
    BackHandler(enabled = showPlayer) { showPlayer = false }

    Box(modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                LargeFlexibleTopAppBar(
                    title = {
                        Text(
                            text = "${countryFlag(code)} ${localizedCountryName(code, code)}",
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    subtitle = {
                        val language = state.language.displayName
                        Text(
                            text = if (state.loading) {
                                stringResource(R.string.language_subtitle, language)
                            } else {
                                stringResource(
                                    R.string.stations_subtitle,
                                    pluralStringResource(
                                        R.plurals.stations_count,
                                        state.stations.size,
                                        state.stations.size
                                    ),
                                    language
                                )
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    actions = {

                        IconButton(onClick = { showLanguageDialog = true }) {
                            Text(
                                text = state.language.tag.uppercase(),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(onClick = vm::changeCountry) {
                            Icon(
                                imageVector = Icons.Rounded.Place,
                                contentDescription = stringResource(R.string.change_country)
                            )
                        }
                    },
                    scrollBehavior = scrollBehavior
                )
            },
            bottomBar = {
                MiniPlayer(
                    state = state.player,
                    onTogglePlay = vm::togglePlayPause,
                    onStop = vm::stopPlayback,
                    onExpand = { showPlayer = true }
                )
            }
        ) { padding ->
            Column(Modifier.padding(padding)) {
                SearchField(
                    query = state.query,
                    onQueryChange = vm::onQueryChange,
                    placeholder = stringResource(R.string.search_station),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                )

                Row(
                    Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.tab == StationsTab.ALL,
                        onClick = { vm.setTab(StationsTab.ALL) },
                        label = { Text(stringResource(R.string.tab_all)) }
                    )
                    FilterChip(
                        selected = state.tab == StationsTab.FAVORITES,
                        onClick = { vm.setTab(StationsTab.FAVORITES) },
                        label = { Text(stringResource(R.string.tab_favorites, state.favorites.size)) }
                    )
                }

                val visible = remember(state.stations, state.favorites, state.tab, state.query) {
                    state.visible
                }
                when {
                    state.showLoading -> LoadingState()
                    state.showError -> ErrorState(
                        stringResource(R.string.error_connection),
                        onRetry = vm::retry
                    )
                    visible.isEmpty() -> EmptyState(
                        stringResource(
                            if (state.tab == StationsTab.FAVORITES && state.query.isBlank())
                                R.string.favorites_empty
                            else
                                R.string.no_stations_match
                        )
                    )
                    else -> LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        itemsIndexed(
                            items = visible,
                            key = { _, s -> s.id },
                            contentType = { _, _ -> "station" }
                        ) { index, station ->
                            val isCurrent = state.player.station?.id == station.id
                            StationRow(
                                station = station,
                                index = index,
                                count = visible.size,
                                isFavorite = station.id in state.favoriteIds,
                                player = if (isCurrent) state.player else null,
                                onToggleFavorite = vm::toggleFavorite,
                                onClick = vm::onStationClick
                            )
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = showPlayer && state.player.station != null,
            enter = slideInVertically { it },
            exit = slideOutVertically { it }
        ) {
            NowPlayingScreen(
                state = state.player,
                isFavorite = state.player.station?.id in state.favoriteIds,
                onTogglePlay = vm::togglePlayPause,
                onStop = vm::stopPlayback,
                onToggleFavorite = { state.player.station?.let(vm::toggleFavorite) },
                onSetSleepTimer = vm::setSleepTimer,
                onClose = { showPlayer = false }
            )
        }
    }

    if (showLanguageDialog) {
        LanguageDialog(
            current = state.languageOverride,
            onSelect = vm::setLanguageOverride,
            onDismiss = { showLanguageDialog = false }
        )
    }
}

@Composable
private fun StationRow(
    station: Station,
    index: Int,
    count: Int,
    isFavorite: Boolean,
    player: PlayerUiState?,
    onToggleFavorite: (Station) -> Unit,
    onClick: (Station) -> Unit
) {
    val isCurrent = player != null

    val details = remember(station.id) {
        buildList {
            if (station.codec.isNotBlank()) {
                add(if (station.bitrate > 0) "${station.codec} ${station.bitrate}k" else station.codec)
            }
            addAll(station.tagList.take(2))
        }.joinToString(" · ")
    }

    val supporting = if (player != null) {
        player.error?.let { stringResource(it.messageRes) }
            ?: player.nowPlaying
            ?: stringResource(
                when {
                    player.isBuffering -> R.string.status_connecting
                    player.isPlaying -> R.string.status_playing
                    else -> R.string.status_paused
                }
            )
    } else details

    SegmentedItem(
        index = index,
        count = count,
        onClick = { onClick(station) },
        containerColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainer
    ) {
        StationLogo(station)
        Column(Modifier.weight(1f)) {
            Text(
                text = station.name.trim(),
                style = MaterialTheme.typography.titleMedium,
                color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (supporting.isNotEmpty()) {
                Text(
                    text = supporting,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        IconButton(onClick = { onToggleFavorite(station) }) {
            Icon(
                imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                contentDescription = stringResource(
                    if (isFavorite) R.string.favorite_remove else R.string.favorite_add
                ),
                tint = if (isFavorite) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}