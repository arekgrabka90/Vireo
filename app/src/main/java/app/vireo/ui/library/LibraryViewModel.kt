package app.vireo.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.vireo.data.LibraryRepository
import app.vireo.data.ProfileRepository
import app.vireo.data.ProfileSettingsRepository
import app.vireo.data.local.LibraryItemEntity
import app.vireo.domain.model.ContentType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val watchlist: List<LibraryItemEntity> = emptyList(),
    val continueWatching: List<LibraryItemEntity> = emptyList(),
    val watchedContentKeys: Set<String> = emptySet(),
)

data class TmdbLibraryTarget(
    val type: ContentType,
    val id: Int,
)

fun LibraryItemEntity.tmdbTarget(): TmdbLibraryTarget? {
    val parts = contentKey.split(':')
    if (parts.size < 3 || parts[0] != "tmdb") return null

    val type = runCatching { ContentType.valueOf(parts[1]) }.getOrNull()
        ?: return null
    if (type == ContentType.LIVE) return null

    val id = parts[2].toIntOrNull() ?: return null
    return TmdbLibraryTarget(type, id)
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val library: LibraryRepository,
    private val profiles: ProfileRepository,
    private val profileSettingsRepository: ProfileSettingsRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LibraryUiState())
    val state: StateFlow<LibraryUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            profiles.activeIdFlow
                .flatMapLatest { profileId ->
                    if (profileId <= 0L) {
                        flowOf(LibraryUiState())
                    } else {
                        flow {
                            // Never leave the previous profile's data visible
                            // while the new profile settings are initialized.
                            emit(LibraryUiState())

                            val profile = profiles.get(profileId)
                                ?: return@flow

                            profileSettingsRepository.ensure(
                                profileId = profile.id,
                                kidsMode = profile.kids,
                            )

                            emitAll(
                                combine(
                                    library.observeWatchlist(profileId),
                                    library.observeContinueWatching(profileId),
                                    library.observeAll(profileId),
                                    profileSettingsRepository.observe(profileId),
                                ) { watchlist, continueWatching, allItems, settings ->
                                    applyLibraryVisibilityPolicy(
                                        watchlist = watchlist,
                                        continueWatching = continueWatching,
                                        allItems = allItems,
                                        showWatched = settings.showWatched,
                                        continueWatchingEnabled =
                                            settings.continueWatchingEnabled,
                                    )
                                }
                            )
                        }
                    }
                }
                .collect { _state.value = it }
        }
    }

    fun removeFromMyList(item: LibraryItemEntity) {
        val profileId = profiles.activeId
        viewModelScope.launch {
            library.setWatchlisted(
                profileId = profileId,
                contentKey = item.contentKey,
                title = item.title,
                contentType = item.contentType,
                posterUrl = item.posterUrl,
                backdropUrl = item.backdropUrl,
                enabled = false,
            )
        }
    }

    fun removeFromContinueWatching(item: LibraryItemEntity) {
        val profileId = profiles.activeId

        viewModelScope.launch {
            library.clearProgress(
                profileId = profileId,
                contentKey = item.contentKey,
            )
        }
    }

    fun markWatched(item: LibraryItemEntity) {
        val profileId = profiles.activeId
        viewModelScope.launch {
            library.setWatched(
                profileId = profileId,
                contentKey = item.contentKey,
                watched = !item.watched,
                title = item.title,
                contentType = item.contentType,
                posterUrl = item.posterUrl,
                backdropUrl = item.backdropUrl,
            )
        }
    }
}
