package com.example.restredder.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Green40,
    onPrimary = Color.White,
    primaryContainer = GreenContainerLight,
    onPrimaryContainer = OnGreenContainerLight,

    secondary = Brown40,
    onSecondary = Color.White,
    secondaryContainer = BrownContainerLight,
    onSecondaryContainer = OnBrownContainerLight,

    tertiary = Honey40,
    onTertiary = Color.White,
    tertiaryContainer = HoneyContainerLight,
    onTertiaryContainer = OnHoneyContainerLight,

    background = Cream,
    onBackground = OnCream,
    surface = Cream,
    onSurface = OnCream,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight
)

private val DarkColorScheme = darkColorScheme(
    primary = Green80,
    onPrimary = OnGreen80,
    primaryContainer = GreenContainerDark,
    onPrimaryContainer = GreenContainerLight,

    secondary = Brown80,
    onSecondary = OnBrown80,
    secondaryContainer = BrownContainerDark,
    onSecondaryContainer = BrownContainerLight,

    tertiary = Honey80,
    onTertiary = OnHoney80,
    tertiaryContainer = HoneyContainerDark,
    onTertiaryContainer = HoneyContainerLight,

    background = WarmDark,
    onBackground = OnWarmDark,
    surface = WarmDark,
    onSurface = OnWarmDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark
)

@Composable
fun RestRedderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // No dynamic (wallpaper) colors: the app always uses its own palette.
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
