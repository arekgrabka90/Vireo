package app.vireo.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Graphite = Color(0xFF0E0F12)
private val Surface = Color(0xFF17191D)
private val SurfaceHigh = Color(0xFF22252B)
private val Accent = Color(0xFF7C5CFF)
private val TextPrimary = Color(0xFFF2F3F5)
private val TextSecondary = Color(0xFF9AA0AB)

private val Scheme = darkColorScheme(
    primary = Accent,
    onPrimary = Color.White,
    background = Graphite,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceHigh,
    onSurfaceVariant = TextSecondary,
)

@Composable
fun VireoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
