package app.vireo.player.hud

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import coil.compose.AsyncImage

/**
 * The image primitive: natural aspect ratio (Fit), high filter quality, no background
 * (transparent PNGs stay transparent). Reports load failure so the caller can fall back to text.
 */
@Composable
internal fun HudLogo(
    url: String,
    contentDescription: String?,
    onFailed: () -> Unit,
    modifier: Modifier = Modifier,
    alignment: Alignment = Alignment.CenterStart,
) {
    AsyncImage(
        model = url,
        contentDescription = contentDescription,
        modifier = modifier,
        alignment = alignment,
        contentScale = ContentScale.Fit,
        filterQuality = FilterQuality.High,
        onError = { onFailed() },
    )
}

/**
 * Exactly one representation: the real logo OR the text title. Never both, never a poster/backdrop.
 */
@Composable
internal fun HudLogoOrTitle(
    logoUrl: String?,
    title: String,
    titleStyle: TextStyle,
    logoMaxWidth: Dp,
    logoMaxHeight: Dp,
    modifier: Modifier = Modifier,
    titleMaxLines: Int = 2,
    alignment: Alignment = Alignment.CenterStart,
    textAlign: TextAlign = TextAlign.Start,
) {
    var failed by remember(logoUrl) { mutableStateOf(false) }
    val usableUrl = logoUrl?.trim()?.takeIf { isUsableLogoUrl(it) }

    if (usableUrl != null && !failed) {
        HudLogo(
            url = usableUrl,
            contentDescription = title.ifBlank { null },
            onFailed = { failed = true },
            modifier = modifier.sizeIn(maxWidth = logoMaxWidth, maxHeight = logoMaxHeight),
            alignment = alignment,
        )
    } else if (title.isNotBlank()) {
        Text(
            text = title,
            style = titleStyle,
            textAlign = textAlign,
            maxLines = titleMaxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = modifier,
        )
    }
}
