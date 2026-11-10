package app.vireo.ui.search

import app.vireo.data.tmdb.TmdbMultiSearchResult

internal fun mergeSearchResults(
    current: TmdbMultiSearchResult,
    next: TmdbMultiSearchResult,
): TmdbMultiSearchResult = TmdbMultiSearchResult(
    media = (current.media + next.media)
        .distinctBy { "${it.type.name}:${it.tmdbId}" },
    people = (current.people + next.people)
        .distinctBy { it.tmdbId }
        .sortedByDescending { it.popularity },
    page = next.page,
    totalPages = next.totalPages,
    totalResults = next.totalResults,
)
