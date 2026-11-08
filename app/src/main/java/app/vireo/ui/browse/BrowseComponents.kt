package app.vireo.ui.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vireo.R
import app.vireo.domain.model.ContentType
import app.vireo.domain.model.Title
import app.vireo.ui.components.FocusableBox
import app.vireo.ui.components.PosterCard
import coil.compose.AsyncImage

fun ContentType.navLabel(): Int = when (this) {
    ContentType.MOVIE -> R.string.nav_movies
    ContentType.SERIES -> R.string.nav_series
    ContentType.LIVE -> R.string.nav_live
}

@Composable
fun Hero(
    title: Title,
    backdropUrl: String? = null,
    overview: String? = null,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surfaceVariant),
    ) {
        AsyncImage(
            model = backdropUrl?.takeIf { it.isNotBlank() } ?: title.posterUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Black.copy(alpha = 0.04f),
                            0.42f to Color.Black.copy(alpha = 0.24f),
                            1.0f to colors.background.copy(alpha = 0.98f),
                        ),
                    ),
                ),
        )
        Column(
            modifier = Modifier.align(Alignment.BottomStart).padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                title.name,
                style = MaterialTheme.typography.headlineMedium,
                color = colors.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val summary = overview?.trim()?.takeIf { it.isNotBlank() }
            if (summary != null) {
                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.88f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val rating = title.rating?.takeIf { it.isNotBlank() && it != "0" }
            if (rating != null) {
                Text("★ $rating", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun TitleRow(row: RowData, onSeeAll: () -> Unit, onTitle: (Title) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                row.category.name,
                style = MaterialTheme.typography.titleMedium,
                color = colors.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            FocusableBox(onClick = onSeeAll) {
                Text(
                    stringResource(R.string.see_all),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onSurface,
                )
            }
        }
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 4.dp),
        ) {
            items(row.titles, key = { it.id }) { t ->
                PosterCard(title = t, onClick = { onTitle(t) })
            }
        }
    }
}

@Composable
fun SectionPlaceholder(title: String, hint: String) {
    val colors = MaterialTheme.colorScheme
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium, color = colors.onBackground)
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                hint,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
fun NoSourceCard(onOpenSources: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colors.surfaceVariant)
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text("📡", fontSize = 40.sp)
        Text(
            stringResource(R.string.no_source_title),
            style = MaterialTheme.typography.titleLarge,
            color = colors.onSurface,
        )
        Text(
            stringResource(R.string.no_source_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Button(onClick = onOpenSources) { Text(stringResource(R.string.open_settings)) }
    }
}

@Composable
fun ErrorBlock(message: String, onRetry: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(message, color = colors.error)
        FocusableBox(onClick = onRetry) {
            Text(stringResource(R.string.retry), color = colors.onSurface)
        }
    }
}
