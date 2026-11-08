package app.vireo.data.tmdb

import app.vireo.domain.model.ContentType

internal object TmdbCatalogMapper {

    fun map(
        dto: TmdbCatalogDto,
        type: ContentType,
    ): TmdbCatalogItem? {
        if (dto.id <= 0) return null
        if (type != ContentType.MOVIE && type != ContentType.SERIES) return null

        val title = when (type) {
            ContentType.MOVIE -> dto.title ?: dto.originalTitle
            ContentType.SERIES -> dto.name ?: dto.originalName
            ContentType.LIVE -> null
        }?.trim()?.takeIf { it.isNotBlank() } ?: return null

        val originalTitle = when (type) {
            ContentType.MOVIE -> dto.originalTitle
            ContentType.SERIES -> dto.originalName
            ContentType.LIVE -> null
        }

        val releaseDate = when (type) {
            ContentType.MOVIE -> dto.releaseDate
            ContentType.SERIES -> dto.firstAirDate
            ContentType.LIVE -> null
        }?.takeIf { it.isNotBlank() }

        return TmdbCatalogItem(
            tmdbId = dto.id,
            type = type,
            title = title,
            originalTitle = originalTitle?.takeIf { it.isNotBlank() },
            releaseDate = releaseDate,
            year = releaseDate?.take(4)?.toIntOrNull(),
            overview = dto.overview?.takeIf { it.isNotBlank() },
            posterUrl = imageUrl(dto.posterPath, "w500"),
            backdropUrl = imageUrl(dto.backdropPath, "w780"),
            rating = dto.voteAverage.takeIf { it > 0.0 },
            voteCount = dto.voteCount,
            genreIds = dto.genreIds,
            originalLanguage = dto.originalLanguage,
            popularity = dto.popularity,
        )
    }

    private fun imageUrl(path: String?, size: String): String? {
        val value = path?.trim()?.takeIf { it.isNotBlank() } ?: return null
        if (value.startsWith("https://") || value.startsWith("http://")) {
            return value
        }
        return "https://image.tmdb.org/t/p/$size/${value.trimStart('/')}"
    }
}
