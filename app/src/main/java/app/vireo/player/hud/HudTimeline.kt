package app.vireo.player.hud

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import app.vireo.player.PlayerProgressState

/** Non-interactive line used for the Live programme progress. */
@Composable
internal fun HudProgressLine(
    fraction: Float,
    modifier: Modifier = Modifier,
    showThumb: Boolean = true,
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(HudDimens.ProgressLineHeight),
    ) {
        val stroke = HudDimens.ProgressLineThickness.toPx()
        val radius = HudDimens.ProgressThumbRadius.toPx()
        val centerY = size.height / 2f
        val playedX = size.width * fraction.coerceIn(0f, 1f)

        drawLine(
            color = HudColors.TimelineRemaining,
            start = Offset(0f, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        if (playedX > 0f) {
            drawLine(
                color = HudColors.Accent,
                start = Offset(0f, centerY),
                end = Offset(playedX, centerY),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
        if (showThumb) {
            drawCircle(
                color = HudColors.Accent,
                radius = radius,
                center = Offset(playedX.coerceIn(radius, (size.width - radius).coerceAtLeast(radius)), centerY),
            )
        }
    }
}

/**
 * VOD timeline: position — slider — duration. Call it only when the duration is known.
 * Keeps the existing Slider behaviour and focus reporting.
 */
@Composable
internal fun HudSeekTimeline(
    progress: PlayerProgressState,
    onSeekTo: (Long) -> Unit,
    onInteraction: () -> Unit,
    onFocusChange: (Boolean) -> Unit,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    val duration = progress.normalizedDurationMs
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = formatPlayerTime(progress.normalizedPositionMs), style = HudType.time(compact))

        Slider(
            value = progress.normalizedPositionMs.coerceAtMost(duration).toFloat(),
            onValueChange = {
                onInteraction()
                onSeekTo(it.toLong())
            },
            enabled = progress.isSeekable && duration > 0L,
            valueRange = 0f..duration.toFloat(),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
                .onFocusChanged { onFocusChange(it.hasFocus) },
            colors = SliderDefaults.colors(
                thumbColor = HudColors.Accent,
                activeTrackColor = HudColors.Accent,
                inactiveTrackColor = HudColors.TimelineRemaining,
                disabledThumbColor = HudColors.Accent.copy(alpha = 0.35f),
                disabledActiveTrackColor = HudColors.Accent.copy(alpha = 0.35f),
            ),
        )

        Text(text = formatPlayerTime(duration), style = HudType.time(compact))
    }
}
