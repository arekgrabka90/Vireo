package app.vireo.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import app.vireo.data.LibraryRepository
import app.vireo.data.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class PlayerUiState(
    val item: PlayItem? = null,
    val runtime: PlayerRuntimeState = PlayerRuntimeState(),
    val progress: PlayerProgressState = PlayerProgressState(),
    val resumePositionMs: Long? = null,
    val retryToken: Int = 0,
) {
    val hasNext: Boolean
        get() = runtime.hasNext

    val error: String?
        get() = runtime.errorMessage
}

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val session: PlaybackSession,
    private val library: LibraryRepository,
    private val profiles: ProfileRepository,
) : ViewModel() {

    // Profil jest przypisany do całej sesji odtwarzania.
    private val playbackProfileId = profiles.activeId
    private var advancing = false

    private fun createState(
        item: PlayItem?,
        resumePositionMs: Long? = null,
    ) = PlayerUiState(
        item = item,
        runtime = PlayerRuntimeState(
            title = item?.title.orEmpty(),
            contentType = item?.contentType,
            hasNext = session.hasNext(),
        ),
        resumePositionMs = resumePositionMs,
    )

    private val _state = MutableStateFlow(createState(session.current()))
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    suspend fun resumePosition(item: PlayItem): Long {
        if (item.contentType == "LIVE") return 0L
        val contentKey = item.contentKey ?: return 0L
        return library.resumePosition(playbackProfileId, contentKey)
    }

    private suspend fun persistProgress(
        item: PlayItem?,
        positionMs: Long,
        durationMs: Long,
    ) {
        val target = item ?: return
        if (target.contentType == "LIVE") return
        if (target.contentKey.isNullOrBlank()) return
        if (durationMs <= 0L) return

        library.saveProgress(
            playbackProfileId,
            target,
            positionMs,
            durationMs,
        )
    }

    fun saveProgress(
        item: PlayItem?,
        positionMs: Long,
        durationMs: Long,
    ) {
        val target = item ?: return
        if (target.contentType == "LIVE") return
        if (target.contentKey.isNullOrBlank()) return
        if (durationMs <= 0L) return

        viewModelScope.launch {
            persistProgress(target, positionMs, durationMs)
        }
    }

    fun updateProgress(
        positionMs: Long,
        durationMs: Long,
        bufferedPositionMs: Long,
        isSeekable: Boolean,
    ) {
        val progress = normalizePlayerProgress(
            positionMs,
            durationMs,
            bufferedPositionMs,
            isSeekable,
        )
        _state.update { it.copy(progress = progress) }
    }

    fun onPlaybackStateChanged(
        playbackState: Int,
        isPlaying: Boolean,
    ) {
        val transport = when (playbackState) {
            Player.STATE_BUFFERING -> PlayerTransportState.BUFFERING
            Player.STATE_READY -> PlayerTransportState.READY
            Player.STATE_ENDED -> PlayerTransportState.ENDED
            Player.STATE_IDLE -> PlayerTransportState.IDLE
            else -> PlayerTransportState.IDLE
        }

        _state.update { current ->
            current.copy(
                runtime = current.runtime.copy(
                    title = current.item?.title.orEmpty(),
                    contentType = current.item?.contentType,
                    transportState = transport,
                    isPlaying = isPlaying,
                    hasNext = session.hasNext(),
                )
            )
        }
    }

    fun onControlsVisibilityChanged(visible: Boolean) {
        _state.update {
            it.copy(runtime = it.runtime.copy(controlsVisible = visible))
        }
    }

    fun next(positionMs: Long, durationMs: Long) {
        if (advancing) return

        val current = session.current() ?: return
        advancing = true

        viewModelScope.launch {
            try {
                persistProgress(current, positionMs, durationMs)

                val next = session.next()
                if (next == null) {
                    _state.update {
                        it.copy(
                            runtime = it.runtime.copy(
                                transportState = PlayerTransportState.ENDED,
                                isPlaying = false,
                                hasNext = false,
                            )
                        )
                    }
                    return@launch
                }

                _state.value = createState(next)
            } finally {
                advancing = false
            }
        }
    }

    fun retry() {
        _state.update { current ->
            current.copy(
                runtime = current.runtime.copy(
                    transportState = PlayerTransportState.IDLE,
                    errorMessage = null,
                    isPlaying = false,
                ),
                resumePositionMs = current.progress.positionMs,
                retryToken = current.retryToken + 1,
            )
        }
    }

    fun onPlaybackError(
        error: PlaybackException,
        positionMs: Long,
    ) {
        val sourceRelated = when (error.errorCode) {
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS,
            PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND,
            PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED,
            PlaybackException.ERROR_CODE_PARSING_MANIFEST_UNSUPPORTED -> true
            else -> false
        }

        if (sourceRelated) {
            val alternative = session.nextAlternative()
            if (alternative != null) {
                _state.update { current ->
                    current.copy(
                        item = alternative,
                        runtime = current.runtime.copy(
                            title = alternative.title,
                            contentType = alternative.contentType,
                            transportState = PlayerTransportState.IDLE,
                            isPlaying = false,
                            errorMessage = null,
                        ),
                        resumePositionMs = positionMs.coerceAtLeast(0L),
                        retryToken = current.retryToken + 1,
                    )
                }
                return
            }
        }

        onError(error.message ?: error.errorCodeName)
    }

    fun onError(message: String) {
        val withoutUrls = message.replace(
            Regex("https?://[^\\s]+", RegexOption.IGNORE_CASE),
            "[ukryty adres strumienia]",
        )
        val safeMessage = withoutUrls.replace(
            Regex(
                "(?i)(username|user|password|pass|token|api_key)=([^&\\s]+)",
            ),
            "$1=[ukryte]",
        )

        _state.update {
            it.copy(
                runtime = it.runtime.copy(
                    transportState = PlayerTransportState.ERROR,
                    isPlaying = false,
                    errorMessage = safeMessage,
                )
            )
        }
    }
}
