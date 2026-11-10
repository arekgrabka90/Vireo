package app.vireo.player.controller

import app.vireo.player.PlayItem
import app.vireo.player.PlayerTransportState
import app.vireo.player.engine.PlayerEngine
import app.vireo.player.engine.PlayerEngineError
import app.vireo.player.engine.PlayerEngineState
import app.vireo.player.session.PlaybackQueue
import app.vireo.player.session.PlaybackSessionState
import app.vireo.player.source.PlaybackSource
import dagger.hilt.android.scopes.ViewModelScoped
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlayerControllerState(
    val session: PlaybackSessionState? = null,
    val engine: PlayerEngineState = PlayerEngineState(),
    val isReleased: Boolean = false,
) {
    val currentItem: PlayItem?
        get() = session?.currentItem

    val hasNext: Boolean
        get() = session?.hasNext == true

    val hasPrevious: Boolean
        get() = session?.hasPrevious == true

    val error: PlayerEngineError?
        get() = engine.error
}

/**
 * Coordinates one ViewModel-scoped playback session and its engine.
 *
 * This class deliberately has no Compose or Media3 dependency.
 * Commands are synchronous because PlayerEngine currently exposes
 * a synchronous command contract.
 */
@ViewModelScoped
class PlayerController @Inject constructor(
    private val engine: PlayerEngine,
) {
    private val controllerScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

    private val _state = MutableStateFlow(
        PlayerControllerState(engine = engine.state.value)
    )

    val state: StateFlow<PlayerControllerState> = _state.asStateFlow()

    init {
        controllerScope.launch {
            engine.state.collect { engineState ->
                _state.update { current ->
                    if (current.isReleased) {
                        current
                    } else {
                        current.copy(engine = engineState)
                    }
                }
            }
        }
    }

    @Synchronized
    fun open(
        items: List<PlayItem>,
        startIndex: Int,
        source: PlaybackSource,
        autoPlay: Boolean = true,
    ): PlaybackSessionState {
        checkActive()
        require(items.isNotEmpty()) { "Playback queue cannot be empty." }
        require(source.url.isNotBlank()) { "Playback source URL cannot be blank." }

        val session = PlaybackSessionState.create(
            queue = PlaybackQueue.create(items, startIndex),
            activeSource = source,
        )

        _state.update { it.copy(session = session) }

        execute {
            engine.setSource(source, 0L)
            engine.prepare()
            if (autoPlay) engine.play()
        }

        return session
    }

    @Synchronized
    fun prepare() {
        execute { engine.prepare() }
    }

    @Synchronized
    fun play() {
        execute { engine.play() }
    }

    @Synchronized
    fun pause() {
        execute { engine.pause() }
    }

    @Synchronized
    fun seekTo(positionMs: Long) {
        execute {
            engine.seekTo(positionMs.coerceAtLeast(0L))
        }
    }

    /**
     * Changes the active source without changing the current queue item.
     * The current position and playing state are preserved by default.
     */
    @Synchronized
    fun switchSource(
        source: PlaybackSource,
        positionMs: Long? = null,
        resumePlayback: Boolean? = null,
    ): Boolean {
        checkActive()

        val session = _state.value.session ?: return false
        require(source.url.isNotBlank()) { "Playback source URL cannot be blank." }

        val engineState = engine.state.value
        val targetPosition = (
            positionMs ?: engineState.progress.positionMs
        ).coerceAtLeast(0L)
        val shouldResume = resumePlayback ?: engineState.isPlaying

        val succeeded = execute {
            engine.setSource(source, targetPosition)
            engine.prepare()
            if (shouldResume) engine.play()
        }

        if (succeeded) {
            _state.update { current ->
                current.copy(
                    session = session.copy(activeSource = source)
                )
            }
        }

        return succeeded
    }

    @Synchronized
    fun next(
        source: PlaybackSource,
        autoPlay: Boolean = true,
    ): Boolean {
        checkActive()
        val session = _state.value.session ?: return false
        val nextQueue = session.queue.next() ?: return false

        return moveToQueue(nextQueue, source, autoPlay)
    }

    @Synchronized
    fun previous(
        source: PlaybackSource,
        autoPlay: Boolean = true,
    ): Boolean {
        checkActive()
        val session = _state.value.session ?: return false
        val previousQueue = session.queue.previous() ?: return false

        return moveToQueue(previousQueue, source, autoPlay)
    }

    @Synchronized
    fun moveTo(
        index: Int,
        source: PlaybackSource,
        autoPlay: Boolean = true,
    ): Boolean {
        checkActive()
        val session = _state.value.session ?: return false
        val queue = session.queue.moveTo(index) ?: return false

        return moveToQueue(queue, source, autoPlay)
    }

    private fun moveToQueue(
        queue: PlaybackQueue,
        source: PlaybackSource,
        autoPlay: Boolean,
    ): Boolean {
        require(source.url.isNotBlank()) { "Playback source URL cannot be blank." }

        val session = _state.value.session ?: return false
        val item = queue.current ?: return false

        val succeeded = execute {
            engine.setSource(source, 0L)
            engine.prepare()
            if (autoPlay) engine.play()
        }

        if (succeeded) {
            _state.update { current ->
                current.copy(
                    session = session.copy(
                        queue = queue,
                        activeSource = source,
                    )
                )
            }
        }

        return succeeded
    }

    /**
     * Permanently releases this controller and its engine.
     * Repeated release calls are safe.
     */
    @Synchronized
    fun release() {
        if (_state.value.isReleased) return

        try {
            engine.release()
        } finally {
            controllerScope.cancel()
            _state.update {
                it.copy(
                    engine = PlayerEngineState(),
                    isReleased = true,
                )
            }
        }
    }

    private fun execute(action: () -> Unit): Boolean {
        checkActive()

        return try {
            action()
            true
        } catch (error: Exception) {
            publishError(error)
            false
        }
    }

    private fun publishError(error: Exception) {
        val message = sanitizeError(error.message)

        _state.update { current ->
            current.copy(
                engine = current.engine.copy(
                    transportState = PlayerTransportState.ERROR,
                    isPlaying = false,
                    error = PlayerEngineError(
                        code = -1,
                        message = message,
                    ),
                )
            )
        }
    }

    private fun checkActive() {
        check(!_state.value.isReleased) {
            "PlayerController has already been released."
        }
    }

    private fun sanitizeError(message: String?): String {
        val withoutUrls = message.orEmpty().replace(
            Regex("https?://[^\\s]+", RegexOption.IGNORE_CASE),
            "[hidden stream address]",
        )

        val withoutCredentials = withoutUrls.replace(
            Regex(
                "(?i)(username|user|password|pass|token|api_key)=([^&\\s]+)"
            ),
            "$1=[hidden]",
        )

        return withoutCredentials.ifBlank { "Playback failed." }
    }
}
