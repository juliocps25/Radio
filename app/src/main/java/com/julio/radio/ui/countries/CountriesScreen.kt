@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package com.julio.radio.ui.countries

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.julio.radio.R
import com.julio.radio.data.model.Country
import com.julio.radio.domain.countryFlag
import com.julio.radio.domain.localizedCountryName
import com.julio.radio.ui.components.EmptyState
import com.julio.radio.ui.components.ErrorState
import com.julio.radio.ui.components.LoadingState
import com.julio.radio.ui.components.SearchField
import com.julio.radio.ui.components.SegmentedItem

@Composable
fun CountriesScreen(
    modifier: Modifier = Modifier,
    vm: CountriesViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.choose_country_title)) },
                subtitle = {
                    if (!state.loading && state.error == null) {
                        Text(stringResource(R.string.countries_with_stations, state.countries.size))
                    }
                },
                scrollBehavior = scrollBehavior
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            SearchField(
                query = state.query,
                onQueryChange = vm::onQueryChange,
                placeholder = stringResource(R.string.search_country),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )

            when {
                state.loading -> LoadingState()
                state.error != null -> ErrorState(stringResource(R.string.error_connection), onRetry = vm::load)
                state.filtered.isEmpty() -> EmptyState(stringResource(R.string.no_countries_match))
                else -> {
                    val list = state.filtered
                    LazyColumn(
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        itemsIndexed(list, key = { _, c -> c.code }) { index, country ->
                            SegmentedItem(
                                index = index,
                                count = list.size,
                                onClick = { vm.selectCountry(country.code) }
                            ) {
                                FlagBadge(country.code)
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        text = localizedCountryName(country.code, country.name),
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    Text(
                                        text = pluralStringResource(R.plurals.stations_count, country.stationCount, country.stationCount),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FlagBadge(code: String) {
    val shape = MaterialShapes.Cookie9Sided.toShape()
    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Text(countryFlag(code), style = MaterialTheme.typography.headlineSmall)
    }
}