package app.vireo.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vireo.R
import app.vireo.domain.model.Category
import app.vireo.domain.model.ContentType
import app.vireo.domain.model.Title
import app.vireo.ui.browse.ErrorBlock
import app.vireo.ui.browse.Hero
import app.vireo.ui.browse.RowData
import app.vireo.ui.browse.TitleRow
import app.vireo.ui.components.Wordmark
import app.vireo.ui.components.PosterCard
import app.vireo.ui.library.LibraryViewModel
import app.vireo.ui.library.tmdbTarget
import kotlinx.coroutines.delay

private fun formatRemainingDuration(durationMs: Long): String {
    val totalMinutes = ((durationMs + 59_999L) / 60_000L)
        .coerceAtLeast(0L)
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L

    return when {
        hours > 0L && minutes > 0L -> "$hours godz. $minutes min"
        hours > 0L -> "$hours godz."
        else -> "$minutes min"
    }
}

@Composable
fun HomeScreen(
    onOpenTitle: (ContentType, Int, String) -> Unit,
    onOpenCatalog: (ContentType) -> Unit,
    vm: TmdbHomeViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val libraryVm: LibraryViewModel = hiltViewModel()
    val libraryState by libraryVm.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val horizontalPadding = when {
        screenWidth >= 1200 -> 64.dp
        screenWidth >= 840 -> 48.dp
        screenWidth >= 600 -> 32.dp
        else -> 20.dp
    }

    val movieTitles = state.movies.map { it.toHomeTitle() }
    val seriesTitles = state.series.map { it.toHomeTitle() }

    val myListItems = libraryState.watchlist.mapNotNull { item ->
        val target = item.tmdbTarget() ?: return@mapNotNull null
        target to Title(
            id = item.contentKey,
            name = item.title,
            posterUrl = item.posterUrl,
            rating = null,
            type = target.type,
            categoryId = null,
        )
    }
    val allTrending = (state.movies + state.series)
        .distinctBy { "${it.type.name}:${it.tmdbId}" }

    val heroItems = allTrending
        .filter { !it.backdropUrl.isNullOrBlank() }
        .sortedByDescending { it.popularity }
        .take(5)
        .ifEmpty {
            allTrending
                .filter { !it.posterUrl.isNullOrBlank() }
                .sortedByDescending { it.popularity }
                .take(5)
        }

    val heroKeys = heroItems.map { "${it.type.name}:${it.tmdbId}" }
    val heroIndex = remember(heroKeys) { mutableIntStateOf(0) }

    LaunchedEffect(heroKeys) {
        if (heroKeys.size > 1) {
            while (true) {
                delay(10_000L)
                heroIndex.intValue =
                    (heroIndex.intValue + 1) % heroKeys.size
            }
        }
    }

    val heroItem = heroItems.getOrNull(heroIndex.intValue)
    val hero = heroItem?.toHomeTitle()

    fun open(title: Title) {
        val id = title.id.toIntOrNull() ?: return
        onOpenTitle(title.type, id, title.name)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.background),
        contentPadding = PaddingValues(
            vertical = if (screenWidth < 600) 16.dp else 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = horizontalPadding),
            ) {
                Wordmark()
            }
        }

        if (state.loading && movieTitles.isEmpty() && seriesTitles.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding),
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        if (state.error != null && movieTitles.isEmpty() && seriesTitles.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding),
                ) {
                    ErrorBlock(state.error ?: "TMDB error", vm::refresh)
                }
            }
        }

        if (hero != null && heroItem != null) {
            item(key = "home-cinematic-hero") {
                Crossfade(
                    targetState = heroIndex.intValue,
                    animationSpec = tween(durationMillis = 1000),
                    label = "home-hero-crossfade",
                ) { index ->
                    val activeItem = heroItems.getOrNull(index)
                    if (activeItem != null) {
                        val activeTitle = activeItem.toHomeTitle()
                        Hero(
                            title = activeTitle,
                            backdropUrl = activeItem.backdropUrl,
                            overview = activeItem.overview,
                            position = index + 1,
                            total = heroItems.size,
                            onClick = { open(activeTitle) },
                        )
                    }
                }
            }
        }

        val resumable = libraryState.continueWatching.mapNotNull { item ->
            val target = item.tmdbTarget() ?: return@mapNotNull null
            Triple(item, target, Title(
                id = item.contentKey,
                name = item.title,
                posterUrl = item.posterUrl,
                rating = null,
                type = target.type,
                categoryId = null,
            ))
        }

        if (resumable.isNotEmpty()) {
            item(key = "continue-watching") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Kontynuuj oglądanie",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onBackground,
                        modifier = Modifier.padding(horizontal = horizontalPadding),
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(
                            start = horizontalPadding + 6.dp,
                            end = horizontalPadding + 20.dp,
                            top = 8.dp,
                            bottom = 14.dp,
                        ),
                    ) {
                        items(resumable, key = { it.first.contentKey }) { entry ->
                            val item = entry.first
                            val progress = if (item.durationMs > 0L) {
                                (item.positionMs.toFloat() / item.durationMs.toFloat())
                                    .coerceIn(0f, 1f)
                            } else 0f

                            val remainingMs = (item.durationMs - item.positionMs)
                                .coerceAtLeast(0L)
                            val remainingText = if (item.durationMs > 0L) {
                                "Pozostało ${formatRemainingDuration(remainingMs)}"
                            } else {
                                null
                            }

                            PosterCard(
                                title = entry.third,
                                topProgress = progress,
                                belowPosterText = remainingText,
                                onClick = {
                                    onOpenTitle(
                                        entry.second.type,
                                        entry.second.id,
                                        item.title,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }

        if (myListItems.isNotEmpty()) {
            item(key = "my-list") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Moja lista",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onBackground,
                        modifier = Modifier.padding(horizontal = horizontalPadding),
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(
                            start = horizontalPadding + 6.dp,
                            end = horizontalPadding + 20.dp,
                            top = 8.dp,
                            bottom = 14.dp,
                        ),
                    ) {
                        items(
                            myListItems,
                            key = { it.first.id.toString() },
                        ) { entry ->
                            PosterCard(
                                title = entry.second,
                                onClick = {
                                    onOpenTitle(
                                        entry.first.type,
                                        entry.first.id,
                                        entry.second.name,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }

        if (movieTitles.isNotEmpty()) {
            item {
                TitleRow(
                    row = RowData(
                        Category("tmdb-trending-movies", "Na czasie — Filmy", ContentType.MOVIE),
                        movieTitles.take(20),
                    ),
                    horizontalPadding = horizontalPadding,
                    onSeeAll = { onOpenCatalog(ContentType.MOVIE) },
                    onTitle = ::open,
                )
            }
        }

        if (seriesTitles.isNotEmpty()) {
            item {
                TitleRow(
                    row = RowData(
                        Category("tmdb-trending-series", "Na czasie — Seriale", ContentType.SERIES),
                        seriesTitles.take(20),
                    ),
                    horizontalPadding = horizontalPadding,
                    onSeeAll = { onOpenCatalog(ContentType.SERIES) },
                    onTitle = ::open,
                )
            }
        }

        if (!state.loading && movieTitles.isEmpty() && seriesTitles.isEmpty()) {
            item {
                Text(
                    state.error ?: stringResource(R.string.empty_titles),
                    color = colors.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding),
                )
            }
        }
    }
}
