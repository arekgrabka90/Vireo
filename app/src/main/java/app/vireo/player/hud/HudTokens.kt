package app.vireo.player.hud

import app.vireo.ui.theme.VireoColors
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal object HudColors {
    val Graphite = VireoColors.surface
    val GraphiteFocused = VireoColors.selectedSurface
    val TextPrimary = VireoColors.textPrimary
    val TextSecondary = VireoColors.textSecondary
    val TextTertiary = VireoColors.textSecondary.copy(alpha = 0.72f)
    val Hairline = VireoColors.outline
    val TimelineRemaining = VireoColors.textSecondary.copy(alpha = 0.34f)

    /**
     * The single accent. Monochrome by default (project palette: graphite/black/white).
     * To match the blue reference, change only this line, e.g. Color(0xFF4DA3FF).
     */
    val Accent = VireoColors.accent
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
