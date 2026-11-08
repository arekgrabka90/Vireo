package app.vireo.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import app.vireo.domain.model.ContentType
import app.vireo.ui.components.FocusableBox
import app.vireo.ui.components.PosterCard
import app.vireo.ui.home.toHomeTitle

@Composable
fun TmdbCatalogScreen(
    onBack: () -> Unit,
    onOpenTitle: (ContentType, Int, String) -> Unit,
    vm: TmdbCatalogViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    val title = if (vm.type == ContentType.SERIES) "Na czasie — Seriale"
        else "Na czasie — Filmy"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 24.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            FocusableBox(onClick = onBack) {
                Text(stringResource(R.string.back), color = colors.onSurface)
            }
            Text(title, style = MaterialTheme.typography.headlineSmall, color = colors.onBackground)
        }

        if (state.loading) CircularProgressIndicator()

        state.error?.let { Text(it, color = colors.error) }

        if (state.items.isNotEmpty()) {
            LazyVerticalGrid(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                columns = GridCells.Adaptive(148.dp),
                contentPadding = PaddingValues(
                    horizontal = 6.dp,
                    vertical = 8.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                items(
                    state.items,
                    key = { "${it.type.name}_${it.tmdbId}" },
                ) { item ->
                    val mapped = item.toHomeTitle()
                    PosterCard(title = mapped, onClick = {
                        onOpenTitle(item.type, item.tmdbId, item.title)
                    })
                }
            }
        }

        if (state.page < state.totalPages && state.page > 0) {
            Button(
                onClick = vm::loadNextPage,
                enabled = !state.loadingMore,
            ) {
                if (state.loadingMore) CircularProgressIndicator()
                else Text("Wczytaj kolejne")
            }
        }

        Spacer(Modifier.height(4.dp))
    }
}
