package app.vireo.ui.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import app.vireo.ui.components.ScreenTitle

@Composable
fun BrowseScreen(
    type: ContentType,
    onOpenCategory: (ContentType, Category) -> Unit,
    onOpenTitle: (Title) -> Unit,
    onOpenSources: () -> Unit,
    vm: BrowseViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(type) { vm.ensure(type) }
    val colors = MaterialTheme.colorScheme
    val rows = state.rows[type].orEmpty()
    val hero = rows.firstNotNullOfOrNull { r -> r.titles.firstOrNull { !it.posterUrl.isNullOrBlank() } }
    val error = state.errors[type]

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.background),
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { ScreenTitle(stringResource(type.navLabel())) }
        when {
            state.noSource -> item { NoSourceCard(onOpenSources) }
            type in state.loading -> item { CircularProgressIndicator() }
            error != null -> item { ErrorBlock(error) { vm.retry(type) } }
            rows.isEmpty() -> item {
                Text(stringResource(R.string.empty), color = colors.onSurfaceVariant)
            }
            else -> {
                if (hero != null) item { Hero(hero) }
                items(rows, key = { it.category.id }) { row ->
                    TitleRow(
                        row = row,
                        onSeeAll = { onOpenCategory(type, row.category) },
                        onTitle = onOpenTitle,
                    )
                }
            }
        }
    }
}
