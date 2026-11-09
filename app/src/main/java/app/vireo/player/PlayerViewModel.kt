package app.vireo.player

import androidx.lifecycle.ViewModel
import androidx.media3.common.PlaybackException
import androidx.lifecycle.viewModelScope
import app.vireo.data.LibraryRepository
import app.vireo.data.ProfileRepository
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class PlayerUiState(
    val item: PlayItem? = null,
    val hasNext: Boolean = false,
    val error: String? = null,
    val resumePositionMs: Long? = null,
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val session: PlaybackSession,
    private val library: LibraryRepository,
    private val profiles: ProfileRepository,
) : ViewModel() {

    // Profil zostaje przypisany do calej sesji odtwarzania.
    // Zmiana aktywnego profilu nie przeniesie postepu do innego profilu.
    private val playbackProfileId = profiles.activeId
    private var advancing = false

    private val _state = MutableStateFlow(
        PlayerUiState(item = session.current(), hasNext = session.hasNext()),
    )
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    suspend fun resumePosition(item: PlayItem): Long {
        val contentKey = item.contentKey ?: return 0L
        return library.resumePosition(playbackProfileId, contentKey)
    }

    private suspend fun persistProgress(
        item: PlayItem?,
        positionMs: Long,
        durationMs: Long,
    ) {
        val target = item ?: return
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
        viewModelScope.launch {
            persistProgress(target, positionMs, durationMs)
        }
    }

    fun next(positionMs: Long, durationMs: Long) {
        if (advancing) return

        val current = session.current() ?: return
        advancing = true

        viewModelScope.launch {
            try {
                // Najpierw zapisujemy finalny postep. Dopiero potem
                // zmieniamy pozycje w kolejce odtwarzania.
                persistProgress(current, positionMs, durationMs)

                val next = session.next() ?: return@launch
                _state.value = PlayerUiState(
                    item = next,
                    hasNext = session.hasNext(),
                )
            } finally {
                advancing = false
            }
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
                _state.update {
                    it.copy(
                        item = alternative,
                        error = null,
                        resumePositionMs = positionMs.coerceAtLeast(0L),
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
        _state.update { st -> st.copy(error = safeMessage) }
    }
}
