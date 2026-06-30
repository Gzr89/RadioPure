package com.radiopure.app.radiopure.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val RadioPureBackground = Color(0xFF141419)
val RadioPureSurface = Color(0xFF141419)

private val DarkColorScheme = darkColorScheme(
    background = RadioPureBackground,
    surface = RadioPureSurface,
    onBackground = Color.White,
    onSurface = Color.White,
)

@Composable
fun RadioPureTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content,
    )
}
