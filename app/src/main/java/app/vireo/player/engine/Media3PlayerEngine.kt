package app.vireo.player.engine

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import app.vireo.player.PlayerProgressState
import app.vireo.player.PlayerTransportState
import app.vireo.player.normalizePlayerProgress
import app.vireo.player.source.PlaybackSource
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Media3-backed implementation of the engine contract.
 *
 * The ExoPlayer instance is internal to the implementation. A later
 * integration step can connect the Android PlayerView without adding
 * Media3 dependencies to the PlayerEngine contract.
 */
class Media3PlayerEngine @Inject constructor(
    @ApplicationContext context: Context,
) : PlayerEngine {

    private val player = ExoPlayer.Builder(context)
        .setSeekBackIncrementMs(10_000L)
        .setSeekForwardIncrementMs(10_000L)
        .build()

    private val _state = MutableStateFlow(PlayerEngineState())
    override val state: StateFlow<PlayerEngineState> = _state.asStateFlow()

    private var released = false

    private val listener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            updatePlaybackState()
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            updatePlaybackState()
        }

        override fun onPlayerError(error: PlaybackException) {
            _state.value = _state.value.copy(
                transportState = PlayerTransportState.ERROR,
                isPlaying = false,
                progress = readProgress(),
                error = PlayerEngineError(
                    code = error.errorCode,
                    message = sanitizePlayerEngineError(error.message),
                ),
            )
        }

        override fun onEvents(player: Player, events: Player.Events) {
            _state.value = _state.value.copy(progress = readProgress())
        }
    }

    init {
        player.addListener(listener)
    }

    override fun setSource(
        source: PlaybackSource,
        startPositionMs: Long,
    ) {
        checkNotReleased()

        val httpFactory = DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(source.headers)

        val dataSourceFactory = DefaultDataSource.Factory(
            player.applicationLooper.let { playerContext() },
            httpFactory,
        )

        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
        val mediaSource = mediaSourceFactory.createMediaSource(
            MediaItem.fromUri(source.url)
        )

        player.setMediaSource(
            mediaSource,
            startPositionMs.coerceAtLeast(0L),
        )

        _state.value = PlayerEngineState()
    }

    /*
     * The engine retains the application context supplied at construction.
     * This avoids holding an Activity or Compose view.
     */
    private fun playerContext(): Context = applicationContext

    private val applicationContext: Context =
        contextApplication()

    private fun contextApplication(): Context =
        storedApplicationContext

    private val storedApplicationContext: Context =
        context

    override fun prepare() {
        checkNotReleased()
        player.prepare()
    }

    override fun play() {
        checkNotReleased()
        player.play()
    }

    override fun pause() {
        if (released) return
        player.pause()
    }

    override fun seekTo(positionMs: Long) {
        checkNotReleased()
        player.seekTo(positionMs.coerceAtLeast(0L))
    }

    override fun release() {
        if (released) return
        released = true
        player.removeListener(listener)
        player.release()
        _state.value = PlayerEngineState()
    }

    private fun checkNotReleased() {
        check(!released) { "PlayerEngine has already been released." }
    }

    private fun updatePlaybackState() {
        if (released) return

        _state.value = _state.value.copy(
            transportState = media3TransportState(player.playbackState),
            isPlaying = player.isPlaying,
            progress = readProgress(),
            error = null,
        )
    }

    private fun readProgress(): PlayerProgressState =
        normalizePlayerProgress(
            positionMs = player.currentPosition,
            durationMs = player.duration,
            bufferedPositionMs = player.bufferedPosition,
            isSeekable = player.isCurrentMediaItemSeekable,
        )
}

internal fun media3TransportState(
    playbackState: Int,
): PlayerTransportState = when (playbackState) {
    Player.STATE_BUFFERING -> PlayerTransportState.BUFFERING
    Player.STATE_READY -> PlayerTransportState.READY
    Player.STATE_ENDED -> PlayerTransportState.ENDED
    Player.STATE_IDLE -> PlayerTransportState.IDLE
    else -> PlayerTransportState.IDLE
}

internal fun sanitizePlayerEngineError(message: String?): String {
    val safeMessage = message.orEmpty()
        .replace(
            Regex("https?://[^\\s]+", RegexOption.IGNORE_CASE),
            "[ukryty adres strumienia]",
        )
        .trim()

    return safeMessage.ifBlank { "Playback failed." }
}
