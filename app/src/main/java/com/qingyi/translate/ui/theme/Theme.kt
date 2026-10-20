package com.qingyi.translate.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val QingYiColors = lightColorScheme(
    primary = QingAccent,
    onPrimary = Color.White,
    primaryContainer = QingAccentSoft,
    onPrimaryContainer = QingOnSurface,
    background = QingBg,
    onBackground = QingOnSurface,
    surface = QingCard,
    onSurface = QingOnSurface,
    surfaceVariant = QingPill,
    onSurfaceVariant = QingMuted,
    outline = QingDivider,
    error = QingError,
)

@Composable
fun QingYiTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = QingYiColors,
        typography = Typography(),
        content = content,
    )
}
