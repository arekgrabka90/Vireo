package app.vireo.ui.search

import app.vireo.data.tmdb.TmdbDiscoveryFilters

internal fun TmdbDiscoveryFilters.withYear(value: Int?): TmdbDiscoveryFilters =
    copy(year = value, decade = null)

internal fun TmdbDiscoveryFilters.withDecade(value: Int?): TmdbDiscoveryFilters =
    copy(decade = value, year = null)

internal fun TmdbDiscoveryFilters.withGenres(values: List<Int>): TmdbDiscoveryFilters =
    copy(genreIds = values.distinct().sorted())

internal fun TmdbDiscoveryFilters.withOriginalLanguage(value: String?): TmdbDiscoveryFilters =
    copy(originalLanguage = value?.trim()?.lowercase()?.takeIf { it.isNotBlank() })
