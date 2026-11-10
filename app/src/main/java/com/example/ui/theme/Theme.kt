package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val NomaticDarkColorScheme = darkColorScheme(
    primary = NomaticRed,
    onPrimary = Color.White,
    primaryContainer = NomaticRedSoft,
    onPrimaryContainer = NomaticWhite,
    secondary = NomaticWhite,
    onSecondary = NomaticBlack,
    secondaryContainer = NomaticElevatedDark,
    onSecondaryContainer = NomaticWhite,
    tertiary = NomaticEmerald,
    onTertiary = NomaticBlack,
    background = NomaticBlack,
    onBackground = NomaticWhite,
    surface = NomaticCarbon,
    onSurface = NomaticWhite,
    surfaceVariant = NomaticSurfaceDark,
    onSurfaceVariant = NomaticGrayLight,
    outline = NomaticBorderDark,
    outlineVariant = NomaticElevatedDark,
    error = NomaticRedBright,
    onError = Color.White
)

private val NomaticLightColorScheme = lightColorScheme(
    primary = NomaticRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD8),
    onPrimaryContainer = NomaticInk,
    secondary = NomaticInk,
    onSecondary = Color.White,
    secondaryContainer = NomaticElevatedLight,
    onSecondaryContainer = NomaticInk,
    tertiary = NomaticEmerald,
    onTertiary = Color.White,
    background = NomaticPaper,
    onBackground = NomaticInk,
    surface = NomaticSurfaceLight,
    onSurface = NomaticInk,
    surfaceVariant = NomaticElevatedLight,
    onSurfaceVariant = NomaticInkMuted,
    outline = NomaticBorderLight,
    outlineVariant = NomaticElevatedLight,
    error = NomaticRed,
    onError = Color.White
)

val NomaticShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(26.dp)
)

@Composable
fun NomaticTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) NomaticDarkColorScheme else NomaticLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = NomaticShapes,
        content = content
    )
}
