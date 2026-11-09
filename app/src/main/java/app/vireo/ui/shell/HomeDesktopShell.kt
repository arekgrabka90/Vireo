package app.vireo.ui.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vireo.domain.model.ContentType
import app.vireo.domain.model.Title
import app.vireo.ui.components.PosterCard
import app.vireo.ui.home.HomeScreen
import app.vireo.ui.home.formatHomeRemainingDuration
import app.vireo.ui.home.homeProgressFraction
import app.vireo.ui.library.LibraryViewModel
import app.vireo.ui.library.tmdbTarget

private data class HomeSidebarItem(
    val label: String,
    val symbol: String,
    val route: String,
)

private val homeSidebarItems = listOf(
    HomeSidebarItem("Home", "⌂", "home"),
    HomeSidebarItem("Favorites", "♡", "mylist"),
    HomeSidebarItem("Live TV", "▣", "live"),
    HomeSidebarItem("TV Guide", "▦", "guide"),
    HomeSidebarItem("Recordings", "●", "recordings"),
    HomeSidebarItem("Settings", "⚙", "settings"),
    HomeSidebarItem("Support", "?", "more"),
)

@Composable
fun HomeDesktopShell(
    onNavigate: (String) -> Unit,
    onOpenTmdbTitle: (ContentType, Int, String) -> Unit,
    onOpenTmdbCatalog: (ContentType) -> Unit,
    onSwitchProfile: () -> Unit,
    libraryVm: LibraryViewModel = hiltViewModel(),
) {
    val libraryState by libraryVm.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    val screenWidth = LocalConfiguration.current.screenWidthDp

    val sidebarWidth = when {
        screenWidth >= 1200 -> 260.dp
        screenWidth >= 900 -> 244.dp
        else -> 216.dp
    }

    val continueCardWidth = (sidebarWidth - 40.dp)
        .coerceIn(160.dp, 220.dp)

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(16.dp),
        color = Color(0xFF111A1D),
        shape = RoundedCornerShape(26.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            HomeSidebar(
                items = libraryState.continueWatching,
                sidebarWidth = sidebarWidth,
                continueCardWidth = continueCardWidth,
                onNavigate = onNavigate,
                onOpenTitle = onOpenTmdbTitle,
                onRemove = libraryVm::removeFromContinueWatching,
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .focusGroup()
                    .padding(start = 12.dp, end = 18.dp, top = 12.dp),
            ) {
                HomeTopBar(
                    onSearch = { onNavigate("search") },
                    onOpenCatalog = onOpenTmdbCatalog,
                    onSwitchProfile = onSwitchProfile,
                )

                HomeScreen(
                    onOpenTitle = onOpenTmdbTitle,
                    onOpenCatalog = onOpenTmdbCatalog,
                    showContinueWatching = false,
                    desktopShell = true,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun HomeSidebar(
    items: List<app.vireo.data.local.LibraryItemEntity>,
    sidebarWidth: androidx.compose.ui.unit.Dp,
    continueCardWidth: androidx.compose.ui.unit.Dp,
    onNavigate: (String) -> Unit,
    onOpenTitle: (ContentType, Int, String) -> Unit,
    onRemove: (app.vireo.data.local.LibraryItemEntity) -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .width(sidebarWidth)
            .fillMaxHeight()
            .focusGroup()
            .padding(start = 18.dp, top = 22.dp, bottom = 14.dp, end = 12.dp),
    ) {
        Text(
            text = "VIREO",
            color = colors.onBackground,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(start = 8.dp, bottom = 30.dp),
        )

        homeSidebarItems.forEach { item ->
            HomeSidebarEntry(
                item = item,
                onClick = { onNavigate(item.route) },
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = "CONTINUE WATCHING",
            color = colors.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 8.dp, bottom = 12.dp),
        )

        val resumable = items.mapNotNull { item ->
            val target = item.tmdbTarget() ?: return@mapNotNull null
            val title = Title(
                id = item.contentKey,
                name = item.title,
                posterUrl = item.posterUrl,
                rating = null,
                type = target.type,
                categoryId = null,
            )
            Triple(item, target, title)
        }

        if (resumable.isEmpty()) {
            Text(
                text = "Brak rozpoczętych tytułów",
                color = colors.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(
                    items = resumable,
                    key = { it.first.contentKey },
                ) { entry ->
                    val item = entry.first
                    val progress = homeProgressFraction(
                        item.positionMs,
                        item.durationMs,
                    )
                    val remaining = if (item.durationMs > 0L) {
                        "Pozostało ${
                            formatHomeRemainingDuration(
                                (item.durationMs - item.positionMs).coerceAtLeast(0L)
                            )
                        }"
                    } else {
                        null
                    }

                    PosterCard(
                        title = entry.third,
                        imageUrl = item.backdropUrl
                            ?.takeIf { it.isNotBlank() }
                            ?: item.posterUrl,
                        isLandscape = true,
                        cardWidth = continueCardWidth,
                        topProgress = progress,
                        topStartLabel = if (entry.second.type == ContentType.MOVIE) {
                            "FILM"
                        } else {
                            "SERIAL"
                        },
                        topEndLabel = remaining?.removePrefix("Pozostało "),
                        belowPosterText = remaining,
                        showPlayOnFocus = true,
                        onRemove = { onRemove(item) },
                        onClick = {
                            onOpenTitle(
                                entry.second.type,
                                entry.second.id,
                                item.title,
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeSidebarEntry(
    item: HomeSidebarItem,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var focused by remember { mutableStateOf(false) }
    val selected = item.route == "home"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (selected) Color.White.copy(alpha = 0.09f)
                else if (focused) colors.surfaceVariant
                else Color.Transparent,
            )
            .onFocusChanged { focused = it.hasFocus }
            .clickable(
                role = Role.Button,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        Text(
            text = item.symbol,
            color = if (selected || focused) colors.onBackground
                else colors.onSurfaceVariant,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = item.label,
            color = if (selected || focused) colors.onBackground
                else colors.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

@Composable
private fun HomeTopBar(
    onSearch: () -> Unit,
    onOpenCatalog: (ContentType) -> Unit,
    onSwitchProfile: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var menuExpanded by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box {
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(
                        role = Role.Button,
                        onClick = { menuExpanded = true },
                    ),
                color = colors.surfaceVariant,
                shape = RoundedCornerShape(14.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Movies", color = colors.onSurface)
                    Text("⌄", color = colors.onSurfaceVariant)
                }
            }

            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
            ) {
                DropdownMenuItem(
                    text = { Text("Movies") },
                    onClick = {
                        menuExpanded = false
                        onOpenCatalog(ContentType.MOVIE)
                    },
                )
                DropdownMenuItem(
                    text = { Text("Series") },
                    onClick = {
                        menuExpanded = false
                        onOpenCatalog(ContentType.SERIES)
                    },
                )
            }
        }

        Surface(
            modifier = Modifier
                .weight(1f)
                .height(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable(
                    role = Role.Button,
                    onClick = onSearch,
                ),
            color = colors.surfaceVariant,
            shape = RoundedCornerShape(14.dp),
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = "Szukaj",
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(21.dp),
                )
                Text(
                    text = "Movies, series, shows...",
                    color = colors.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        Surface(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .clickable(
                    role = Role.Button,
                    onClick = onSwitchProfile,
                ),
            color = colors.surfaceVariant,
            shape = RoundedCornerShape(14.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Filled.AccountCircle,
                    contentDescription = "Zmień profil",
                    tint = colors.onSurfaceVariant,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
    }
}
