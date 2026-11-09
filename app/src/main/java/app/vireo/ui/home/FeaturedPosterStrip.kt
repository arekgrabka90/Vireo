package app.vireo.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vireo.data.tmdb.TmdbCatalogItem
import coil.compose.AsyncImage

@Composable
fun FeaturedPosterStrip(
    items: List<TmdbCatalogItem>,
    activeIndex: Int,
    horizontalPadding: Dp,
    onActivate: (Int) -> Unit,
    onOpen: (TmdbCatalogItem) -> Unit,
) {
    if (items.isEmpty()) return

    val screenWidth = LocalConfiguration.current.screenWidthDp
    val height = if (screenWidth >= 840) 420.dp else 300.dp
    val shape = RoundedCornerShape(22.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = horizontalPadding)
            .height(height)
            .clip(shape)
        ,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        items.forEachIndexed { index, item ->
            val active = index == activeIndex
            val hasBackdrop = !item.backdropUrl.isNullOrBlank()

            val cardWeight by animateFloatAsState(
                targetValue = if (active) 4.8f else 0.72f,
                animationSpec = tween(durationMillis = 850),
                label = "featured-card-width",
            )

            val posterAlpha by animateFloatAsState(
                targetValue = if (active && hasBackdrop) 0f else 1f,
                animationSpec = tween(durationMillis = 850),
                label = "featured-poster-fade",
            )

            val backdropAlpha by animateFloatAsState(
                targetValue = if (active && hasBackdrop) 1f else 0f,
                animationSpec = tween(durationMillis = 850),
                label = "featured-backdrop-fade",
            )

            var focused by remember { mutableStateOf(false) }
            val colors = MaterialTheme.colorScheme

            Box(
                modifier = Modifier
                    .weight(cardWeight)
                    .fillMaxHeight()
                    .onFocusChanged { focused = it.hasFocus }
                    .border(
                        width = if (focused) 2.dp else 0.dp,
                        color = if (focused) colors.primary else Color.Transparent,
                    )
                    .clickable {
                        if (active) onOpen(item) else onActivate(index)
                    },
            ) {
                if (!item.posterUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.posterUrl,
                        contentDescription = item.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(posterAlpha),
                    )
                }

                if (hasBackdrop) {
                    AsyncImage(
                        model = item.backdropUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .alpha(backdropAlpha),
                    )
                }

                if (active) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.18f),
                                        Color.Black.copy(alpha = 0.88f),
                                    ),
                                ),
                            ),
                    )

                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth()
                            .padding(
                                horizontal = if (screenWidth >= 840) 28.dp else 18.dp,
                                vertical = 22.dp,
                            ),
                        verticalArrangement = Arrangement.spacedBy(7.dp),
                    ) {
                        Text(
                            text = item.title,
                            color = Color.White,
                            fontSize = if (screenWidth >= 840) 30.sp else 23.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )

                        val year = item.year?.toString()
                            ?: item.releaseDate?.take(4).orEmpty()

                        val metadata = listOfNotNull(
                            year.takeIf { it.isNotBlank() },
                            item.rating?.let { String.format(java.util.Locale.US, "%.1f", it) },
                        ).joinToString("  •  ")

                        if (metadata.isNotBlank()) {
                            Text(
                                text = metadata,
                                color = Color.White.copy(alpha = 0.88f),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }

                        if (!item.overview.isNullOrBlank()) {
                            Text(
                                text = item.overview,
                                color = Color.White.copy(alpha = 0.9f),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = if (screenWidth >= 840) 3 else 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}
