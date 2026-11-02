package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = GeminiDarkPrimary,
    onPrimary = GeminiDarkOnPrimary,
    primaryContainer = GeminiDarkPrimaryContainer,
    onPrimaryContainer = GeminiDarkOnPrimaryContainer,
    secondary = GeminiDarkSecondary,
    onSecondary = GeminiDarkOnSecondary,
    secondaryContainer = GeminiDarkSecondaryContainer,
    onSecondaryContainer = GeminiDarkOnSecondaryContainer,
    tertiary = GeminiDarkTertiary,
    background = GeminiDarkBackground,
    onBackground = GeminiDarkTextPrimary,
    surface = GeminiDarkSurface,
    onSurface = GeminiDarkTextPrimary,
    surfaceVariant = GeminiDarkSurfaceVariant,
    onSurfaceVariant = GeminiDarkTextSecondary,
    outline = GeminiDarkOutline
)

private val LightColorScheme = lightColorScheme(
    primary = GeminiLightPrimary,
    onPrimary = GeminiLightOnPrimary,
    primaryContainer = GeminiLightPrimaryContainer,
    onPrimaryContainer = GeminiLightOnPrimaryContainer,
    secondary = GeminiLightSecondary,
    onSecondary = GeminiLightOnSecondary,
    secondaryContainer = GeminiLightSecondaryContainer,
    onSecondaryContainer = GeminiLightOnSecondaryContainer,
    tertiary = GeminiLightTertiary,
    background = GeminiLightBackground,
    onBackground = GeminiLightTextPrimary,
    surface = GeminiLightSurface,
    onSurface = GeminiLightTextPrimary,
    surfaceVariant = GeminiLightSurfaceVariant,
    onSurfaceVariant = GeminiLightTextSecondary,
    outline = GeminiLightOutline
)

val ExpressiveShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun MemoFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = ExpressiveShapes,
        content = content
    )
}
