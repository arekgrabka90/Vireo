package app.vireo.ui.search

import androidx.compose.foundation.shape.CircleShape

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vireo.R
import app.vireo.domain.model.ContentType
import app.vireo.ui.components.FocusableBox
import app.vireo.ui.components.PosterCard
import app.vireo.ui.home.toHomeTitle
import app.vireo.ui.library.LibraryViewModel
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
fun SearchScreen(
    onOpenTitle: (ContentType, Int, String) -> Unit,
    vm: SearchViewModel = hiltViewModel(),
) {
    var section by rememberSaveable { mutableStateOf(false) }
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier.fillMaxSize().padding(top = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            stringResource(R.string.nav_search),
            style = MaterialTheme.typography.headlineMedium,
            color = colors.onBackground,
            modifier = Modifier.padding(horizontal = 22.dp),
        )

        Row(
            modifier = Modifier.padding(horizontal = 22.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            FocusableBox(onClick = { section = false }, selected = !section) {
                Text(stringResource(R.string.search_tab_search), color = colors.onSurface)
            }
            FocusableBox(onClick = { section = true }, selected = section) {
                Text(stringResource(R.string.search_tab_discover), color = colors.onSurface)
            }
        }

        if (section) {
            DiscoveryScreen(onOpenTitle = onOpenTitle)
        } else {
            SearchResultsContent(onOpenTitle = onOpenTitle, vm = vm)
        }
    }
}

@Composable
private fun SearchResultsContent(
    onOpenTitle: (ContentType, Int, String) -> Unit,
    vm: SearchViewModel,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val libraryVm: LibraryViewModel = hiltViewModel()
    val libraryState by libraryVm.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    val gridState = rememberLazyGridState()

    LaunchedEffect(
        state.results.size,
        state.people.size,
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
            vertical = 12.dp,
        ),
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(span = { GridItemSpan(maxLineSpan) }) {
            OutlinedTextField(
                value = state.query,
                onValueChange = vm::onQueryChanged,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text(stringResource(R.string.search_input_label)) },
                placeholder = { Text(stringResource(R.string.search_input_hint)) },
            )
        }

        if (state.recent.isNotEmpty() && state.query.isBlank()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stringResource(R.string.search_recent),
                            color = colors.onSurface,
                            modifier = Modifier.weight(1f),
                        )
                        FocusableBox(onClick = vm::clearHistory) {
                            Text(
                                stringResource(R.string.discovery_clear),
                                color = colors.primary,
                            )
                        }
                    }
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(state.recent) { query ->
                            FocusableBox(onClick = { vm.searchRecent(query) }) {
                                Text(query, color = colors.onSurface)
                            }
                        }
                    }
                }
            }
        }

        if (state.loading && state.results.isEmpty() && state.people.isEmpty()) {
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
                    Text(
                    stringResource(
                        when (error) {
                            SearchError.FIRST_PAGE -> R.string.search_error
                            SearchError.PAGE -> R.string.search_page_error
                            SearchError.EMPTY -> R.string.search_empty
                        },
                    ),
                    color = colors.error,
                )
                    FocusableBox(onClick = vm::retry) {
                        Text(
                            stringResource(R.string.search_retry),
                            color = colors.onSurface,
                        )
                    }
                }
            }
        }

        if (
            state.query.isNotBlank() &&
            !state.loading &&
            state.error == null &&
            state.results.isEmpty() &&
            state.people.isEmpty()
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    stringResource(R.string.search_empty),
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 18.dp),
                )
            }
        }

        if (state.people.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        stringResource(R.string.search_people),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onSurface,
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(
                            state.people,
                            key = { "person_${it.tmdbId}" },
                        ) { person ->
                            Column(
                                modifier = Modifier.width(112.dp).padding(4.dp),
                                verticalArrangement = Arrangement.spacedBy(7.dp),
                            ) {
                                if (person.profileUrl != null) {
                                    AsyncImage(
                                        model = person.profileUrl,
                                        contentDescription = person.name,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(132.dp)
                                            .clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Crop,
                                    )
                                } else {
                                    FocusableBox(
                                        onClick = {},
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(132.dp),
                                    ) {
                                        Text(
                                            person.name.take(1).uppercase(),
                                            color = colors.onSurface,
                                        )
                                    }
                                }

                                Text(
                                    person.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colors.onSurface,
                                )

                                person.knownFor.forEach { known ->
                                    FocusableBox(
                                        onClick = {
                                            onOpenTitle(
                                                known.type,
                                                known.tmdbId,
                                                known.title,
                                            )
                                        },
                                    ) {
                                        Text(
                                            known.title,
                                            color = colors.onSurface,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (state.results.isNotEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(
                    stringResource(R.string.search_results_count, state.totalResults),
                    color = colors.onSurfaceVariant,
                )
            }
        }

        items(
            state.results,
            key = { "${it.type.name}:${it.tmdbId}" },
        ) { item ->
            PosterCard(
                title = item.toHomeTitle(),
                isWatched = "tmdb:${item.type.name}:${item.tmdbId}" in
                    libraryState.watchedContentKeys,
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
