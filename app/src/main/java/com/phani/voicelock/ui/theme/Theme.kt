package com.phani.voicelock.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Ink = Color(0xFF0B0B14)
val Surface1 = Color(0xFF14141F)
val Surface2 = Color(0xFF1D1D2C)
val Violet = Color(0xFF8B7BFF)
val Mint = Color(0xFF3DDC97)
val Coral = Color(0xFFFF6B6B)
val TextHigh = Color(0xFFF2F2F8)
val TextMuted = Color(0xFF9A9AB0)

private val Scheme = darkColorScheme(
    primary = Violet,
    onPrimary = Ink,
    secondary = Mint,
    onSecondary = Ink,
    error = Coral,
    background = Ink,
    onBackground = TextHigh,
    surface = Surface1,
    onSurface = TextHigh,
    surfaceVariant = Surface2,
    onSurfaceVariant = TextMuted,
)

@Composable
fun VoiceLockTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}
