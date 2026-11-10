package app.vireo.data.tmdb

import app.vireo.domain.model.ContentType

internal object TmdbMultiSearchMapper {

    fun map(dto: TmdbMultiSearchResultDto): TmdbMultiSearchMapped? {
        return when (dto.mediaType.lowercase()) {
            "movie" -> {
                val item = TmdbCatalogMapper.map(dto.toCatalogDto(), ContentType.MOVIE)
                    ?: return null
                TmdbMultiSearchMapped.Media(item)
            }

            "tv" -> {
                val item = TmdbCatalogMapper.map(dto.toCatalogDto(), ContentType.SERIES)
                    ?: return null
                TmdbMultiSearchMapped.Media(item)
            }

            "person" -> {
                if (dto.id <= 0) return null
                val name = dto.name?.trim()?.takeIf { it.isNotBlank() }
                    ?: return null

                val knownFor = dto.knownFor.mapNotNull { known ->
                    val type = when (known.mediaType.lowercase()) {
                        "movie" -> ContentType.MOVIE
                        "tv" -> ContentType.SERIES
                        else -> return@mapNotNull null
                    }
                    TmdbCatalogMapper.map(known.toCatalogDto(), type)
                }.distinctBy { "${it.type.name}:${it.tmdbId}" }
                    .take(MAX_KNOWN_FOR)

                TmdbMultiSearchMapped.Person(
                    TmdbPersonSearchItem(
                        tmdbId = dto.id,
                        name = name,
                        profileUrl = imageUrl(dto.profilePath, "w185"),
                        popularity = dto.popularity,
                        knownFor = knownFor,
                    )
                )
            }

            else -> null
        }
    }

    private fun TmdbMultiSearchResultDto.toCatalogDto() = TmdbCatalogDto(
        id = id,
        title = title,
        name = name,
        originalTitle = originalTitle,
        originalName = originalName,
        overview = overview,
        releaseDate = releaseDate,
        firstAirDate = firstAirDate,
        posterPath = posterPath,
        backdropPath = backdropPath,
        voteAverage = voteAverage,
        voteCount = voteCount,
        genreIds = genreIds,
        originalLanguage = originalLanguage,
        popularity = popularity,
    )

    private fun imageUrl(path: String?, size: String): String? {
        val value = path?.trim()?.takeIf { it.isNotBlank() } ?: return null
        if (value.startsWith("https://") || value.startsWith("http://")) return value
        return "https://image.tmdb.org/t/p/$size/${value.trimStart('/')}"
    }

    private const val MAX_KNOWN_FOR = 5
}

internal sealed interface TmdbMultiSearchMapped {
    data class Media(val item: TmdbCatalogItem) : TmdbMultiSearchMapped
    data class Person(val item: TmdbPersonSearchItem) : TmdbMultiSearchMapped
}
