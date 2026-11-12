# VIREO — Premium Player HUD: prompt for ChatGPT

> **How to use this file.** Paste the whole document into ChatGPT (with GitHub access to the VIREO repository).
> It defines the **goal, the rules and the target look**, and contains **reference Kotlin code**.
> ChatGPT already sees the repository, so it must locate the real files itself and adapt the reference code to them.
> The reference code was written against a source snapshot (VIREO branch `work/vireo-development`, HEAD `ec2b35c`, 2026-11-11).
> **It has not been compiled or run.** Treat it as a precise design, not as copy-paste-ready truth.

---

## 0. Role and working method

You are a senior Android/Kotlin engineer (Jetpack Compose, Compose for TV, Media3, Coil).
Implement a **Premium Player HUD** for VIREO. The result must be a **focused, modular implementation — not a general application refactor.**

Rules for how you work:

- Avoid speculative changes. Do not expand the scope without a clear technical reason, and state the reason when you do.
- Do not assume that similarly named classes in VIREO and ARVIO (a second project that may be in the same GitHub account) have identical responsibilities. **Implement only in VIREO.** ARVIO is not a source of code for this task.
- **Phase 1 — architecture assessment (no implementation yet).** Read the real code and report briefly (max ~40 lines):
  1. what already exists and is reused as is,
  2. every place where the reference code in this file does not match the repository (names, signatures, versions),
  3. the exact list of files to add / modify / remove, with a one-line justification each,
  4. the open points from section 9 that you could verify, and those you could not.
  Then **wait for my confirmation**.
- **Phase 2 — implementation** after confirmation. Compile mentally against the real code, keep public signatures stable, and list what you could not verify.

---

## 1. Goal in one paragraph

Replace the current player HUD (`PlayerHudOverlay.kt`) with a cinematic, minimal, TV-style overlay that looks like the reference interface described in section 4 for **Live TV**, and gives **Movies, Series and Other** content a similar look adapted to their content. The HUD is a **transparent overlay above the real video** — the video (including the frozen frame while paused) must be visible underneath at all times.

---

## 2. Verified facts about the current VIREO code (snapshot `ec2b35c`)

- Stack: Kotlin, Compose BOM `2024.06.00`, `androidx.tv:tv-material:1.0.0`, Material3, `material-icons-core` only (no extended icons), Coil `coil-compose:2.6.0`, **Media3 `1.11.1`**, Hilt, Room. `minSdk = 24` (so **no `java.time` without desugaring** — use `SimpleDateFormat`/`DateFormat`).
- Package `app.vireo.player` contains: `PlayerScreen.kt`, `PlayerViewModel.kt`, `PlayerHudOverlay.kt`, `PlayerHudState.kt`, `PlayerRuntimeState.kt`, `PlaybackSession.kt` (defines `PlayItem`, `PlaybackEpisode`), `engine/Media3PlayerEngine.kt`, `controller/PlayerController.kt`.
- `PlayerHudState(content, transportState, isPlaying, hasPrevious, hasNext, controlsVisible, errorMessage)` with `sealed interface PlayerHudContent { Movie, Series, Live, Other }`, built by `internal fun buildPlayerHudState(...)`.
  - `Movie(title, description, year, genre, duration, rating, posterUrl, backdropUrl)` — **no logo field**.
  - `Series(title, description, year, genre, rating, posterUrl, backdropUrl, currentEpisode, currentEpisodeDescription, currentEpisodeThumbnailUrl, episodes)` — **no logo field**.
  - `Live(channelName, epgChannelId, streamId, logoUrl, currentProgram: EpgProgram?, currentProgramDescription, nextProgram, recentChannels: List<RecentLiveChannel>)`.
  - `Other(title, posterUrl)`.
  - `RecentLiveChannel(sourceId: Long, streamId: String, channelName, epgChannelId, logoUrl, lastPlayedAt: Long)`.
- `PlayerProgressState(positionMs, durationMs, bufferedPositionMs, isSeekable)` is a **separate stream** from `PlayerHudState` (good: frequent time updates do not rebuild the whole HUD). Keep it that way.
- `PlayerTransportState { IDLE, BUFFERING, READY, ENDED, ERROR }`.
- `EpgProgram(channelId, title, description, startTimeMs, endTimeMs, category, iconUrl)` with `hasValidTimeRange` and `contains(timeMs)`; `EpgNowNext(current, next)`.
- `PlaybackEpisode(title, seasonNumber, episodeNumber, description, duration, thumbnailUrl)`.
- `PlayerScreen.kt`: a black `Box` → `AndroidView { PlayerView }` (`useController = false`) → `PlayerHudOverlay(...)` → error block. So the HUD is already layered **above** the video surface. Keep this.
- `PlayerHudOverlay` signature today: `(hudState, progress, onBack, onPlayPause, onSeekBackward, onSeekForward, onSeekTo, onPrevious, onNext, onVisibilityChanged)`. It owns the visibility state machine (`visible`, `interactionTick`, `hasFocusedControl`, auto-hide after 6 s only while playing and with no focused control, tap-to-toggle). **Preserve this behaviour.**
- Helpers used by the current overlay and **not** included in the snapshot: `app.vireo.ui.components.FocusableBox(onClick, modifier, onFocusChange, content)` and `app.vireo.ui.theme.VireoColors`. Reuse `FocusableBox` exactly as the current overlay does.
- TMDB logos already exist for details: `TmdbDetail.logoUrl` (via `TmdbDetailMapper.selectLogoUrl`) and `TmdbDetailScreen` already shows it with a load-failure fallback. Reuse that source, do not add new network calls in the player.

