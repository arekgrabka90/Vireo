package app.vireo.ui.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vireo.data.ContentRepository
import app.vireo.data.NoSourceException
import app.vireo.domain.model.Category
import app.vireo.domain.model.ContentType
import app.vireo.domain.model.Title
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val ROWS = 10
private const val ROW_ITEMS = 40

data class RowData(val category: Category, val titles: List<Title>)

data class BrowseState(
    val categories: Map<ContentType, List<Category>> = emptyMap(),
    val rows: Map<ContentType, List<RowData>> = emptyMap(),
    val loading: Set<ContentType> = emptySet(),
    val errors: Map<ContentType, String> = emptyMap(),
    val noSource: Boolean = false,
)

@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val repo: ContentRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(BrowseState())
    val state: StateFlow<BrowseState> = _state.asStateFlow()

    private var seenVersion = repo.sourceVersion

    fun ensure(type: ContentType) {
        if (seenVersion != repo.sourceVersion) {
            seenVersion = repo.sourceVersion
            _state.value = BrowseState()
        }
        val st = _state.value
        if (type in st.loading || st.categories[type] != null) return
        load(type)
    }

    fun retry(type: ContentType) = load(type)

    private fun load(type: ContentType) {
        viewModelScope.launch {
            _state.update { st -> st.copy(loading = st.loading + type, errors = st.errors - type) }
            runCatching {
                val cats = repo.categories(type).getOrThrow()
                val rows: List<RowData> = if (type == ContentType.LIVE) {
                    emptyList()
                } else {
                    coroutineScope {
                        cats.take(ROWS)
                            .map { c ->
                                async {
                                    val items = repo.titles(type, c.id).getOrDefault(emptyList())
                                    RowData(c, items.take(ROW_ITEMS))
                                }
                            }
                            .awaitAll()
                            .filter { r -> r.titles.isNotEmpty() }
                    }
                }
                cats to rows
            }.onSuccess { (cats, rows) ->
                _state.update { st ->
                    st.copy(
                        loading = st.loading - type,
                        noSource = false,
                        categories = st.categories + (type to cats),
                        rows = st.rows + (type to rows),
                    )
                }
            }.onFailure { e ->
                _state.update { st ->
                    if (e is NoSourceException) {
                        st.copy(loading = st.loading - type, noSource = true)
                    } else {
                        st.copy(
                            loading = st.loading - type,
                            errors = st.errors + (type to (e.message ?: "Error")),
                        )
                    }
                }
            }
        }
    }
}
