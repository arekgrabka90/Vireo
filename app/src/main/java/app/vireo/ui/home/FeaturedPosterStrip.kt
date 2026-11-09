package app.vireo.ui.home

import android.content.res.Configuration
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vireo.data.tmdb.TmdbCatalogItem
import app.vireo.domain.model.ContentType
import coil.compose.AsyncImage
import java.util.Locale

@Composable
fun CinematicHero(
    items: List<TmdbCatalogItem>,
    activeIndex: Int,
    onActivate: (Int) -> Unit,
    onOpen: (TmdbCatalogItem) -> Unit,
) {
    if (items.isEmpty()) return

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val isTelevision =
        (configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) ==
            Configuration.UI_MODE_TYPE_TELEVISION

    val heroHeight = when {
        isTelevision -> 600.dp
        screenWidth >= 1200 -> 560.dp
        screenWidth >= 840 -> 540.dp
        screenWidth >= 600 -> 500.dp
        else -> 430.dp
    }

    val horizontalPadding = if (screenWidth >= 840) 52.dp else 24.dp
    val safeTop = WindowInsets.statusBars
        .asPaddingValues()
        .calculateTopPadding()

    val safeIndex = activeIndex.coerceIn(0, items.lastIndex)
    val activeItem = items[safeIndex]

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(heroHeight)
            .background(Color.Black)
            .clickable { onOpen(activeItem) },
    ) {
        Crossfade(
            targetState = activeItem,
            modifier = Modifier.fillMaxSize(),
            animationSpec = tween(durationMillis = 700),
            label = "home-cinematic-hero",
        ) { item ->
            Box(modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = item.backdropUrl
                        ?.takeIf { it.isNotBlank() }
                        ?: item.posterUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    alignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize(),
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Black.copy(alpha = 0.68f),
                                    0.20f to Color.Black.copy(alpha = 0.30f),
                                    0.48f to Color.Transparent,
                                    1.0f to Color.Black.copy(alpha = 0.97f),
                                ),
                            ),
                        ),
                )

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Black.copy(alpha = 0.34f),
                                    0.48f to Color.Transparent,
                                    1.0f to Color.Transparent,
                                ),
                            ),
                        ),
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .fillMaxWidth(0.86f)
                        .padding(
                            start = horizontalPadding,
                            end = 12.dp,
                            bottom = if (screenWidth >= 840) 38.dp else 28.dp,
                        ),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "POLECANE",
                        color = Color.White.copy(alpha = 0.78f),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 2.sp,
                    )

                    Text(
                        text = item.title,
                        color = Color.White,
                        style = if (screenWidth >= 840) {
                            MaterialTheme.typography.displaySmall
                        } else {
                            MaterialTheme.typography.headlineLarge
                        },
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    val year = item.year?.toString()
                        ?: item.releaseDate?.take(4).orEmpty()

                    val rating = item.rating?.let {
                        String.format(Locale.US, "%.1f", it)
                    }

                    val kind = if (item.type == ContentType.MOVIE) {
                        "FILM"
                    } else {
                        "SERIAL"
                    }

                    val metadata = listOfNotNull(
                        kind,
                        year.takeIf { it.isNotBlank() },
                        rating?.let { "★ $it" },
                    ).joinToString("  •  ")

                    Text(
                        text = metadata,
                        color = Color.White.copy(alpha = 0.88f),
                        style = MaterialTheme.typography.labelLarge,
                    )

                    item.overview
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                        ?.let { overview ->
                            Text(
                                text = overview,
                                color = Color.White.copy(alpha = 0.90f),
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = if (screenWidth >= 840) 3 else 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 5.dp),
                    ) {
                        items.take(5).forEachIndexed { index, _ ->
                            var focused by remember(index) {
                                mutableStateOf(false)
                            }

                            val selected = index == safeIndex
                            val indicatorShape = RoundedCornerShape(8.dp)

                            Box(
                                modifier = Modifier
                                    .width(30.dp)
                                    .height(26.dp)
                                    .onFocusChanged {
                                        focused = it.isFocused
                                    }
                                    .clip(indicatorShape)
                                    .border(
                                        width = if (focused) 1.dp else 0.dp,
                                        color = if (focused) Color.White
                                            else Color.Transparent,
                                        shape = indicatorShape,
                                    )
                                    .clickable {
                                        onActivate(index)
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(if (selected) 22.dp else 7.dp)
                                        .height(3.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (selected) Color.White
                                            else Color.White.copy(alpha = 0.52f),
                                        ),
                                )
                            }
                        }
                    }
                }
            }
        }

        Text(
            text = "VIREO",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(
                    start = 28.dp,
                    top = safeTop + 16.dp,
                ),
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 5.sp,
        )
    }
}
