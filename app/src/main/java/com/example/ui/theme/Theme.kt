package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private val AmoledDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = VoidBlack,
    primaryContainer = CyanDim,
    onPrimaryContainer = TextPrimary,
    secondary = UltraViolet,
    onSecondary = TextPrimary,
    secondaryContainer = ElevatedObsidian,
    onSecondaryContainer = TextPrimary,
    tertiary = NeonEmerald,
    onTertiary = VoidBlack,
    background = VoidBlack,
    onBackground = TextPrimary,
    surface = ObsidianSurface,
    onSurface = TextPrimary,
    surfaceVariant = ElevatedObsidian,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder,
    outlineVariant = TextMuted,
    error = CrimsonAlert,
    onError = TextPrimary
)

private val AppShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun DynamicIslandProTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AmoledDarkColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