### Defects in the current overlay that this task must remove

1. `PlayerHudOverlay.kt` (~lines 94–136): a **full-width `AsyncImage` with `ContentScale.Crop`**, 62–72 % of the screen height, drawn from `backdropUrl ?: posterUrl` (Live: `logoUrl`), placed over the video. **Delete it.** No poster/backdrop/channel image may cover the video.
2. The Live branch uses a hard-coded placeholder text `"Live television"` when there is no EPG programme. Remove all placeholders.
3. `contentSubtitle()` returns hard-coded labels (`"SERIES"`, `"LIVE TELEVISION"`, `"NOW PLAYING"`). Remove.
4. The time row always renders `0:00 … 0:00` even when the duration is unknown. Render it only when the duration is real.
5. No `@Preview` exists in the project. Add previews including missing-data cases.

---

## 3. Core design principles (non-negotiable)

### Visual design
- Premium cinematic aesthetic. **Graphite, black and white palette.** Minimal, elegant, restrained.
- Subtle gradients and edge shading. No heavy panels. Consistent typography and spacing.
- Short, smooth animations. Clear visual hierarchy. No clutter, no excessive metadata.
- Accent colour: the reference uses a blue accent; this project's rule is graphite/black/white. Implement **one `Accent` token that defaults to white** and can be switched to a muted blue in one line.

### Video visibility (critical)
- The video must remain visible underneath the HUD **at all times**.
- When the user pauses, the actual **frozen video frame stays visible**.
- Never replace the video with a poster, backdrop, channel image, loading screen or any full-screen artwork.
- The HUD is a **transparent Compose overlay**. Legibility comes only from **subtle gradient scrims** at the top and bottom edges.

### Logo and title rules (movies, series, channels)
- Show the **actual logo** when a valid logo URL is available. Otherwise show the appropriate **text title**.
- Never show logo and title simultaneously as competing representations of the same thing.
- Never substitute a poster or backdrop for a missing logo.
- Preserve the natural aspect ratio. Never stretch. Use `ContentScale.Fit`.
- Keep rendering sharp, with sensible maximum dimensions.
- Transparent PNG logos must render correctly **without any forced background**.
- If a logo URL fails to load, fall back to the text title (no blank gap, no placeholder image).

### Metadata
- Show only information that is genuinely available.
- Never fabricate resolution, FPS, bitrate, codec, HDR status, audio channels, language, source quality, EPG information or other technical data.
- Never render empty labels or placeholder metadata. If a block has no real data, the block does not exist.

---

## 4. Target look

### 4.1 Reference interface (Live TV) — layout only

A landscape, remote-controlled TV OSD. Everything fits fully on screen (nothing is cropped). In the reference screenshot the background is black; **in VIREO it is the live video, so the background is transparent.**

```
┌──────────────────────────────────────────────────────────────────┐
│ [×]                                      (spinner) śr., 11 lis, 14:40│  top: close button (left), clock (right)
│                                                                  │
│                                                                  │
│                         (real video visible)                     │
│                                                                  │
│  ┌────────┐  Programme title (largest, bold, white)             │
│  │  logo  │  14:40 – 15:40  ·  27 min left     (muted gray)     │  info block
│  │  slot  │  Description, up to 3 lines, light gray, ellipsis   │
│  └────────┘  15:40 – 16:40  Next programme title                │
│  ────────────────────●───────────────────────────────────────── │  timeline: played = accent, thumb, rest gray
│        [‹]   [−10]   (⏸)   [+10]   [›]                          │  transport row (only real controls)
│  ┌──────┐┌──────┐┌──────┐┌──────┐                               │  tile row (Live only, only with real data)
│  │Guide ││logo  ││logo  ││logo  │                               │
│  └──────┘└──────┘└──────┘└──────┘                               │
└──────────────────────────────────────────────────────────────────┘
```

Details:
1. **Top right:** one line with date and time (device clock, locale-aware, 12/24 h per system setting), muted white, right-aligned. A small thin buffering spinner appears to its left **only while `transportState == BUFFERING`**.
2. **Info block (Live):** an identity slot on the left (the channel **logo only**, `ContentScale.Fit`, no background) and a text column on the right:
   - **Programme title** — the largest, boldest, brightest text on screen.
   - **Metadata line:** time range in muted gray, then remaining minutes. (The reference also shows a tiny mini progress bar and a quality tag; VIREO **omits both**: the mini bar would duplicate the timeline, and VIREO has no real quality data.)
   - **Description:** lighter-gray paragraph, max 3 lines, ellipsis.
   - **Next line:** muted time range + slightly lighter title.
3. **Timeline (Live):** thin full-width line = progress of the current programme (`now` within `startTimeMs..endTimeMs`), accent-coloured played part, round thumb, gray remainder. Non-interactive. Rendered only when the programme time range is valid.
4. **Tile row (Live only):** horizontally scrollable rounded graphite tiles, D-pad focusable; the focused tile is slightly larger and lighter with a hairline border.
   - An optional first tile "Guide" (list icon + label) **only if** an `onOpenGuide` callback is supplied.
   - Tiles for **recent channels** (logo or channel-name text — never both), **only if** an `onSelectRecentChannel` callback is supplied. The currently playing channel is excluded.
   - The row does not exist when there is nothing to show.
5. **No bottom "more" chevron** (VIREO has nothing below the HUD — an affordance without a target would be fabricated).

