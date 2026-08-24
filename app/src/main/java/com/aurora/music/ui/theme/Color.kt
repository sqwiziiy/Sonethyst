package com.aurora.music.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Sonethyst brand palette.
// Used when Material You is disabled. Dynamic Color remains fully independent.
val SonethystPurple = Color(0xFF9B7CFF)
val SonethystPurpleDeep = Color(0xFF6848C8)
val SonethystAmethyst = Color(0xFFB267FF)
val SonethystLavender = Color(0xFFC4B5FD)
val SonethystPurpleSoft = Color(0xFFE6DAFF)

val DarkBackground = Color(0xFF0D0A12)
val DarkSurface = Color(0xFF17121F)
val DarkSurfaceElevated = Color(0xFF20182B)
val DarkSurfaceHigh = Color(0xFF2A2037)
val DarkOutline = Color(0xFF40344D)
val TextPrimaryDark = Color(0xFFF6F1FA)
val TextSecondaryDark = Color(0xFFB9ADBF)

val LightBackground = Color(0xFFFCF9FF)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceElevated = Color(0xFFF5EFFA)
val LightOutline = Color(0xFFE5D9EE)
val TextPrimaryLight = Color(0xFF19131F)
val TextSecondaryLight = Color(0xFF62566B)

val PlayerGradient = listOf(
    Color(0xFF2D1E4A),
    Color(0xFF171020),
    DarkBackground,
)

val AuthGradient = listOf(
    Color(0xFF24153D),
    DarkBackground,
)

fun brandGradient() =
    Brush.linearGradient(listOf(SonethystPurple, SonethystAmethyst))

data class AccentPreset(val name: String, val seed: Color)

val AccentPresets = listOf(
    AccentPreset("Sonethyst", SonethystPurple),
    AccentPreset("Amethyst", SonethystAmethyst),
    AccentPreset("Lavender", SonethystLavender),
    AccentPreset("Violet", Color(0xFF8B5CF6)),
    AccentPreset("Blue", Color(0xFF3B82F6)),
    AccentPreset("Sky", Color(0xFF38BDF8)),
    AccentPreset("Teal", Color(0xFF14B8A6)),
    AccentPreset("Green", Color(0xFF22C55E)),
    AccentPreset("Amber", Color(0xFFF7B733)),
    AccentPreset("Rose", Color(0xFFFF4F91)),
    AccentPreset("Mono", Color(0xFFB8B0B4)),
)
