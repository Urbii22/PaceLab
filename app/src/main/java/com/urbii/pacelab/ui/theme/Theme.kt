package com.urbii.pacelab.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF416A5A),
    onPrimary = Color.White,
    secondary = Color(0xFF52645B),
    tertiary = Color(0xFF3F6371),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA5D2BB),
    secondary = Color(0xFFB7CCBF),
    tertiary = Color(0xFFA7CFDD),
)

@Composable
fun PaceLabTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors, content = content)
}
