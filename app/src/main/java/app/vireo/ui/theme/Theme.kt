package app.vireo.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val VireoMaterialShapes = Shapes(
    extraSmall = RoundedCornerShape(VireoRadii.xs),
    small = RoundedCornerShape(VireoRadii.sm),
    medium = RoundedCornerShape(VireoRadii.md),
    large = RoundedCornerShape(VireoRadii.xl),
    extraLarge = RoundedCornerShape(VireoRadii.xxl),
)

private val VireoColorScheme = darkColorScheme(
    primary = VireoColors.accent,
    onPrimary = VireoColors.onAccent,
    primaryContainer = VireoColors.accentContainer,
    onPrimaryContainer = VireoColors.onAccentContainer,

    secondary = VireoColors.secondary,
    onSecondary = VireoColors.onAccent,
    secondaryContainer = VireoColors.selectedSurface,
    onSecondaryContainer = VireoColors.textPrimary,

    tertiary = VireoColors.tertiary,
    onTertiary = VireoColors.onAccent,
    tertiaryContainer = VireoColors.selectedSurface,
    onTertiaryContainer = VireoColors.textPrimary,

    background = VireoColors.background,
    onBackground = VireoColors.textPrimary,

    surface = VireoColors.surface,
    onSurface = VireoColors.textPrimary,
    surfaceVariant = VireoColors.surfaceElevated,
    onSurfaceVariant = VireoColors.textSecondary,

    outline = VireoColors.outline,
    outlineVariant = VireoColors.outlineVariant,

    surfaceTint = VireoColors.background,
)

private val VireoTypography = Typography().let { base ->
    base.copy(
        displayLarge = base.displayLarge.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-1.2).sp,
        ),
        displayMedium = base.displayMedium.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.8).sp,
        ),
        headlineLarge = base.headlineLarge.copy(
            fontWeight = FontWeight.SemiBold,
            letterSpacing = (-0.4).sp,
        ),
        headlineMedium = base.headlineMedium.copy(
            fontWeight = FontWeight.SemiBold,
        ),
        titleLarge = base.titleLarge.copy(
            fontWeight = FontWeight.SemiBold,
        ),
    )
}

@Composable
fun VireoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VireoColorScheme,
        typography = VireoTypography,
        shapes = VireoMaterialShapes,
        content = content,
    )
}
