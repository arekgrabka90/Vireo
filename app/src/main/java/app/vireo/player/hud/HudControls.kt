package app.vireo.player.hud

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.vireo.player.PlayerTransportState
import app.vireo.R
import androidx.compose.ui.res.stringResource
import app.vireo.ui.components.FocusableBox

@Composable
internal fun HudTopBar(
    transportState: PlayerTransportState,
    nowMs: Long,
    timeFormat: HudTimeFormat,
    compact: Boolean,
    onBack: () -> Unit,
    onFocusChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HudIconButton(onClick = onBack, description = stringResource(R.string.hud_back), onFocusChange = onFocusChange) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = HudColors.TextPrimary,
                modifier = Modifier.size(22.dp),
            )
        }

        Spacer(Modifier.weight(1f))

        // Real buffering state only.
        if (transportState == PlayerTransportState.BUFFERING) {
            CircularProgressIndicator(
                modifier = Modifier.size(HudDimens.SpinnerSize),
                strokeWidth = 2.dp,
                color = HudColors.TextSecondary,
                trackColor = Color.Transparent,
            )
            Spacer(Modifier.width(12.dp))
        }

        HudClock(nowMs = nowMs, timeFormat = timeFormat, compact = compact)
    }
}

@Composable
internal fun HudTransportRow(
    isPlaying: Boolean,
    hasPrevious: Boolean,
    hasNext: Boolean,
    showSeek: Boolean,
    compact: Boolean,
    onPlayPause: () -> Unit,
    onPrevious: () -> Unit,
    onSeekBackward: () -> Unit,
    onSeekForward: () -> Unit,
    onNext: () -> Unit,
    onInteraction: () -> Unit,
    onFocusChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val playPauseDescription = stringResource(
        if (isPlaying) R.string.hud_pause else R.string.hud_play,
    )
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (hasPrevious) {
            HudIconButton(
                onClick = { onInteraction(); onPrevious() },
                description = stringResource(R.string.hud_previous),
                onFocusChange = onFocusChange,
            ) { Text("‹", color = HudColors.TextPrimary, fontSize = 28.sp) }
            Spacer(Modifier.width(12.dp))
        }

        if (showSeek) {
            HudIconButton(
                onClick = { onInteraction(); onSeekBackward() },
                description = stringResource(R.string.hud_seek_backward),
                onFocusChange = onFocusChange,
            ) {
                Text(stringResource(R.string.hud_seek_backward_label), color = HudColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(16.dp))
        }

        FocusableBox(
            onClick = { onInteraction(); onPlayPause() },
            modifier = Modifier
                .size(if (compact) 52.dp else 60.dp)
                .semantics { contentDescription = playPauseDescription },
            onFocusChange = onFocusChange,
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                HudPlayPauseGlyph(isPlaying = isPlaying, tint = HudColors.TextPrimary)
            }
        }

        if (showSeek) {
            Spacer(Modifier.width(16.dp))
            HudIconButton(
                onClick = { onInteraction(); onSeekForward() },
                description = stringResource(R.string.hud_seek_forward),
                onFocusChange = onFocusChange,
            ) {
                Text(stringResource(R.string.hud_seek_forward_label), color = HudColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (hasNext) {
            Spacer(Modifier.width(12.dp))
            HudIconButton(
                onClick = { onInteraction(); onNext() },
                description = stringResource(R.string.hud_next),
                onFocusChange = onFocusChange,
            ) { Text("›", color = HudColors.TextPrimary, fontSize = 28.sp) }
        }
    }
}

/** Vector-drawn glyph (material-icons-core has no Pause icon and we do not add the extended set). */
@Composable
internal fun HudPlayPauseGlyph(
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    tint: Color = Color.Black,
) {
    Canvas(modifier = modifier.size(24.dp)) {
        if (isPlaying) {
            val barWidth = size.width * 0.26f
            val gap = size.width * 0.16f
            val left = (size.width - 2 * barWidth - gap) / 2f
            val top = size.height * 0.12f
            val barHeight = size.height * 0.76f
            val radius = CornerRadius(barWidth * 0.25f)
            drawRoundRect(tint, Offset(left, top), Size(barWidth, barHeight), radius)
            drawRoundRect(tint, Offset(left + barWidth + gap, top), Size(barWidth, barHeight), radius)
        } else {
            val path = Path().apply {
                moveTo(size.width * 0.24f, size.height * 0.10f)
                lineTo(size.width * 0.90f, size.height * 0.50f)
                lineTo(size.width * 0.24f, size.height * 0.90f)
                close()
            }
            drawPath(path, tint)
        }
    }
}

@Composable
internal fun HudIconButton(
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
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            content()
        }
    }
}