### 4.2 Logo/title behaviour for Live (strict)
| Logo URL usable & loads | Programme in EPG | Result |
|---|---|---|
| yes | yes | logo in slot; programme title as main title; **channel name not shown** |
| yes | no | logo in slot; no title text (nothing invented) |
| no / failed | yes | small overline = channel name (text); programme title as main title |
| no / failed | no | channel name as main title |

### 4.3 Movies
Same visual language, no slot and no tile row:
- **Title area:** the **TMDB logo** (`ContentScale.Fit`) if available, otherwise the movie title as text. Never both.
- **Metadata line:** `year · genre · duration · ★ rating` — only the parts that exist.
- **Description:** up to 3 lines.
- **Timeline:** time — slider — duration, rendered only if the duration is known. Transport row with −10 / +10 when seekable.

### 4.4 Series
As movies, plus the episode line:
- **Title area:** series logo or series title (never both).
- **Episode line (semi-bold):** `S01 · E02 · Episode title` — from `currentEpisode`, only existing parts.
- **Metadata line:** `year · genre · episode duration · ★ rating`.
- **Description:** the **episode** description (2–3 lines).
- The current non-interactive "episode chips" row is **removed** from the HUD by default (it has no action). If a shelf is wanted later, reuse `HudTileRow` with an `onSelectEpisode` callback — **not part of this task**.

### 4.5 Other
Title as text only (no poster, no backdrop), standard timeline and transport.

### 4.6 Common
- Top-left: small close/back button (kept from the current HUD for touch/mouse use).
- Auto-hide, tap-to-toggle and focus-keeps-visible behaviour unchanged.
- Enter/exit: fade + tiny vertical slide, ~220 ms in / ~160 ms out.

---

## 5. Scope

### Add (package `app.vireo.player.hud`)
| File | Purpose |
|---|---|
| `HudTokens.kt` | colours, dimensions, spacing, typography, motion, the few UI literals |
| `HudFormatters.kt` | **pure** functions (unit-testable): logo URL check, programme progress/remaining, metadata joining, episode label, time formatting, `HudTimeFormat` |
| `HudClock.kt` | `rememberWallClockMs()`, `rememberHudTimeFormat()`, `HudClock` |
| `HudScrims.kt` | top and bottom gradient scrims |
| `HudLogoOrTitle.kt` | `HudLogo` (Coil, Fit) and `HudLogoOrTitle` |
| `HudTimeline.kt` | `HudProgressLine` (Live) and `HudSeekTimeline` (VOD) |
| `HudInfoBlock.kt` | per-content-type info blocks |
| `HudControls.kt` | top bar, transport row, play/pause glyph, icon button |
| `HudTileRow.kt` | `HudTile`, `buildHudTiles`, `HudTileRow` |
| `HudPreviews.kt` | `@Preview`s including missing-data cases |
| tests | `HudFormattersTest`, `HudTilesTest`, additions to `PlayerHudStateTest` |

### Modify
- `PlayerHudOverlay.kt` — rewrite as a thin composition of the components above. **Keep the existing parameters and the visibility state machine**, add only optional parameters with defaults.
- `PlayerHudState.kt` — add `logoUrl` to `Movie` and `Series`; document the meaning of `Live.logoUrl`.
- `PlaybackSession.kt` — add `logoUrl: String? = null` to `PlayItem` (default keeps every existing call site and test compiling).
- The code that creates `PlayItem` for movies/series (likely around the TMDB detail screens/view models) — pass the TMDB `logoUrl`. **Find the real creation sites; do not guess.**
- `PlayerScreen.kt` — only if wiring the optional `onSelectRecentChannel` / `onOpenGuide` callbacks (see 9.4).

### Remove
- The full-screen `AsyncImage` and its `backdrop` selection in `PlayerHudOverlay.kt`.
- `contentSubtitle()` hard-coded labels, `"Live television"`, the always-on `0:00` time row, the non-interactive episode chips, the `"BUFFERING"` text (replaced by the spinner).

### Out of scope
General app refactoring, other screens, Media3 upgrade, new data models beyond the fields above, adding `material-icons-extended`, new network calls in the player, ARVIO code.

---

## 6. Data model changes

### 6.1 `PlaybackSession.kt` — `PlayItem`
```kotlin
data class PlayItem(
    // ...existing fields unchanged...
    val episodes: List<PlaybackEpisode> = emptyList(),
    /**
     * Real logo of the content: TMDB title logo for movies/series, channel icon for live.
     * Never a poster or a backdrop.
     */
    val logoUrl: String? = null,
)
```

### 6.2 `PlayerHudState.kt`
```kotlin
sealed interface PlayerHudContent {
    data class Movie(
        val title: String,
        val description: String?,
        val year: String?,
        val genre: String?,
        val duration: String?,
        val rating: String?,
        val posterUrl: String?,
        val backdropUrl: String?,
        /** Real title logo (TMDB). Null when unavailable. Never a poster/backdrop. */
        val logoUrl: String? = null,
    ) : PlayerHudContent

    data class Series(
        val title: String,
        val description: String?,
        val year: String?,
        val genre: String?,
        val rating: String?,
        val posterUrl: String?,
        val backdropUrl: String?,
        val currentEpisode: PlaybackEpisode?,
        val currentEpisodeDescription: String?,
        val currentEpisodeThumbnailUrl: String?,
        val episodes: List<PlaybackEpisode>,
        /** Real series logo (TMDB). Null when unavailable. Never a poster/backdrop. */
        val logoUrl: String? = null,
    ) : PlayerHudContent

    // Live and Other: unchanged. Live.logoUrl is the CHANNEL logo/icon (see 9.1).
}
```
In `buildPlayerHudState` pass `logoUrl = item.logoUrl` for `Movie` and `Series`. For `Live` keep the channel icon source, but resolve open point 9.1 first.

