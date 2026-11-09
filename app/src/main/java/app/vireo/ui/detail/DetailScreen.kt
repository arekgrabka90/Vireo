package app.vireo.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vireo.R
import app.vireo.domain.model.Episode
import app.vireo.ui.components.FocusableBox
import coil.compose.AsyncImage

@Composable
fun DetailScreen(
    onBack: () -> Unit,
    onPlay: () -> Unit,
    vm: DetailViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    val movie = state.movie
    val series = state.series
    val title = movie?.name ?: series?.name ?: vm.name
    val image = movie?.backdropUrl ?: movie?.posterUrl ?: series?.backdropUrl ?: series?.posterUrl
    val rating = movie?.rating ?: series?.rating
    val meta = listOfNotNull(
        movie?.year ?: series?.year,
        movie?.genre ?: series?.genre,
        movie?.duration,
        rating?.takeIf { it.isNotBlank() && it != "0" }?.let { "★ $it" },
    ).joinToString("  •  ")
    val plot = movie?.plot ?: series?.plot
    val cast = movie?.cast ?: series?.cast
    val director = movie?.director
    val error = state.error
    val episodes = series?.seasons?.firstOrNull { it.number == state.season }?.episodes.orEmpty()

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.background),
        contentPadding = PaddingValues(start = 32.dp,top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 24.dp,end = 32.dp,bottom = 24.dp,),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            FocusableBox(onClick = onBack) {
                Text(stringResource(R.string.back), color = colors.onSurface)
            }
        }
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(colors.surfaceVariant),
            ) {
                if (!image.isNullOrBlank()) {
                    AsyncImage(
                        model = image,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.TopCenter,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, colors.background.copy(alpha = 0.95f)),
                            ),
                        ),
                )
                Text(
                    title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.onBackground,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.align(Alignment.BottomStart).padding(20.dp),
                )
            }
        }
        if (state.loading) item { CircularProgressIndicator() }
        if (error != null) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(error, color = colors.error)
                    FocusableBox(onClick = vm::load) {
                        Text(stringResource(R.string.retry), color = colors.onSurface)
                    }
                }
            }
        }
        if (meta.isNotBlank()) {
            item { Text(meta, style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant) }
        }
        if (movie != null) {
            item {
                Button(onClick = {
                    vm.playMovie()
                    onPlay()
                }) {
                    Text(stringResource(R.string.play))
                }
            }
        }
        if (!plot.isNullOrBlank()) {
            item { Text(plot, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface) }
        }
        if (!cast.isNullOrBlank()) {
            item {
                Text(
                    stringResource(R.string.cast, cast),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
        if (!director.isNullOrBlank()) {
            item {
                Text(
                    stringResource(R.string.director, director),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }
        if (series != null && series.seasons.isNotEmpty()) {
            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(series.seasons, key = { it.number }) { s ->
                        FocusableBox(
                            onClick = { vm.selectSeason(s.number) },
                            selected = s.number == state.season,
                        ) {
                            Text(stringResource(R.string.season, s.number), color = colors.onSurface)
                        }
                    }
                }
            }
            items(episodes, key = { it.id }) { ep ->
                EpisodeRow(ep = ep, onClick = {
                    vm.playEpisode(state.season, ep.id)
                    onPlay()
                })
            }
        }
    }
}

@Composable
private fun EpisodeRow(ep: Episode, onClick: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    FocusableBox(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(112.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.background),
            ) {
                if (!ep.thumbUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ep.thumbUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "${ep.number}. ${ep.title}",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!ep.duration.isNullOrBlank()) {
                    Text(ep.duration, style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                }
                if (!ep.plot.isNullOrBlank()) {
                    Text(
                        ep.plot,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
