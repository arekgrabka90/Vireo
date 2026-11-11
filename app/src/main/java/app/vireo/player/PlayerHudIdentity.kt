package app.vireo.player

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.net.URI

@Composable
internal fun PlayerHudIdentity(
    content: PlayerHudContent,
    wide: Boolean,
    isBuffering: Boolean,
) {
    val title = when (content) {
        is PlayerHudContent.Movie -> content.title
        is PlayerHudContent.Series -> content.title
        is PlayerHudContent.Live -> content.channelName
        is PlayerHudContent.Other -> content.title
    }

    val logoUrl = when (content) {
        is PlayerHudContent.Movie -> content.logoUrl
        is PlayerHudContent.Series -> content.logoUrl
        is PlayerHudContent.Live -> content.logoUrl
        is PlayerHudContent.Other -> null
    }?.trim()?.takeIf { it.isNotBlank() }

    val isTmdbContent =
        content is PlayerHudContent.Movie || content is PlayerHudContent.Series

    val validLogo = remember(logoUrl) {
        runCatching {
            val uri = URI(logoUrl.orEmpty())
            (uri.scheme == "https" || uri.scheme == "http") &&
                !uri.host.isNullOrBlank()
        }.getOrDefault(false)
    }

    var imageFailed by remember(logoUrl) { mutableStateOf(false) }

    val animateLogo = isBuffering && isTmdbContent
    val transition = rememberInfiniteTransition(label = "player-hud-logo")

    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (animateLogo) 1.07f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "player-hud-logo-scale",
    )

    val alpha by transition.animateFloat(
        initialValue = 0.82f,
        targetValue = if (animateLogo) 1f else 0.82f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "player-hud-logo-alpha",
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        if (validLogo && !imageFailed) {
            AsyncImage(
                model = logoUrl,
                contentDescription = title,
                onError = { imageFailed = true },
                modifier = Modifier
                    .heightIn(max = if (wide) 56.dp else 46.dp)
                    .widthIn(max = if (wide) 240.dp else 180.dp)
                    .graphicsLayer {
                        scaleX = if (animateLogo) scale else 1f
                        scaleY = if (animateLogo) scale else 1f
                        this.alpha = if (animateLogo) alpha else 1f
                    },
                contentScale = ContentScale.Fit,
            )
        } else {
            if (title.isNotBlank()) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = if (wide) 25.sp else 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (animateLogo) {
                Spacer(Modifier.width(10.dp))
                CircularProgressIndicator(
                    modifier = Modifier.width(20.dp),
                    color = Color.White.copy(alpha = 0.82f),
                    strokeWidth = 2.dp,
                )
            }
        }
    }
}
