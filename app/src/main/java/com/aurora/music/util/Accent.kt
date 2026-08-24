package com.aurora.music.util

import androidx.compose.ui.graphics.Color
import kotlin.math.absoluteValue

val AccentPalette = listOf(
    Color(0xFF9B7CFF), Color(0xFFB267FF), Color(0xFF8B5CF6),
    Color(0xFF7C6DF2), Color(0xFF6366F1), Color(0xFF38BDF8),
    Color(0xFFC084FC), Color(0xFFA78BFA),
)

// stable accent per id so an item always looks the same
fun accentFor(seed: String): Color = AccentPalette[seed.hashCode().absoluteValue % AccentPalette.size]
