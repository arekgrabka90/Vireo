package app.vireo.ui.more

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vireo.R
import app.vireo.ui.components.FocusableBox
import app.vireo.ui.components.ScreenTitle

private data class MoreItem(val route: String, val label: Int, val icon: String)

private val MoreItems = listOf(
    MoreItem("search", R.string.nav_search, "🔍"),
    MoreItem("guide", R.string.nav_guide, "🗓️"),
    MoreItem("mylist", R.string.nav_mylist, "⭐"),
    MoreItem("recordings", R.string.nav_recordings, "⏺️"),
    MoreItem("settings", R.string.nav_settings, "⚙️"),
)

@Composable
fun MoreScreen(onNavigate: (String) -> Unit) {
    val colors = MaterialTheme.colorScheme
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.background),
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenTitle(stringResource(R.string.nav_more)) }
        items(MoreItems, key = { it.route }) { m ->
            FocusableBox(onClick = { onNavigate(m.route) }, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(m.icon, fontSize = 22.sp)
                    Text(stringResource(m.label), color = colors.onSurface)
                }
            }
        }
    }
}
