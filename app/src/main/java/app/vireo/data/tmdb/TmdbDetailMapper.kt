package app.vireo.data.tmdb

import app.vireo.domain.model.ContentType

internal object TmdbDetailMapper {
    fun map(
        dto: TmdbDetailDto,
        type: ContentType,
        images: TmdbImagesDto? = null,
    ): TmdbDetail? {
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
            imdbId = dto.externalIds?.imdbId?.trim()
                ?.takeIf { Regex("(?i)^tt[0-9]{5,12}$").matches(it) }
                ?.lowercase(),
            seasons = dto.seasons
                .asSequence()
                .filter { it.seasonNumber >= 0 }
                .map {
                    TmdbSeasonSummary(
                        seasonNumber = it.seasonNumber,
                        name = it.name?.trim()?.takeIf(String::isNotBlank),
                        episodeCount = it.episodeCount.coerceAtLeast(0),
                        posterUrl = imageUrl(it.posterPath, "w342"),
                    )
                }
                .distinctBy { it.seasonNumber }
                .sortedBy { it.seasonNumber }
                .toList(),
            logoUrl = selectLogoUrl(images),
        )
    }

    private fun selectLogoUrl(images: TmdbImagesDto?): String? {
        val logo = images?.logos.orEmpty()
            .asSequence()
            .filter { !it.filePath.isNullOrBlank() }
            .filterNot { it.filePath!!.endsWith(".svg", ignoreCase = true) }
            .sortedWith(
                compareByDescending<TmdbLogoDto> { it.iso6391 == "en" }
                    .thenByDescending { it.voteAverage }
                    .thenByDescending { it.width }
            )
            .firstOrNull()

        return imageUrl(logo?.filePath, "w500")
    }

    fun mapEpisode(dto: TmdbEpisodeDto): TmdbEpisodeItem? {
        if (dto.seasonNumber < 0 || dto.episodeNumber <= 0) return null
        val title = dto.name?.trim()?.takeIf { it.isNotBlank() }
            ?: "Odcinek ${dto.episodeNumber}"
        return TmdbEpisodeItem(
            tmdbEpisodeId = dto.id,
            seasonNumber = dto.seasonNumber,
            episodeNumber = dto.episodeNumber,
            title = title,
            overview = dto.overview?.trim()?.takeIf { it.isNotBlank() },
            stillUrl = imageUrl(dto.stillPath, "w780"),
            runtimeMinutes = dto.runtime?.takeIf { it > 0 },
            airDate = dto.airDate?.trim()?.takeIf { it.isNotBlank() },
        )
    }

    private fun imageUrl(path: String?, size: String): String? {
        val value = path?.trim()?.takeIf { it.isNotBlank() } ?: return null
        if (value.startsWith("https://") || value.startsWith("http://")) return value
        return "https://image.tmdb.org/t/p/$size/${value.trimStart('/')}"
    }
}
