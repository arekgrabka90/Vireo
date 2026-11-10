package app.vireo.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.KeyboardType
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vireo.R
import app.vireo.data.tmdb.TmdbDiscoveryCategory
import app.vireo.data.tmdb.TmdbDiscoverySort
import app.vireo.data.tmdb.TmdbGenreMatch
import app.vireo.domain.model.ContentType
import app.vireo.ui.components.FocusableBox
import app.vireo.ui.components.PosterCard
import app.vireo.ui.home.toHomeTitle
import app.vireo.ui.library.LibraryViewModel
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun DiscoveryScreen(
    onOpenTitle: (ContentType, Int, String) -> Unit,
    vm: DiscoveryViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val libraryVm: LibraryViewModel = hiltViewModel()
    val libraryState by libraryVm.state.collectAsStateWithLifecycle()
    val gridState = rememberLazyGridState()
    val colors = MaterialTheme.colorScheme

    LaunchedEffect(
        state.items.size,
        state.loadingMore,
        state.page,
        state.totalPages,
    ) {
        snapshotFlow {
            val info = gridState.layoutInfo
            info.visibleItemsInfo.lastOrNull()?.index ?: -1
        }.distinctUntilChanged().collect { last ->
            val total = gridState.layoutInfo.totalItemsCount
            if (total > 0 && last >= total - 5) {
                vm.loadNextPage()
            }
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(145.dp),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            horizontal = 22.dp,
            vertical = 18.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            Text(
                stringResource(R.string.discovery_heading),
                style = MaterialTheme.typography.headlineMedium,
                color = colors.onBackground,
            )
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SelectChip(
                    stringResource(R.string.discovery_movies),
                    state.type == ContentType.MOVIE,
                ) { vm.selectType(ContentType.MOVIE) }
                SelectChip(
                    stringResource(R.string.discovery_series),
                    state.type == ContentType.SERIES,
                ) { vm.selectType(ContentType.SERIES) }
            }
        }

        item(span = { GridItemSpan(maxLineSpan) }) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(discoveryCategories) { category ->
                    SelectChip(
                        stringResource(category.label),
                        state.category == category.value,
                    ) { vm.selectCategory(category.value) }
                }
            }
        }

        if (state.category == TmdbDiscoveryCategory.CUSTOM) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                DiscoveryFilterPanel(
                    state = state,
                    onGenres = vm::setGenres,
                    onGenreMatch = vm::setGenreMatch,
                    onRating = vm::setMinimumRating,
                    onVotes = vm::setMinimumVotes,
                    onYear = vm::setYear,
                    onDecade = vm::setDecade,
                    onLanguage = vm::setOriginalLanguage,
                    onSort = vm::setSort,
                    onClear = vm::clearFilters,
                )
            }
        }

        if (state.loading && state.items.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        state.error?.let { error ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(error, color = colors.error)
                    FocusableBox(onClick = vm::retry) {
                        Text(
                            stringResource(R.string.discovery_retry),
                            color = colors.onSurface,
                        )
                    }
                }
            }
        }

        items(
            items = state.items,
            key = { "${it.type.name}:${it.tmdbId}" },
        ) { item ->
            PosterCard(
                title = item.toHomeTitle(),
                isWatched = "tmdb:${item.type.name}:${item.tmdbId}" in
                    libraryState.watchedContentKeys,
                belowPosterText = item.year?.toString(),
                onClick = {
                    onOpenTitle(item.type, item.tmdbId, item.title)
                },
            )
        }

        if (state.loadingMore) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(18.dp),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

private data class DiscoveryCategoryOption(
    val value: TmdbDiscoveryCategory,
    val label: Int,
)

private val discoveryCategories = listOf(
    DiscoveryCategoryOption(TmdbDiscoveryCategory.TRENDING_WEEK, R.string.discovery_trending),
    DiscoveryCategoryOption(TmdbDiscoveryCategory.POPULAR_THIS_YEAR, R.string.discovery_popular_year),
    DiscoveryCategoryOption(TmdbDiscoveryCategory.TOP_RATED, R.string.discovery_top_rated),
    DiscoveryCategoryOption(TmdbDiscoveryCategory.NEW_RELEASES, R.string.discovery_new_releases),
    DiscoveryCategoryOption(TmdbDiscoveryCategory.HIDDEN_GEMS, R.string.discovery_hidden_gems),
    DiscoveryCategoryOption(TmdbDiscoveryCategory.CUSTOM, R.string.discovery_custom),
)

