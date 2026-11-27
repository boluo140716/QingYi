package com.qingyi.translate.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color

// A calm mint palette in the spirit of Google Translate, but with its own
// accent so the app does not impersonate Google's trade dress.

// Light ---------------------------------------------------------------------
internal val LightBg = Color(0xFFE8F1E4)
internal val LightCard = Color(0xFFF3F8F0)
internal val LightPill = Color(0xFFFBFDF9)
internal val LightAccent = Color(0xFF2E6B4F)
internal val LightAccentSoft = Color(0xFFD6E9D8)
internal val LightOnAccent = Color(0xFFFFFFFF)
internal val LightOnSurface = Color(0xFF1E2B22)
internal val LightMuted = Color(0xFF68786C)
internal val LightDivider = Color(0xFFDCE6DA)
internal val LightError = Color(0xFFB3261E)

// Dark: same mint identity, lifted so text keeps its contrast on near-black. --
internal val DarkBg = Color(0xFF101512)
internal val DarkCard = Color(0xFF18201A)
internal val DarkPill = Color(0xFF202A22)
internal val DarkAccent = Color(0xFF8FD3AC)
internal val DarkAccentSoft = Color(0xFF223328)
internal val DarkOnAccent = Color(0xFF10251A)
internal val DarkOnSurface = Color(0xFFE4EDE6)
internal val DarkMuted = Color(0xFF93A398)
internal val DarkDivider = Color(0xFF27332A)
internal val DarkError = Color(0xFFF2A6A0)

private val isDark: Boolean
    @Composable
    @ReadOnlyComposable
    get() = isSystemInDarkTheme()

val QingBg: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isDark) DarkBg else LightBg

val QingCard: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isDark) DarkCard else LightCard

val QingPill: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isDark) DarkPill else LightPill

val QingAccent: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isDark) DarkAccent else LightAccent

val QingAccentSoft: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isDark) DarkAccentSoft else LightAccentSoft

/** Text and icons drawn on top of [QingAccent] fills. */
val QingOnAccent: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isDark) DarkOnAccent else LightOnAccent

val QingOnSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isDark) DarkOnSurface else LightOnSurface

val QingMuted: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isDark) DarkMuted else LightMuted

val QingDivider: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isDark) DarkDivider else LightDivider

val QingError: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isDark) DarkError else LightError
