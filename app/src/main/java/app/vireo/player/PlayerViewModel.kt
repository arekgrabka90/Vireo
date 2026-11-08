package app.vireo.player

import androidx.lifecycle.ViewModel
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
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val session: PlaybackSession,
) : ViewModel() {

    private val _state = MutableStateFlow(
        PlayerUiState(item = session.current(), hasNext = session.hasNext()),
    )
    val state: StateFlow<PlayerUiState> = _state.asStateFlow()

    fun next() {
        val n = session.next() ?: return
        _state.value = PlayerUiState(item = n, hasNext = session.hasNext())
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
