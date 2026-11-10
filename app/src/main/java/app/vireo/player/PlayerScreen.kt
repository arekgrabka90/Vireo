package app.vireo.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.util.TypedValue
import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.C
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.ui.PlayerView
import app.vireo.R
import app.vireo.ui.components.FocusableBox
import kotlinx.coroutines.delay

@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    onBack: () -> Unit,
    vm: PlayerViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by vm.state.collectAsStateWithLifecycle()
    val epg by vm.epgState.collectAsStateWithLifecycle()
    val settings by vm.playbackSettings.collectAsStateWithLifecycle()
    val settingsReady by vm.playbackSettingsReady.collectAsStateWithLifecycle()
    val player = remember(vm) { vm.playerForView() }
    val resumeAfterLifecycleStop = remember(player) {
        mutableStateOf(false)
    }

    DisposableEffect(Unit) {
        val activity = context as? Activity
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        onDispose {
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener, AnalyticsListener {

            override fun onPlaybackStateChanged(
                eventTime: AnalyticsListener.EventTime,
                playbackState: Int,
            ) {
                if (!PlaybackCallbackGuard.isCurrentMediaItem(
                        eventTime.mediaItem.mediaId,
                        player.currentMediaItem?.mediaId,
                    )
                ) return

                if (!PlaybackCallbackGuard.isCurrentPlaybackState(
                        playbackState,
                        player.playbackState,
                    )
                ) return

                vm.onPlaybackStateChanged(
                    player.playbackState,
                    player.isPlaying,
                )

                if (
                    playbackState == Player.STATE_ENDED &&
                    vm.autoplayNextEnabled()
                ) {
                    vm.next(player.currentPosition, player.duration)
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (!PlaybackCallbackGuard.isCurrentBooleanValue(
                        isPlaying,
                        player.isPlaying,
                    )
                ) return

                vm.onPlaybackStateChanged(player.playbackState, isPlaying)
            }

            override fun onPlayWhenReadyChanged(
                playWhenReady: Boolean,
                reason: Int,
            ) {
                if (!PlaybackCallbackGuard.isCurrentBooleanValue(
                        playWhenReady,
                        player.playWhenReady,
                    )
                ) return

                vm.onPlayWhenReadyChanged(playWhenReady, reason)
            }

            override fun onPlayerError(
                eventTime: AnalyticsListener.EventTime,
                error: PlaybackException,
            ) {
                if (!PlaybackCallbackGuard.isCurrentMediaItem(
                        eventTime.mediaItem.mediaId,
                        player.currentMediaItem?.mediaId,
                    )
                ) return

                if (!PlaybackCallbackGuard.isCurrentError(
                        error,
                        player.playerError,
                    )
                ) return

                vm.onPlaybackError(
                    error = error,
                    positionMs = player.currentPosition,
                )
            }
        }

        player.addListener(listener)
        player.addAnalyticsListener(listener)
        onDispose {
            player.removeListener(listener)
            player.removeAnalyticsListener(listener)
        }
    }

    DisposableEffect(lifecycleOwner, player) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    resumeAfterLifecycleStop.value =
                        vm.state.value.desiredPlayWhenReady &&
                            vm.state.value.error == null

                    val item = vm.state.value.item
                    vm.saveProgress(
                        item,
                        player.currentPosition,
                        player.duration,
                    )
                    vm.pausePlayback()
                }

                Lifecycle.Event.ON_START -> {
                    if (
                        resumeAfterLifecycleStop.value &&
                        vm.state.value.error == null
                    ) {
                        vm.resumePlayback()
                    }
                    resumeAfterLifecycleStop.value = false
                }

                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    LaunchedEffect(
        settingsReady,
        state.item?.url,
        state.item?.contentKey,
        state.retryToken,
    ) {
        if (!settingsReady) return@LaunchedEffect
        val item = state.item ?: return@LaunchedEffect
        val resumeAt = state.resumePositionMs ?: vm.resumePosition(item)

        vm.startPlayback(item, resumeAt)
    }

    LaunchedEffect(state.item?.url, state.item?.contentKey) {
        val item = state.item ?: return@LaunchedEffect
        var saveCounter = 0

        while (true) {
            delay(1_000)

            vm.updateProgress(
                positionMs = player.currentPosition,
                durationMs = player.duration,
                bufferedPositionMs = player.bufferedPosition,
                isSeekable = player.isCurrentMediaItemSeekable,
            )

            saveCounter++
            if (saveCounter >= 15) {
                saveCounter = 0
                vm.saveProgress(
                    item,
                    player.currentPosition,
                    player.duration,
                )
            }
        }
    }

    DisposableEffect(player) {
        onDispose {
            val latestItem = vm.state.value.item
            vm.saveProgress(
                latestItem,
                player.currentPosition,
                player.duration,
            )
            vm.pausePlayback()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
    ) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    setPlayer(player)
                    keepScreenOn = true
                    useController = true
                    controllerShowTimeoutMs = 5_000
                    setShowRewindButton(true)
                    setShowFastForwardButton(true)
                    setControllerVisibilityListener(
                        PlayerView.ControllerVisibilityListener { visibility ->
                            vm.onControlsVisibilityChanged(visibility == View.VISIBLE)
                        }
                    )
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                view.player = player
                if (settingsReady) {
                    player.trackSelectionParameters =
                        player.trackSelectionParameters.buildUpon()
                            .setPreferredAudioLanguage(
                                settings.preferredAudioLanguage.takeUnless {
                                    it == "system"
                                }
                            )
                            .setPreferredTextLanguage(
                                settings.preferredSubtitleLanguage.takeUnless {
                                    it == "system"
                                }
                            )
                            .setTrackTypeDisabled(
                                C.TRACK_TYPE_TEXT,
                                !settings.subtitlesEnabled,
                            )
                            .build()
                    view.subtitleView?.setFixedTextSize(
                        TypedValue.COMPLEX_UNIT_SP,
                        settings.subtitleSizeSp.toFloat(),
                    )
                }
            },
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FocusableBox(onClick = onBack) {
                Text(stringResource(R.string.back), color = Color.White)
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    state.item?.title.orEmpty(),
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                epg.current?.let { currentProgram ->
                    Text(
                        currentProgram.title,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (settings.liveTvHudShowEpgDescription) {
                        currentProgram.description
                            ?.takeIf { it.isNotBlank() }
                            ?.let { description ->
                                Text(
                                    description,
                                    color = Color.White.copy(alpha = 0.82f),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                    }
                }
            }

            if (state.hasPrevious) {
                FocusableBox(
                    onClick = {
                        vm.previous(player.currentPosition, player.duration)
                    }
                ) {
                    Text(
                        stringResource(R.string.previous_episode),
                        color = Color.White,
                    )
                }
            }

            if (state.hasNext) {
                FocusableBox(
                    onClick = {
                        vm.next(player.currentPosition, player.duration)
                    }
                ) {
                    Text(
                        stringResource(R.string.next_episode),
                        color = Color.White,
                    )
                }
            }
        }

        state.error?.let { error ->
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    error,
                    color = MaterialTheme.colorScheme.error,
                )
                FocusableBox(onClick = { vm.retry() }) {
                    Text("Retry", color = Color.White)
                }
            }
        }
    }
}
