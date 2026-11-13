package com.jinian.anniversary.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.sp
import com.jinian.anniversary.data.settings.AppSettings
import com.jinian.anniversary.data.settings.ThemeColor
import com.jinian.anniversary.data.settings.ThemeMode

private val LightBackground = Color(0xFFF5F5F3)
private val LightSurface = Color(0xFFFCFCFB)
private val LightSurfaceLow = Color(0xFFF8F8F6)
private val LightSurfaceContainer = Color(0xFFF1F1EE)
private val LightSurfaceHigh = Color(0xFFECECEA)
private val DarkBackground = Color(0xFF0B0B0A)
private val DarkSurface = Color(0xFF151515)
private val DarkSurfaceLow = Color(0xFF1A1A19)
private val DarkSurfaceContainer = Color(0xFF20201E)
private val DarkSurfaceHigh = Color(0xFF262624)

private fun parse(hex: String) = Color(android.graphics.Color.parseColor(hex))

private fun blend(color: Color, target: Color, fraction: Float): Color {
    return Color(
        red = color.red + (target.red - color.red) * fraction,
        green = color.green + (target.green - color.green) * fraction,
        blue = color.blue + (target.blue - color.blue) * fraction,
        alpha = 1f,
    )
}

private fun relativeLuminance(color: Color): Float {
    return 0.2126f * color.red + 0.7152f * color.green + 0.0722f * color.blue
}

private fun colorScheme(themeColor: ThemeColor, dark: Boolean) = if (dark) {
    val base = parse(themeColor.hex)
    val primary = blend(base, Color.White, if (themeColor == ThemeColor.LEMON_YELLOW) 0.06f else 0.20f)
    val primaryContainer = blend(base, Color.Black, 0.20f)
    val onPrimaryContainer = blend(base, Color.White, 0.68f)
    val onPrimary = if (relativeLuminance(primary) > 0.62f) Color(0xFF141412) else Color.White
    darkColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = blend(primary, Color.White, 0.14f),
        tertiary = blend(primary, Color.White, 0.10f),
        background = DarkBackground,
        onBackground = Color(0xFFEDEDEB),
        surface = DarkSurface,
        onSurface = Color(0xFFEDEDEB),
        surfaceVariant = DarkSurfaceLow,
        onSurfaceVariant = Color(0xFFB9B9B5),
        surfaceTint = primary,
        surfaceContainerLowest = Color(0xFF0B0B0A),
        surfaceContainerLow = DarkSurfaceLow,
        surfaceContainer = DarkSurfaceContainer,
        surfaceContainerHigh = DarkSurfaceHigh,
        surfaceContainerHighest = Color(0xFF2C2C2A),
    )
} else {
    val base = parse(themeColor.hex)
    val primary = base
    val primaryContainer = blend(base, Color.White, 0.88f)
    val onPrimaryContainer = blend(base, Color.Black, 0.68f)
    val onPrimary = if (relativeLuminance(primary) > 0.62f) Color(0xFF141412) else Color.White
    lightColorScheme(
        primary = primary,
        onPrimary = onPrimary,
        primaryContainer = primaryContainer,
        onPrimaryContainer = onPrimaryContainer,
        secondary = primary,
        tertiary = primary,
        background = LightBackground,
        onBackground = Color(0xFF1B1B19),
        surface = LightSurface,
        onSurface = Color(0xFF1B1B19),
        surfaceVariant = LightSurfaceLow,
        onSurfaceVariant = Color(0xFF64645F),
        surfaceTint = primary,
        surfaceContainerLowest = Color(0xFFFFFFFF),
        surfaceContainerLow = LightSurfaceLow,
        surfaceContainer = LightSurfaceContainer,
        surfaceContainerHigh = LightSurfaceHigh,
        surfaceContainerHighest = Color(0xFFE4E4E1),
    )
}

private fun typography(dark: Boolean): Typography {
    val weight = if (dark) FontWeight(360) else FontWeight(400)
    val medium = if (dark) FontWeight(460) else FontWeight(500)
    val body = TextStyle(
        fontWeight = weight,
        lineHeight = 22.sp,
        lineBreak = LineBreak.Paragraph,
    )
    val title = TextStyle(
        fontWeight = medium,
        lineHeight = 30.sp,
        lineBreak = LineBreak.Paragraph,
    )
    val large = TextStyle(
        fontWeight = medium,
        lineHeight = 52.sp,
        lineBreak = LineBreak.Paragraph,
        fontFeatureSettings = "tnum",
    )
    return Typography(
        displayLarge = large.copy(fontSize = 52.sp),
        displayMedium = large.copy(fontSize = 42.sp),
        headlineMedium = title.copy(fontSize = 24.sp),
        headlineSmall = title.copy(fontSize = 20.sp),
        titleLarge = title.copy(fontSize = 20.sp),
        titleMedium = title.copy(fontSize = 16.sp, lineHeight = 24.sp),
        bodyLarge = body.copy(fontSize = 16.sp),
        bodyMedium = body.copy(fontSize = 14.sp, lineHeight = 20.sp),
        bodySmall = body.copy(fontSize = 12.sp, lineHeight = 16.sp),
        labelLarge = body.copy(fontSize = 14.sp, fontWeight = medium),
        labelMedium = body.copy(fontSize = 12.sp, fontWeight = weight),
        labelSmall = body.copy(fontSize = 11.sp, fontWeight = weight),
    )
}

@Composable
fun JinianTheme(
    settings: AppSettings,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    MaterialTheme(
        colorScheme = colorScheme(settings.themeColor, darkTheme),
        typography = typography(darkTheme),
        content = content,
    )
}