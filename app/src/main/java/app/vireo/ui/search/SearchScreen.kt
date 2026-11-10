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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
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
            SearchResultsContent(
                onOpenTitle = onOpenTitle,
                vm = vm,
            )
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

    LazyVerticalGrid(
        columns = GridCells.Adaptive(145.dp),
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

        if (state.loading) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        state.error?.let { error ->
            item(span = { GridItemSpan(maxLineSpan) }) {
                Text(error, color = colors.error)
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
                                modifier = Modifier.padding(8.dp),
                                verticalArrangement = Arrangement.spacedBy(7.dp),
                            ) {
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
                    stringResource(R.string.search_results_count, state.results.size),
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
    }
}
