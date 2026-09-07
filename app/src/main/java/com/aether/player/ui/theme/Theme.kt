package com.aether.player.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aether.player.data.prefs.AccentColor
import com.aether.player.data.prefs.AetherSettings
import com.aether.player.data.prefs.PerformanceMode
import com.aether.player.data.prefs.ThemeMode

val LocalGlass = staticCompositionLocalOf { GlassTokens() }
val LocalAnimScale = staticCompositionLocalOf { 1f }

fun animDur(baseMs: Int, scale: Float): Int =
    (baseMs * scale).toInt().coerceIn(1, 2000)

@Immutable
data class GlassTokens(
    val surface: Color = Color.White.copy(alpha = 0.08f),
    val surfaceStrong: Color = Color.White.copy(alpha = 0.14f),
    val border: Color = Color.White.copy(alpha = 0.14f),
    val highlight: Color = Color.White.copy(alpha = 0.22f),
    val blur: Boolean = true,
    val intensity: Float = 0.72f,
    val corner = 24.dp,
)

fun accentOf(accent: AccentColor): Color = when (accent) {
    AccentColor.CYAN -> Color(0xFF5CE1E6)
    AccentColor.BLUE -> Color(0xFF4DA3FF)
    AccentColor.PURPLE -> Color(0xFFB08CFF)
    AccentColor.GREEN -> Color(0xFF6EE7B7)
    AccentColor.ORANGE -> Color(0xFFFFB86B)
    AccentColor.RED -> Color(0xFFFF6B81)
    AccentColor.PINK -> Color(0xFFFF8BD2)
    AccentColor.SYSTEM -> Color(0xFF5CE1E6)
}

@Composable
fun AetherTheme(
    settings: AetherSettings = AetherSettings(),
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (settings.themeMode) {
        ThemeMode.AUTO -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK, ThemeMode.AMOLED -> true
    }
    val amoled = settings.themeMode == ThemeMode.AMOLED || (settings.themeMode == ThemeMode.AUTO && systemDark)
    val accent = accentOf(settings.accent)
    val context = LocalContext.current
    val dynamic = settings.dynamicColor && settings.accent == AccentColor.SYSTEM && Build.VERSION.SDK_INT >= 31
    val hi = settings.highContrast
    val scheme = when {
        dynamic && dark -> dynamicDarkColorScheme(context)
        dynamic && !dark -> dynamicLightColorScheme(context)
        dark -> darkColorScheme(
            primary = accent,
            onPrimary = Color(0xFF041016),
            secondary = Color(0xFF9AA4FF),
            tertiary = Color(0xFF7CFFE1),
            background = if (amoled || hi) Color.Black else Color(0xFF0C0D14),
            surface = if (amoled || hi) Color.Black else Color(0xFF12131C),
            onBackground = Color.White,
            onSurface = Color.White,
            onSurfaceVariant = if (hi) Color.White else Color(0xFFB7BCC9),
            outline = if (hi) Color.White.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.12f),
            error = Color(0xFFFF6B81),
        )
        else -> lightColorScheme(
            primary = accent,
            onPrimary = Color.White,
            background = Color(0xFFF3F5FA),
            surface = Color.White,
            onBackground = Color.Black,
            onSurface = Color.Black,
            onSurfaceVariant = if (hi) Color.Black else Color(0xFF3A3F4B),
            outline = if (hi) Color.Black.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.1f),
        )
    }
    val glass = GlassTokens(
        surface = if (dark) Color.White.copy(alpha = 0.06f + settings.glassIntensity * 0.06f)
        else Color.White.copy(alpha = 0.55f),
        surfaceStrong = if (dark) Color.White.copy(alpha = 0.12f + settings.glassIntensity * 0.08f)
        else Color.White.copy(alpha = 0.78f),
        border = when {
            hi && dark -> Color.White.copy(alpha = 0.38f)
            hi -> Color.Black.copy(alpha = 0.3f)
            dark -> Color.White.copy(alpha = 0.12f)
            else -> Color.Black.copy(alpha = 0.08f)
        },
        highlight = if (dark) Color.White.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.7f),
        blur = settings.performanceMode != PerformanceMode.BATTERY_SAVER && settings.blurIntensity > 0.15f,
        intensity = settings.glassIntensity,
    )
    val animScale = if (settings.reduceMotion) 0f else settings.animationScale.coerceIn(0.25f, 2f)
    val typography = MaterialTheme.typography.copy(
        headlineLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 32.sp, letterSpacing = (-0.4).sp),
        headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 24.sp, letterSpacing = (-0.2).sp),
        titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
        titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp),
        bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp),
        bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
        labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 13.sp, letterSpacing = 0.2.sp),
    )
    CompositionLocalProvider(LocalGlass provides glass, LocalAnimScale provides animScale) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}
