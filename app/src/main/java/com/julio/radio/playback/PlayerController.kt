package com.julio.radio.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.julio.radio.R
import com.julio.radio.data.RadioRepository
import com.julio.radio.data.model.Station
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

enum class PlayerError(@StringRes val messageRes: Int) {
    PLAYBACK(R.string.error_playback),
    START(R.string.error_player_start)
}

data class PlayerUiState(
    val station: Station? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val nowPlaying: String? = null,
    val error: PlayerError? = null,
    val sleepTimerMinutes: Int? = null,
    val sleepTimerRemainingMs: Long? = null
)

private const val MAX_RETRIES = 3
private const val RETRY_DELAY_MS = 2_000L

// Errores que suelen ser pasajeros (red caída, timeout, ventana en vivo HLS)
private val RETRYABLE_ERRORS = setOf(
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
    PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
    PlaybackException.ERROR_CODE_IO_UNSPECIFIED,
    PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW
)

@Singleton
class PlayerController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: RadioRepository
) {
    private val _state = MutableStateFlow(PlayerUiState())
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var controller: MediaController? = null
    private var connecting = false
    private val pending = mutableListOf<(MediaController) -> Unit>()

    private var retries = 0
    private var retryJob: Job? = null
    private var timerJob: Job? = null

    private val listener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) retries = 0
            _state.update {
                it.copy(
                    isBuffering = playbackState == Player.STATE_BUFFERING ||
                            (it.isBuffering && retryJob?.isActive == true),
                    error = if (playbackState == Player.STATE_READY) null else it.error
                )
            }
        }

        override fun onMediaMetadataChanged(mediaMetadata: MediaMetadata) {
            // Si el stream envía el título de la canción (ICY), reemplaza al nombre de la emisora.
            val title = mediaMetadata.title?.toString()?.trim()
            val stationName = _state.value.station?.name?.trim()
            val now = title?.takeIf { it.isNotEmpty() && it != stationName }
            _state.update { it.copy(nowPlaying = now) }
        }

        override fun onPlayerError(error: PlaybackException) {
            if (error.errorCode in RETRYABLE_ERRORS && retries < MAX_RETRIES) {
                retries++
                _state.update { it.copy(isBuffering = true, isPlaying = false, error = null) }
                retryJob?.cancel()
                retryJob = scope.launch {
                    delay(RETRY_DELAY_MS * retries)
                    controller?.let { c ->
                        c.prepare()
                        c.play()
                    }
                }
            } else {
                retries = 0
                _state.update {
                    it.copy(isBuffering = false, isPlaying = false, error = PlayerError.PLAYBACK)
                }
            }
        }
    }

    private fun withController(block: (MediaController) -> Unit) {
        val c = controller
        if (c != null) {
            block(c)
        } else {
            pending += block
            connect()
        }
    }

    private fun connect() {
        if (connecting) return
        connecting = true
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        val future = MediaController.Builder(context, token).buildAsync()
        future.addListener({
            connecting = false
            runCatching { future.get() }
                .onSuccess { c ->
                    controller = c
                    c.addListener(listener)
                    val actions = pending.toList()
                    pending.clear()
                    actions.forEach { it(c) }
                }
                .onFailure {
                    pending.clear()
                    _state.update { s ->
                        s.copy(isBuffering = false, error = PlayerError.START)
                    }
                }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun resetRetries() {
        retries = 0
        retryJob?.cancel()
    }

    fun play(station: Station) {
        resetRetries()
        _state.update {
            it.copy(station = station, isBuffering = true, nowPlaying = null, error = null)
        }
        withController { c ->
            c.setMediaItem(station.toMediaItem())
            c.prepare()
            c.play()
        }
        scope.launch { repository.registerClick(station.id) }
    }

    fun togglePlayPause() {
        resetRetries()
        withController { c ->
            when {
                // Detenida por pausa o por error: reconectar en vivo
                c.playbackState == Player.STATE_IDLE && c.mediaItemCount > 0 -> {
                    _state.update { it.copy(isBuffering = true, error = null) }
                    c.prepare()
                    c.play()
                }
                c.playWhenReady -> c.pause()
                else -> c.play()
            }
        }
    }

    fun stop() {
        resetRetries()
        timerJob?.cancel()
        withController { c ->
            c.stop()
            c.clearMediaItems()
        }
        _state.value = PlayerUiState()
    }

    /** [minutes] = null cancela el temporizador. Al terminar, detiene la radio. */
    fun setSleepTimer(minutes: Int?) {
        timerJob?.cancel()
        if (minutes == null) {
            _state.update { it.copy(sleepTimerMinutes = null, sleepTimerRemainingMs = null) }
            return
        }
        val total = minutes * 60_000L
        _state.update { it.copy(sleepTimerMinutes = minutes, sleepTimerRemainingMs = total) }
        timerJob = scope.launch {
            var remaining = total
            while (remaining > 0) {
                delay(1_000)
                remaining -= 1_000
                _state.update { it.copy(sleepTimerRemainingMs = remaining) }
            }
            stop()
        }
    }

    private fun Station.toMediaItem(): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(name.trim())
            .setIsPlayable(true)
            .apply { if (favicon.isNotBlank()) setArtworkUri(Uri.parse(favicon)) }
            .build()
        return MediaItem.Builder()
            .setMediaId(id)
            .setUri(streamUrl)
            .setMediaMetadata(metadata)
            .build()
    }
}