`posterUrl` / `backdropUrl` stay in the model (other code and tests use them) but the **HUD never renders them**.

---

## 7. Reference implementation (Kotlin)

All files below are in `app.vireo.player.hud` unless stated. Components are `internal`, stateless where possible.

### 7.1 `HudTokens.kt`
```kotlin
package app.vireo.player.hud

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal object HudColors {
    val Graphite = Color(0xFF1B1C1F)
    val GraphiteFocused = Color(0xFF2B2D32)
    val TextPrimary = Color.White
    val TextSecondary = Color.White.copy(alpha = 0.72f)
    val TextTertiary = Color.White.copy(alpha = 0.52f)
    val Hairline = Color.White.copy(alpha = 0.55f)
    val TimelineRemaining = Color.White.copy(alpha = 0.28f)

    /**
     * The single accent. Monochrome by default (project palette: graphite/black/white).
     * To match the blue reference, change only this line, e.g. Color(0xFF4DA3FF).
     */
    val Accent = Color.White
}

internal object HudDimens {
    val IdentitySlot = 104.dp
    val TitleLogoMaxWidth = 320.dp
    val TitleLogoMaxHeight = 88.dp
    val TitleLogoMaxHeightCompact = 56.dp
    val TileWidth = 156.dp
    val TileHeight = 88.dp
    val TileLogoMaxWidth = 104.dp
    val TileLogoMaxHeight = 44.dp
    val ProgressLineHeight = 14.dp
    val ProgressLineThickness = 3.dp
    val ProgressThumbRadius = 6.dp
    val TopScrimHeight = 140.dp
    val SpinnerSize = 18.dp
}

internal object HudSpacing {
    val ScreenHorizontal = 56.dp
    val ScreenHorizontalCompact = 24.dp
    val ScreenTop = 32.dp
    val ScreenTopCompact = 20.dp
    val ScreenBottom = 28.dp
    val ScreenBottomCompact = 18.dp
    val Block = 16.dp
    val Row = 12.dp
    val Tile = 10.dp
    val Small = 6.dp
}

internal object HudMotion {
    const val EnterMs = 220
    const val ExitMs = 160
    const val FocusMs = 120
    const val ClockTickMs = 15_000L
}

/** One font family; hierarchy comes from size, weight and brightness only. */
internal object HudType {
    fun title(compact: Boolean) = TextStyle(
        fontSize = if (compact) 24.sp else 34.sp,
        lineHeight = if (compact) 30.sp else 40.sp,
        fontWeight = FontWeight.Bold,
        color = HudColors.TextPrimary,
    )

    fun overline(compact: Boolean) = TextStyle(
        fontSize = if (compact) 12.sp else 14.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.8.sp,
        color = HudColors.TextSecondary,
    )

    fun episode(compact: Boolean) = TextStyle(
        fontSize = if (compact) 15.sp else 18.sp,
        fontWeight = FontWeight.SemiBold,
        color = HudColors.TextPrimary,
    )

    fun meta(compact: Boolean) = TextStyle(
        fontSize = if (compact) 13.sp else 16.sp,
        fontWeight = FontWeight.Medium,
        color = HudColors.TextSecondary,
    )

    fun body(compact: Boolean) = TextStyle(
        fontSize = if (compact) 13.sp else 16.sp,
        lineHeight = if (compact) 18.sp else 22.sp,
        color = HudColors.TextSecondary,
    )

    fun next(compact: Boolean) = TextStyle(
        fontSize = if (compact) 13.sp else 16.sp,
        color = HudColors.TextSecondary,
    )

    fun time(compact: Boolean) = TextStyle(
        fontSize = if (compact) 12.sp else 14.sp,
        color = HudColors.TextSecondary,
    )

    fun clock(compact: Boolean) = TextStyle(
        fontSize = if (compact) 14.sp else 18.sp,
        fontWeight = FontWeight.Medium,
        color = HudColors.TextSecondary,
    )

    fun tile() = TextStyle(
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = HudColors.TextPrimary,
    )
}

/**
 * The few UI literals of the HUD in one place.
 * TODO: move to string resources (the project currently mixes hard-coded English and resources).
 */
internal object HudStrings {
    const val Back = "Back"
    const val Previous = "Previous"
    const val Next = "Next"
    const val SeekBackward = "Back 10 seconds"
    const val SeekForward = "Forward 10 seconds"
    const val SeekBackwardLabel = "−10"
    const val SeekForwardLabel = "+10"
    const val Guide = "Guide"
    const val Play = "Play"
    const val Pause = "Pause"
    fun minutesLeft(minutes: Int) = "$minutes min left"
}
```

