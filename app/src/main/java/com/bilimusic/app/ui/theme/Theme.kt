package com.bilimusic.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val BiliTypography = androidx.compose.material3.Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 18.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp)
)

const val DEFAULT_SEED_COLOR = "00A1D6"
val PRESET_COLORS = listOf("00A1D6", "6750A4", "B3261E", "00897B", "388E3C", "FBC02D")

private fun hexToColor(hex: String): Color {
    return Color(android.graphics.Color.parseColor("#$hex"))
}

private fun darkScheme(seed: Color) = darkColorScheme(
    primary = seed,
    onPrimary = Color.White,
    primaryContainer = seed.copy(alpha = 0.2f),
    onPrimaryContainer = seed,
    secondary = Color(0xFF9C9CFF),
    onSecondary = Color(0xFF1A1A2E),
    secondaryContainer = seed.copy(alpha = 0.15f),
    onSecondaryContainer = seed.copy(alpha = 0.9f),
    background = Color(0xFF0F0F0F),
    onBackground = Color(0xFFE8E8E8),
    surface = Color(0xFF1A1A1A),
    onSurface = Color(0xFFE8E8E8),
    surfaceVariant = Color(0xFF2A2A2A),
    onSurfaceVariant = Color(0xFFB0B0B0),
    error = Color(0xFFCF6679),
    outline = Color(0xFF444444)
)

private fun lightScheme(seed: Color) = lightColorScheme(
    primary = seed,
    onPrimary = Color.White,
    primaryContainer = seed.copy(alpha = 0.15f),
    onPrimaryContainer = seed,
    secondary = Color(0xFF5C5CFF),
    onSecondary = Color.White,
    secondaryContainer = seed.copy(alpha = 0.1f),
    onSecondaryContainer = seed.copy(alpha = 0.8f),
    background = Color(0xFFF8F8F8),
    onBackground = Color(0xFF1A1A1A),
    surface = Color.White,
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFE8E8E8),
    onSurfaceVariant = Color(0xFF555555),
    error = Color(0xFFB3261E),
    outline = Color(0xFFCCCCCC)
)

@Composable
fun BiliMusicTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = true,
    seedColorHex: String = DEFAULT_SEED_COLOR,
    content: @Composable () -> Unit
) {
    val isDark = if (darkTheme) true else androidx.compose.foundation.isSystemInDarkTheme()
    val seed = hexToColor(seedColorHex)
    val colorScheme = if (isDark) darkScheme(seed) else lightScheme(seed)

    MaterialTheme(
        colorScheme = colorScheme,
        typography = BiliTypography,
        content = content
    )
}
