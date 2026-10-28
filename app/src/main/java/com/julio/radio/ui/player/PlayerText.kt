package com.julio.radio.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.julio.radio.R
import com.julio.radio.playback.PlayerUiState

@Composable
fun playerSubtitle(state: PlayerUiState): String =
    state.error?.let { stringResource(it.messageRes) }
        ?: state.nowPlaying
        ?: stringResource(
            when {
                state.isBuffering -> R.string.status_connecting
                state.isPlaying -> R.string.status_live
                else -> R.string.status_paused
            }
        )