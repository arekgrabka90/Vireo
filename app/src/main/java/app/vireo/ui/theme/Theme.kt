package app.vireo.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val Graphite = Color(0xFF0E0F12)
private val Surface = Color(0xFF17191D)
private val SurfaceHigh = Color(0xFF22252B)
private val Accent = Color(0xFF7C5CFF)
private val TextPrimary = Color(0xFFF2F3F5)
private val TextSecondary = Color(0xFF9AA0AB)

private val VireoShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val Scheme = darkColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    background = Graphite,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceHigh,
    onSurfaceVariant = TextSecondary,
    primaryContainer = Color(0xFF2B2252),
    onPrimaryContainer = Color(0xFFEAE4FF),
    secondary = Color(0xFFB8A9FF),
    outline = Color(0xFF353943),
    surfaceTint = Accent,
)

@Composable
fun VireoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Scheme,
        shapes = VireoShapes,
        content = content,
    )
}
