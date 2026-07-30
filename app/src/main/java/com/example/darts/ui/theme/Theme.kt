package com.example.darts.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = LimePrimary,
    onPrimary = BlackPrimary,

    secondary = LimeSecondary,
    onSecondary = BlackPrimary,

    background = BlackPrimary,
    onBackground = TextPrimary,

    surface = BlackSurface,
    onSurface = TextPrimary,

    // Optional but useful
    outline = Divider
)

@Composable
fun DartsTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}