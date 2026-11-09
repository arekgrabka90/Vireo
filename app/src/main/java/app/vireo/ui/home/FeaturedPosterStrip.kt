package app.vireo.ui.home

import android.content.res.Configuration
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vireo.data.tmdb.TmdbCatalogItem
import app.vireo.data.tmdb.TmdbDetail
import app.vireo.domain.model.ContentType
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import java.util.Locale

@Composable
fun CinematicHero(
    items: List<TmdbCatalogItem>,
    activeIndex: Int,
    onActivate: (Int) -> Unit,
    onOpen: (TmdbCatalogItem) -> Unit,
    onHeroFocusChanged: (Boolean) -> Unit = {},
    details: Map<String, TmdbDetail> = emptyMap(),
) {
    if (items.isEmpty()) return

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    val isTelevision =
        (configuration.uiMode and Configuration.UI_MODE_TYPE_MASK) ==
            Configuration.UI_MODE_TYPE_TELEVISION

    val expandedLayout = shouldUseExpandedHomeHero(
        screenWidthDp = configuration.screenWidthDp,
        screenHeightDp = configuration.screenHeightDp,
        smallestScreenWidthDp = configuration.smallestScreenWidthDp,
        isTelevision = isTelevision,
    )

    val safeIndex = activeIndex.coerceIn(0, items.lastIndex)

    if (expandedLayout) {
        ExpandedCinematicHero(
            items = items.take(5),
            activeIndex = safeIndex.coerceAtMost(items.take(5).lastIndex),
            onActivate = onActivate,
            onOpen = onOpen,
            onHeroFocusChanged = onHeroFocusChanged,
            isTelevision = isTelevision,
            screenWidth = screenWidth,
            details = details,
        )
    } else {
        SinglePanelCinematicHero(
            items = items,
            activeIndex = safeIndex,
            onActivate = onActivate,
            onOpen = onOpen,
            onHeroFocusChanged = onHeroFocusChanged,
            details = details,
        )
    }
}

@Composable
private fun ExpandedCinematicHero(
    items: List<TmdbCatalogItem>,
    activeIndex: Int,
    onActivate: (Int) -> Unit,
    onOpen: (TmdbCatalogItem) -> Unit,
    onHeroFocusChanged: (Boolean) -> Unit,
    isTelevision: Boolean,
    screenWidth: Int,
    details: Map<String, TmdbDetail>,
) {
    if (items.isEmpty()) return

    val heroHeight = when {
        isTelevision -> 560.dp
        screenWidth >= 1200 -> 500.dp
        else -> 460.dp
    }

    val horizontalPadding = when {
        screenWidth >= 1600 -> 64.dp
        screenWidth >= 1200 -> 52.dp
        else -> 32.dp
    }

    val safeTop = WindowInsets.statusBars
        .asPaddingValues()
        .calculateTopPadding()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(heroHeight),
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(heroHeight - 62.dp)
                .padding(horizontal = horizontalPadding)
                .onFocusChanged { onHeroFocusChanged(it.hasFocus) }
                .focusGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items.forEachIndexed { index, item ->
                val selected = index == activeIndex
                val detail = details[homeHeroKey(item.type, item.tmdbId)]
                var focused by remember(item.type, item.tmdbId) {
                    mutableStateOf(false)
                }

                val animatedWeight by animateFloatAsState(
                    targetValue = if (selected) 5.4f else 1f,
                    animationSpec = spring(
                        dampingRatio = 0.86f,
                        stiffness = 170f,
                    ),
                    label = "home-hero-card-width",
                )

                val shape = RoundedCornerShape(22.dp)

                Box(
                    modifier = Modifier
                        .weight(animatedWeight)
                        .fillMaxHeight()
                        .clip(shape)
                        .background(Color(0xFF111318))
                        .onFocusChanged {
                            focused = it.isFocused
                        }
                        .clickable {
                            if (shouldOpenHomeHeroOnClick(selected)) {
                                onOpen(item)
                            } else {
                                onActivate(index)
                            }
                        },
                ) {
                    val backdropUrl = detail?.backdropUrl ?: item.backdropUrl
                    val posterUrl = detail?.posterUrl ?: item.posterUrl
                    val hasBackdrop = selected && !backdropUrl.isNullOrBlank()
                    HeroArtwork(
                        imageUrl = homeHeroArtworkUrl(
                            backdropUrl = backdropUrl,
                            posterUrl = posterUrl,
                            selected = selected,
                        ),
                        title = item.title,
                        contentScale = if (hasBackdrop) ContentScale.Crop else ContentScale.Fit,
                        modifier = Modifier.fillMaxSize(),
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0.0f to Color.Black.copy(alpha = 0.12f),
                                        0.34f to Color.Black.copy(alpha = 0.10f),
                                        0.68f to Color.Black.copy(alpha = if (selected) 0.42f else 0.18f),
                                        1.0f to Color.Black.copy(alpha = if (selected) 0.96f else 0.34f),
                                    ),
                                ),
                            ),
                    )

                    if (selected) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.horizontalGradient(
                                        colorStops = arrayOf(
                                            0.0f to Color.Black.copy(alpha = 0.38f),
                                            0.62f to Color.Transparent,
                                            1.0f to Color.Transparent,
                                        ),
                                    ),
                                ),
                        )

                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "POLECANE",
                                color = Color.White.copy(alpha = 0.82f),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 2.sp,
                            )

                            HeroTitleLogo(
                                title = item.title,
                                logoUrl = detail?.logoUrl,
                                maxHeight = if (screenWidth >= 1200) 92.dp else 78.dp,
                            )

                            Text(
                                text = heroMetadata(item, detail),
                                color = Color.White.copy(alpha = 0.96f),
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )

                            (detail?.overview ?: item.overview)
                                ?.trim()
                                ?.takeIf { it.isNotBlank() }
                                ?.let { overview ->
                                    Text(
                                        text = overview,
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = if (screenWidth >= 1200) 3 else 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(50))
                                .background(Color.Black.copy(alpha = 0.58f))
                                .padding(horizontal = 9.dp, vertical = 5.dp),
                        ) {
                            Text(
                                text = "${index + 1}",
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    if (focused) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .border(
                                    width = 3.dp,
                                    color = Color.White,
                                    shape = shape,
                                ),
                        )
                    }
                }
            }
        }

        Text(
            text = "VIREO",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(
                    start = horizontalPadding,
                    top = safeTop + 12.dp,
                ),
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 5.sp,
        )
    }
}

