package app.vireo.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vireo.R
import app.vireo.domain.model.Category
import app.vireo.domain.model.ContentType
import app.vireo.domain.model.Title
import app.vireo.ui.browse.ErrorBlock
import app.vireo.ui.browse.RowData
import app.vireo.ui.browse.TitleRow
import app.vireo.ui.components.PosterCard
import app.vireo.ui.library.LibraryViewModel
import app.vireo.ui.library.tmdbTarget
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    onOpenTitle: (ContentType, Int, String) -> Unit,
    onOpenCatalog: (ContentType) -> Unit,
    showContinueWatching: Boolean = true,
    desktopShell: Boolean = false,
    modifier: Modifier = Modifier,
    vm: TmdbHomeViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val libraryVm: LibraryViewModel = hiltViewModel()
    val libraryState by libraryVm.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val horizontalPadding = if (desktopShell) {
        24.dp
    } else {
        when {
            screenWidth >= 1200 -> 64.dp
            screenWidth >= 840 -> 48.dp
            screenWidth >= 600 -> 32.dp
            else -> 20.dp
        }
    }
    val continueCardWidth = when {
        screenWidth >= 1200 -> 340.dp
        screenWidth >= 840 -> 300.dp
        screenWidth >= 600 -> 260.dp
        else -> 190.dp
    }

    val movieTitles = state.movies.map { it.toHomeTitle() }
    val seriesTitles = state.series.map { it.toHomeTitle() }

    val myListItems = libraryState.watchlist.mapNotNull { item ->
        val target = item.tmdbTarget() ?: return@mapNotNull null
        target to Title(
            id = item.contentKey,
            name = item.title,
            posterUrl = item.posterUrl,
            rating = null,
            type = target.type,
            categoryId = null,
        )
    }
    val allTrending = (state.movies + state.series)
        .distinctBy { "${it.type.name}:${it.tmdbId}" }

    val heroItems = allTrending
        .filter {
            !it.backdropUrl.isNullOrBlank() ||
                !it.posterUrl.isNullOrBlank()
        }
        .sortedByDescending { it.popularity }
        .take(5)

    val heroKeys = heroItems.map { "${it.type.name}:${it.tmdbId}" }
    val activeHeroKey = rememberSaveable {
        mutableStateOf<String?>(null)
    }
    val heroFocused = remember { mutableStateOf(false) }
    val heroIndex = resolveHomeHeroIndex(heroKeys, activeHeroKey.value)

    LaunchedEffect(heroKeys, heroFocused.value, activeHeroKey.value) {
        if (heroKeys.isEmpty()) return@LaunchedEffect

        if (activeHeroKey.value !in heroKeys) {
            activeHeroKey.value = heroKeys.first()
            return@LaunchedEffect
        }

        if (!heroFocused.value && heroKeys.size > 1) {
            delay(10_000L)

            val current = resolveHomeHeroIndex(
                heroKeys,
                activeHeroKey.value,
            )

            activeHeroKey.value = heroKeys[(current + 1) % heroKeys.size]
        }
    }

    fun open(title: Title) {
        val id = title.id.toIntOrNull() ?: return
        onOpenTitle(title.type, id, title.name)
    }

    val resumable = libraryState.continueWatching.mapNotNull { item ->
        val target = item.tmdbTarget() ?: return@mapNotNull null
        Triple(item, target, Title(
            id = item.contentKey,
            name = item.title,
            posterUrl = item.posterUrl,
            rating = null,
            type = target.type,
            categoryId = null,
        ))
    }

    val continueKeys = resumable.map { it.first.contentKey }

    val continueFocusRequesters = remember(continueKeys) {
        continueKeys.associateWith { FocusRequester() }
    }

    val continueListState = rememberLazyListState()

    val pendingContinueFocusRestore =
        remember { mutableStateOf<Pair<String, String?>?>(null) }

    LaunchedEffect(continueKeys, pendingContinueFocusRestore.value) {
        val pending = pendingContinueFocusRestore.value
            ?: return@LaunchedEffect

        if (pending.first !in continueKeys) {
            val targetKey = pending.second
            val targetIndex = targetKey?.let {
                continueKeys.indexOf(it)
            } ?: -1

            if (targetIndex >= 0) {
                continueListState.animateScrollToItem(targetIndex)
                withFrameNanos { }

                continueFocusRequesters[targetKey]?.requestFocus()
            }

            pendingContinueFocusRestore.value = null
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize().background(colors.background),
        contentPadding = PaddingValues(vertical = 0.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {

        if (state.loading && movieTitles.isEmpty() && seriesTitles.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding),
                ) {
                    CircularProgressIndicator()
                }
            }
        }

        if (state.error != null && movieTitles.isEmpty() && seriesTitles.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding),
                ) {
                    ErrorBlock(state.error ?: "TMDB error", vm::refresh)
                }
            }
        }

        if (heroItems.isNotEmpty()) {
            item(key = "home-featured-poster-strip") {
                CinematicHero(
                    items = heroItems,
                    activeIndex = heroIndex.coerceIn(
                        0,
                        heroItems.lastIndex,
                    ),
                    onActivate = { index ->
                        activeHeroKey.value = heroKeys.getOrNull(index)
                    },
                    onHeroFocusChanged = { focused ->
                        heroFocused.value = focused
                    },
                    onOpen = { item ->
                        onOpenTitle(
                            item.type,
                            item.tmdbId,
                            item.title,
                        )
                    },
                )
            }
        }

        if (showContinueWatching && resumable.isNotEmpty()) {
            item(key = "continue-watching") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Kontynuuj oglądanie",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.onBackground,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = horizontalPadding),
                    )
                    LazyRow(
                        state = continueListState,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth().focusGroup(),
                        contentPadding = PaddingValues(
                            start = horizontalPadding + 6.dp,
                            end = horizontalPadding + 20.dp,
                            top = 8.dp,
                            bottom = 14.dp,
                        ),
                    ) {
                        items(resumable, key = { it.first.contentKey }) { entry ->
                            val item = entry.first
                            val progress = homeProgressFraction(
                                item.positionMs,
                                item.durationMs,
                            )
                            val remainingMs = (item.durationMs - item.positionMs)
                                .coerceAtLeast(0L)
                            val remainingText = if (item.durationMs > 0L) {
                                "Pozostało ${formatHomeRemainingDuration(remainingMs)}"
                            } else {
                                null
                            }

                            val backdrop = item.backdropUrl
                                ?.takeIf { it.isNotBlank() }
                                ?: allTrending.firstOrNull {
                                    it.tmdbId == entry.second.id &&
                                        it.type == entry.second.type
                                }?.backdropUrl

                            PosterCard(
                                title = entry.third,
                                imageUrl = backdrop
                                    ?.takeIf { it.isNotBlank() }
                                    ?: entry.third.posterUrl,
                                isLandscape = true,
                                cardWidth = continueCardWidth,
                                topProgress = progress,
                                topStartLabel = if (entry.second.type == ContentType.MOVIE) {
                                    "FILM"
                                } else {
                                    "SERIAL"
                                },
                                topEndLabel = remainingText?.removePrefix("Pozostało "),
                                belowPosterText = remainingText,
                                isWatched = item.watched,
                                showPlayOnFocus = true,
                                focusRequester = continueFocusRequesters[item.contentKey],
                                onRemove = {
                                    val currentIndex =
                                        continueKeys.indexOf(item.contentKey)

                                    val nextFocusKey =
                                        continueKeys.getOrNull(currentIndex + 1)
                                            ?: continueKeys.getOrNull(currentIndex - 1)

                                    pendingContinueFocusRestore.value =
                                        item.contentKey to nextFocusKey

                                    libraryVm.removeFromContinueWatching(item)
                                },
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

        if (myListItems.isNotEmpty()) {
            item(key = "my-list") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Moja lista",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.onBackground,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = horizontalPadding),
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth().focusGroup(),
                        contentPadding = PaddingValues(
                            start = horizontalPadding + 6.dp,
                            end = horizontalPadding + 20.dp,
                            top = 8.dp,
                            bottom = 14.dp,
                        ),
                    ) {
                        items(
                            myListItems,
                            key = { it.second.id },
                        ) { entry ->
                            PosterCard(
                                title = entry.second,
                                isWatched = entry.second.id in libraryState.watchedContentKeys,
                                onClick = {
                                    onOpenTitle(
                                        entry.first.type,
                                        entry.first.id,
                                        entry.second.name,
                                    )
                                },
                            )
                        }
                    }
                }
            }
        }

        if (movieTitles.isNotEmpty()) {
            item {
                TitleRow(
                    row = RowData(
                        Category("tmdb-trending-movies", "Na czasie — Filmy", ContentType.MOVIE),
                        movieTitles.take(20),
                    ),
                    horizontalPadding = horizontalPadding,
                    onSeeAll = { onOpenCatalog(ContentType.MOVIE) },
                    onTitle = ::open,
                    watchedKeys = libraryState.watchedContentKeys,
                    focusGroupEnabled = true,
                )
            }
        }

        if (seriesTitles.isNotEmpty()) {
            item {
                TitleRow(
                    row = RowData(
                        Category("tmdb-trending-series", "Na czasie — Seriale", ContentType.SERIES),
                        seriesTitles.take(20),
                    ),
                    horizontalPadding = horizontalPadding,
                    onSeeAll = { onOpenCatalog(ContentType.SERIES) },
                    onTitle = ::open,
                    watchedKeys = libraryState.watchedContentKeys,
                    focusGroupEnabled = true,
                )
            }
        }

        if (!state.loading && movieTitles.isEmpty() && seriesTitles.isEmpty()) {
            item {
                Text(
                    state.error ?: stringResource(R.string.empty_titles),
                    color = colors.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = horizontalPadding),
                )
            }
        }
    }
}
