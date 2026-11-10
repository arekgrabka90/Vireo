package app.vireo.ui.library

import app.vireo.data.local.LibraryItemEntity

/**
 * Applies profile visibility preferences without modifying persisted library
 * records. Watched markers for catalog/search items remain independent from
 * the My List visibility preference.
 */
internal fun applyLibraryVisibilityPolicy(
    watchlist: List<LibraryItemEntity>,
    continueWatching: List<LibraryItemEntity>,
    allItems: List<LibraryItemEntity>,
    showWatched: Boolean,
    continueWatchingEnabled: Boolean,
): LibraryUiState = LibraryUiState(
    watchlist = if (showWatched) {
        watchlist
    } else {
        watchlist.filterNot { it.watched }
    },
    continueWatching = if (continueWatchingEnabled) {
        continueWatching
    } else {
        emptyList()
    },
    watchedContentKeys = allItems
        .asSequence()
        .filter { it.watched }
        .map { it.contentKey }
        .toSet(),
)
