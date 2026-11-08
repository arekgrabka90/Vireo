package app.vireo.ui.list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vireo.R
import app.vireo.domain.model.ContentType
import app.vireo.domain.model.Title
import app.vireo.ui.components.FocusableBox
import app.vireo.ui.components.PosterCard

@Composable
fun TitleListScreen(
    onBack: () -> Unit,
    onOpenTitle: (Title) -> Unit,
    onPlay: () -> Unit,
    vm: TitleListViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    val error = state.error

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 32.dp, vertical = 24.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            FocusableBox(onClick = onBack) {
                Text(stringResource(R.string.back), color = colors.onSurface)
            }
            Text(
                vm.categoryName,
                style = MaterialTheme.typography.headlineSmall,
                color = colors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Spacer(Modifier.height(16.dp))
        when {
            state.loading -> CircularProgressIndicator()
            error != null -> Text(error, color = colors.error)
            state.titles.isEmpty() -> Text(
                stringResource(R.string.empty_titles),
                color = colors.onSurfaceVariant,
            )
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(if (vm.type == ContentType.LIVE) 176.dp else 148.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(state.titles, key = { it.id }) { t ->
                    PosterCard(title = t, onClick = { if (t.type == ContentType.LIVE) vm.playLive(t, onPlay) else onOpenTitle(t) })
                }
            }
        }
    }
}
