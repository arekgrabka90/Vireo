package app.vireo.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import app.vireo.ui.browse.BrowseViewModel
import app.vireo.ui.browse.ErrorBlock
import app.vireo.ui.browse.Hero
import app.vireo.ui.browse.NoSourceCard
import app.vireo.ui.browse.SectionPlaceholder
import app.vireo.ui.browse.TitleRow
import app.vireo.ui.components.Wordmark

@Composable
fun HomeScreen(
    onOpenCategory: (ContentType, Category) -> Unit,
    onOpenTitle: (Title) -> Unit,
    onOpenSources: () -> Unit,
    vm: BrowseViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) {
        vm.ensure(ContentType.MOVIE)
        vm.ensure(ContentType.SERIES)
    }
    val colors = MaterialTheme.colorScheme
    val movieRows = state.rows[ContentType.MOVIE].orEmpty()
    val seriesRows = state.rows[ContentType.SERIES].orEmpty()
    val hero = movieRows.firstNotNullOfOrNull { r -> r.titles.firstOrNull { !it.posterUrl.isNullOrBlank() } }
        ?: seriesRows.firstNotNullOfOrNull { r -> r.titles.firstOrNull { !it.posterUrl.isNullOrBlank() } }
    val nothingYet = movieRows.isEmpty() && seriesRows.isEmpty()
    val error = state.errors[ContentType.MOVIE] ?: state.errors[ContentType.SERIES]

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.background),
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Wordmark() }
        when {
            state.noSource -> item { NoSourceCard(onOpenSources) }
            nothingYet && state.loading.isNotEmpty() -> item { CircularProgressIndicator() }
            nothingYet && error != null -> item {
                ErrorBlock(error) {
                    vm.retry(ContentType.MOVIE)
                    vm.retry(ContentType.SERIES)
                }
            }
            else -> {
                if (hero != null) item { Hero(hero) }
                item {
                    SectionPlaceholder(
                        stringResource(R.string.continue_watching),
                        stringResource(R.string.continue_hint),
                    )
                }
                items(movieRows.take(3), key = { "m_" + it.category.id }) { row ->
                    TitleRow(
                        row = row,
                        onSeeAll = { onOpenCategory(ContentType.MOVIE, row.category) },
                        onTitle = onOpenTitle,
                    )
                }
                items(seriesRows.take(3), key = { "s_" + it.category.id }) { row ->
                    TitleRow(
                        row = row,
                        onSeeAll = { onOpenCategory(ContentType.SERIES, row.category) },
                        onTitle = onOpenTitle,
                    )
                }
            }
        }
    }
}