### 7.2 `HudFormatters.kt` (pure, JVM-testable)
```kotlin
package app.vireo.player.hud

import app.vireo.domain.model.EpgProgram
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** True only for an absolute http(s) URL. Anything else is treated as "no logo". */
internal fun isUsableLogoUrl(url: String?): Boolean {
    val value = url?.trim().orEmpty()
    return value.startsWith("http://", ignoreCase = true) ||
        value.startsWith("https://", ignoreCase = true)
}

/** Fraction of the programme elapsed at [nowMs], or null when the range is invalid or [nowMs] is outside it. */
internal fun EpgProgram.progressAt(nowMs: Long): Float? {
    if (!hasValidTimeRange || !contains(nowMs)) return null
    return ((nowMs - startTimeMs).toDouble() / (endTimeMs - startTimeMs).toDouble())
        .toFloat()
        .coerceIn(0f, 1f)
}

/** Whole minutes left (rounded up), or null when the programme is not running at [nowMs]. */
internal fun EpgProgram.remainingMinutesAt(nowMs: Long): Int? {
    if (!hasValidTimeRange || !contains(nowMs)) return null
    return (((endTimeMs - nowMs) + 59_999L) / 60_000L).toInt()
}

/** "2026 · Drama · 120 min · ★ 8.1" built only from parts that exist; null when nothing exists. */
internal fun joinMetadata(
    year: String?,
    genre: String?,
    duration: String?,
    rating: String?,
): String? {
    val parts = listOfNotNull(
        year?.trim()?.takeIf { it.isNotEmpty() },
        genre?.trim()?.takeIf { it.isNotEmpty() },
        duration?.trim()?.takeIf { it.isNotEmpty() },
        rating?.trim()?.takeIf { it.isNotEmpty() }?.let { "★ $it" },
    )
    return parts.takeIf { it.isNotEmpty() }?.joinToString("  ·  ")
}

/** "S01 · E02 · Title" using only the parts that exist; null when nothing exists. */
internal fun formatEpisodeLabel(season: Int?, episode: Int?, title: String?): String? {
    val parts = listOfNotNull(
        season?.let { "S${it.toString().padStart(2, '0')}" },
        episode?.let { "E${it.toString().padStart(2, '0')}" },
        title?.trim()?.takeIf { it.isNotEmpty() },
    )
    return parts.takeIf { it.isNotEmpty() }?.joinToString("  ·  ")
}

internal fun formatPlayerTime(valueMs: Long): String {
    val totalSeconds = valueMs.coerceAtLeast(0L) / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
    }
}

/**
 * Wall-clock formatting with injected patterns so it is testable on the JVM.
 * Not thread-safe (SimpleDateFormat): use on the main thread only.
 */
internal class HudTimeFormat(
    clockPattern: String,
    timePattern: String,
    locale: Locale,
    timeZone: TimeZone = TimeZone.getDefault(),
) {
    private val clockFormat = SimpleDateFormat(clockPattern, locale).apply { this.timeZone = timeZone }
    private val timeFormat = SimpleDateFormat(timePattern, locale).apply { this.timeZone = timeZone }

    fun clock(epochMs: Long): String = clockFormat.format(Date(epochMs))
    fun time(epochMs: Long): String = timeFormat.format(Date(epochMs))
    fun range(startMs: Long, endMs: Long): String = "${time(startMs)} – ${time(endMs)}"
}
```

### 7.3 `HudClock.kt`
```kotlin
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
```

### 7.4 `HudScrims.kt`
```kotlin
package app.vireo.player.hud

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp

/** Legibility only. Never opaque, never an image: the video stays visible. */
@Composable
internal fun HudTopScrim(modifier: Modifier = Modifier, height: Dp = HudDimens.TopScrimHeight) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.55f),
                    1f to Color.Transparent,
                ),
            ),
    )
}

@Composable
internal fun HudBottomScrim(height: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.45f to Color.Black.copy(alpha = 0.42f),
                    1f to Color.Black.copy(alpha = 0.88f),
                ),
            ),
    )
}
```

### 7.5 `HudLogoOrTitle.kt`
```kotlin
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
```

### 7.6 `HudTimeline.kt`
```kotlin
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
```

### 7.7 `HudInfoBlock.kt`
```kotlin
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
    val program: EpgProgram? = content.currentProgram
    val programTitle = program?.title?.trim().orEmpty()
    val rangeLine = program
        ?.takeIf { it.hasValidTimeRange }
        ?.let { p ->
            listOfNotNull(
                timeFormat.range(p.startTimeMs, p.endTimeMs),
                p.remainingMinutesAt(nowMs)?.let { HudStrings.minutesLeft(it) },
            ).joinToString("  ·  ")
        }
    val description = content.currentProgramDescription?.trim()?.takeIf { it.isNotEmpty() }
    val next = content.nextProgram?.takeIf { it.title.isNotBlank() && it.hasValidTimeRange }

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
    val episodeLabel = formatEpisodeLabel(episode?.seasonNumber, episode?.episodeNumber, episode?.title)
    val metadata = joinMetadata(content.year, content.genre, episode?.duration, content.rating)
    val description = content.currentEpisodeDescription?.trim()?.takeIf { it.isNotEmpty() }

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
```
> Note: replace `logoUrl!!` with a local `val` smart-cast if the compiler complains; the intent is "use the trimmed, validated URL".

### 7.8 `HudControls.kt`
```kotlin
package app.vireo.player.hud

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
        HudIconButton(onClick = onBack, description = HudStrings.Back, onFocusChange = onFocusChange) {
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
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (hasPrevious) {
            HudIconButton(
                onClick = { onInteraction(); onPrevious() },
                description = HudStrings.Previous,
                onFocusChange = onFocusChange,
            ) { Text("‹", color = HudColors.TextPrimary, fontSize = 28.sp) }
            Spacer(Modifier.width(12.dp))
        }

        if (showSeek) {
            HudIconButton(
                onClick = { onInteraction(); onSeekBackward() },
                description = HudStrings.SeekBackward,
                onFocusChange = onFocusChange,
            ) {
                Text(HudStrings.SeekBackwardLabel, color = HudColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(16.dp))
        }

        FocusableBox(
            onClick = { onInteraction(); onPlayPause() },
            modifier = Modifier
                .size(if (compact) 52.dp else 60.dp)
                .clip(CircleShape)
                .background(Color.White)
                .semantics { contentDescription = if (isPlaying) HudStrings.Pause else HudStrings.Play },
            onFocusChange = onFocusChange,
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                HudPlayPauseGlyph(isPlaying = isPlaying)
            }
        }

        if (showSeek) {
            Spacer(Modifier.width(16.dp))
            HudIconButton(
                onClick = { onInteraction(); onSeekForward() },
                description = HudStrings.SeekForward,
                onFocusChange = onFocusChange,
            ) {
                Text(HudStrings.SeekForwardLabel, color = HudColors.TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (hasNext) {
            Spacer(Modifier.width(12.dp))
            HudIconButton(
                onClick = { onInteraction(); onNext() },
                description = HudStrings.Next,
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
```

