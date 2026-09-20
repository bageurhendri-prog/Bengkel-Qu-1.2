package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class AppColorTheme(val label: String, val primaryColor: Color) {
    BENGKEL_GREEN("Hijau Bengkel Qu", BengkelGreenPrimary),
    EMERALD_RACING("Emerald Sport", EmeraldPrimary),
    OCEAN_TEAL("Ocean Teal", TealPrimary),
    DARK_SLATE("Dark Slate Pro", DarkSlatePrimary)
}

private val BengkelGreenColorScheme = lightColorScheme(
    primary = BengkelGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = BengkelGreenContainer,
    onPrimaryContainer = BengkelGreenPrimaryVariant,
    secondary = BengkelGreenLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC8E6C9),
    onSecondaryContainer = Color(0xFF1B5E20),
    tertiary = BengkelAccentOrange,
    onTertiary = Color.White,
    background = BengkelGreenBackground,
    onBackground = Color(0xFF1A1C19),
    surface = BengkelGreenSurface,
    onSurface = Color(0xFF1A1C19),
    surfaceVariant = Color(0xFFE0E5DF),
    onSurfaceVariant = Color(0xFF434842),
    outline = Color(0xFF737971)
)

private val EmeraldColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = Color(0xFF004D40),
    secondary = Color(0xFF26A69A),
    onSecondary = Color.White,
    background = Color(0xFFF4FAF9),
    onBackground = Color(0xFF191C1C),
    surface = Color.White,
    onSurface = Color(0xFF191C1C)
)

private val TealColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    primaryContainer = TealContainer,
    onPrimaryContainer = Color(0xFF004D40),
    secondary = Color(0xFF00ACC1),
    onSecondary = Color.White,
    background = Color(0xFFF2F9FA),
    onBackground = Color(0xFF191C1C),
    surface = Color.White,
    onSurface = Color(0xFF191C1C)
)

private val DarkSlateColorScheme = darkColorScheme(
    primary = DarkSlatePrimary,
    onPrimary = Color(0xFF003915),
    primaryContainer = Color(0xFF005322),
    onPrimaryContainer = Color(0xFF8FF89E),
    secondary = Color(0xFF81C784),
    onSecondary = Color(0xFF003917),
    background = DarkSlateBackground,
    onBackground = Color(0xFFE1E3DE),
    surface = DarkSlateSurface,
    onSurface = Color(0xFFE1E3DE),
    surfaceVariant = DarkSlateCard,
    onSurfaceVariant = Color(0xFFC2C9BD)
)

@Composable
fun MyApplicationTheme(
    selectedTheme: AppColorTheme = AppColorTheme.BENGKEL_GREEN,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme: ColorScheme = when (selectedTheme) {
        AppColorTheme.BENGKEL_GREEN -> if (darkTheme) DarkSlateColorScheme else BengkelGreenColorScheme
        AppColorTheme.EMERALD_RACING -> if (darkTheme) DarkSlateColorScheme else EmeraldColorScheme
        AppColorTheme.OCEAN_TEAL -> if (darkTheme) DarkSlateColorScheme else TealColorScheme
        AppColorTheme.DARK_SLATE -> DarkSlateColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
