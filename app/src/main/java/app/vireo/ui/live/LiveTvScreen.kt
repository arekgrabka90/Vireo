package app.vireo.ui.live

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vireo.R
import app.vireo.domain.model.Category
import app.vireo.domain.model.ContentType
import app.vireo.ui.browse.BrowseViewModel
import app.vireo.ui.browse.ErrorBlock
import app.vireo.ui.browse.NoSourceCard
import app.vireo.ui.components.FocusableBox
import app.vireo.ui.components.ScreenTitle

@Composable
fun LiveTvScreen(
    onOpenCategory: (ContentType, Category) -> Unit,
    onOpenSources: () -> Unit,
    vm: BrowseViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { vm.ensure(ContentType.LIVE) }
    val colors = MaterialTheme.colorScheme
    val cats = state.categories[ContentType.LIVE].orEmpty()
    val error = state.errors[ContentType.LIVE]

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.background),
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenTitle(stringResource(R.string.nav_live)) }
        when {
            state.noSource -> item { NoSourceCard(onOpenSources) }
            ContentType.LIVE in state.loading -> item { CircularProgressIndicator() }
            error != null -> item { ErrorBlock(error) { vm.retry(ContentType.LIVE) } }
            cats.isEmpty() -> item {
                Text(stringResource(R.string.empty), color = colors.onSurfaceVariant)
            }
            else -> items(cats, key = { it.id }) { c ->
                FocusableBox(
                    onClick = { onOpenCategory(ContentType.LIVE, c) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(c.name, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
