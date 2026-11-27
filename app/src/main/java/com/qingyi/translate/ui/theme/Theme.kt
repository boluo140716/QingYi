package com.qingyi.translate.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = LightAccent,
    onPrimary = LightOnAccent,
    primaryContainer = LightAccentSoft,
    onPrimaryContainer = LightOnSurface,
    background = LightBg,
    onBackground = LightOnSurface,
    surface = LightCard,
    onSurface = LightOnSurface,
    surfaceVariant = LightPill,
    onSurfaceVariant = LightMuted,
    outline = LightDivider,
    error = LightError,
)

private val DarkColors = darkColorScheme(
    primary = DarkAccent,
    onPrimary = DarkOnAccent,
    primaryContainer = DarkAccentSoft,
    onPrimaryContainer = DarkOnSurface,
    background = DarkBg,
    onBackground = DarkOnSurface,
    surface = DarkCard,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkPill,
    onSurfaceVariant = DarkMuted,
    outline = DarkDivider,
    error = DarkError,
)

@Composable
fun QingYiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        typography = Typography(),
        content = content,
    )
}
