package app.vireo.ui.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.statusBars
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
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
    position: Int = 1,
    total: Int = 1,
    onClick: (() -> Unit)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val heroHeight = when {
        screenWidth >= 1200 -> 600.dp
        screenWidth >= 840 -> 560.dp
        screenWidth >= 600 -> 500.dp
        else -> 430.dp
    }
    val interaction = onClick?.let { action ->
        Modifier.clickable(onClick = action)
    } ?: Modifier
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(heroHeight)
            .background(colors.surfaceVariant)
            .then(interaction),
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
                            0.0f to Color.Black.copy(alpha = 0.30f),
                            0.42f to Color.Black.copy(alpha = 0.48f),
                            1.0f to colors.background.copy(alpha = 0.99f),
                        ),
                    ),
                ),
        )
        Text(
            text = "VIREO",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(
                    start = 28.dp,
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 12.dp,
                ),
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            letterSpacing = 6.sp,
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(
                    horizontal = if (screenWidth >= 840) 56.dp else 28.dp,
                    vertical = if (screenWidth >= 840) 34.dp else 25.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                "TOP 5  ·  $position / $total",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.78f),
            )
            Text(
                title.name,
                style = if (screenWidth >= 840) {
                    MaterialTheme.typography.displaySmall
                } else {
                    MaterialTheme.typography.headlineMedium
                },
                color = Color.White,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val summary = overview?.trim()?.takeIf { it.isNotBlank() }
            if (summary != null) {
                Text(
                    summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.88f),
                    maxLines = if (screenWidth >= 840) 3 else 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val rating = title.rating?.takeIf { it.isNotBlank() && it != "0" }
            if (rating != null) {
                Text(
                    "★ $rating",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.9f),
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(total.coerceAtMost(5)) { index ->
                    Box(
                        modifier = Modifier
                            .width(if (index == position - 1) 24.dp else 8.dp)
                            .height(3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (index == position - 1) Color.White
                                else Color.White.copy(alpha = 0.42f)
                            ),
                    )
                }
            }
        }
    }
}

@Composable
fun TitleRow(
    row: RowData,
    horizontalPadding: androidx.compose.ui.unit.Dp = 20.dp,
    onSeeAll: () -> Unit,
    onTitle: (Title) -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = horizontalPadding)
                .padding(bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                row.category.name,
                style = MaterialTheme.typography.titleLarge,
                color = colors.onBackground,
                fontWeight = FontWeight.SemiBold,
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
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(
                start = horizontalPadding + 6.dp,
                end = horizontalPadding + 20.dp,
                top = 10.dp,
                bottom = 14.dp,
            ),
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