@Composable
private fun SelectChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FocusableBox(onClick = onClick, selected = selected) {
        Text(
            label,
            color = if (selected) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun DiscoveryFilterPanel(
    state: DiscoveryState,
    onGenres: (List<Int>) -> Unit,
    onGenreMatch: (TmdbGenreMatch) -> Unit,
    onRating: (Double?) -> Unit,
    onVotes: (Int?) -> Unit,
    onYear: (Int?) -> Unit,
    onDecade: (Int?) -> Unit,
    onLanguage: (String?) -> Unit,
    onSort: (TmdbDiscoverySort) -> Unit,
    onClear: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val filters = state.filters

    var yearText by remember(filters.year) {
        mutableStateOf(filters.year?.toString().orEmpty())
    }
    var decadeText by remember(filters.decade) {
        mutableStateOf(filters.decade?.toString().orEmpty())
    }
    var languageText by remember(filters.originalLanguage) {
        mutableStateOf(filters.originalLanguage.orEmpty())
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.discovery_filters),
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
                modifier = Modifier.weight(1f),
            )
            FocusableBox(onClick = onClear) {
                Text(
                    stringResource(R.string.discovery_clear),
                    color = colors.primary,
                )
            }
        }

        Text(stringResource(R.string.discovery_genres), color = colors.onSurface)
        if (state.genresLoading) {
            CircularProgressIndicator()
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                items(state.genres, key = { it.id }) { genre ->
                    SelectChip(
                        genre.name,
                        genre.id in filters.genreIds,
                    ) {
                        val updated = filters.genreIds.toMutableSet()
                        if (!updated.add(genre.id)) updated.remove(genre.id)
                        onGenres(updated.toList())
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SelectChip(
                stringResource(R.string.discovery_any_genre),
                filters.genreMatch == TmdbGenreMatch.ANY,
            ) { onGenreMatch(TmdbGenreMatch.ANY) }
            SelectChip(
                stringResource(R.string.discovery_all_genres),
                filters.genreMatch == TmdbGenreMatch.ALL,
            ) { onGenreMatch(TmdbGenreMatch.ALL) }
        }

        Text(stringResource(R.string.discovery_min_rating), color = colors.onSurface)
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf(null, 6.0, 7.0, 8.0, 9.0).forEach { value ->
                SelectChip(
                    value?.let { "${it.toInt()}+" }
                        ?: stringResource(R.string.discovery_any),
                    filters.minRating == value,
                ) { onRating(value) }
            }
        }

        Text(stringResource(R.string.discovery_min_votes), color = colors.onSurface)
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf(null, 100, 500, 1000).forEach { value ->
                SelectChip(
                    value?.toString()
                        ?: stringResource(R.string.discovery_any),
                    filters.minVoteCount == value,
                ) { onVotes(value) }
            }
        }

        Text(stringResource(R.string.discovery_year_or_decade), color = colors.onSurface)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = yearText,
                onValueChange = {
                    yearText = it.filter(Char::isDigit).take(4)
                    val parsed = yearText.toIntOrNull()
                    if (yearText.isBlank() || parsed in 1870..2200) {
                        onYear(parsed)
                    }
                },
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(R.string.discovery_year)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            OutlinedTextField(
                value = decadeText,
                onValueChange = {
                    decadeText = it.filter(Char::isDigit).take(4)
                    val parsed = decadeText.toIntOrNull()
                    if (decadeText.isBlank() ||
                        (parsed != null && parsed in 1870..2190 && parsed % 10 == 0)
                    ) {
                        onDecade(parsed)
                    }
                },
                modifier = Modifier.weight(1f),
                label = { Text(stringResource(R.string.discovery_decade)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        }

        OutlinedTextField(
            value = languageText,
            onValueChange = {
                languageText = it.filter(Char::isLetter).take(2).lowercase()
                if (languageText.isBlank() || languageText.length == 2) {
                    onLanguage(languageText.ifBlank { null })
                }
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text(stringResource(R.string.discovery_original_language)) },
            placeholder = { Text("en, ja, ko") },
            singleLine = true,
        )

        Text(stringResource(R.string.discovery_sort), color = colors.onSurface)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            val sorts = listOf(
                TmdbDiscoverySort.POPULARITY_DESC to R.string.discovery_sort_popular,
                TmdbDiscoverySort.RATING_DESC to R.string.discovery_sort_rating,
                TmdbDiscoverySort.VOTE_COUNT_DESC to R.string.discovery_sort_votes,
                TmdbDiscoverySort.RELEASE_DATE_DESC to R.string.discovery_sort_date,
                TmdbDiscoverySort.POPULARITY_ASC to R.string.discovery_sort_hidden,
            )
            sorts.forEach { (sort, label) ->
                SelectChip(
                    stringResource(label),
                    filters.sortBy == sort,
                ) { onSort(sort) }
            }
        }
    }
}
