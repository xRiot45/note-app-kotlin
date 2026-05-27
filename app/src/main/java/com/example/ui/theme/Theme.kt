package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = PureWhite,
    onPrimary = PureBlack,
    secondary = Neutral400,
    onSecondary = PureWhite,
    background = Neutral950Dark,
    onBackground = PureWhite,
    surface = Neutral900Dark,
    onSurface = PureWhite,
    surfaceVariant = Neutral800,
    onSurfaceVariant = PureWhite,
    outline = Neutral800
)

private val LightColorScheme = lightColorScheme(
    primary = PureBlack,
    onPrimary = PureWhite,
    secondary = Neutral500,
    onSecondary = Neutral950,
    background = PureWhite,
    onBackground = Neutral950,
    surface = PureWhite,
    onSurface = Neutral950,
    surfaceVariant = Neutral100,
    onSurfaceVariant = Neutral950,
    outline = Neutral200
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Strict Black & White theme as requested
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
