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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vireo.R
import app.vireo.data.xtream.TmdbXtreamMatchConfidence
import app.vireo.data.xtream.TmdbXtreamMatchStatus
import app.vireo.data.xtream.TmdbXtreamMatchVariant
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

    Column(
        modifier = Modifier.fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState()),
    ) {
        FocusableBox(
            onClick = onBack,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
        ) {
            Text(stringResource(R.string.back), color = colors.onSurface)
        }

        if (state.loading) {
            Box(
                modifier = Modifier.fillMaxWidth().height(260.dp),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
        }

        state.error?.let {
            Text(it, color = colors.error, modifier = Modifier.padding(24.dp))
            Text(
                vm.fallbackName,
                style = MaterialTheme.typography.headlineMedium,
                color = colors.onBackground,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }

        if (detail != null) {
            if (detail.backdropUrl != null) {
                AsyncImage(
                    model = detail.backdropUrl,
                    contentDescription = detail.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().height(230.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalAlignment = Alignment.Top,
            ) {
                if (detail.posterUrl != null) {
                    AsyncImage(
                        model = detail.posterUrl,
                        contentDescription = detail.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(width = 116.dp, height = 174.dp),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        detail.title,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.onBackground,
                    )
                    val metadata = listOfNotNull(
                        detail.year?.toString(),
                        detail.runtimeMinutes?.let { "$it min" },
                    ).joinToString(" • ")
                    if (metadata.isNotBlank()) {
                        Text(metadata, color = colors.onSurfaceVariant)
                    }
                    detail.rating?.let {
                        Text(
                            "★ ${"%.1f".format(java.util.Locale.US, it)}",
                            color = colors.primary,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    if (detail.genres.isNotEmpty()) {
                        Text(detail.genres.joinToString(" • "), color = colors.onSurfaceVariant)
                    }
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Opis", style = MaterialTheme.typography.titleLarge, color = colors.onBackground)
                Text(
                    detail.overview?.takeIf { it.isNotBlank() } ?: "Brak opisu dla tego tytułu.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurface,
                )

                if (detail.cast.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Obsada", style = MaterialTheme.typography.titleLarge, color = colors.onBackground)
                    detail.cast.forEach { member ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(member.name, color = colors.onSurface)
                            if (member.character.isNotBlank()) {
                                Text(member.character, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                Text(
                    "Dostępność w Twoich źródłach",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onBackground,
                )

                if (state.availabilityLoading) {
                    CircularProgressIndicator()
                }

                state.availabilityError?.let {
                    Text(it, color = colors.error)
                }

                state.availability?.let { availability ->
                    val failedSourceIds = availability.syncReport.failures
                        .map { it.sourceId }
                        .distinct()

                    if (failedSourceIds.isNotEmpty()) {
                        val failedNames = failedSourceIds
                            .map { id ->
                                availability.sourceNames[id] ?: "Źródło $id"
                            }
                            .take(3)
                            .joinToString(", ")

                        Text(
                            "Nie udało się zsynchronizować ${failedSourceIds.size} źródeł. " +
                                "Wyniki mogą być niepełne. Problem: $failedNames",
                            color = colors.tertiary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }

                    when (availability.match.status) {
                        TmdbXtreamMatchStatus.NOT_FOUND -> Text(
                            if (failedSourceIds.isEmpty()) {
                                "Nie znaleziono tego tytułu w podłączonych źródłach. " +
                                    "Tytuł pozostaje w katalogu TMDB."
                            } else {
                                "Nie znaleziono dopasowania w sprawdzonych źródłach. " +
                                    "Nie można wykluczyć dostępności w źródłach, których synchronizacja się nie powiodła."
                            },
                            color = colors.onSurfaceVariant,
                        )
                        TmdbXtreamMatchStatus.NEEDS_REVIEW -> Text(
                            "Znaleziono możliwe dopasowania. Wymagają ręcznej weryfikacji.",
                            color = colors.onSurfaceVariant,
                        )
                        TmdbXtreamMatchStatus.MATCHED -> Text(
                            "Znaleziono potwierdzone dopasowanie. Sprawdź również warianty wymagające weryfikacji.",
                            color = colors.onSurfaceVariant,
                        )
                    }

                    availability.match.variants.forEach { variant ->
                        SourceVariantRow(
                            variant = variant,
                            isSeries = detail.type.name == "SERIES",
                            sourceName = availability.sourceNames[variant.sourceId]
                                ?: "Źródło ${variant.sourceId}",
                            onMoviePlay = { vm.playMovie(variant, onPlay) },
                            onSeriesSelect = { vm.selectSeriesSource(variant) },
                        )
                    }
                }

                state.playbackError?.let { Text(it, color = colors.error) }

                if (state.seriesLoading) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        Text(
                            "Pobieranie odcinków z wybranego źródła...",
                            color = colors.onSurfaceVariant,
                        )
                    }
                }

                state.selectedSeries?.let { series ->
                    Text(
                        "Odcinki — ${series.name}",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.onBackground,
                    )
                    series.seasons.forEach { season ->
                        Text(
                            "Sezon ${season.number}",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.onSurface,
                        )
                        season.episodes.forEach { episode ->
                            Button(onClick = { vm.playEpisode(episode.id, onPlay) }) {
                                Text("${episode.number}. ${episode.title}")
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
private fun SourceVariantRow(
    variant: TmdbXtreamMatchVariant,
    isSeries: Boolean,
    sourceName: String,
    onMoviePlay: () -> Unit,
    onSeriesSelect: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    var showReviewConfirmation by remember(variant.sourceId, variant.streamId) {
        mutableStateOf(false)
    }
    val needsReview = variant.confidence == TmdbXtreamMatchConfidence.NEEDS_REVIEW

    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(sourceName, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
        Text(variant.title, color = colors.onSurfaceVariant)
        Text(
            when (variant.confidence) {
                TmdbXtreamMatchConfidence.EXACT_TITLE -> "Potwierdzone — zgodny tytuł i rok"
                TmdbXtreamMatchConfidence.ORIGINAL_TITLE -> "Potwierdzone — zgodny tytuł oryginalny i rok"
                TmdbXtreamMatchConfidence.NEEDS_REVIEW -> "Wymaga weryfikacji — niepełna zgodność roku"
            },
            color = if (needsReview) colors.tertiary else colors.primary,
            style = MaterialTheme.typography.bodySmall,
        )
        Button(
            onClick = {
                if (needsReview) {
                    showReviewConfirmation = true
                } else if (isSeries) {
                    onSeriesSelect()
                } else {
                    onMoviePlay()
                }
            },
        ) {
            Text(
                when {
                    needsReview && isSeries -> "Sprawdź i wybierz serial"
                    needsReview -> "Sprawdź i odtwórz"
                    isSeries -> "Wybierz serial"
                    else -> "Odtwórz"
                }
            )
        }
    }

    if (showReviewConfirmation) {
        AlertDialog(
            onDismissRequest = { showReviewConfirmation = false },
            title = { Text("Weryfikacja dopasowania") },
            text = {
                Text(
                    "Źródło „$sourceName” zawiera pozycję „${variant.title}”, " +
                        "ale rok wydania nie został jednoznacznie potwierdzony. " +
                        "Czy rozpoznajesz ten tytuł i chcesz ręcznie zaakceptować ten wariant?"
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showReviewConfirmation = false
                        if (isSeries) onSeriesSelect() else onMoviePlay()
                    },
                ) {
                    Text(if (isSeries) "Potwierdź i wybierz" else "Potwierdź i odtwórz")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReviewConfirmation = false }) {
                    Text("Anuluj")
                }
            },
        )
    }
}
