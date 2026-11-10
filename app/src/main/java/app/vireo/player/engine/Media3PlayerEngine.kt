package app.vireo.player.engine

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import app.vireo.player.PlaybackCallbackGuard
import app.vireo.player.PlayerProgressState
import app.vireo.player.PlayerTransportState
import app.vireo.player.normalizePlayerProgress
import app.vireo.player.source.PlaybackSource
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Media3-backed implementation of the engine contract.
 *
 * This ViewModel-scoped engine owns the ExoPlayer instance shared with
 * PlayerView through an internal bridge. The PlayerEngine contract
 * remains independent of Media3.
 */
@OptIn(UnstableApi::class)
@ViewModelScoped
class Media3PlayerEngine @Inject constructor(
    @ApplicationContext private val context: Context,
) : PlayerEngine {

    private val player = ExoPlayer.Builder(context)
        .setSeekBackIncrementMs(10_000L)
        .setSeekForwardIncrementMs(10_000L)
        .build()

    private val _state = MutableStateFlow(PlayerEngineState())
    override val state: StateFlow<PlayerEngineState> = _state.asStateFlow()

    private var released = false
    private var mediaItemSequence = 0L

    private val listener = object : Player.Listener, AnalyticsListener {
        override fun onPlaybackStateChanged(
            eventTime: AnalyticsListener.EventTime,
            playbackState: Int,
        ) {
            if (!isCurrentEvent(eventTime)) return
            updatePlaybackState()
        }

        override fun onIsPlayingChanged(
            eventTime: AnalyticsListener.EventTime,
            isPlaying: Boolean,
        ) {
            if (!isCurrentEvent(eventTime)) return
            updatePlaybackState()
        }

        override fun onPlayWhenReadyChanged(
            eventTime: AnalyticsListener.EventTime,
            playWhenReady: Boolean,
            reason: Int,
        ) {
            if (!isCurrentEvent(eventTime)) return
            updatePlaybackState()
        }

        override fun onPlayerError(
            eventTime: AnalyticsListener.EventTime,
            error: PlaybackException,
        ) {
            if (!isCurrentEvent(eventTime)) return

            if (!PlaybackCallbackGuard.isCurrentError(
                    error,
                    player.playerError,
                )
            ) return

            _state.value = _state.value.copy(
                transportState = PlayerTransportState.ERROR,
                isPlaying = false,
                playWhenReady = player.playWhenReady,
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
        player.addAnalyticsListener(listener)
    }

    override fun setSource(
        source: PlaybackSource,
        startPositionMs: Long,
    ) {
        checkNotReleased()

        val httpFactory = DefaultHttpDataSource.Factory()
            .setDefaultRequestProperties(source.headers)

        val dataSourceFactory = DefaultDataSource.Factory(
            context,
            httpFactory,
        )

        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
        val mediaItem = MediaItem.Builder()
            .setUri(source.url)
            .setMediaId("vireo-source-${++mediaItemSequence}")
            .build()
        val mediaSource = mediaSourceFactory.createMediaSource(mediaItem)

        // Nie pozostawiamy starego źródła ani jego bufora przy zmianie.
        player.stop()
        player.clearMediaItems()

        player.setMediaSource(
            mediaSource,
            startPositionMs.coerceAtLeast(0L),
        )

        _state.value = PlayerEngineState()
    }

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
        player.removeAnalyticsListener(listener)
        player.release()
        _state.value = PlayerEngineState()
    }

    /** Internal PlayerView bridge; the engine contract stays Media3-independent. */
    internal fun playerForView(): ExoPlayer {
        checkNotReleased()
        return player
    }

    private fun isCurrentEvent(
        eventTime: AnalyticsListener.EventTime,
    ): Boolean = PlaybackCallbackGuard.isCurrentMediaItem(
        eventTime.timeline,
        eventTime.windowIndex,
        player.currentMediaItem?.mediaId,
    )

    private fun checkNotReleased() {
        check(!released) { "PlayerEngine has already been released." }
    }

    private fun updatePlaybackState() {
        if (released) return

        _state.value = _state.value.copy(
            transportState = media3TransportState(player.playbackState),
            isPlaying = player.isPlaying,
            playWhenReady = player.playWhenReady,
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
