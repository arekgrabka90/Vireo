package app.vireo.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import app.vireo.player.engine.Media3PlayerEngine
import app.vireo.player.source.PlaybackSource
import app.vireo.data.ContentRepository
import app.vireo.data.LibraryRepository
import app.vireo.data.EpgRepository
import app.vireo.domain.model.EpgNowNext
import app.vireo.data.ProfileRepository
import app.vireo.data.ProfileSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import app.vireo.di.ApplicationScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class PlayerUiState(
    val item: PlayItem? = null,
    val runtime: PlayerRuntimeState = PlayerRuntimeState(),
    val resumePositionMs: Long? = null,
    val desiredPlayWhenReady: Boolean = true,
    val retryToken: Int = 0,
) {
    val hasNext: Boolean
        get() = runtime.hasNext

    val hasPrevious: Boolean
        get() = runtime.hasPrevious

    val error: String?
        get() = runtime.errorMessage
}

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val session: PlaybackSession,
    private val media3PlayerEngine: Media3PlayerEngine,
    private val contentRepository: ContentRepository,
    private val library: LibraryRepository,
    private val profiles: ProfileRepository,
    private val epgRepository: EpgRepository,
    private val profileSettingsRepository: ProfileSettingsRepository,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : ViewModel() {

    // Profil jest przypisany do całej sesji odtwarzania.
    private val playbackProfileId = profiles.activeId
    val playbackSettings = profileSettingsRepository.observe(playbackProfileId)

    private val _playbackSettingsReady = MutableStateFlow(false)
    val playbackSettingsReady: StateFlow<Boolean> =
        _playbackSettingsReady.asStateFlow()

    private var settingsInitializationJob: Job? = null
    private var playbackResolutionJob: Job? = null
    private var resolvingPlayback = false
    private var advancing = false
    private var handlingPlaybackError = false
    private var recoveryAttempts = 0
    private var recoveryItem: PlayItem? = null
    private val playbackOperationGuard = PlaybackOperationGuard()

    private fun invalidatePlaybackOperation() {
        playbackOperationGuard.invalidate()
        playbackResolutionJob?.cancel()
        playbackResolutionJob = null
        resolvingPlayback = false
        handlingPlaybackError = false
    }

    fun autoplayNextEnabled(): Boolean = playbackSettings.value.autoplayNext

    private fun createState(
        item: PlayItem?,
        resumePositionMs: Long? = null,
    ) = PlayerUiState(
        item = item,
        runtime = PlayerRuntimeState(
            title = item?.title.orEmpty(),
            contentType = item?.contentType,
            hasPrevious = session.hasPrevious(),
            hasNext = session.hasNext(),
        ),
        resumePositionMs = resumePositionMs,
    )

    private val _state = MutableStateFlow(createState(session.current()))
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    private val _progress = MutableStateFlow(PlayerProgressState())
    val progress: StateFlow<PlayerProgressState> = _progress.asStateFlow()

    private val _epgState = MutableStateFlow(EpgNowNext(null, null))
    val epgState: StateFlow<EpgNowNext> = _epgState.asStateFlow()
    private var epgJob: Job? = null

    init {
        initializePlaybackSettings()
    }

    private fun initializePlaybackSettings() {
        if (_playbackSettingsReady.value ||
            settingsInitializationJob?.isActive == true
        ) return

        settingsInitializationJob = viewModelScope.launch {
            try {
                val profile = requireNotNull(profiles.get(playbackProfileId)) {
                    "Nie można odnaleźć profilu przypisanego do sesji odtwarzania."
                }

                profileSettingsRepository.ensure(
                    profileId = profile.id,
                    kidsMode = profile.kids,
                )

                _playbackSettingsReady.value = true
                observeEpg(session.current())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                onError(
                    error.message
                        ?: "Nie udało się wczytać ustawień profilu."
                )
            }
        }
    }

    private fun observeEpg(item: PlayItem?) {
        epgJob?.cancel()
        _epgState.value = EpgNowNext(null, null)

        if (item?.contentType != "LIVE") return
        val streamId = item.streamId?.takeIf { it.isNotBlank() } ?: return

        epgJob = viewModelScope.launch {
            while (true) {
                epgRepository.nowNext(
                    streamId = streamId,
                    refreshIntervalMinutes = playbackSettings.value.epgRefreshIntervalMinutes,
                ).onSuccess {
                    _epgState.value = it
                }
                delay(30_000L)
            }
        }
    }

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

        applicationScope.launch {
            runCatching {
                persistProgress(target, positionMs, durationMs)
            }
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
        _progress.value = progress
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
                    hasPrevious = session.hasPrevious(),
                    hasNext = session.hasNext(),
                )
            )
        }
    }

    fun onPlayWhenReadyChanged(
        playWhenReady: Boolean,
        reason: Int,
    ) {
        if (resolvingPlayback) return

        if (
            reason == Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST ||
            reason == Player.PLAY_WHEN_READY_CHANGE_REASON_REMOTE
        ) {
            _state.update {
                it.copy(desiredPlayWhenReady = playWhenReady)
            }
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
        invalidatePlaybackOperation()

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
                                hasPrevious = session.hasPrevious(),
                                hasNext = false,
                            )
                        )
                    }
                    return@launch
                }

                _progress.value = PlayerProgressState()
                _state.value = createState(next)
                observeEpg(next)
            } finally {
                advancing = false
            }
        }
    }

    fun previous(positionMs: Long, durationMs: Long) {
        if (advancing) return

        val current = session.current() ?: return
        advancing = true
        invalidatePlaybackOperation()

        viewModelScope.launch {
            try {
                persistProgress(current, positionMs, durationMs)

                val previous = session.previous()
                if (previous == null) {
                    _state.update {
                        it.copy(
                            runtime = it.runtime.copy(
                                hasPrevious = session.hasPrevious(),
                                hasNext = session.hasNext(),
                            )
                        )
                    }
                    return@launch
                }

                _progress.value = PlayerProgressState()
                _state.value = createState(previous)
                observeEpg(previous)
            } finally {
                advancing = false
            }
        }
    }

    fun retry() {
        recoveryAttempts = 0
        handlingPlaybackError = false

        if (!_playbackSettingsReady.value) {
            _state.update { current ->
                current.copy(
                    runtime = current.runtime.copy(
                        transportState = PlayerTransportState.IDLE,
                        errorMessage = null,
                        isPlaying = false,
                    )
                )
            }
            initializePlaybackSettings()
            return
        }

        _state.update { current ->
            current.copy(
                runtime = current.runtime.copy(
                    transportState = PlayerTransportState.IDLE,
                    errorMessage = null,
                    isPlaying = false,
                ),
                resumePositionMs = _progress.value.positionMs,
                retryToken = current.retryToken + 1,
            )
        }
    }

    fun onPlaybackError(
        error: PlaybackException,
        positionMs: Long,
    ) {
        // Ignore callbacks while another playback operation is active.
        if (
            resolvingPlayback ||
            advancing ||
            handlingPlaybackError ||
            _state.value.item == null
        ) return

        handlingPlaybackError = true

        val action = PlaybackRecoveryPolicy.classify(error.errorCode)
        val safePosition = positionMs.coerceAtLeast(0L)

        if (
            action == PlaybackRecoveryAction.RETRY_CURRENT &&
            recoveryAttempts < PlaybackRecoveryPolicy.MAX_CURRENT_SOURCE_RETRIES
        ) {
            recoveryAttempts++
            _progress.value = _progress.value.copy(
                positionMs = safePosition,
            )

            _state.update { current ->
                current.copy(
                    runtime = current.runtime.copy(
                        transportState = PlayerTransportState.IDLE,
                        isPlaying = false,
                        errorMessage = null,
                    ),
                    resumePositionMs = safePosition,
                    retryToken = current.retryToken + 1,
                )
            }
            return
        }

        if (
            action == PlaybackRecoveryAction.FAILOVER ||
            action == PlaybackRecoveryAction.RETRY_CURRENT
        ) {
            val alternative = session.nextAlternative()

            if (alternative != null) {
                _progress.value = _progress.value.copy(
                    positionMs = safePosition,
                )
                observeEpg(alternative)

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
                        resumePositionMs = safePosition,
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

    internal fun playerForView(): Player =
        media3PlayerEngine.playerForView()

    fun startPlayback(item: PlayItem, positionMs: Long) {
        if (recoveryItem != item) {
            recoveryItem = item
            recoveryAttempts = 0
        }

        val operation = playbackOperationGuard.begin()
        playbackResolutionJob?.cancel()
        handlingPlaybackError = false
        resolvingPlayback = true

        // Preserve the user's play/pause intent while resolving a fresh URL.
        media3PlayerEngine.pause()

        playbackResolutionJob = viewModelScope.launch {
            try {
                val source = contentRepository.resolvePlaybackSource(item)

                if (
                    !playbackOperationGuard.isCurrent(operation) ||
                    _state.value.item != item
                ) return@launch

                val startPosition = if (item.contentType == "LIVE") {
                    0L
                } else {
                    positionMs.coerceAtLeast(0L)
                }

                media3PlayerEngine.setSource(source, startPosition)
                media3PlayerEngine.prepare()

                if (!playbackOperationGuard.isCurrent(operation)) {
                    return@launch
                }

                val shouldPlay = _state.value.desiredPlayWhenReady

                if (shouldPlay) {
                    media3PlayerEngine.play()
                } else {
                    media3PlayerEngine.pause()
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (
                    playbackOperationGuard.isCurrent(operation) &&
                    _state.value.item == item
                ) {
                    onError(error.message ?: "Playback failed.")
                }
            } finally {
                if (playbackOperationGuard.isCurrent(operation)) {
                    resolvingPlayback = false
                    playbackResolutionJob = null
                }
            }
        }
    }

    fun pausePlayback() {
        _state.update { it.copy(desiredPlayWhenReady = false) }
        media3PlayerEngine.pause()
    }

    fun resumePlayback() {
        _state.update { it.copy(desiredPlayWhenReady = true) }
        media3PlayerEngine.play()
    }

    override fun onCleared() {
        epgJob?.cancel()
        invalidatePlaybackOperation()
        media3PlayerEngine.release()
        super.onCleared()
    }

}
