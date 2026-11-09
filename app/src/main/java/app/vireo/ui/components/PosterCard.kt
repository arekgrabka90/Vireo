package app.vireo.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.vireo.domain.model.ContentType
import app.vireo.domain.model.Title
import coil.compose.AsyncImage

@Composable
fun PosterCard(
    title: Title,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    topProgress: Float? = null,
    belowPosterText: String? = null,
    isWatched: Boolean = false,
    imageUrl: String? = title.posterUrl,
    isLandscape: Boolean = false,
) {
    var focused by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = when {
            focused -> 1.045f
            hovered -> 1.035f
            pressed -> 0.985f
            else -> 1f
        },
        label = "poster-focus-scale",
    )

    val shape = RoundedCornerShape(16.dp)
    val colors = MaterialTheme.colorScheme
    val live = title.type == ContentType.LIVE
    val landscape = isLandscape || live

    Column(
        modifier = modifier.width(
            when {
                isLandscape -> 260.dp
                live -> 180.dp
                else -> 148.dp
            },
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(
                    if (landscape) 16f / 9f else 2f / 3f,
                )
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .hoverable(interactionSource)
                .onFocusChanged { focused = it.hasFocus }
                .clip(shape)
                .background(colors.surfaceVariant)
                .border(
                    width = if (focused || hovered) 2.dp else 0.dp,
                    color = if (focused || hovered) Color.White
                        else Color.Transparent,
                    shape = shape,
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ),
        ) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = title.name,
                    contentScale = if (live && !isLandscape) {
                        ContentScale.Fit
                    } else {
                        ContentScale.Crop
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            if (live && !isLandscape) 14.dp else 0.dp,
                        ),
                )
            } else {
                Text(
                    title.name.take(1).uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    color = colors.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center),
                )
            }

            if (isLandscape) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(62.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.72f),
                                ),
                            ),
                        ),
                )
            }

            if (topProgress != null) {
                LinearProgressIndicator(
                    progress = topProgress.coerceIn(0f, 1f),
                    modifier = Modifier
                        .align(
                            if (isLandscape) Alignment.BottomCenter
                            else Alignment.TopCenter,
                        )
                        .fillMaxWidth()
                        .height(4.dp),
                )
            }

            if (isWatched) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(7.dp),
                    color = Color.White.copy(alpha = 0.92f),
                    shape = RoundedCornerShape(7.dp),
                ) {
                    Text(
                        "✓",
                        color = Color(0xFF17191E),
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(
                            horizontal = 7.dp,
                            vertical = 3.dp,
                        ),
                    )
                }
            }
        }

        if (!belowPosterText.isNullOrBlank()) {
            Text(
                belowPosterText,
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
        } else {
            Spacer(Modifier.height(7.dp))
        }

        Text(
            title.name,
            style = MaterialTheme.typography.labelLarge,
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        val rating = title.rating?.takeIf {
            it.isNotBlank() && it != "0"
        }

        if (rating != null) {
            Text(
                "★ $rating",
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurfaceVariant,
            )
        }
    }
}
