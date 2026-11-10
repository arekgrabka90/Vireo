package app.vireo.ui.list

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vireo.data.ContentRepository
import app.vireo.domain.model.ContentType
import app.vireo.domain.model.Title
import app.vireo.player.PlayItem
import app.vireo.player.PlaybackSession
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TitleListState(
    val loading: Boolean = true,
    val error: String? = null,
    val titles: List<Title> = emptyList(),
)

@HiltViewModel
class TitleListViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: ContentRepository,
    private val session: PlaybackSession,
) : ViewModel() {

    val type: ContentType = ContentType.valueOf(savedStateHandle.get<String>("type") ?: "MOVIE")
    private val categoryId: String = savedStateHandle.get<String>("categoryId").orEmpty()
    val categoryName: String = savedStateHandle.get<String>("name").orEmpty()

    private val _state = MutableStateFlow(TitleListState())
    val state: StateFlow<TitleListState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.update { st -> st.copy(loading = true, error = null) }
            repo.titles(type, categoryId)
                .onSuccess { list -> _state.update { st -> st.copy(loading = false, titles = list) } }
                .onFailure { e -> _state.update { st -> st.copy(loading = false, error = e.message ?: "Error") } }
        }
    }

    fun playLive(title: Title, onReady: () -> Unit) {
        viewModelScope.launch {
            val url = repo.liveUrl(title.id) ?: return@launch
            session.start(
                listOf(
                    PlayItem(
                        title = title.name,
                        url = url,
                        contentType = ContentType.LIVE.name,
                        epgChannelId = title.epgChannelId,
                    )
                ),
                0,
            )
            onReady()
        }
    }
}
