package app.vireo.player.hud

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import app.vireo.R
import app.vireo.domain.model.EpgProgram
import app.vireo.player.PlayerHudContent

@Composable
internal fun HudInfoBlock(
    content: PlayerHudContent,
    nowMs: Long,
    timeFormat: HudTimeFormat,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    when (content) {
        is PlayerHudContent.Live -> LiveInfoBlock(content, nowMs, timeFormat, compact, modifier)
        is PlayerHudContent.Movie -> MovieInfoBlock(content, compact, modifier)
        is PlayerHudContent.Series -> SeriesInfoBlock(content, compact, modifier)
        is PlayerHudContent.Other -> OtherInfoBlock(content, compact, modifier)
    }
}

@Composable
private fun LiveInfoBlock(
    content: PlayerHudContent.Live,
    nowMs: Long,
    timeFormat: HudTimeFormat,
    compact: Boolean,
    modifier: Modifier,
) {
    var logoFailed by remember(content.logoUrl) { mutableStateOf(false) }
    val logoUrl = content.logoUrl?.trim()?.takeIf { isUsableLogoUrl(it) }
    val showLogo = logoUrl != null && !logoFailed

    val channelName = content.channelName.trim()
    val program: EpgProgram? = content.currentProgram?.takeIf { it.contains(nowMs) && it.title.isNotBlank() }
    val programTitle = program?.title?.trim().orEmpty()
    val rangeLine = program
        ?.takeIf { it.hasValidTimeRange }
        ?.let { p ->
            listOfNotNull(
                timeFormat.range(p.startTimeMs, p.endTimeMs),
                p.remainingMinutesAt(nowMs)?.let { stringResource(R.string.hud_minutes_left, it) },
            ).joinToString("  ·  ")
        }
    val description = program?.description?.trim()?.takeIf { it.isNotEmpty() }
    val next = content.nextProgram?.takeIf { it.title.isNotBlank() && it.hasValidTimeRange && it.startTimeMs > nowMs }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (showLogo) {
            // Fixed slot: no layout shift while the logo loads. No background: transparent PNGs stay clean.
            Box(
                modifier = Modifier.size(if (compact) 72.dp else HudDimens.IdentitySlot),
                contentAlignment = Alignment.Center,
            ) {
                HudLogo(
                    url = logoUrl!!,
                    contentDescription = channelName.ifEmpty { null },
                    onFailed = { logoFailed = true },
                    modifier = Modifier.sizeIn(
                        maxWidth = if (compact) 72.dp else HudDimens.IdentitySlot,
                        maxHeight = if (compact) 72.dp else HudDimens.IdentitySlot,
                    ),
                    alignment = Alignment.Center,
                )
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(HudSpacing.Small),
        ) {
            when {
                programTitle.isNotEmpty() -> {
                    // Channel name as text only when there is no logo (never both).
                    if (!showLogo && channelName.isNotEmpty()) {
                        Text(
                            text = channelName.uppercase(),
                            style = HudType.overline(compact),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        text = programTitle,
                        style = HudType.title(compact),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                !showLogo && channelName.isNotEmpty() -> Text(
                    text = channelName,
                    style = HudType.title(compact),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            rangeLine?.let { Text(text = it, style = HudType.meta(compact), maxLines = 1) }
            description?.let { HudDescription(it, compact, maxLines = 3) }

            next?.let {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = timeFormat.range(it.startTimeMs, it.endTimeMs),
                        style = HudType.next(compact).copy(color = HudColors.TextTertiary),
                        maxLines = 1,
                    )
                    Text(
                        text = it.title.trim(),
                        style = HudType.next(compact),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
            }
        }
    }
}

@Composable
private fun MovieInfoBlock(
    content: PlayerHudContent.Movie,
    compact: Boolean,
    modifier: Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(if (compact) 0.96f else 0.7f),
        verticalArrangement = Arrangement.spacedBy(HudSpacing.Small),
    ) {
        HudTitleOrLogo(content.logoUrl, content.title, compact)
        joinMetadata(content.year, content.genre, content.duration, content.rating)
            ?.let { Text(text = it, style = HudType.meta(compact), maxLines = 1, overflow = TextOverflow.Ellipsis) }
        content.description?.trim()?.takeIf { it.isNotEmpty() }
            ?.let { HudDescription(it, compact, maxLines = 3) }
    }
}

@Composable
private fun SeriesInfoBlock(
    content: PlayerHudContent.Series,
    compact: Boolean,
    modifier: Modifier,
) {
    val episode = content.currentEpisode
    val episodeLabel = formatEpisodeLabel(
        content.currentSeasonNumber ?: episode?.seasonNumber,
        content.currentEpisodeNumber ?: episode?.episodeNumber,
        content.currentEpisodeTitle ?: episode?.title,
    )
    val metadata = joinMetadata(content.year, content.genre, episode?.duration, content.rating)
    val description = content.currentEpisodeDescription?.trim()?.takeIf { it.isNotEmpty() && !content.descriptionIsSeriesFallback }

    Column(
        modifier = modifier.fillMaxWidth(if (compact) 0.96f else 0.7f),
        verticalArrangement = Arrangement.spacedBy(HudSpacing.Small),
    ) {
        HudTitleOrLogo(content.logoUrl, content.title, compact)
        episodeLabel?.let {
            Text(text = it, style = HudType.episode(compact), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        metadata?.let {
            Text(text = it, style = HudType.meta(compact), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        description?.let { HudDescription(it, compact, maxLines = if (compact) 2 else 3) }
    }
}

@Composable
private fun OtherInfoBlock(
    content: PlayerHudContent.Other,
    compact: Boolean,
    modifier: Modifier,
) {
    if (content.title.isBlank()) return
    Column(modifier = modifier.fillMaxWidth(if (compact) 0.96f else 0.7f)) {
        Text(
            text = content.title,
            style = HudType.title(compact),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HudTitleOrLogo(logoUrl: String?, title: String, compact: Boolean) {
    HudLogoOrTitle(
        logoUrl = logoUrl,
        title = title,
        titleStyle = HudType.title(compact),
        logoMaxWidth = HudDimens.TitleLogoMaxWidth,
        logoMaxHeight = if (compact) HudDimens.TitleLogoMaxHeightCompact else HudDimens.TitleLogoMaxHeight,
    )
}

@Composable
private fun HudDescription(text: String, compact: Boolean, maxLines: Int) {
    Text(
        text = text,
        style = HudType.body(compact),
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}
