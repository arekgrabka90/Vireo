package app.vireo.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import app.vireo.domain.model.Title
import app.vireo.ui.components.PosterCard

@Composable
fun LibraryScreen(
    onOpenTitle: (ContentType, Int, String) -> Unit,
    vm: LibraryViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier.fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            stringResource(R.string.nav_mylist),
            style = MaterialTheme.typography.headlineMedium,
            color = colors.onBackground,
        )

        if (state.watchlist.isEmpty()) {
            Text(
                "Twoja lista jest jeszcze pusta. Dodaj filmy lub seriale z ekranu szczegółów.",
                color = colors.onSurfaceVariant,
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(state.watchlist, key = { it.contentKey }) { item ->
                    val target = item.tmdbTarget()
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        PosterCard(
                            title = Title(
                                id = item.contentKey,
                                name = item.title,
                                posterUrl = item.posterUrl,
                                rating = null,
                                type = target?.type ?: ContentType.MOVIE,
                                categoryId = null,
                            ),
                            onClick = {
                                if (target != null) {
                                    onOpenTitle(target.type, target.id, item.title)
                                }
                            },
                        )

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                item.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.onSurface,
                            )
                            Text(
                                if (item.watched) "Obejrzane" else "Na Twojej liście",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.onSurfaceVariant,
                            )
                            TextButton(onClick = { vm.markWatched(item) }) {
                                Text("Oznacz jako obejrzane")
                            }
                            TextButton(onClick = { vm.removeFromMyList(item) }) {
                                Text("Usuń z listy")
                            }
                        }
                    }
                }
            }
        }
    }
}
