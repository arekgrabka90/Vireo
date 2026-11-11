package app.vireo.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vireo.ui.components.FocusableBox
import coil.compose.AsyncImage

@Composable
internal fun SeriesHudContent(
    content: PlayerHudContent.Series,
    wide: Boolean,
    isPlaying: Boolean,
    onEpisodeSelected: (Int, Int) -> Unit,
) {
    val seasons = remember(content.episodes) {
        content.episodes
            .map { it.seasonNumber }
            .distinct()
            .sorted()
    }

    var selectedSeason by remember(
        content.currentSeasonNumber,
        seasons,
    ) {
        mutableIntStateOf(
            content.currentSeasonNumber
                ?: seasons.firstOrNull()
                ?: -1
        )
    }

    val visibleSeason = selectedSeason.takeIf { it in seasons }
        ?: content.currentSeasonNumber?.takeIf { it in seasons }
        ?: seasons.firstOrNull()

    val visibleEpisodes = content.episodes.filter {
        it.seasonNumber == visibleSeason
    }

    val metadata = listOfNotNull(
        content.year?.takeIf { it.isNotBlank() },
        content.genre?.takeIf { it.isNotBlank() },
        content.rating?.takeIf { it.isNotBlank() }?.let { "★ $it" },
    )

    Column(
        modifier = Modifier.fillMaxWidth(if (wide) 0.76f else 0.96f),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (metadata.isNotEmpty()) {
            Text(
                metadata.joinToString("   ·   "),
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        val episodeTitle = content.currentEpisodeTitle
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        if (
            episodeTitle != null ||
            content.currentSeasonNumber != null ||
            content.currentEpisodeNumber != null
        ) {
            val label = buildString {
                content.currentSeasonNumber?.let {
                    append("S${it.toString().padStart(2, '0')}")
                }
                content.currentEpisodeNumber?.let {
                    if (isNotEmpty()) append("  ·  ")
                    append("E${it.toString().padStart(2, '0')}")
                }
                episodeTitle?.let {
                    if (isNotEmpty()) append("  ·  ")
                    append(it)
                }
            }

            Text(
                label,
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        content.currentEpisodeDescription
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { description ->
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    if (content.descriptionIsSeriesFallback) {
                        Text(
                            "OPIS SERIALU",
                            color = Color.White.copy(alpha = 0.58f),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                        )
                    }

                    Text(
                        description,
                        color = Color.White.copy(alpha = 0.84f),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = when {
                            isPlaying -> 1
                            wide -> 3
                            else -> 2
                        },
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

        if (seasons.isNotEmpty()) {
            Text(
                "SEZONY",
                color = Color.White.copy(alpha = 0.58f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(seasons, key = { it }) { season ->
                    FocusableBox(
                        onClick = { selectedSeason = season },
                        selected = season == visibleSeason,
                    ) {
                        Text(
                            "Sezon $season",
                            color = Color.White,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }

        if (visibleEpisodes.isNotEmpty()) {
            Text(
                "ODCINKI",
                color = Color.White.copy(alpha = 0.58f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
            )

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(
                    items = visibleEpisodes,
                    key = { "S${it.seasonNumber}E${it.episodeNumber}" },
                ) { episode ->
                    val current =
                        episode.seasonNumber == content.currentSeasonNumber &&
                            episode.episodeNumber == content.currentEpisodeNumber

                    SeriesEpisodeCard(
                        episode = episode,
                        current = current,
                        wide = wide,
                        onClick = {
                            onEpisodeSelected(
                                episode.seasonNumber,
                                episode.episodeNumber,
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun SeriesEpisodeCard(
    episode: PlaybackEpisode,
    current: Boolean,
    wide: Boolean,
    onClick: () -> Unit,
) {
    val cardWidth = if (wide) 154.dp else 128.dp
    val cardHeight = if (wide) 132.dp else 120.dp

    if (episode.isSelectable) {
        FocusableBox(
            onClick = onClick,
            selected = current,
            modifier = Modifier
                .width(cardWidth)
                .height(cardHeight),
        ) {
            SeriesEpisodeCardBody(
                episode = episode,
                current = current,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    } else {
        Surface(
            modifier = Modifier
                .width(cardWidth)
                .height(cardHeight),
            color = Color.White.copy(alpha = 0.08f),
            shape = MaterialTheme.shapes.medium,
        ) {
            SeriesEpisodeCardBody(
                episode = episode,
                current = current,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
            )
        }
    }
}

@Composable
private fun SeriesEpisodeCardBody(
    episode: PlaybackEpisode,
    current: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val thumbnail = episode.thumbnailUrl
            ?.trim()
            ?.takeIf { it.startsWith("https://") || it.startsWith("http://") }

        if (thumbnail != null) {
            AsyncImage(
                model = thumbnail,
                contentDescription = episode.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f),
                contentScale = ContentScale.Fit,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(Color.Black.copy(alpha = 0.38f)),
            )
        }

        Text(
            "S${episode.seasonNumber.toString().padStart(2, '0')} · " +
                "E${episode.episodeNumber.toString().padStart(2, '0')}",
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )

        episode.title.trim().takeIf { it.isNotBlank() }?.let {
            Text(
                it,
                color = Color.White.copy(alpha = 0.88f),
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        val stateLabel = when {
            current -> "TERAZ"
            episode.isWatched -> "OBEJRZANY"
            !episode.isSelectable -> "POZA KOLEJKĄ"
            else -> null
        }

        stateLabel?.let {
            Text(
                it,
                color = if (current) Color.White else Color.White.copy(alpha = 0.62f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
        }
    }
}
