package app.vireo.player.hud

import android.text.format.DateFormat
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.delay

/**
 * Real wall-clock time. Call it inside the visible HUD content so the ticker
 * stops when the HUD leaves composition.
 */
@Composable
internal fun rememberWallClockMs(tickMs: Long = HudMotion.ClockTickMs): State<Long> {
    val now = remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(tickMs) {
        while (true) {
            now.longValue = System.currentTimeMillis()
            delay(tickMs)
        }
    }
    return now
}

/** Locale-aware patterns that follow the system 12/24 h setting. */
@Composable
internal fun rememberHudTimeFormat(): HudTimeFormat {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val is24Hour = DateFormat.is24HourFormat(context)
    return remember(locale, is24Hour) {
        HudTimeFormat(
            clockPattern = DateFormat.getBestDateTimePattern(
                locale,
                if (is24Hour) "EEEdMMMHm" else "EEEdMMMhm",
            ),
            timePattern = DateFormat.getBestDateTimePattern(locale, if (is24Hour) "Hm" else "hm"),
            locale = locale,
        )
    }
}

@Composable
internal fun HudClock(
    nowMs: Long,
    timeFormat: HudTimeFormat,
    compact: Boolean,
    modifier: Modifier = Modifier,
) {
    Text(
        text = timeFormat.clock(nowMs),
        style = HudType.clock(compact),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier,
    )
}
