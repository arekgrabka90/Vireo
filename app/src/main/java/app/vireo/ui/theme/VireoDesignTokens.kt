package app.vireo.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Central color palette for VIREO Premium UI.
 *
 * White is the primary accent. Supporting colors remain neutral
 * to preserve contrast and the cinematic dark appearance.
 */
object VireoColors {
    val background = Color(0xFF0E0F12)
    val surface = Color(0xFF17191D)
    val surfaceElevated = Color(0xFF22252B)

    val accent = Color(0xFFFFFFFF)
    val onAccent = Color(0xFF0E0F12)
    val accentContainer = Color(0xFF30343D)
    val onAccentContainer = Color(0xFFF2F3F5)

    val textPrimary = Color(0xFFF2F3F5)
    val textSecondary = Color(0xFF9AA0AB)

    val secondary = Color(0xFFC4C8D0)
    val tertiary = Color(0xFFB6BBC5)

    val outline = Color(0xFF353943)
    val outlineVariant = Color(0xFF292D35)

    val selectedSurface = Color(0xFF30343D)
    val pressedSurface = Color(0xFF3A3F49)
}

/**
 * Shared spacing scale.
 * Components can reuse these values instead of defining
 * unrelated spacing measurements.
 */
object VireoSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
    val xxxl = 40.dp
    val huge = 48.dp
}

/**
 * Shared corner radius scale.
 */
object VireoRadii {
    val xs = 6.dp
    val sm = 10.dp
    val md = 14.dp
    val lg = 18.dp
    val xl = 24.dp
    val xxl = 30.dp
}