### 7.9 `HudTileRow.kt`
```kotlin
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material3.Icon
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
    if (hasSelectChannelAction) {
        content.recentChannels
            .filter { it.streamId != content.streamId }
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
                    description = HudStrings.Guide,
                    onClick = { onInteraction(); onOpenGuide?.invoke() },
                    onFocusChange = onFocusChange,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.List,
                            contentDescription = null,
                            tint = HudColors.TextPrimary,
                            modifier = Modifier.size(30.dp),
                        )
                        Text(text = HudStrings.Guide, style = HudType.tile(), maxLines = 1)
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
```

### 7.10 `PlayerHudOverlay.kt` (rewrite; package `app.vireo.player`)
```kotlin
package app.vireo.player

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
    onVisibilityChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    onSelectRecentChannel: ((RecentLiveChannel) -> Unit)? = null,
    onOpenGuide: (() -> Unit)? = null,
) {
    // ---- visibility state machine: unchanged behaviour ----
    var visible by remember { mutableStateOf(true) }
    var interactionTick by remember { mutableIntStateOf(0) }
    var hasFocusedControl by remember { mutableStateOf(false) }

    val registerInteraction: () -> Unit = {
        visible = true
        interactionTick++
    }
    val handleFocusChange: (Boolean) -> Unit = { focused ->
        hasFocusedControl = focused
        if (focused) visible = true
        interactionTick++
    }

    LaunchedEffect(visible) { onVisibilityChanged(visible) }

    LaunchedEffect(visible, hudState.isPlaying, interactionTick, hasFocusedControl) {
        if (visible && hudState.isPlaying && !hasFocusedControl) {
            delay(6_000L)
            visible = false
        }
    }
    // ---------------------------------------------------------

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val compact = maxWidth < 850.dp
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
                    visible = !visible
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

                    HudTransportRow(
                        isPlaying = hudState.isPlaying,
                        hasPrevious = hudState.hasPrevious,
                        hasNext = hudState.hasNext,
                        showSeek = progress.isSeekable,
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
                                hasGuideAction = onOpenGuide != null,
                            ),
                            onOpenGuide = onOpenGuide,
                            onSelectChannel = onSelectRecentChannel,
                            onInteraction = registerInteraction,
                            onFocusChange = handleFocusChange,
                        )
                    }
                }
            }
        }
    }
}
```