@Composable
private fun SinglePanelCinematicHero(
    items: List<TmdbCatalogItem>,
    activeIndex: Int,
    onActivate: (Int) -> Unit,
    onOpen: (TmdbCatalogItem) -> Unit,
    onHeroFocusChanged: (Boolean) -> Unit,
    details: Map<String, TmdbDetail>,
) {
    val configuration = LocalConfiguration.current
    val isLandscape =
        configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    // The phone panel keeps a fixed footprint; only its artwork and metadata change.
    val heroHeight = if (isLandscape) 360.dp else 430.dp
    val horizontalPadding = 24.dp
    val safeTop = WindowInsets.statusBars
        .asPaddingValues()
        .calculateTopPadding()
    val safeIndex = activeIndex.coerceIn(0, items.lastIndex)
    val item = items[safeIndex]
    val detail = details[homeHeroKey(item.type, item.tmdbId)]
    val backdropUrl = detail?.backdropUrl ?: item.backdropUrl
    val posterUrl = detail?.posterUrl ?: item.posterUrl
    val hasBackdrop = !backdropUrl.isNullOrBlank()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(heroHeight)
            .onFocusChanged { onHeroFocusChanged(it.hasFocus) }
            .background(Color.Black)
            .clickable { onOpen(item) },
    ) {
        HeroArtwork(
            imageUrl = homeHeroArtworkUrl(backdropUrl, posterUrl, selected = true),
            title = item.title,
            contentScale = if (hasBackdrop) ContentScale.Crop else ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0.0f to Color.Black.copy(alpha = 0.62f),
                            0.22f to Color.Black.copy(alpha = 0.36f),
                            0.48f to Color.Black.copy(alpha = 0.10f),
                            0.72f to Color.Black.copy(alpha = 0.64f),
                            1.0f to Color.Black.copy(alpha = 0.98f),
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
                            0.0f to Color.Black.copy(alpha = 0.42f),
                            0.52f to Color.Black.copy(alpha = 0.12f),
                            1.0f to Color.Transparent,
                        ),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(0.88f)
                .padding(
                    start = horizontalPadding,
                    end = 12.dp,
                    bottom = 26.dp,
                ),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Text(
                text = "POLECANE",
                color = Color.White.copy(alpha = 0.82f),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 2.sp,
            )

            HeroTitleLogo(
                title = item.title,
                logoUrl = detail?.logoUrl,
                maxHeight = 76.dp,
            )

            Text(
                text = heroMetadata(item, detail),
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            (detail?.overview ?: item.overview)
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.let { overview ->
                    Text(
                        text = overview,
                        color = Color.White.copy(alpha = 0.96f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

            Row(
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = 5.dp)
                    .focusGroup(),
            ) {
                items.take(5).forEachIndexed { index, _ ->
                    val selected = index == safeIndex
                    var focused by remember(index) {
                        mutableStateOf(false)
                    }
                    val indicatorShape = RoundedCornerShape(8.dp)

                    Box(
                        modifier = Modifier
                            .width(30.dp)
                            .height(26.dp)
                            .onFocusChanged { focused = it.isFocused }
                            .clip(indicatorShape)
                            .border(
                                width = if (focused) 2.dp else 0.dp,
                                color = if (focused) Color.White else Color.Transparent,
                                shape = indicatorShape,
                            )
                            .clickable { onActivate(index) },
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

        Text(
            text = "VIREO",
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 28.dp, top = safeTop + 16.dp),
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 5.sp,
        )
    }
}

@Composable
private fun HeroTitleLogo(
    title: String,
    logoUrl: String?,
    maxHeight: androidx.compose.ui.unit.Dp,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val logoWidth = maxWidth / 3f
        Crossfade(
            targetState = logoUrl?.takeIf { it.isNotBlank() },
            modifier = Modifier.fillMaxWidth(),
            animationSpec = tween(durationMillis = 480),
            label = "home-hero-title-logo",
        ) { visibleLogoUrl ->
            if (visibleLogoUrl != null) {
                val context = LocalContext.current
                val request = remember(visibleLogoUrl, context) {
                    ImageRequest.Builder(context)
                        .data(visibleLogoUrl)
                        .crossfade(500)
                        .build()
                }
                AsyncImage(
                    model = request,
                    contentDescription = "$title logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .width(logoWidth)
                        .height(maxHeight),
                )
            } else {
                Text(
                    text = title,
                    color = Color.White,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun HeroArtwork(
    imageUrl: String?,
    title: String,
    contentScale: ContentScale,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val request = remember(imageUrl, context) {
        imageUrl?.takeIf { it.isNotBlank() }?.let { url ->
            ImageRequest.Builder(context)
                .data(url)
                .memoryCacheKey("vireo-home-hero:$url")
                .crossfade(false)
                .build()
        }
    }
    var lastSuccessfulPainter by remember {
        mutableStateOf<Painter?>(null)
    }
    LaunchedEffect(imageUrl) {
        if (imageUrl.isNullOrBlank()) lastSuccessfulPainter = null
    }

    val painter = rememberAsyncImagePainter(
        model = request,
        onState = { state ->
            if (
                state is AsyncImagePainter.State.Success &&
                !imageUrl.isNullOrBlank() &&
                state.result.request.data == imageUrl &&
                lastSuccessfulPainter !== state.painter
            ) {
                lastSuccessfulPainter = state.painter
            }
        },
    )
    val visiblePainter = when (val state = painter.state) {
        is AsyncImagePainter.State.Success ->
            if (state.result.request.data == imageUrl) {
                state.painter
            } else {
                lastSuccessfulPainter ?: painter
            }
        else -> lastSuccessfulPainter ?: painter
    }

    Box(
        modifier = modifier.background(Color(0xFF17191D)),
        contentAlignment = Alignment.Center,
    ) {
        if (!imageUrl.isNullOrBlank()) {
            Crossfade(
                targetState = visiblePainter,
                animationSpec = tween(durationMillis = 360),
                label = "home-hero-artwork-crossfade",
            ) { loadedPainter ->
                Image(
                    painter = loadedPainter,
                    contentDescription = title,
                    contentScale = contentScale,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

private fun heroMetadata(
    item: TmdbCatalogItem,
    detail: TmdbDetail?,
): String {
    val year = detail?.year?.toString()
        ?: item.year?.toString()
        ?: item.releaseDate?.take(4).orEmpty()

    val ratingValue = detail?.rating ?: item.rating
    val rating = ratingValue
        ?.takeIf { it > 0.0 }
        ?.let { String.format(Locale.US, "%.1f", it) }

    val genres = detail?.genres
        .orEmpty()
        .asSequence()
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString(" / ")
        .takeIf { it.isNotBlank() }

    val kind = if (item.type == ContentType.MOVIE) "FILM" else "SERIAL"

    return listOfNotNull(
        kind,
        year.takeIf { it.isNotBlank() },
        genres,
        rating?.let { "★ $it" },
    ).joinToString("  •  ")
}
