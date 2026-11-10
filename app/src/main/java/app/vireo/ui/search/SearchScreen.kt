package app.vireo.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vireo.R
import app.vireo.ui.components.FocusableBox
import app.vireo.ui.components.PosterCard
import app.vireo.ui.home.toHomeTitle
import app.vireo.ui.library.LibraryViewModel

@Composable
fun SearchScreen(
    onOpenTitle: (app.vireo.domain.model.ContentType, Int, String) -> Unit,
    vm: SearchViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val libraryVm: LibraryViewModel = hiltViewModel()
    val libraryState by libraryVm.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            stringResource(R.string.nav_search),
            style = MaterialTheme.typography.headlineMedium,
            color = colors.onBackground,
        )

        OutlinedTextField(
            value = state.query,
            onValueChange = vm::onQueryChanged,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Filmy, seriale i osoby") },
            placeholder = { Text("Wpisz tytuł lub nazwisko...") },
        )

        if (state.recent.isNotEmpty() && state.query.isBlank()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Ostatnie wyszukiwania",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.onSurface,
                    modifier = Modifier.weight(1f),
                )
                FocusableBox(onClick = vm::clearHistory) {
                    Text("Wyczyść", color = colors.primary)
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

        if (state.loading) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        state.error?.let { Text(it, color = colors.error) }

        if (state.people.isNotEmpty()) {
            Text(
                "Osoby",
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.people, key = { "person_${it.tmdbId}" }) { person ->
                    Column(
                        modifier = Modifier
                            .background(
                                colors.surfaceVariant,
                                MaterialTheme.shapes.medium,
                            )
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            person.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.onSurface,
                        )

                        if (person.knownFor.isNotEmpty()) {
                            Text(
                                "Znane produkcje",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.onSurfaceVariant,
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

        if (state.results.isNotEmpty()) {
            Text(
                "${state.results.size} wyników",
                style = MaterialTheme.typography.labelLarge,
                color = colors.onSurfaceVariant,
            )

            LazyVerticalGrid(
                columns = GridCells.Adaptive(140.dp),
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentPadding = PaddingValues(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(
                    state.results,
                    key = { "${it.type.name}_${it.tmdbId}" },
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
        } else if (
            !state.loading &&
            state.query.isNotBlank() &&
            state.error == null
        ) {
            Spacer(Modifier.weight(1f))
        }
    }
}
