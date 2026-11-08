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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.vireo.R
import app.vireo.ui.components.FocusableBox
import coil.compose.AsyncImage

@Composable
fun TmdbDetailScreen(
    onBack: () -> Unit,
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
            ) {
                CircularProgressIndicator()
            }
        }

        state.error?.let {
            Text(
                it,
                color = colors.error,
                modifier = Modifier.padding(24.dp),
            )
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
                        Text(
                            detail.genres.joinToString(" • "),
                            color = colors.onSurfaceVariant,
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "Opis",
                    style = MaterialTheme.typography.titleLarge,
                    color = colors.onBackground,
                )
                Text(
                    detail.overview?.takeIf { it.isNotBlank() }
                        ?: "Brak opisu dla tego tytułu.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.onSurface,
                )

                if (detail.cast.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Obsada",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.onBackground,
                    )
                    detail.cast.forEach { member ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(member.name, color = colors.onSurface)
                            if (member.character.isNotBlank()) {
                                Text(
                                    member.character,
                                    color = colors.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))
    }
}
