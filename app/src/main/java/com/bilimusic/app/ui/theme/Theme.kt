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
    headlineLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 30.sp),
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
    onPrimary = Color(0xFF071013),
    primaryContainer = seed.copy(alpha = 0.22f),
    onPrimaryContainer = Color(0xFFEAFDFF),
    secondary = Color(0xFFFFCF70),
    onSecondary = Color(0xFF241600),
    secondaryContainer = Color(0xFF332B1E),
    onSecondaryContainer = Color(0xFFFFE2A8),
    background = Color(0xFF08090B),
    onBackground = Color(0xFFF3F4F6),
    surface = Color(0xFF111318),
    onSurface = Color(0xFFF3F4F6),
    surfaceVariant = Color(0xFF1C2027),
    onSurfaceVariant = Color(0xFFB8C0CC),
    error = Color(0xFFFFB4AB),
    outline = Color(0xFF343A44)
)

private fun lightScheme(seed: Color) = lightColorScheme(
    primary = seed,
    onPrimary = Color.White,
    primaryContainer = seed.copy(alpha = 0.14f),
    onPrimaryContainer = Color(0xFF0B3D49),
    secondary = Color(0xFFE4A72D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFF1CC),
    onSecondaryContainer = Color(0xFF3B2600),
    background = Color(0xFFF7F8FA),
    onBackground = Color(0xFF111318),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF111318),
    surfaceVariant = Color(0xFFE9ECF1),
    onSurfaceVariant = Color(0xFF58606B),
    error = Color(0xFFB3261E),
    outline = Color(0xFFD5DAE1)
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
