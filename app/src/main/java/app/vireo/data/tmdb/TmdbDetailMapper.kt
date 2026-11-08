package app.vireo.data.tmdb

import app.vireo.domain.model.ContentType

internal object TmdbDetailMapper {
    fun map(dto: TmdbDetailDto, type: ContentType): TmdbDetail? {
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

        return TmdbDetail(
            tmdbId = dto.id,
            type = type,
            title = title,
            originalTitle = originalTitle?.takeIf { it.isNotBlank() },
            overview = dto.overview?.takeIf { it.isNotBlank() },
            releaseDate = releaseDate,
            year = releaseDate?.take(4)?.toIntOrNull(),
            posterUrl = imageUrl(dto.posterPath, "w500"),
            backdropUrl = imageUrl(dto.backdropPath, "w1280"),
            rating = dto.voteAverage.takeIf { it > 0.0 },
            voteCount = dto.voteCount,
            genres = dto.genres.map { it.name.trim() }.filter { it.isNotBlank() }.distinct(),
            runtimeMinutes = when (type) {
                ContentType.MOVIE -> dto.runtime
                ContentType.SERIES -> dto.episodeRunTime.firstOrNull()
                ContentType.LIVE -> null
            }?.takeIf { it > 0 },
            cast = dto.credits?.cast.orEmpty()
                .asSequence()
                .filter { it.id > 0 && it.name.isNotBlank() }
                .take(20)
                .map {
                    TmdbCastMember(
                        id = it.id,
                        name = it.name.trim(),
                        character = it.character.trim(),
                        profileUrl = imageUrl(it.profilePath, "w185"),
                    )
                }
                .toList(),
        )
    }

    private fun imageUrl(path: String?, size: String): String? {
        val value = path?.trim()?.takeIf { it.isNotBlank() } ?: return null
        if (value.startsWith("https://") || value.startsWith("http://")) return value
        return "https://image.tmdb.org/t/p/$size/${value.trimStart('/')}"
    }
}
