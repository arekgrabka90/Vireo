package app.vireo.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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

@Composable
fun HomeScreen(
    onOpenTitle: (ContentType, Int, String) -> Unit,
    onOpenCatalog: (ContentType) -> Unit,
    vm: TmdbHomeViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme

    val movieTitles = state.movies.map { it.toHomeTitle() }
    val seriesTitles = state.series.map { it.toHomeTitle() }
    val heroItem = state.movies.firstOrNull { !it.backdropUrl.isNullOrBlank() }
        ?: state.series.firstOrNull { !it.backdropUrl.isNullOrBlank() }
        ?: state.movies.firstOrNull { !it.posterUrl.isNullOrBlank() }
        ?: state.series.firstOrNull { !it.posterUrl.isNullOrBlank() }
    val hero = heroItem?.toHomeTitle()

    fun open(title: Title) {
        val id = title.id.toIntOrNull() ?: return
        onOpenTitle(title.type, id, title.name)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.background),
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item { Wordmark() }

        if (state.loading && movieTitles.isEmpty() && seriesTitles.isEmpty()) {
            item { CircularProgressIndicator() }
        }

        if (state.error != null && movieTitles.isEmpty() && seriesTitles.isEmpty()) {
            item {
                ErrorBlock(state.error ?: "TMDB error", vm::refresh)
            }
        }

        if (hero != null) {
            item {
                Hero(
                    title = hero,
                    backdropUrl = heroItem?.backdropUrl,
                    overview = heroItem?.overview,
                )
            }
        }

        if (movieTitles.isNotEmpty()) {
            item {
                TitleRow(
                    row = RowData(
                        Category("tmdb-trending-movies", "Na czasie — Filmy", ContentType.MOVIE),
                        movieTitles.take(20),
                    ),
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
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
