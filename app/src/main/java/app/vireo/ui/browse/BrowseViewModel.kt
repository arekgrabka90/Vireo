package app.vireo.ui.browse

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vireo.data.ContentRepository
import app.vireo.data.NoSourceException
import app.vireo.data.StaleSourceException
import app.vireo.domain.model.Category
import app.vireo.domain.model.ContentType
import app.vireo.domain.model.Title
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
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

        val current = _state.value
        if (type in current.loading || current.categories[type] != null) return

        load(type)
    }

    fun retry(type: ContentType) = load(type)

    private fun load(type: ContentType) {
        if (type in _state.value.loading) return

        val requestVersion = repo.sourceVersion

        _state.update { current ->
            current.copy(
                loading = current.loading + type,
                errors = current.errors - type,
            )
        }

        viewModelScope.launch {
            try {
                val categories = repo.categories(type).getOrThrow()

                val rows: List<RowData> =
                    if (type == ContentType.LIVE) {
                        emptyList()
                    } else {
                        coroutineScope {
                            categories.take(ROWS)
                                .map { category ->
                                    async {
                                        val titles = repo.titles(
                                            type,
                                            category.id,
                                        ).getOrThrow()

                                        RowData(
                                            category,
                                            titles.take(ROW_ITEMS),
                                        )
                                    }
                                }
                                .awaitAll()
                                .filter { it.titles.isNotEmpty() }
                        }
                    }

                if (requestVersion != repo.sourceVersion) {
                    resetAndReload(type)
                    return@launch
                }

                _state.update { current ->
                    current.copy(
                        loading = current.loading - type,
                        noSource = false,
                        categories = current.categories +
                            (type to categories),
                        rows = current.rows + (type to rows),
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (
                    error is StaleSourceException ||
                    requestVersion != repo.sourceVersion
                ) {
                    resetAndReload(type)
                    return@launch
                }

                _state.update { current ->
                    if (error is NoSourceException) {
                        current.copy(
                            loading = current.loading - type,
                            noSource = true,
                        )
                    } else {
                        current.copy(
                            loading = current.loading - type,
                            errors = current.errors +
                                (type to (error.message ?: "Error")),
                        )
                    }
                }
            }
        }
    }

    private fun resetAndReload(type: ContentType) {
        seenVersion = repo.sourceVersion
        _state.value = BrowseState()
        ensure(type)
    }
}
