package com.ingenico.ecrsimulator.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

private val ColorScheme = androidx.compose.material3.darkColorScheme(
    primary = Palette.mint,
    secondary = Palette.cyan,
    tertiary = Palette.teal,
    background = Palette.background,
    surface = Palette.deepSurface,
    onPrimary = Palette.deepSurface,
    onSecondary = Palette.deepSurface,
    onTertiary = Palette.deepSurface,
    onBackground = Palette.white,
    onSurface = Palette.white
)

@Composable
fun ECRSimulatorTheme(
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ColorScheme,
        typography = Typography,
        content = content
    )
}