### 7.11 `HudPreviews.kt` (including missing-data cases)
```kotlin
package app.vireo.player.hud

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import app.vireo.domain.model.EpgProgram
import app.vireo.player.PlaybackEpisode
import app.vireo.player.PlayerHudContent
import app.vireo.player.PlayerHudOverlay
import app.vireo.player.PlayerHudState
import app.vireo.player.PlayerProgressState
import app.vireo.player.PlayerTransportState
import app.vireo.player.RecentLiveChannel

/** Stands in for the real video so the transparency of the HUD is visible in previews. */
@Composable
private fun PreviewVideo(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.linearGradient(listOf(Color(0xFF4A5C70), Color(0xFF10151B)))),
    ) { content() }
}

private fun state(content: PlayerHudContent, playing: Boolean = true) = PlayerHudState(
    content = content,
    transportState = PlayerTransportState.READY,
    isPlaying = playing,
    hasPrevious = false,
    hasNext = false,
    controlsVisible = true,
    errorMessage = null,
)

@Composable
private fun PreviewHud(
    hud: PlayerHudState,
    progress: PlayerProgressState = PlayerProgressState(),
    withActions: Boolean = false,
) {
    PreviewVideo {
        PlayerHudOverlay(
            hudState = hud,
            progress = progress,
            onBack = {}, onPlayPause = {}, onSeekBackward = {}, onSeekForward = {},
            onSeekTo = {}, onPrevious = {}, onNext = {}, onVisibilityChanged = {},
            onSelectRecentChannel = if (withActions) ({}) else null,
            onOpenGuide = if (withActions) ({}) else null,
        )
    }
}

private fun liveContent(
    withProgram: Boolean,
    withLogo: Boolean,
    withNext: Boolean = true,
): PlayerHudContent.Live {
    val now = System.currentTimeMillis()
    val current = EpgProgram(
        channelId = "c1",
        title = "Sample programme",
        description = "A short sample description used only in previews. It is long enough to show the three-line limit and the ellipsis behaviour of the HUD description block.",
        startTimeMs = now - 20 * 60_000L,
        endTimeMs = now + 40 * 60_000L,
    )
    return PlayerHudContent.Live(
        channelName = "Sample Channel",
        epgChannelId = "c1",
        streamId = "100",
        // Network is unavailable in previews, so a logo URL renders the text fallback here.
        logoUrl = if (withLogo) "https://example.invalid/logo.png" else null,
        currentProgram = if (withProgram) current else null,
        currentProgramDescription = if (withProgram) current.description else null,
        nextProgram = if (withProgram && withNext) {
            current.copy(title = "Next sample programme", startTimeMs = current.endTimeMs, endTimeMs = current.endTimeMs + 60 * 60_000L)
        } else null,
        recentChannels = listOf(
            RecentLiveChannel(1, "201", "News One", null, null, 0L),
            RecentLiveChannel(1, "202", "Sport Two", null, null, 0L),
            RecentLiveChannel(1, "203", "Movies Three", null, null, 0L),
        ),
    )
}

@Preview(name = "Live – full data + tiles", widthDp = 1280, heightDp = 720)
@Composable
private fun LiveFullPreview() = PreviewHud(
    hud = state(liveContent(withProgram = true, withLogo = true)),
    withActions = true,
)

@Preview(name = "Live – no logo, no EPG", widthDp = 1280, heightDp = 720)
@Composable
private fun LiveEmptyPreview() = PreviewHud(
    hud = state(liveContent(withProgram = false, withLogo = false)),
)

@Preview(name = "Movie – no logo (title text)", widthDp = 1280, heightDp = 720)
@Composable
private fun MoviePreview() = PreviewHud(
    hud = state(
        PlayerHudContent.Movie(
            title = "Sample Movie", description = "Short movie description for the preview.",
            year = "2026", genre = "Drama", duration = "2 h 04 min", rating = "8.1",
            posterUrl = null, backdropUrl = null, logoUrl = null,
        ),
        playing = false, // paused: the video behind must stay visible
    ),
    progress = PlayerProgressState(positionMs = 1_800_000L, durationMs = 7_440_000L, isSeekable = true),
)

@Preview(name = "Movie – only a title", widthDp = 1280, heightDp = 720)
@Composable
private fun MovieMinimalPreview() = PreviewHud(
    hud = state(
        PlayerHudContent.Movie(
            title = "Sample Movie", description = null, year = null, genre = null,
            duration = null, rating = null, posterUrl = null, backdropUrl = null,
        ),
    ),
)

@Preview(name = "Series – episode", widthDp = 1280, heightDp = 720)
@Composable
private fun SeriesPreview() {
    val episode = PlaybackEpisode(
        title = "Sample episode", seasonNumber = 1, episodeNumber = 2,
        description = "Episode description for the preview.", duration = "48 min",
    )
    PreviewHud(
        hud = state(
            PlayerHudContent.Series(
                title = "Sample Series", description = "Series description.", year = "2025",
                genre = "Thriller", rating = "7.9", posterUrl = null, backdropUrl = null,
                currentEpisode = episode, currentEpisodeDescription = episode.description,
                currentEpisodeThumbnailUrl = null, episodes = listOf(episode),
            ),
        ),
        progress = PlayerProgressState(positionMs = 600_000L, durationMs = 2_880_000L, isSeekable = true),
    )
}

@Preview(name = "Compact (phone)", widthDp = 411, heightDp = 891)
@Composable
private fun CompactPreview() = PreviewHud(hud = state(liveContent(withProgram = true, withLogo = false)))
```

---

## 8. Tests

### 8.1 `HudFormattersTest.kt`
```kotlin
package app.vireo.player.hud

import app.vireo.domain.model.EpgProgram
import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HudFormattersTest {

    private val program = EpgProgram(channelId = "c", title = "t", startTimeMs = 0L, endTimeMs = 3_600_000L)

    @Test fun `logo url must be absolute http or https`() {
        assertTrue(isUsableLogoUrl("https://img.example/logo.png"))
        assertTrue(isUsableLogoUrl("  HTTP://img.example/logo.png "))
        assertFalse(isUsableLogoUrl(null))
        assertFalse(isUsableLogoUrl(""))
        assertFalse(isUsableLogoUrl("/relative/logo.png"))
        assertFalse(isUsableLogoUrl("file:///sdcard/logo.png"))
    }

    @Test fun `progress is null outside the programme and exact inside`() {
        assertEquals(0.5f, program.progressAt(1_800_000L)!!, 0.0001f)
        assertNull(program.progressAt(-1L))
        assertNull(program.progressAt(3_600_000L))
        assertNull(program.copy(endTimeMs = 0L).progressAt(0L))
    }

    @Test fun `remaining minutes round up and are null outside the programme`() {
        assertEquals(30, program.remainingMinutesAt(1_800_000L))
        assertEquals(1, program.remainingMinutesAt(3_599_000L))
        assertNull(program.remainingMinutesAt(3_600_000L))
    }

    @Test fun `metadata uses only existing parts`() {
        assertEquals("2026  ·  Drama  ·  ★ 8.1", joinMetadata("2026", " Drama ", null, "8.1"))
        assertNull(joinMetadata(null, "", " ", null))
    }

    @Test fun `episode label uses only existing parts`() {
        assertEquals("S01  ·  E02  ·  Pilot", formatEpisodeLabel(1, 2, "Pilot"))
        assertEquals("E05", formatEpisodeLabel(null, 5, null))
        assertNull(formatEpisodeLabel(null, null, " "))
    }

    @Test fun `player time formats minutes and hours`() {
        assertEquals("0:00", formatPlayerTime(-5L))
        assertEquals("1:05", formatPlayerTime(65_000L))
        assertEquals("1:01:01", formatPlayerTime(3_661_000L))
    }

    @Test fun `time format uses injected patterns and zone`() {
        val format = HudTimeFormat("EEE, d MMM, HH:mm", "HH:mm", Locale.US, TimeZone.getTimeZone("UTC"))
        assertEquals("Thu, 1 Jan, 00:00", format.clock(0L))
        assertEquals("00:00 – 01:00", format.range(0L, 3_600_000L))
    }
}
```

