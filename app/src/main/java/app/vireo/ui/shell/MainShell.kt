package app.vireo.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.vireo.R
import app.vireo.ui.components.FocusableBox
import app.vireo.domain.model.Category
import app.vireo.domain.model.ContentType
import app.vireo.domain.model.Title
import app.vireo.ui.components.PlaceholderScreen
import app.vireo.ui.home.HomeScreen
import app.vireo.ui.library.LibraryScreen
import app.vireo.ui.live.LiveTvScreen
import app.vireo.ui.more.MoreScreen
import app.vireo.ui.settings.SettingsScreen
import app.vireo.ui.settings.SourcesScreen
import app.vireo.ui.search.SearchScreen

private data class Dest(val route: String, val label: Int, val icon: String)

private val SearchDest = Dest("search", R.string.nav_search, "🔍")
private val HomeDest = Dest("home", R.string.nav_home, "🏠")
private val LiveDest = Dest("live", R.string.nav_live, "📺")
private val GuideDest = Dest("guide", R.string.nav_guide, "🗓️")
private val MyListDest = Dest("mylist", R.string.nav_mylist, "⭐")
private val RecordingsDest = Dest("recordings", R.string.nav_recordings, "⏺️")
private val SettingsDest = Dest("settings", R.string.nav_settings, "⚙️")
private val MoreDest = Dest("more", R.string.nav_more, "☰")

@Composable
fun MainShell(
    onOpenCategory: (ContentType, Category) -> Unit,
    onOpenTitle: (Title) -> Unit,
    onOpenTmdbTitle: (ContentType, Int, String) -> Unit,
    onOpenTmdbCatalog: (ContentType) -> Unit,
    onSwitchProfile: () -> Unit,
) {
    val inner = rememberNavController()
    val entry by inner.currentBackStackEntryAsState()
    val route = entry?.destination?.route.orEmpty()
    val colors = MaterialTheme.colorScheme

    val go: (String) -> Unit = { destination ->
        inner.navigate(destination) {
            popUpTo(inner.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val topItems = listOf(
        HomeDest,
        LiveDest,
        GuideDest,
        MyListDest,
        RecordingsDest,
        SearchDest,
        SettingsDest,
        MoreDest,
    )

    fun selected(destination: Dest): Boolean =
        if (destination.route == "more") {
            route == "more" || route.startsWith("more/")
        } else {
            route == destination.route ||
                route.startsWith(destination.route + "/")
        }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface)
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "VIREO",
                color = colors.onSurface,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(end = 24.dp),
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                topItems.forEach { destination ->
                    FocusableBox(
                        onClick = { go(destination.route) },
                        selected = selected(destination),
                    ) {
                        Text(
                            text = "${destination.icon}  ${stringResource(destination.label)}",
                            color = if (selected(destination)) {
                                colors.onSurface
                            } else {
                                colors.onSurfaceVariant
                            },
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            ShellNavHost(
                inner,
                go,
                onOpenCategory,
                onOpenTitle,
                onOpenTmdbTitle,
                onOpenTmdbCatalog,
                onSwitchProfile,
            )
        }
    }
}

@Composable
private fun ShellNavHost(
    inner: NavHostController,
    go: (String) -> Unit,
    onOpenCategory: (ContentType, Category) -> Unit,
    onOpenTitle: (Title) -> Unit,
    onOpenTmdbTitle: (ContentType, Int, String) -> Unit,
    onOpenTmdbCatalog: (ContentType) -> Unit,
    onSwitchProfile: () -> Unit,
) {
    val openSources: () -> Unit = { inner.navigate("settings/sources") }
    val back: () -> Unit = { inner.popBackStack() }

    NavHost(navController = inner, startDestination = "home") {
        composable("home") {
            HomeScreen(
                onOpenTitle = onOpenTmdbTitle,
                onOpenCatalog = onOpenTmdbCatalog,
            )
        }
        composable("live") {
            LiveTvScreen(onOpenCategory, openSources)
        }
        composable("guide") {
            PlaceholderScreen(R.string.nav_guide, R.string.ph_guide, "🗓️")
        }
        composable("search") {
            SearchScreen(onOpenTitle = onOpenTmdbTitle)
        }
        composable("mylist") {
            LibraryScreen(onOpenTitle = onOpenTmdbTitle)
        }
        composable("recordings") {
            PlaceholderScreen(R.string.nav_recordings, R.string.ph_recordings, "⏺️")
        }
        composable("more") {
            MoreScreen(onNavigate = go)
        }
        composable("settings") {
            SettingsScreen(
                onNavigate = { r -> inner.navigate(r) },
                onSwitchProfile = onSwitchProfile,
            )
        }
        composable("settings/sources") { SourcesScreen(onBack = back) }
        composable("settings/playback") {
            PlaceholderScreen(R.string.set_playback, R.string.ph_playback, "▶️", back)
        }
        composable("settings/appearance") {
            PlaceholderScreen(R.string.set_appearance, R.string.ph_appearance, "🎨", back)
        }
        composable("settings/epg") {
            PlaceholderScreen(R.string.set_epg, R.string.ph_epg, "🗓️", back)
        }
        composable("settings/parental") {
            PlaceholderScreen(R.string.set_parental, R.string.ph_parental, "🔒", back)
        }
        composable("settings/about") {
            PlaceholderScreen(R.string.set_about, R.string.about_msg, "ℹ️", back)
        }
    }
}
