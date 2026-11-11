package app.vireo.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import app.vireo.ui.components.FocusableBox
import app.vireo.ui.theme.VireoColors
import kotlinx.coroutines.delay

@Composable
fun PlayerHudOverlay(
    hudState: PlayerHudState,
    progress: PlayerProgressState,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeekBackward: () -> Unit,
    onSeekForward: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onVisibilityChanged: (Boolean) -> Unit,
) {
    var visible by remember { mutableStateOf(true) }
    var interactionTick by remember { mutableStateOf(0) }
    var hasFocusedControl by remember { mutableStateOf(false) }

    val registerInteraction: () -> Unit = {
        visible = true
        interactionTick++
    }
    val handleFocusChange: (Boolean) -> Unit = { focused ->
        hasFocusedControl = focused
        if (focused) visible = true
        interactionTick++
    }

    LaunchedEffect(visible) {
        onVisibilityChanged(visible)
    }

    LaunchedEffect(visible, hudState.isPlaying, interactionTick, hasFocusedControl) {
        if (visible && hudState.isPlaying && !hasFocusedControl) {
            delay(6_000L)
            visible = false
        }
    }

    val backdrop = when (val content = hudState.content) {
        is PlayerHudContent.Movie -> content.backdropUrl ?: content.posterUrl
        is PlayerHudContent.Series -> content.backdropUrl ?: content.posterUrl
        is PlayerHudContent.Live -> content.logoUrl
        is PlayerHudContent.Other -> content.posterUrl
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val containerMaxHeight = maxHeight
        val wide = maxWidth >= 850.dp
        val horizontal = if (wide) 56.dp else 24.dp
        val top = if (wide) 38.dp else 22.dp

        // Tapnięcie poza kontrolkami pokazuje lub ukrywa HUD.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                ) {
                    visible = !visible
                hasFocusedControl = false
                interactionTick++
                },
        )

        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 35 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 35 }),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                if (!backdrop.isNullOrBlank()) {
                    AsyncImage(
                        model = backdrop,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(if (wide) containerMaxHeight * 0.72f else containerMaxHeight * 0.62f)
                            .align(Alignment.TopCenter),
                        contentScale = ContentScale.Crop,
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Black.copy(alpha = 0.84f),
                                0.25f to Color.Black.copy(alpha = 0.42f),
                                0.55f to Color.Transparent,
                                0.78f to Color.Black.copy(alpha = 0.72f),
                                1f to Color.Black.copy(alpha = 0.96f),
                            )
                        ),
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .fillMaxWidth()
                        .padding(horizontal = horizontal, vertical = top),
                    verticalArrangement = Arrangement.spacedBy(if (wide) 18.dp else 10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        HudIconButton(
                            onClick = { registerInteraction(); onBack() },
                            description = "Back",
                            onFocusChange = handleFocusChange,
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(22.dp),
                            )
                        }

                        Spacer(Modifier.width(18.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = contentTitle(hudState.content),
                                color = Color.White,
                                fontSize = if (wide) 25.sp else 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = contentSubtitle(hudState.content),
                                color = Color.White.copy(alpha = 0.72f),
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        if (hudState.transportState == PlayerTransportState.BUFFERING) {
                            Text(
                                "BUFFERING",
                                color = Color.White.copy(alpha = 0.78f),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    ContentInformation(
                        content = hudState.content,
                        wide = wide,
                    )
                }

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = horizontal)
                        .padding(bottom = if (wide) 34.dp else 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            formatPlayerTime(progress.positionMs),
                            color = Color.White.copy(alpha = 0.78f),
                            style = MaterialTheme.typography.labelMedium,
                        )

                        Slider(
                            value = progress.positionMs
                                .coerceAtLeast(0L)
                                .toFloat()
                                .coerceAtMost(progress.durationMs.coerceAtLeast(1L).toFloat()),
                            onValueChange = { registerInteraction(); onSeekTo(it.toLong()) },
                            enabled = progress.isSeekable && progress.durationMs > 0L,
                            valueRange = 0f..progress.durationMs.coerceAtLeast(1L).toFloat(),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                                .onFocusChanged { handleFocusChange(it.hasFocus) },
                            colors = SliderDefaults.colors(
                                thumbColor = Color.White,
                                activeTrackColor = Color.White,
                                inactiveTrackColor = Color.White.copy(alpha = 0.28f),
                                disabledThumbColor = Color.White.copy(alpha = 0.35f),
                                disabledActiveTrackColor = Color.White.copy(alpha = 0.35f),
                            ),
                        )

                        Text(
                            formatPlayerTime(progress.durationMs),
                            color = Color.White.copy(alpha = 0.78f),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (hudState.hasPrevious) {
                            HudIconButton(onClick = { registerInteraction(); onPrevious() }, description = "Previous", onFocusChange = handleFocusChange) {
                                Text("‹", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        HudIconButton(onClick = { registerInteraction(); onSeekBackward() }, description = "Back 10 seconds", onFocusChange = handleFocusChange) {
                            Text("↶10", color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }

                        Spacer(Modifier.width(16.dp))

                        FocusableBox(
                            onClick = { registerInteraction(); onPlayPause() },
                            modifier = Modifier
                                .size(if (wide) 64.dp else 56.dp)
                                .clip(CircleShape)
                                .background(Color.White),
                            onFocusChange = handleFocusChange,
                        ) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(if (hudState.isPlaying) "Ⅱ" else "▶", color = Color.Black, style = MaterialTheme.typography.headlineMedium)
                            }
                        }

                        Spacer(Modifier.width(16.dp))

                        HudIconButton(onClick = { registerInteraction(); onSeekForward() }, description = "Forward 10 seconds", onFocusChange = handleFocusChange) {
                            Text("↷10", color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                        }

                        Spacer(Modifier.width(12.dp))

                        if (hudState.hasNext) {
                            HudIconButton(onClick = { registerInteraction(); onNext() }, description = "Next", onFocusChange = handleFocusChange) {
                                Text("›", color = Color.White, style = MaterialTheme.typography.headlineMedium)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ContentInformation(
    content: PlayerHudContent,
    wide: Boolean,
) {
    when (content) {
        is PlayerHudContent.Movie -> {
            Column(
                modifier = Modifier.fillMaxWidth(if (wide) 0.66f else 0.94f),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                MetadataLine(content.year, content.genre, content.duration, content.rating)
                content.description?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        it,
                        color = Color.White.copy(alpha = 0.84f),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = if (wide) 3 else 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }

        is PlayerHudContent.Series -> {
            Column(
                modifier = Modifier.fillMaxWidth(if (wide) 0.72f else 0.96f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MetadataLine(content.year, content.genre, null, content.rating)
                content.currentEpisode?.let { episode ->
                    Text(
                        "S${episode.seasonNumber.toString().padStart(2, '0')}  ·  E${episode.episodeNumber.toString().padStart(2, '0')}  ·  ${episode.title}",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                content.currentEpisodeDescription
                    ?.takeIf { it.isNotBlank() }
                    ?.let {
                        Text(
                            it,
                            color = Color.White.copy(alpha = 0.82f),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = if (wide) 2 else 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                if (content.episodes.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        content.episodes.take(if (wide) 5 else 3).forEach { episode ->
                            val current = episode == content.currentEpisode
                            Surface(
                                color = if (current) Color.White else Color.White.copy(alpha = 0.13f),
                                shape = RoundedCornerShape(9.dp),
                            ) {
                                Text(
                                    "S${episode.seasonNumber} E${episode.episodeNumber}",
                                    color = if (current) Color.Black else Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        is PlayerHudContent.Live -> {
            Column(
                modifier = Modifier.fillMaxWidth(if (wide) 0.72f else 0.96f),
                verticalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color.White,
                        shape = RoundedCornerShape(5.dp),
                    ) {
                        Text(
                            "LIVE",
                            color = Color.Black,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        content.currentProgram?.title ?: "Live television",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                content.currentProgramDescription
                    ?.takeIf { it.isNotBlank() }
                    ?.let {
                        Text(
                            it,
                            color = Color.White.copy(alpha = 0.82f),
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = if (wide) 3 else 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                content.nextProgram?.let {
                    Text(
                        "UP NEXT  ·  ${it.title}",
                        color = Color.White.copy(alpha = 0.68f),
                        style = MaterialTheme.typography.labelMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (content.recentChannels.isNotEmpty()) {
                    Text(
                        "RECENT CHANNELS",
                        color = Color.White.copy(alpha = 0.55f),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        content.recentChannels.take(if (wide) 5 else 3).forEach {
                            Surface(
                                color = Color.White.copy(alpha = 0.12f),
                                shape = RoundedCornerShape(8.dp),
                            ) {
                                Text(
                                    it.channelName,
                                    color = Color.White.copy(alpha = 0.9f),
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        is PlayerHudContent.Other -> Unit
    }
}

@Composable
private fun MetadataLine(
    year: String?,
    genre: String?,
    duration: String?,
    rating: String?,
) {
    val values = listOfNotNull(
        year?.takeIf { it.isNotBlank() },
        genre?.takeIf { it.isNotBlank() },
        duration?.takeIf { it.isNotBlank() },
        rating?.takeIf { it.isNotBlank() }?.let { "★ $it" },
    )

    if (values.isNotEmpty()) {
        Text(
            values.joinToString("   ·   "),
            color = Color.White.copy(alpha = 0.72f),
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HudIconButton(
    onClick: () -> Unit,
    description: String,
    onFocusChange: (Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    FocusableBox(
        onClick = onClick,
        onFocusChange = onFocusChange,
        modifier = Modifier
            .size(46.dp)
            .semantics { contentDescription = description },
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
    }
}

private fun contentTitle(content: PlayerHudContent): String = when (content) {
    is PlayerHudContent.Movie -> content.title
    is PlayerHudContent.Series -> content.title
    is PlayerHudContent.Live -> content.channelName
    is PlayerHudContent.Other -> content.title
}

private fun contentSubtitle(content: PlayerHudContent): String = when (content) {
    is PlayerHudContent.Movie -> listOfNotNull(content.year, content.genre).joinToString("  ·  ")
    is PlayerHudContent.Series -> "SERIES"
    is PlayerHudContent.Live -> "LIVE TELEVISION"
    is PlayerHudContent.Other -> "NOW PLAYING"
}

private fun formatPlayerTime(value: Long): String {
    val seconds = (value.coerceAtLeast(0L) / 1_000L)
    val hours = seconds / 3_600L
    val minutes = (seconds % 3_600L) / 60L
    val remaining = seconds % 60L

    return if (hours > 0L) {
        "%d:%02d:%02d".format(hours, minutes, remaining)
    } else {
        "%d:%02d".format(minutes, remaining)
    }
}
