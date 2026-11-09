package app.vireo.ui.shell

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.vireo.R
import app.vireo.domain.model.Category
import app.vireo.domain.model.ContentType
import app.vireo.domain.model.Title
import app.vireo.ui.components.PlaceholderScreen
import app.vireo.ui.home.HomeScreen
import app.vireo.ui.shell.HomeDesktopShell
import app.vireo.ui.library.LibraryScreen
import app.vireo.ui.live.LiveTvScreen
import app.vireo.ui.more.MoreScreen
import app.vireo.ui.settings.SettingsScreen
import app.vireo.ui.settings.SourcesScreen
import app.vireo.ui.search.SearchScreen

private data class Dest(val route: String, val label: Int)

private val HomeDest = Dest("home", R.string.nav_home)
private val LiveDest = Dest("live", R.string.nav_live)
private val GuideDest = Dest("guide", R.string.nav_guide)
private val MyListDest = Dest("mylist", R.string.nav_mylist)
private val RecordingsDest = Dest("recordings", R.string.nav_recordings)
private val SearchDest = Dest("search", R.string.nav_search)
private val SettingsDest = Dest("settings", R.string.nav_settings)
private val MoreDest = Dest("more", R.string.nav_more)

private data class BottomDest(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val mobileDestinations = listOf(
    BottomDest("home", "Główna", Icons.Filled.Home),
    BottomDest("live", "TV", Icons.Filled.PlayArrow),
    BottomDest("mylist", "Moja lista", Icons.Filled.Favorite),
    BottomDest("search", "Szukaj", Icons.Filled.Search),
    BottomDest("more", "Więcej", Icons.Filled.Menu),
)

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
    val configuration = LocalConfiguration.current

    val isTelevision =
        (configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) ==
            Configuration.UI_MODE_TYPE_TELEVISION
    val useBottomNavigation =
        !isTelevision && configuration.screenWidthDp < 600

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

    val isSelected: (Dest) -> Boolean = { destination ->
        if (destination.route == "more") {
            route == "more" || route.startsWith("more/")
        } else {
            route == destination.route ||
                route.startsWith(destination.route + "/")
        }
    }

    if (useBottomNavigation) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(colors.background),
        ) {
            if (route != "home") {
                MobileHeader(
                    onSearch = { go("search") },
                    onSwitchProfile = onSwitchProfile,
                )
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
                    modifier = Modifier.fillMaxSize(),
                )

                if (route == "home") {
                    MobileHomeActions(
                        onSearch = { go("search") },
                        onSwitchProfile = onSwitchProfile,
                        modifier = Modifier.align(Alignment.TopEnd),
                    )
                }
            }

            FloatingBottomNavigation(
                route = route,
                onNavigate = go,
            )
        }
    } else {
        if (route == "home") {
            HomeDesktopShell(
                onNavigate = go,
                onOpenTmdbTitle = onOpenTmdbTitle,
                onOpenTmdbCatalog = onOpenTmdbCatalog,
                onSwitchProfile = onSwitchProfile,
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(colors.background),
            ) {
                ShellNavHost(
                    inner,
                    go,
                    onOpenCategory,
                    onOpenTitle,
                    onOpenTmdbTitle,
                    onOpenTmdbCatalog,
                    onSwitchProfile,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 70.dp),
                )

                FloatingTopNavigation(
                    items = topItems,
                    isSelected = isSelected,
                    onNavigate = go,
                    onSwitchProfile = onSwitchProfile,
                    showBrand = true,
                    modifier = Modifier.align(Alignment.TopCenter),
                )
            }
        }
    }
}

@Composable
private fun MobileHeader(
    onSearch: () -> Unit,
    onSwitchProfile: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "VIREO",
            color = colors.onBackground,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
        )

        Spacer(Modifier.weight(1f))

        HeaderAction(Icons.Filled.Search, "Szukaj", onSearch)
        HeaderAction(Icons.Filled.AccountCircle, "Zmień profil", onSwitchProfile)
    }
}

@Composable
private fun MobileHomeActions(
    onSearch: () -> Unit,
    onSwitchProfile: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeaderAction(Icons.Filled.Search, "Szukaj", onSearch)
        HeaderAction(Icons.Filled.AccountCircle, "Zmień profil", onSwitchProfile)
    }
}

@Composable
private fun HeaderAction(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = colors.onSurfaceVariant,
            modifier = Modifier.size(23.dp),
        )
    }
}

@Composable
private fun FloatingTopNavigation(
    items: List<Dest>,
    isSelected: (Dest) -> Boolean,
    onNavigate: (String) -> Unit,
    onSwitchProfile: () -> Unit,
    showBrand: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 26.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showBrand) {
            Text(
                text = "VIREO",
                color = colors.onBackground,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(end = 24.dp),
            )
        } else {
            Spacer(Modifier.width(120.dp))
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            items.forEach { destination ->
                FloatingNavItem(
                    label = stringResource(destination.label),
                    selected = isSelected(destination),
                    onClick = { onNavigate(destination.route) },
                )
            }
        }

        HeaderAction(
            Icons.Filled.AccountCircle,
            "Zmień profil",
            onSwitchProfile,
        )
    }
}

@Composable
private fun FloatingNavItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var focused by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .onFocusChanged { focused = it.hasFocus }
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label,
            color = if (selected || focused) colors.onBackground
                else colors.onSurfaceVariant,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
        )

        Spacer(Modifier.height(5.dp))

        Box(
            modifier = Modifier
                .width(if (selected || focused) 18.dp else 5.dp)
                .height(2.dp)
                .clip(CircleShape)
                .background(
                    if (selected || focused) colors.primary
                    else Color.Transparent
                ),
        )
    }
}

@Composable
private fun FloatingBottomNavigation(
    route: String,
    onNavigate: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(26.dp)

    val moreSelected = route !in setOf(
        "home", "live", "mylist", "search",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clip(shape)
            .background(colors.surface.copy(alpha = 0.96f))
            .border(
                width = 1.dp,
                color = colors.outline.copy(alpha = 0.18f),
                shape = shape,
            )
            .padding(horizontal = 5.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        mobileDestinations.forEach { destination ->
            val selected = if (destination.route == "more") {
                moreSelected
            } else {
                route == destination.route ||
                    route.startsWith(destination.route + "/")
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(
                        role = Role.Button,
                        onClick = { onNavigate(destination.route) },
                    )
                    .padding(vertical = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (selected) colors.primary.copy(alpha = 0.13f)
                            else Color.Transparent
                        )
                        .padding(horizontal = 13.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = destination.icon,
                        contentDescription = null,
                        tint = if (selected) colors.primary
                            else colors.onSurfaceVariant,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Text(
                    text = destination.label,
                    color = if (selected) colors.onSurface
                        else colors.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
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
    modifier: Modifier = Modifier,
) {
    val openSources: () -> Unit = { inner.navigate("settings/sources") }
    val back: () -> Unit = { inner.popBackStack() }

    NavHost(navController = inner, startDestination = "home", modifier = modifier) {
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
