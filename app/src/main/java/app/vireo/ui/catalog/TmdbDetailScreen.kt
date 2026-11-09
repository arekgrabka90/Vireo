package app.vireo.ui.catalog

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vireo.R
import app.vireo.data.xtream.TmdbXtreamMatchConfidence
import app.vireo.data.xtream.TmdbXtreamMatchVariant
import app.vireo.data.tmdb.TmdbEpisodeItem
import app.vireo.ui.components.FocusableBox
import coil.compose.AsyncImage

@Composable
fun TmdbDetailScreen(
    onBack: () -> Unit,
    onPlay: () -> Unit,
    vm: TmdbDetailViewModel = hiltViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val colors = MaterialTheme.colorScheme
    val detail = state.detail
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val detailHeroHeight = when {
        screenWidth >= 1200 -> 720.dp
        screenWidth >= 840 -> 680.dp
        screenWidth >= 600 -> 620.dp
        else -> 560.dp
    }

    var showSourcePicker by remember { mutableStateOf(false) }
    var pendingReview by remember { mutableStateOf<TmdbXtreamMatchVariant?>(null) }

    val variants = state.availability?.match?.variants.orEmpty()
    val isSeries = detail?.type?.name == "SERIES"
    val canWatch = detail != null &&
        !state.availabilityLoading &&
        variants.isNotEmpty() &&
        (!isSeries || !state.seriesLoading)

    fun startVariant(variant: TmdbXtreamMatchVariant) {
        if (detail?.type?.name == "SERIES") {
            vm.selectSeriesSource(variant)
        } else {
            vm.playMovie(variant, onPlay)
        }
    }

    fun watch() {
        if (!canWatch) return

        if (isSeries) {
            // Po załadowaniu katalogu WATCH uruchamia pierwszy
            // nieobejrzany odcinek, a nie losowy odcinek serialu.
            if (state.selectedSeries != null) {
                vm.playNextEpisode(onPlay)
            } else {
                // Wiele źródeł lub dopasowanie wymagające weryfikacji.
                showSourcePicker = true
            }
            return
        }

        if (
            variants.size == 1 &&
            variants.first().confidence != TmdbXtreamMatchConfidence.NEEDS_REVIEW
        ) {
            startVariant(variants.first())
        } else {
            showSourcePicker = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState()),
    ) {
        if (state.loading) {
            Box(
                modifier = Modifier.fillMaxWidth().height(360.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        }

        state.error?.let { error ->
            Column(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(error, color = colors.error)
                Text(
                    vm.fallbackName,
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.onBackground,
                )
                OutlinedButton(onClick = onBack) {
                    Text(stringResource(R.string.back))
                }
            }
        }

        if (detail != null) {
            // CINEMATIC HERO
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = detailHeroHeight)
                    .wrapContentHeight()
                    .background(colors.surfaceVariant),
            ) {
                if (!detail.backdropUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = detail.backdropUrl,
                        contentDescription = detail.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else if (!detail.posterUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = detail.posterUrl,
                        contentDescription = detail.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.0f to Color.Black.copy(alpha = 0.24f),
                                0.38f to Color.Black.copy(alpha = 0.34f),
                                0.72f to Color.Black.copy(alpha = 0.78f),
                                1.0f to colors.background,
                            ),
                        ),
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = detailHeroHeight)
                        .padding(horizontal = 24.dp, vertical = 18.dp),
                ) {
                    FocusableBox(
                        onClick = onBack,
                        modifier = Modifier.padding(
                            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp,
                            bottom = 12.dp,
                        ),
                    ) {
                        Text(
                            "‹  ${stringResource(R.string.back)}",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }

                    Spacer(Modifier.height(150.dp))

                    if (!detail.logoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = detail.logoUrl,
                            contentDescription = detail.title,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .widthIn(max = 420.dp)
                                .heightIn(max = 96.dp),
                        )
                    } else {
                        Text(
                            detail.title,
                            color = Color.White,
                            style = if (screenWidth >= 840) {
                                MaterialTheme.typography.displaySmall
                            } else {
                                MaterialTheme.typography.headlineMedium
                            },
                            fontWeight = FontWeight.Bold,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        detail.year?.let { DetailMetaChip(it.toString()) }
                        detail.runtimeMinutes?.let { DetailMetaChip("$it min") }
                        detail.rating?.let {
                            DetailMetaChip("★ ${"%.1f".format(java.util.Locale.US, it)}")
                        }
                    }

                    if (detail.genres.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            detail.genres.joinToString("  ·  "),
                            color = Color.White.copy(alpha = 0.78f),
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    detail.overview
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                        ?.let { overview ->
                            Spacer(Modifier.height(12.dp))
                            Text(
                                overview,
                                color = Color.White.copy(alpha = 0.90f),
                                style = if (screenWidth >= 840) {
                                    MaterialTheme.typography.bodyLarge
                                } else {
                                    MaterialTheme.typography.bodyMedium
                                },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }

                    Spacer(Modifier.height(12.dp))

                    if (!state.availabilityLoading && variants.isEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (state.availabilityError != null) {
                                "Odtwarzanie jest chwilowo niedostępne."
                            } else {
                                "Ten tytuł nie jest obecnie dostępny do odtworzenia."
                            },
                            color = Color.White.copy(alpha = 0.72f),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = if (screenWidth >= 840) 48.dp else 20.dp,
                        vertical = 16.dp,
                    )
                    .widthIn(max = 520.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                DetailActionButton(
                    label = if (isSeries) "▶  WATCH NEXT" else "▶  WATCH",
                    primary = true,
                    enabled = canWatch,
                    modifier = Modifier.weight(1.15f),
                    onClick = { watch() },
                )

                DetailActionButton(
                    label = if (state.inMyList) "✓  MY LIST" else "+  MY LIST",
                    primary = false,
                    enabled = true,
                    modifier = Modifier.weight(1f),
                    onClick = vm::toggleMyList,
                )
            }

            // DETAIL CONTENT
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                // SERIES: SEASONS AND EPISODES
                if (detail.type.name == "SERIES") {
                    SeriesEpisodes(
                        seasons = detail.seasons,
                        episodes = state.tmdbEpisodes,
                        selectedSeasonNumber = state.selectedTmdbSeasonNumber,
                        loading = state.tmdbSeasonLoading,
                        error = state.tmdbSeasonError,
                        tmdbId = detail.tmdbId,
                        watchedEpisodeKeys = state.watchedEpisodeKeys,
                        onSelectSeason = vm::selectTmdbSeason,
                        onPlayEpisode = { seasonNumber, episodeNumber ->
                            vm.playEpisodeByNumber(seasonNumber, episodeNumber, onPlay)
                        },
                    )
                }

                if (detail.cast.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Obsada",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.onBackground,
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        items(
                            items = detail.cast,
                            key = { it.id },
                        ) { member ->
                            Column(
                                modifier = Modifier.width(104.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(82.dp)
                                        .clip(CircleShape)
                                        .background(colors.surfaceVariant),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (!member.profileUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = member.profileUrl,
                                            contentDescription = member.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    } else {
                                        Text(
                                            member.name.take(1).uppercase(),
                                            style = MaterialTheme.typography.headlineMedium,
                                            color = colors.onSurfaceVariant,
                                        )
                                    }
                                }

                                Text(
                                    member.name,
                                    color = colors.onSurface,
                                    style = MaterialTheme.typography.titleSmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                )

                                if (member.character.isNotBlank()) {
                                    Text(
                                        member.character,
                                        color = colors.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                }

                state.playbackError?.let {
                    Surface(
                        color = colors.errorContainer,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Text(
                            it,
                            color = colors.onErrorContainer,
                            modifier = Modifier.padding(14.dp),
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }

    // SOURCE PICKER
    if (showSourcePicker) {
        val availability = state.availability

        AlertDialog(
            onDismissRequest = { showSourcePicker = false },
            title = {
                Text(
                    "Wybierz źródło",
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    variants.forEach { variant ->
                        val sourceName = availability?.sourceNames?.get(variant.sourceId)
                            ?: "Źródło ${variant.sourceId}"
                        val needsReview =
                            variant.confidence == TmdbXtreamMatchConfidence.NEEDS_REVIEW

                        Surface(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Text(
                                    sourceName,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    variant.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    if (needsReview) "Wymaga Twojej weryfikacji"
                                    else "Potwierdzone dopasowanie",
                                    color = if (needsReview) {
                                        MaterialTheme.colorScheme.tertiary
                                    } else {
                                        MaterialTheme.colorScheme.primary
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                )
                                Button(
                                    onClick = {
                                        showSourcePicker = false
                                        if (needsReview) {
                                            pendingReview = variant
                                        } else {
                                            startVariant(variant)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        if (needsReview) "Zweryfikuj źródło"
                                        else "Wybierz to źródło",
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSourcePicker = false }) {
                    Text("Zamknij")
                }
            },
        )
    }

    // EXPLICIT CONFIRMATION FOR UNCERTAIN MATCHES
    pendingReview?.let { variant ->
        val sourceName = state.availability?.sourceNames?.get(variant.sourceId)
            ?: "Źródło ${variant.sourceId}"

        AlertDialog(
            onDismissRequest = { pendingReview = null },
            title = { Text("Potwierdź wybór źródła") },
            text = {
                Text(
                    "Pozycja „${variant.title}” w źródle „$sourceName” " +
                        "nie ma pełnego potwierdzenia zgodności z tym tytułem. " +
                        "Jeśli rozpoznajesz właściwy film lub serial, możesz kontynuować."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingReview = null
                        startVariant(variant)
                    },
                ) {
                    Text("Potwierdź")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingReview = null }) {
                    Text("Anuluj")
                }
            },
        )
    }
}

@Composable
private fun DetailActionButton(
    label: String,
    primary: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            focused || hovered -> 1.035f
            pressed -> 0.975f
            else -> 1f
        },
        label = "detail-action-focus",
    )

    val shape = RoundedCornerShape(14.dp)
    val backgroundColor = when {
        primary -> Color.White
        focused || hovered -> Color(0xFF343B48)
        else -> Color(0xFF1D222B)
    }
    val foregroundColor = if (primary) Color(0xFF111318) else Color.White
    val outlineColor = when {
        focused || hovered -> Color.White
        else -> Color.White.copy(alpha = 0.12f)
    }

    Box(
        modifier = modifier
            .height(54.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (enabled) 1f else 0.48f
            }
            .hoverable(interactionSource)
            .onFocusChanged { focused = it.hasFocus }
            .clip(shape)
            .background(backgroundColor)
            .border(
                width = if (focused || hovered) 1.7.dp else 1.dp,
                color = outlineColor,
                shape = shape,
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = foregroundColor,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun DetailMetaChip(label: String) {
    Surface(
        color = Color.White,
        shape = RoundedCornerShape(7.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            color = Color(0xFF17191E),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

@Composable
private fun SeriesEpisodes(
    seasons: List<app.vireo.data.tmdb.TmdbSeasonSummary>,
    episodes: List<TmdbEpisodeItem>,
    selectedSeasonNumber: Int?,
    loading: Boolean,
    error: String?,
    tmdbId: Int,
    watchedEpisodeKeys: Set<String>,
    onSelectSeason: (Int) -> Unit,
    onPlayEpisode: (Int, Int) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    val selectedSeason = selectedSeasonNumber
        ?: seasons.firstOrNull { it.seasonNumber == 1 }?.seasonNumber
        ?: seasons.firstOrNull()?.seasonNumber

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Odcinki",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = colors.onBackground,
        )

        if (seasons.isEmpty()) {
            Text(
                "Informacje o sezonach nie są dostępne w TMDB.",
                color = colors.onSurfaceVariant,
            )
        } else {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(seasons, key = { it.seasonNumber }) { season ->
                    val label = season.name
                        ?.takeIf { it.isNotBlank() }
                        ?: if (season.seasonNumber == 0) "Specjalne"
                           else "Sezon ${season.seasonNumber}"

                    if (season.seasonNumber == selectedSeason) {
                        Button(onClick = { onSelectSeason(season.seasonNumber) }) {
                            Text(label)
                        }
                    } else {
                        OutlinedButton(onClick = { onSelectSeason(season.seasonNumber) }) {
                            Text(label)
                        }
                    }
                }
            }
        }

        if (loading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp))
                Text("Pobieram odcinki z TMDB…", color = colors.onSurfaceVariant)
            }
        }

        error?.let {
            Text(
                it,
                color = colors.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        if (!loading && error == null && seasons.isNotEmpty() && episodes.isEmpty()) {
            Text(
                "Brak odcinków dla wybranego sezonu.",
                color = colors.onSurfaceVariant,
            )
        }

        if (episodes.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                items(
                    items = episodes,
                    key = { "${it.seasonNumber}-${it.episodeNumber}" },
                ) { episode ->
                    val key =
                        "tmdb:SERIES:$tmdbId:S${episode.seasonNumber}E${episode.episodeNumber}"
                    val watched = key in watchedEpisodeKeys

                    var overviewExpanded by remember(episode.tmdbEpisodeId) {
                        mutableStateOf(false)
                    }
                    val cardShape = RoundedCornerShape(16.dp)

                    Column(
                        modifier = Modifier
                            .width(264.dp)
                            .clip(cardShape)
                            .clickable {
                                onPlayEpisode(
                                    episode.seasonNumber,
                                    episode.episodeNumber,
                                )
                            },
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(cardShape)
                                .background(colors.surfaceVariant),
                        ) {
                            if (!episode.stillUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = episode.stillUrl,
                                    contentDescription = episode.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            } else {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "E${episode.episodeNumber}",
                                        color = colors.onSurfaceVariant,
                                        style = MaterialTheme.typography.headlineMedium,
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(9.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                DetailMetaChip("Odcinek ${episode.episodeNumber}")
                                episode.runtimeMinutes?.let {
                                    DetailMetaChip("$it min")
                                }
                            }

                            if (watched) {
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(8.dp),
                                    color = Color.White,
                                    shape = RoundedCornerShape(7.dp),
                                ) {
                                    Text(
                                        "✓ OBEJRZANE",
                                        modifier = Modifier.padding(
                                            horizontal = 8.dp,
                                            vertical = 5.dp,
                                        ),
                                        color = Color(0xFF17191E),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                    )
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp),
                        ) {
                            Text(
                                episode.title,
                                color = colors.onSurface,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )

                            episode.overview
                                ?.takeIf { it.isNotBlank() }
                                ?.let { overview ->
                                    Text(
                                        overview,
                                        color = colors.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = if (overviewExpanded) Int.MAX_VALUE else 3,
                                        overflow = if (overviewExpanded) {
                                            TextOverflow.Clip
                                        } else {
                                            TextOverflow.Ellipsis
                                        },
                                    )

                                    Text(
                                        if (overviewExpanded) "… mniej" else "… więcej",
                                        color = colors.onSurface,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.clickable {
                                            overviewExpanded = !overviewExpanded
                                        },
                                    )
                                }
                        }
                    }
                }
            }
        }
    }
}
