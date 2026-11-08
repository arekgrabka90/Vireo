package app.vireo.domain.model

data class MovieDetails(
    val id: String,
    val name: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val plot: String?,
    val genre: String?,
    val year: String?,
    val duration: String?,
    val cast: String?,
    val director: String?,
    val rating: String?,
    val streamUrl: String,
)

data class Episode(
    val id: String,
    val number: Int,
    val title: String,
    val plot: String?,
    val duration: String?,
    val thumbUrl: String?,
    val streamUrl: String,
)

data class Season(val number: Int, val episodes: List<Episode>)

data class SeriesDetails(
    val id: String,
    val name: String,
    val posterUrl: String?,
    val backdropUrl: String?,
    val plot: String?,
    val genre: String?,
    val year: String?,
    val cast: String?,
    val rating: String?,
    val seasons: List<Season>,
)
