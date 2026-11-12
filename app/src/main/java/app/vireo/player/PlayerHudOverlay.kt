package app.vireo.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import app.vireo.R
import app.vireo.ui.components.FocusableBox
import app.vireo.player.hud.HudGuideOverlay
import app.vireo.player.hud.HudBottomScrim
import app.vireo.player.hud.HudInfoBlock
import app.vireo.player.hud.HudMotion
import app.vireo.player.hud.HudProgressLine
import app.vireo.player.hud.HudSeekTimeline
import app.vireo.player.hud.HudSpacing
import app.vireo.player.hud.HudTileRow
import app.vireo.player.hud.HudTopBar
import app.vireo.player.hud.HudTopScrim
import app.vireo.player.hud.HudTransportRow
import app.vireo.player.hud.buildHudTiles
import app.vireo.player.hud.progressAt
import app.vireo.player.hud.rememberHudTimeFormat
import app.vireo.player.hud.rememberWallClockMs
import kotlinx.coroutines.delay

/**
 * Transparent HUD drawn above the real video surface.
 * It never draws a poster, backdrop or any full-screen image: legibility comes from edge scrims only.
 *
 * The first ten parameters keep the original contract, so PlayerScreen does not have to change.
 */
@Composable
@Suppress("UNUSED_PARAMETER")
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
    onSelectEpisode: (Int, Int) -> Unit,
    onCancelNextEpisodeCountdown: () -> Unit,
    onVisibilityChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onSelectRecentChannel: ((RecentLiveChannel) -> Unit)? = null,
    onOpenGuide: (() -> Unit)? = null,
) {
    // ---- visibility state machine: unchanged behaviour ----
    var visible by remember { mutableStateOf(true) }
    var interactionTick by remember { mutableIntStateOf(0) }
    var hasFocusedControl by remember { mutableStateOf(false) }
    var guideVisible by remember { mutableStateOf(false) }

    val registerInteraction: () -> Unit = {
        visible = true
        interactionTick++
    }
    val handleFocusChange: (Boolean) -> Unit = { focused ->
        hasFocusedControl = focused
        if (focused) visible = true
        interactionTick++
    }

    BackHandler(enabled = guideVisible) {
        guideVisible = false
        registerInteraction()
    }

    LaunchedEffect(visible) { onVisibilityChanged(visible) }

    LaunchedEffect(visible, hudState.isPlaying, interactionTick, hasFocusedControl, guideVisible) {
        if (visible && hudState.isPlaying && !hasFocusedControl && !guideVisible) {
            delay(6_000L)
            visible = false
        }
    }
    // ---------------------------------------------------------

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val compact = maxWidth < 850.dp || maxHeight < 520.dp
        val horizontal = if (compact) HudSpacing.ScreenHorizontalCompact else HudSpacing.ScreenHorizontal
        val top = if (compact) HudSpacing.ScreenTopCompact else HudSpacing.ScreenTop
        val bottom = if (compact) HudSpacing.ScreenBottomCompact else HudSpacing.ScreenBottom
        val screenHeight = maxHeight

        // Tap outside the controls toggles the HUD (unchanged). Transparent: the video stays visible.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) {
                    if (guideVisible) {
                        guideVisible = false
                        visible = true
                    } else {
                        visible = !visible
                    }
                    hasFocusedControl = false
                    interactionTick++
                },
        )

        AnimatedVisibility(
            visible = visible,
            modifier = Modifier.fillMaxSize(),
            enter = fadeIn(tween(HudMotion.EnterMs)) +
                slideInVertically(tween(HudMotion.EnterMs)) { it / 40 },
            exit = fadeOut(tween(HudMotion.ExitMs)) +
                slideOutVertically(tween(HudMotion.ExitMs)) { it / 40 },
        ) {
            val nowMs by rememberWallClockMs()
            val timeFormat = rememberHudTimeFormat()
            val content = hudState.content

            Box(modifier = Modifier.fillMaxSize()) {
                HudTopScrim(modifier = Modifier.align(Alignment.TopCenter))
                HudBottomScrim(
                    height = screenHeight * 0.6f,
                    modifier = Modifier.align(Alignment.BottomCenter),
                )

                HudTopBar(
                    transportState = hudState.transportState,
                    nowMs = nowMs,
                    timeFormat = timeFormat,
                    compact = compact,
                    onBack = { registerInteraction(); onBack() },
                    onFocusChange = handleFocusChange,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(horizontal = horizontal)
                        .padding(top = top),
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(horizontal = horizontal)
                        .padding(bottom = bottom),
                    verticalArrangement = Arrangement.spacedBy(HudSpacing.Block),
                ) {
                    HudInfoBlock(
                        content = content,
                        nowMs = nowMs,
                        timeFormat = timeFormat,
                        compact = compact,
                    )

                    // Timeline: only with real data.
                    when (content) {
                        is PlayerHudContent.Live ->
                            content.currentProgram?.progressAt(nowMs)?.let { HudProgressLine(fraction = it) }

                        else -> if (progress.normalizedDurationMs > 0L) {
                            HudSeekTimeline(
                                progress = progress,
                                onSeekTo = onSeekTo,
                                onInteraction = registerInteraction,
                                onFocusChange = handleFocusChange,
                                compact = compact,
                            )
                        }
                    }

                    hudState.seriesNextEpisodeCountdownSeconds?.let { seconds ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                stringResource(R.string.hud_next_episode_countdown, seconds),
                                color = app.vireo.player.hud.HudColors.TextPrimary,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Spacer(Modifier.width(12.dp))
                            FocusableBox(
                                onClick = { registerInteraction(); onCancelNextEpisodeCountdown() },
                                onFocusChange = handleFocusChange,
                            ) {
                                Text(
                                    stringResource(R.string.hud_cancel),
                                    color = app.vireo.player.hud.HudColors.TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }

                    HudTransportRow(
                        isPlaying = hudState.isPlaying,
                        hasPrevious = hudState.hasPrevious,
                        hasNext = hudState.hasNext,
                        showSeek = content !is PlayerHudContent.Live && progress.isSeekable,
                        compact = compact,
                        onPlayPause = onPlayPause,
                        onPrevious = onPrevious,
                        onSeekBackward = onSeekBackward,
                        onSeekForward = onSeekForward,
                        onNext = onNext,
                        onInteraction = registerInteraction,
                        onFocusChange = handleFocusChange,
                    )

                    if (content is PlayerHudContent.Live) {
                        HudTileRow(
                            tiles = buildHudTiles(
                                content = content,
                                hasSelectChannelAction = onSelectRecentChannel != null,
                                hasGuideAction = true,
                            ),
                            onOpenGuide = {
                                registerInteraction()
                                guideVisible = true
                                onOpenGuide?.invoke()
                            },
                            onSelectChannel = onSelectRecentChannel,
                            onInteraction = registerInteraction,
                            onFocusChange = handleFocusChange,
                        )
                    }
                }

                if (guideVisible && content is PlayerHudContent.Live) {
                    HudGuideOverlay(
                        content = content,
                        onClose = { guideVisible = false; registerInteraction() },
                        onFocusChange = handleFocusChange,
                    )
                }
            }
        }
    }
}
