package com.snowi.snuzznoise.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// 🔹 Enum to handle multiple themes
enum class AppTheme(val label: String, val primaryColor: Color) {
    SAGE("Sage", Sage),
    SUNSET("Sunset", SunsetPrimary),
    NEON("Neon", NeonPrimary),
    OCEAN("Ocean", OceanPrimary)
}

// 🌿 Sage Light (Fixed Contrast)
private val LightSageColors = lightColorScheme(
    primary = Sage,
    onPrimary = SageOnPrimary, // Fixed: White Text on Green Button
    primaryContainer = SageLight,
    onPrimaryContainer = SageDark,

    secondary = SageSecondary,
    onSecondary = SageOnBackground, // Fixed: Dark Text on Light Green
    secondaryContainer = SageLight,
    onSecondaryContainer = SageDark,

    tertiary = SageTertiary,
    onTertiary = SageOnBackground,
    tertiaryContainer = SageLight,
    onTertiaryContainer = SageDark,

    background = SageBackground,
    onBackground = SageOnBackground, // Fixed: Dark Text on Light Background
    surface = Color.White,
    onSurface = SageOnBackground,    // Fixed: Dark Text on White Surface

    surfaceVariant = SageSurfaceVariant,
    onSurfaceVariant = SageOnBackground,
    outline = SageOutline,

    error = SageError,
    onError = SageOnError
)

private val DarkSageColors = darkColorScheme(
    primary = SagePrimaryVibrant,
    onPrimary = SageOnDarkSurface,
    primaryContainer = SageDarkSurface,
    onPrimaryContainer = SagePrimaryVibrant,

    secondary = SageSecondaryVibrant,
    onSecondary = SageOnDarkSurface,
    secondaryContainer = SageDarkSurface,
    onSecondaryContainer = SageSecondaryVibrant,

    tertiary = SageTertiaryVibrant,
    onTertiary = SageOnDarkSurface,
    tertiaryContainer = SageDarkSurface,
    onTertiaryContainer = SageTertiaryVibrant,

    background = SageDarkBackground,
    onBackground = SageOnDarkSurface,
    surface = SageDarkSurface,
    onSurface = SageOnDarkSurface,

    surfaceVariant = SageOutlineDark,
    onSurfaceVariant = SageOnDarkSurface,
    outline = SageOutlineDark,

    error = Color(0xFFF08787),
    onError = SageOnDark // Fixed error text color
)

// 🌅 Tropical Sunset
private val LightSunsetColors = lightColorScheme(
    primary = SunsetPrimary,
    onPrimary = Color.White,
    secondary = SunsetSecondary,
    onSecondary = Color.Black,
    tertiary = SunsetTertiary,
    background = SunsetBackground,
    onBackground = Color.Black, // Ensure text is visible
    surface = Color.White,
    onSurface = Color.Black
)

private val DarkSunsetColors = darkColorScheme(
    primary = SunsetPrimary,
    onPrimary = Color.Black,
    secondary = SunsetSecondary,
    onSecondary = Color.Black,
    tertiary = SunsetTertiary,
    background = SunsetDarkBackground,
    surface = SunsetDarkBackground
)

// 🌌 Cyber Neon
private val LightNeonColors = lightColorScheme(
    primary = NeonPrimary,
    onPrimary = Color.White,
    secondary = NeonSecondary,
    onSecondary = Color.Black,
    tertiary = NeonTertiary,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black
)

private val DarkNeonColors = darkColorScheme(
    primary = NeonPrimary,
    onPrimary = Color.White,
    secondary = NeonSecondary,
    onSecondary = Color.Black,
    tertiary = NeonTertiary,
    background = NeonDarkBackground,
    surface = NeonDarkBackground
)

// 🌊 Ocean Breeze
private val LightOceanColors = lightColorScheme(
    primary = OceanPrimary,
    onPrimary = Color.White,
    secondary = OceanSecondary,
    onSecondary = Color.White,
    tertiary = OceanTertiary,
    background = OceanBackground,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black
)

private val DarkOceanColors = darkColorScheme(
    primary = OceanPrimary,
    onPrimary = Color.Black,
    secondary = OceanSecondary,
    onSecondary = Color.White,
    tertiary = OceanTertiary,
    background = OceanDarkBackground,
    surface = OceanDarkBackground
)

// 🔹 Main Theme Composable
@Composable
fun SnuzzNoiseTheme(
    appTheme: AppTheme = AppTheme.SAGE,
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = when (appTheme) {
        AppTheme.SAGE -> if (useDarkTheme) DarkSageColors else LightSageColors
        AppTheme.SUNSET -> if (useDarkTheme) DarkSunsetColors else LightSunsetColors
        AppTheme.NEON -> if (useDarkTheme) DarkNeonColors else LightNeonColors
        AppTheme.OCEAN -> if (useDarkTheme) DarkOceanColors else LightOceanColors
    }

    MaterialTheme(
        colorScheme = colors,
        // typography = AppTypography, // Uncomment if you have typography defined
        content = content
    )
}