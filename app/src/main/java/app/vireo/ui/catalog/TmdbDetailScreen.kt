package app.vireo.ui.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.vireo.R
import app.vireo.domain.model.ContentType
import app.vireo.ui.components.FocusableBox

@Composable
fun TmdbDetailScreen(
    type: ContentType,
    tmdbId: Int,
    name: String,
    onBack: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 32.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        FocusableBox(onClick = onBack) {
            Text(stringResource(R.string.back), color = colors.onSurface)
        }
        Text(name, style = MaterialTheme.typography.headlineMedium, color = colors.onBackground)
        Text(
            if (type == ContentType.SERIES) "Serial" else "Film",
            style = MaterialTheme.typography.labelLarge,
            color = colors.onSurfaceVariant,
        )
        Text(
            "TMDB ID: $tmdbId",
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
        )
        Text(
            "Katalog TMDB jest aktywny. Integracja szczegółowych metadanych, dopasowanych źródeł Xtream i odtwarzania zostanie podłączona w następnym etapie.",
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurface,
        )
    }
}
