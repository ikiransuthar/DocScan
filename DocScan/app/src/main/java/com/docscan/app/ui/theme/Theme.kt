package com.docscan.app.ui.theme

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

private val DarkColors = darkColorScheme(
    primary = Mint,
    onPrimary = Ink,
    secondary = Azure,
    background = Ink,
    onBackground = Cloud,
    surface = InkRaised,
    onSurface = Cloud,
    surfaceVariant = InkCard,
    onSurfaceVariant = Mist,
    outline = Hairline,
    error = Coral,
    surfaceContainerLowest = Ink,
    surfaceContainerLow = InkRaised,
    surfaceContainer = InkRaised,
    surfaceContainerHigh = InkCard,
    surfaceContainerHighest = InkCard
)

@Composable
fun DocScanTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColors,
        typography = AppTypography,
        shapes = AppShapes
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = DarkColors.background,
            contentColor = DarkColors.onBackground,
            content = content
        )
    }
}
