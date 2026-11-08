package app.vireo.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import app.vireo.domain.model.SeriesDetails
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
                    .height(detailHeroHeight)
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
                        .fillMaxSize()
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

                    Spacer(Modifier.weight(1f))

                    Text(
                        if (detail.type.name == "SERIES") "VIREO ORIGINAL EXPERIENCE · SERIES"
                        else "VIREO CINEMA · MOVIE",
                        color = colors.primary,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                    )

                    Spacer(Modifier.height(8.dp))

                    Text(
                        detail.title,
                        color = Color.White,
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(Modifier.height(10.dp))

                    val metadata = listOfNotNull(
                        detail.year?.toString(),
                        detail.runtimeMinutes?.let { "$it min" },
                    ).joinToString("  ·  ")

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (metadata.isNotBlank()) {
                            Text(metadata, color = Color.White.copy(alpha = 0.88f))
                        }
                        detail.rating?.let {
                            Text(
                                "★ ${"%.1f".format(java.util.Locale.US, it)}",
                                color = Color(0xFFFFD166),
                                fontWeight = FontWeight.Bold,
                            )
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

                    Spacer(Modifier.height(20.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Button(
                            onClick = { watch() },
                            enabled = canWatch,
                            modifier = Modifier.widthIn(min = 148.dp),
                        ) {
                            if (state.availabilityLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(
                                if (isSeries) "▶  WATCH NEXT" else "▶  WATCH",
                                fontWeight = FontWeight.Bold,
                            )
                        }

                        OutlinedButton(onClick = vm::toggleMyList) {
                            Text(
                                if (state.inMyList) "✓  MY LIST" else "+  MY LIST",
                            )
                        }
                    }

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

            // DETAIL CONTENT
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 22.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text(
                    "O filmie",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground,
                )

                Text(
                    detail.overview?.takeIf { it.isNotBlank() }
                        ?: "Opis tego tytułu nie jest jeszcze dostępny.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurface,
                )

                // SERIES: SEASONS AND EPISODES
                if (detail.type.name == "SERIES") {
                    SeriesEpisodes(
                        series = state.selectedSeries,
                        loading = state.seriesLoading,
                        tmdbId = detail.tmdbId,
                        watchedEpisodeKeys = state.watchedEpisodeKeys,
                        onPlayEpisode = { episodeId ->
                            vm.playEpisode(episodeId, onPlay)
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
                                modifier = Modifier.width(116.dp),
                                verticalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp)
                                        .clip(RoundedCornerShape(14.dp))
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
                                            style = MaterialTheme.typography.headlineLarge,
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
                                )

                                if (member.character.isNotBlank()) {
                                    Text(
                                        member.character,
                                        color = colors.onSurfaceVariant,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
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
private fun SeriesEpisodes(
    series: SeriesDetails?,
    loading: Boolean,
    tmdbId: Int,
    watchedEpisodeKeys: Set<String>,
    onPlayEpisode: (String) -> Unit,
) {
    val colors = MaterialTheme.colorScheme

    var selectedSeasonNumber by remember(series?.id) {
        mutableStateOf(series?.seasons?.firstOrNull()?.number ?: 0)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Sezony i odcinki",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = colors.onBackground,
        )

        if (loading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp))
                Text(
                    "Przygotowuję odcinki...",
                    color = colors.onSurfaceVariant,
                )
            }
        }

        if (series != null) {
            Text(
                series.name,
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
            )

            if (series.seasons.isEmpty()) {
                Text(
                    "Lista odcinków nie jest dostępna dla tego źródła.",
                    color = colors.onSurfaceVariant,
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(
                        items = series.seasons,
                        key = { it.number },
                    ) { season ->
                        if (season.number == selectedSeasonNumber) {
                            Button(
                                onClick = { selectedSeasonNumber = season.number },
                            ) {
                                Text("Sezon ${season.number}")
                            }
                        } else {
                            OutlinedButton(
                                onClick = { selectedSeasonNumber = season.number },
                            ) {
                                Text("Sezon ${season.number}")
                            }
                        }
                    }
                }

                val selectedSeason = series.seasons.firstOrNull {
                    it.number == selectedSeasonNumber
                }

                selectedSeason?.let { season ->
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(
                            items = season.episodes,
                            key = { it.id },
                        ) { episode ->
                            val contentKey =
                                "tmdb:SERIES:$tmdbId:S${season.number}E${episode.number}"
                            val watched = contentKey in watchedEpisodeKeys

                            Surface(
                                modifier = Modifier.width(252.dp),
                                color = colors.surfaceVariant.copy(alpha = 0.72f),
                                shape = RoundedCornerShape(16.dp),
                                onClick = { onPlayEpisode(episode.id) },
                            ) {
                                Column {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(142.dp)
                                            .background(colors.surface),
                                    ) {
                                        if (!episode.thumbUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = episode.thumbUrl,
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
                                                    "E${episode.number}",
                                                    color = colors.onSurfaceVariant,
                                                    style = MaterialTheme.typography.headlineMedium,
                                                )
                                            }
                                        }

                                        if (watched) {
                                            Surface(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(8.dp),
                                                color = colors.primaryContainer,
                                                shape = RoundedCornerShape(8.dp),
                                            ) {
                                                Text(
                                                    "✓ OBEJRZANE",
                                                    modifier = Modifier.padding(
                                                        horizontal = 9.dp,
                                                        vertical = 5.dp,
                                                    ),
                                                    color = colors.onPrimaryContainer,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                )
                                            }
                                        } else {
                                            Surface(
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(8.dp),
                                                color = Color.Black.copy(alpha = 0.72f),
                                                shape = RoundedCornerShape(8.dp),
                                            ) {
                                                Text(
                                                    "▶",
                                                    modifier = Modifier.padding(
                                                        horizontal = 10.dp,
                                                        vertical = 5.dp,
                                                    ),
                                                    color = Color.White,
                                                )
                                            }
                                        }
                                    }

                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(5.dp),
                                    ) {
                                        Text(
                                            "ODCINEK ${episode.number}",
                                            color = colors.primary,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                        )

                                        Text(
                                            episode.title,
                                            color = colors.onSurface,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )

                                        episode.duration
                                            ?.takeIf { it.isNotBlank() }
                                            ?.let {
                                                Text(
                                                    it,
                                                    color = colors.onSurfaceVariant,
                                                    style = MaterialTheme.typography.labelSmall,
                                                )
                                            }

                                        episode.plot
                                            ?.takeIf { it.isNotBlank() }
                                            ?.let {
                                                Text(
                                                    it,
                                                    color = colors.onSurfaceVariant,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }

                                        if (watched) {
                                            Text(
                                                "Obejrzano",
                                                color = colors.primary,
                                                style = MaterialTheme.typography.labelSmall,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else if (!loading) {
            Text(
                "Wybierz źródło, aby załadować odcinki.",
                color = colors.onSurfaceVariant,
            )
        }
    }
}
