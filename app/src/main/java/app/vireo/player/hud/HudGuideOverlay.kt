package app.vireo.player.hud

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.vireo.R
import app.vireo.domain.model.EpgProgram
import app.vireo.player.PlayerHudContent

@Composable
internal fun HudGuideOverlay(
    content: PlayerHudContent.Live,
    onClose: () -> Unit,
    onFocusChange: (Boolean) -> Unit,
) {
    val nowState by rememberWallClockMs()
    val timeFormat = rememberHudTimeFormat()
    val current: EpgProgram? = content.currentProgram?.takeIf {
        it.contains(nowState) && it.title.isNotBlank()
    }
    val next = content.nextProgram?.takeIf {
        it.hasValidTimeRange && it.startTimeMs > nowState && it.title.isNotBlank()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.12f)),
        contentAlignment = Alignment.CenterEnd,
    ) {
        Surface(
            modifier = Modifier
                .fillMaxHeight(0.84f)
                .widthIn(min = 280.dp, max = 520.dp)
                .padding(end = 20.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {},
                ),
            shape = RoundedCornerShape(22.dp),
            color = HudColors.Graphite.copy(alpha = 0.94f),
            border = BorderStroke(1.dp, HudColors.Hairline.copy(alpha = 0.24f)),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Text(stringResource(R.string.hud_tv_guide), style = HudType.overline(true))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HudLogoOrTitle(
                        logoUrl = content.logoUrl,
                        title = content.channelName,
                        titleStyle = HudType.title(true),
                        logoMaxWidth = 220.dp,
                        logoMaxHeight = HudDimens.TitleLogoMaxHeightCompact,
                    )
                    Spacer(Modifier.weight(1f))
                    HudIconButton(
                        onClick = onClose,
                        description = stringResource(R.string.hud_back),
                        onFocusChange = onFocusChange,
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = HudColors.TextPrimary,
                        )
                    }
                }

                current?.let { program ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.hud_current_program), style = HudType.overline(true))
                        Text(
                            program.title,
                            style = HudType.title(true),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            timeFormat.range(program.startTimeMs, program.endTimeMs),
                            style = HudType.meta(true),
                        )
                        program.description?.trim()?.takeIf { it.isNotEmpty() }?.let {
                            Text(
                                it,
                                style = HudType.body(true),
                                maxLines = 4,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }

                next?.let { program ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.hud_next_program), style = HudType.overline(true))
                        Text(
                            program.title,
                            style = HudType.episode(true),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            timeFormat.range(program.startTimeMs, program.endTimeMs),
                            style = HudType.meta(true),
                        )
                    }
                }
            }
        }
    }
}