### 8.2 `HudTilesTest.kt`
```kotlin
package app.vireo.player.hud

import app.vireo.player.PlayerHudContent
import app.vireo.player.RecentLiveChannel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HudTilesTest {

    private fun live(vararg recents: RecentLiveChannel) = PlayerHudContent.Live(
        channelName = "Now", epgChannelId = null, streamId = "1", logoUrl = null,
        currentProgram = null, currentProgramDescription = null, nextProgram = null,
        recentChannels = recents.toList(),
    )

    private fun channel(streamId: String) = RecentLiveChannel(1L, streamId, "Ch $streamId", null, null, 0L)

    @Test fun `no actions means no tiles`() {
        assertTrue(buildHudTiles(live(channel("2")), hasSelectChannelAction = false, hasGuideAction = false).isEmpty())
    }

    @Test fun `current channel is excluded and guide comes first`() {
        val tiles = buildHudTiles(live(channel("1"), channel("2")), hasSelectChannelAction = true, hasGuideAction = true)
        assertEquals(listOf("guide", "1:2"), tiles.map { it.key })
    }
}
```

### 8.3 Additions to `PlayerHudStateTest.kt`
- Movie `PlayItem(logoUrl = "https://…")` → `Movie.logoUrl` equals it; `posterUrl`/`backdropUrl` are never copied into `logoUrl`.
- Series: same for `Series.logoUrl`.
- Item without `logoUrl` → `logoUrl == null` (no fallback to poster/backdrop).
- Live: the channel logo source is the one confirmed in open point 9.1.

---

## 9. Open points to verify in the repository (report in Phase 1)

1. **Live `logoUrl` source.** `buildPlayerHudState` and `PlayerViewModel` (~line 585) assign `logoUrl = item.posterUrl` for Live. If, for live items, `posterUrl` actually holds the Xtream channel icon (`stream_icon`), that is acceptable but misleading — then fill the new `PlayItem.logoUrl` at the live creation site and use it. If `posterUrl` can hold anything else, it must **not** be used as a logo. Decide from the code, do not guess.
2. **Where movie/series `PlayItem`s are created** (TMDB detail flow, library/resume flows, Xtream VOD) and whether the TMDB `logoUrl` is available there without new network calls. Items created without a logo simply fall back to the text title.
3. **EPG freshness.** `EpgNowNext` is a snapshot. Confirm that `PlayerViewModel` refreshes it at programme boundaries. The HUD already hides progress/remaining when `now` is outside the programme and never shows a stale mini-bar, but a finished programme's title should not linger.
4. **Channel switching and guide.** Does `PlayerViewModel` already have a method to switch to a recent live channel, and is there a Guide screen/route? If yes, wire `onSelectRecentChannel` / `onOpenGuide` in `PlayerScreen`. If not, either add one minimal VM method (justify it) or leave the tile row inert (it then simply does not render).
5. **HUD wake-up by remote.** With the HUD hidden, a D-pad press must still bring it back (today this relies on the full-screen clickable catcher being focusable). Preserve and test it.
6. **Focus order.** Verify Up/Down/Left/Right between close button → transport row → slider → tile row on a real remote; the focused element must keep the HUD visible (existing `hasFocusedControl` logic).
7. **Library versions.** Confirm `Icons.AutoMirrored.Filled.List`, `CircularProgressIndicator(trackColor = …)`, `AsyncImage(onError, filterQuality)` and `mutableLongStateOf` exist with BOM `2024.06.00` / Coil `2.6.0`; adjust to what compiles.
8. **`FocusableBox` / `VireoColors`** signatures and whether `VireoColors` should feed `HudColors` (keep one palette source if so).
9. **Buffering outside the HUD.** The spinner now lives inside the HUD (visible only when the HUD is visible). If buffering must be shown while the HUD is hidden, add a tiny standalone spinner — **never a loading screen or opaque overlay**. Report it; do not implement unasked.
10. **Hard-coded strings** in `HudStrings` should move to string resources if the project convention requires it.

---

## 10. Acceptance checklist (Definition of Done)

- [ ] No `AsyncImage` (or any image) covers the video; the full-screen backdrop code is gone.
- [ ] Pausing keeps the real frozen frame visible under the HUD.
- [ ] Logo is shown with `ContentScale.Fit`, never stretched, no background; text title only when there is no usable logo or it failed to load; never both for the same entity.
- [ ] Poster/backdrop are never used as a logo substitute.
- [ ] No placeholder or fabricated metadata anywhere (`"Live television"`, `0:00`, quality tags, empty labels are gone).
- [ ] Live shows: clock, logo/channel text, programme title, time range + remaining, description (≤ 3 lines), next line, programme timeline, optional tile row — each only with real data.
- [ ] Movies/Series/Other follow section 4.3–4.5.
- [ ] `PlayerHudOverlay` keeps its original ten parameters; `PlayerScreen` compiles unchanged (apart from optional wiring).
- [ ] Visibility, auto-hide, tap-to-toggle and focus behaviour are unchanged.
- [ ] Colours, spacing, typography and motion come from `HudTokens.kt` only.
- [ ] Previews exist for full-data and missing-data cases; unit tests pass.
- [ ] Final report lists: files added/modified/removed, removed code (explicitly the full-screen `AsyncImage`), and everything that could not be verified.

---

## 11. What to return

1. **Phase 1:** the assessment from section 0, then stop and wait.
2. **Phase 2 (after my "go"):** complete files (not fragments), a short change list, and the "could not verify" list.
