package app.vireo.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import app.vireo.ui.components.FocusableBox

private fun ContentType.labelRes(): Int = when (this) {
    ContentType.MOVIE -> R.string.tab_movies
    ContentType.SERIES -> R.string.tab_series
    ContentType.LIVE -> R.string.tab_live
}

@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    vm: HomeViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 32.dp, vertical = 24.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Vireo",
                style = MaterialTheme.typography.headlineMedium,
                color = colors.primary,
                modifier = Modifier.weight(1f),
            )
            FocusableBox(onClick = { vm.logout(onLogout) }) {
                Text(stringResource(R.string.logout), color = colors.onSurface)
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(ContentType.MOVIE, ContentType.SERIES, ContentType.LIVE).forEach { type ->
                FocusableBox(onClick = { vm.selectTab(type) }, selected = state.tab == type) {
                    Text(stringResource(type.labelRes()), color = colors.onSurface)
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        val error = state.error
        when {
            state.loading -> CircularProgressIndicator()
            error != null -> Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(error, color = colors.error)
                FocusableBox(onClick = vm::load) {
                    Text(stringResource(R.string.retry), color = colors.onSurface)
                }
            }
            else -> {
                val categories = state.categories[state.tab].orEmpty()
                if (categories.isEmpty()) {
                    Text(stringResource(R.string.empty), color = colors.onSurfaceVariant)
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(220.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(categories, key = { it.id }) { category ->
                            FocusableBox(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                                Text(
                                    category.name,
                                    color = colors.onSurface,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
