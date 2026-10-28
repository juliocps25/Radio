@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package com.julio.radio.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.julio.radio.R
import com.julio.radio.data.model.Station
import com.julio.radio.playback.PlayerUiState
import com.julio.radio.ui.components.StationLogo
import java.util.Locale

@Composable
fun NowPlayingScreen(
    state: PlayerUiState,
    isFavorite: Boolean,
    onTogglePlay: () -> Unit,
    onStop: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSetSleepTimer: (Int?) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val station = state.station ?: return

    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier
                .systemBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth()) {
                IconButton(onClick = onClose) {
                    Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = stringResource(R.string.close))
                }
            }

            Spacer(Modifier.height(16.dp))
            SpinningBadge(station = station, playing = state.isPlaying)
            Spacer(Modifier.height(32.dp))

            Text(
                text = station.name.trim(),
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = playerSubtitle(state),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(32.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onToggleFavorite, modifier = Modifier.size(56.dp)) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = stringResource(
                            if (isFavorite) R.string.favorite_remove else R.string.favorite_add
                        ),
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(28.dp)
                    )
                }

                FilledIconButton(onClick = onTogglePlay, modifier = Modifier.size(88.dp)) {
                    when {
                        state.isBuffering -> CircularProgressIndicator(
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 4.dp,
                            color = LocalContentColor.current
                        )
                        state.isPlaying -> Icon(
                            PauseIcon,
                            contentDescription = stringResource(R.string.player_pause),
                            modifier = Modifier.size(44.dp)
                        )
                        else -> Icon(
                            Icons.Rounded.PlayArrow,
                            contentDescription = stringResource(R.string.player_play),
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }

                IconButton(onClick = onStop, modifier = Modifier.size(56.dp)) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.player_stop),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(Modifier.height(40.dp))
            SleepTimerSection(
                minutes = state.sleepTimerMinutes,
                remainingMs = state.sleepTimerRemainingMs,
                onSelect = onSetSleepTimer
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SpinningBadge(station: Station, playing: Boolean) {
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(playing) {
        if (playing) {
            while (true) {
                rotation.snapTo(rotation.value % 360f)
                rotation.animateTo(rotation.value + 360f, tween(24_000, easing = LinearEasing))
            }
        }
    }
    val cookie = MaterialShapes.Cookie12Sided.toShape()

    Box(Modifier.size(280.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { rotationZ = rotation.value }
                .clip(cookie)
                .background(MaterialTheme.colorScheme.primaryContainer)
        )
        StationLogo(station, size = 168.dp)
    }
}

@Composable
private fun SleepTimerSection(
    minutes: Int?,
    remainingMs: Long?,
    onSelect: (Int?) -> Unit
) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.sleep_timer),
            style = MaterialTheme.typography.titleMedium
        )
        if (remainingMs != null) {
            Text(
                text = stringResource(R.string.sleep_timer_remaining, formatClock(remainingMs)),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = minutes == null,
                onClick = { onSelect(null) },
                label = { Text(stringResource(R.string.sleep_timer_off)) }
            )
            for (option in listOf(15, 30, 45, 60)) {
                FilterChip(
                    selected = minutes == option,
                    onClick = { onSelect(option) },
                    label = { Text(stringResource(R.string.minutes_short, option)) }
                )
            }
        }
    }
}

private fun formatClock(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0)
    return String.format(Locale.getDefault(), "%d:%02d", totalSeconds / 60, totalSeconds % 60)
}