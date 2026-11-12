package app.vireo.player.hud

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.vireo.R
import androidx.compose.ui.res.stringResource
import app.vireo.player.PlayerHudContent
import app.vireo.player.RecentLiveChannel

internal sealed interface HudTile {
    val key: String

    data object Guide : HudTile {
        override val key: String = "guide"
    }

    data class Channel(val channel: RecentLiveChannel) : HudTile {
        override val key: String = "${channel.sourceId}:${channel.streamId}"
    }
}

/**
 * Pure and testable. A tile exists only when its action exists and its data is real:
 * - Guide tile only with an onOpenGuide callback;
 * - channel tiles only with an onSelectRecentChannel callback, excluding the channel playing now.
 */
internal fun buildHudTiles(
    content: PlayerHudContent.Live,
    hasSelectChannelAction: Boolean,
    hasGuideAction: Boolean,
): List<HudTile> = buildList {
    if (hasGuideAction) add(HudTile.Guide)
    if (hasSelectChannelAction && content.sourceId != null) {
        content.recentChannels
            .filter { it.sourceId == content.sourceId && it.streamId != content.streamId }
            .distinctBy { it.sourceId to it.streamId }
            .forEach { add(HudTile.Channel(it)) }
    }
}

@Composable
internal fun HudTileRow(
    tiles: List<HudTile>,
    onOpenGuide: (() -> Unit)?,
    onSelectChannel: ((RecentLiveChannel) -> Unit)?,
    onInteraction: () -> Unit,
    onFocusChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (tiles.isEmpty()) return

    LazyRow(
        modifier = modifier.fillMaxWidth(),
        // Vertical/horizontal room so the focused (scaled) tile is never clipped.
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(HudSpacing.Tile),
    ) {
        items(items = tiles, key = { it.key }) { tile ->
            when (tile) {
                HudTile.Guide -> HudTileSurface(
                    description = stringResource(R.string.hud_guide),
                    onClick = { onInteraction(); onOpenGuide?.invoke() },
                    onFocusChange = onFocusChange,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("≡", color = HudColors.TextPrimary, style = HudType.title(true))
                        Text(text = stringResource(R.string.hud_guide), style = HudType.tile(), maxLines = 1)
                    }
                }

                is HudTile.Channel -> HudTileSurface(
                    description = tile.channel.channelName,
                    onClick = { onInteraction(); onSelectChannel?.invoke(tile.channel) },
                    onFocusChange = onFocusChange,
                ) {
                    // Logo OR name, never both.
                    HudLogoOrTitle(
                        logoUrl = tile.channel.logoUrl,
                        title = tile.channel.channelName,
                        titleStyle = HudType.tile(),
                        logoMaxWidth = HudDimens.TileLogoMaxWidth,
                        logoMaxHeight = HudDimens.TileLogoMaxHeight,
                        alignment = Alignment.Center,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun HudTileSurface(
    description: String,
    onClick: () -> Unit,
    onFocusChange: (Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)
    val scale by animateFloatAsState(
        targetValue = if (focused) 1.06f else 1f,
        animationSpec = tween(HudMotion.FocusMs),
        label = "hudTileScale",
    )
    val container by animateColorAsState(
        targetValue = if (focused) HudColors.GraphiteFocused else HudColors.Graphite,
        animationSpec = tween(HudMotion.FocusMs),
        label = "hudTileColor",
    )

    Box(
        modifier = Modifier
            .size(HudDimens.TileWidth, HudDimens.TileHeight)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(shape)
            .background(container)
            .then(if (focused) Modifier.border(1.5.dp, HudColors.Hairline, shape) else Modifier)
            // onFocusChanged must be placed BEFORE the focusable modifier (clickable) it observes.
            .onFocusChanged {
                focused = it.isFocused
                onFocusChange(it.isFocused)
            }
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}
