package app.vireo.ui.home
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.FastOutSlowInEasing

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
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
import androidx.compose.ui.zIndex
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

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(heroHeight),
    ) {
        val contentWidth = maxWidth - horizontalPadding * 2
        val activeWidth = contentWidth * homeHeroPosterWidthFraction(0)
        val cardAreaHeight = heroHeight - 62.dp

        // Side posters use a fixed portrait aspect ratio of 2:3.
        val baseNearHeight =
            cardAreaHeight * homeHeroPosterHeightFraction(1)
        val baseFarHeight =
            cardAreaHeight * homeHeroPosterHeightFraction(2)

        val baseNearWidth = baseNearHeight * (2f / 3f)
        val baseFarWidth = baseFarHeight * (2f / 3f)

        // Keep the entire fan inside the horizontal content margins.
        val sideAllowance = (contentWidth - activeWidth) / 2
        val requiredSideWidth =
            (baseNearWidth + baseFarWidth) * 0.72f

        val sideScale = if (
            requiredSideWidth > sideAllowance &&
            requiredSideWidth > 0.dp
        ) {
            (sideAllowance / requiredSideWidth).coerceIn(0f, 1f)
        } else {
            1f
        }

        val nearHeight = baseNearHeight * sideScale
        val farHeight = baseFarHeight * sideScale
        val nearWidth = nearHeight * (2f / 3f)
        val farWidth = farHeight * (2f / 3f)

        val activeLeft = (maxWidth - activeWidth) / 2

        // Symmetrical layered fan with a visible margin on both sides.
        val nearLeft = activeLeft - nearWidth * 0.72f
        val farLeft = nearLeft - farWidth * 0.72f
        val nearRight = activeLeft + activeWidth - nearWidth * 0.28f
        val farRight =
            activeLeft + activeWidth + nearWidth * 0.72f -
                farWidth * 0.28f

        // Draw distant posters first, then near posters, and the active Hero last.
        val slots = listOf(-2, 2, -1, 1, 0)
            .map { relative ->
                val index = (activeIndex + relative + items.size) % items.size
                Triple(index, relative, items[index])
            }
            .distinctBy { it.first }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .onFocusChanged { onHeroFocusChanged(it.hasFocus) }
                .focusGroup()
                .pointerInput(items, activeIndex) {
                    var dragDistance = 0f
                    var gestureHandled = false
                    val swipeThreshold = 56.dp.toPx()

                    detectHorizontalDragGestures(
                        onDragStart = {
                            dragDistance = 0f
                            gestureHandled = false
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            dragDistance += dragAmount
                            change.consume()

                            if (
                                items.size > 1 &&
                                !gestureHandled &&
                                kotlin.math.abs(dragDistance) >= swipeThreshold
                            ) {
                                val direction =
                                    if (dragDistance < 0f) 1 else -1

                                val nextIndex =
                                    (activeIndex + direction + items.size) %
                                        items.size

                                onActivate(nextIndex)
                                gestureHandled = true
                            }
                        },
                    )
                },
        ) {
            slots.forEach { (index, relative, item) ->
                val distance = kotlin.math.abs(relative)
                val selected = relative == 0
                val detail = details[homeHeroKey(item.type, item.tmdbId)]
                val focusedState = remember(item.type, item.tmdbId) {
                    mutableStateOf(false)
                }
                val focused = focusedState.value
                val cardHeight = when (distance) {
                    0 -> cardAreaHeight
                    1 -> nearHeight
                    else -> farHeight
                }

                val cardWidth = if (selected) {
                    activeWidth
                } else {
                    cardHeight * (2f / 3f)
                }

                val left = when (relative) {
                    -2 -> farLeft
                    -1 -> nearLeft
                    0 -> activeLeft
                    1 -> nearRight
                    else -> farRight
                }

                val top = 62.dp + (cardAreaHeight - cardHeight) / 2
                val shape = RoundedCornerShape(if (selected) 22.dp else 16.dp)

                // ARVIO-style smooth movement between carousel positions.
                val animationSpec = tween<Dp>(
                    durationMillis = 900,
                    easing = FastOutSlowInEasing,
                )

                val animatedLeft by animateDpAsState(
                    targetValue = left,
                    animationSpec = animationSpec,
                    label = "hero-card-left",
                )
                val animatedTop by animateDpAsState(
                    targetValue = top,
                    animationSpec = animationSpec,
                    label = "hero-card-top",
                )
                val animatedWidth by animateDpAsState(
                    targetValue = cardWidth,
                    animationSpec = animationSpec,
                    label = "hero-card-width",
                )
                val animatedHeight by animateDpAsState(
                    targetValue = cardHeight,
                    animationSpec = animationSpec,
                    label = "hero-card-height",
                )

                val targetElevation = when (distance) {
                    0 -> 20.dp
                    1 -> 14.dp
                    else -> 9.dp
                }

                val animatedElevation by animateDpAsState(
                    targetValue = targetElevation,
                    animationSpec = animationSpec,
                    label = "hero-card-elevation",
                )

                Box(
                    modifier = Modifier
                        .shadow(
                            elevation = animatedElevation,
                            shape = shape,
                            clip = false,
                        )
                        .offset(x = animatedLeft, y = animatedTop)
                        .width(animatedWidth)
                        .height(animatedHeight)
                        .zIndex(
                            when (distance) {
                                0 -> 5f
                                1 -> 3f
                                else -> 1f
                            } + if (relative > 0) 0.1f else 0f
                        )
                        .clip(shape)
                        .background(Color(0xFF111318))
                        .onFocusChanged { focusedState.value = it.isFocused }
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
                        contentScale = if (hasBackdrop) {
                            ContentScale.Crop
                        } else {
                            ContentScale.Fit
                        },
                        backgroundColor = if (selected) {
                            Color(0xFF17191D)
                        } else {
                            Color(0xFF08090B)
                        },
                        modifier = Modifier.fillMaxSize(),
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colorStops = arrayOf(
                                        0.0f to Color.Black.copy(
                                            alpha = if (selected) 0.12f else 0.18f
                                        ),
                                        0.45f to Color.Black.copy(
                                            alpha = if (selected) 0.10f else 0.12f
                                        ),
                                        1.0f to Color.Black.copy(
                                            alpha = if (selected) 0.96f else 0.30f
                                        ),
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

                            HeroMetadataChips(item, detail)

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
                                .padding(10.dp)
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
    onOpen: (TmdbCatalogItem) -> Unit,
    onHeroFocusChanged: (Boolean) -> Unit,
    details: Map<String, TmdbDetail>,
) {
    if (items.isEmpty()) return

    // Fixed phone Hero footprint: unchanged between portrait and landscape.
    val heroWidth = 360.dp
    val heroHeight = 240.dp
    val shape = RoundedCornerShape(24.dp)

    val safeIndex = activeIndex.coerceIn(0, items.lastIndex)
    val item = items[safeIndex]
    val detail = details[homeHeroKey(item.type, item.tmdbId)]

    val backdropUrl = detail?.backdropUrl ?: item.backdropUrl
    val posterUrl = detail?.posterUrl ?: item.posterUrl
    val hasBackdrop = !backdropUrl.isNullOrBlank()

    var focused by remember(item.type, item.tmdbId) {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .width(heroWidth)
                .height(heroHeight)
                .clip(shape)
                .background(Color(0xFF111318))
                .border(
                    width = if (focused) 3.dp else 1.dp,
                    color = if (focused) {
                        Color.White
                    } else {
                        Color.White.copy(alpha = 0.16f)
                    },
                    shape = shape,
                )
                .onFocusChanged {
                    focused = it.isFocused
                    onHeroFocusChanged(it.hasFocus)
                }
                .clickable {
                    onOpen(item)
                },
        ) {
            HeroArtwork(
                imageUrl = homeHeroArtworkUrl(
                    backdropUrl,
                    posterUrl,
                    selected = true,
                ),
                title = item.title,
                contentScale = if (hasBackdrop) {
                    ContentScale.Crop
                } else {
                    ContentScale.Fit
                },
                modifier = Modifier.fillMaxSize(),
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.0f to Color.Black.copy(alpha = 0.16f),
                                0.38f to Color.Black.copy(alpha = 0.08f),
                                0.68f to Color.Black.copy(alpha = 0.62f),
                                1.0f to Color.Black.copy(alpha = 0.96f),
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
                                0.0f to Color.Black.copy(alpha = 0.30f),
                                0.58f to Color.Transparent,
                                1.0f to Color.Transparent,
                            ),
                        ),
                    ),
            )

            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp),
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
                    maxHeight = 64.dp,
                )

                HeroMetadataChips(item, detail)
            }
        }

        // The overview deliberately remains outside the rounded Hero panel.
        (detail?.overview ?: item.overview)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { overview ->
                Text(
                    text = overview,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    color = MaterialTheme.colorScheme.onBackground,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                )
            }
    }
}

@Composable
private fun HeroTitleLogo(
    title: String,
    logoUrl: String?,
    maxHeight: androidx.compose.ui.unit.Dp,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        // Keep the source image aspect ratio; only apply a generous safety bound.
        val logoMaxWidth = maxWidth * 0.72f

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
                        .height(maxHeight)
                        .widthIn(max = logoMaxWidth)
                        .wrapContentWidth(Alignment.Start),
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
    backgroundColor: Color = Color(0xFF17191D),
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
        modifier = modifier.background(backgroundColor),
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

@Composable
private fun HeroMetadataChips(
    item: TmdbCatalogItem,
    detail: TmdbDetail?,
) {
    val values = homeHeroMetadataValues(item, detail)
    if (values.isEmpty()) return

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        values.forEach { value ->
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.50f))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    text = value,
                    color = Color(0xFF111318),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
