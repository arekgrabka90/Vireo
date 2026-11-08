package app.vireo.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import app.vireo.ui.components.FocusableBox
import app.vireo.ui.components.ProfileAvatar
import app.vireo.ui.components.ScreenTitle

private data class SettingsItem(val route: String, val title: Int, val desc: Int)

private val SettingsItems = listOf(
    SettingsItem("settings/sources", R.string.set_sources, R.string.set_sources_desc),
    SettingsItem("settings/playback", R.string.set_playback, R.string.set_playback_desc),
    SettingsItem("settings/appearance", R.string.set_appearance, R.string.set_appearance_desc),
    SettingsItem("settings/epg", R.string.set_epg, R.string.set_epg_desc),
    SettingsItem("settings/parental", R.string.set_parental, R.string.set_parental_desc),
    SettingsItem("settings/about", R.string.set_about, R.string.set_about_desc),
)

@Composable
fun SettingsScreen(
    onNavigate: (String) -> Unit,
    onSwitchProfile: () -> Unit,
    vm: SettingsViewModel = hiltViewModel(),
) {
    val active by vm.active.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(colors.background),
        contentPadding = PaddingValues(horizontal = 32.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { ScreenTitle(stringResource(R.string.nav_settings)) }
        item {
            val a = active
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (a != null) {
                    ProfileAvatar(a.name, a.color, 56.dp)
                    Text(
                        a.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.onBackground,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                FocusableBox(onClick = onSwitchProfile) {
                    Text(stringResource(R.string.switch_profile), color = colors.onSurface)
                }
            }
        }
        items(SettingsItems, key = { it.route }) { s ->
            FocusableBox(onClick = { onNavigate(s.route) }, modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        stringResource(s.title),
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.onSurface,
                    )
                    Text(
                        stringResource(s.desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
