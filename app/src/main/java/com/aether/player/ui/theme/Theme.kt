package com.aether.player.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.Dp
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

/** Solid card surfaces (Photos / modern-settings style). */
@Immutable
data class GlassTokens(
    val surface: Color = Color(0xFF1E1E25),
    val surfaceStrong: Color = Color(0xFF26262E),
    val border: Color = Color(0xFF34343D),
    val highlight: Color = Color(0xFF2C2C34),
    val blur: Boolean = false,
    val intensity: Float = 0.72f,
    val corner: Dp = 24.dp,
)

fun accentOf(accent: AccentColor): Color = when (accent) {
    AccentColor.CYAN -> Color(0xFF4DD0E1)
    AccentColor.BLUE -> Color(0xFF4DA3FF)
    AccentColor.PURPLE -> Color(0xFFB08CFF)
    AccentColor.GREEN -> Color(0xFF6EE7B7)
    AccentColor.ORANGE -> Color(0xFFFFB86B)
    AccentColor.RED -> Color(0xFFFF6B81)
    AccentColor.PINK -> Color(0xFFFF8BD2)
    AccentColor.SYSTEM -> Color(0xFF4DD0E1)
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
            background = if (amoled) Color.Black else Color(0xFF101014),
            surface = if (amoled) Color.Black else Color(0xFF17171C),
            surfaceVariant = Color(0xFF232329),
            surfaceContainerLowest = Color(0xFF0C0C10),
            surfaceContainerLow = Color(0xFF17171C),
            surfaceContainer = Color(0xFF1E1E25),
            surfaceContainerHigh = Color(0xFF26262E),
            surfaceContainerHighest = Color(0xFF2E2E36),
            onBackground = Color.White,
            onSurface = Color.White,
            onSurfaceVariant = if (hi) Color.White else Color(0xFFD8DDE7),
            outline = if (hi) Color.White.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.1f),
            error = Color(0xFFFF6B81),
        )
        else -> lightColorScheme(
            primary = accent,
            onPrimary = Color.White,
            background = Color(0xFFF2F3F6),
            surface = Color.White,
            surfaceVariant = Color(0xFFE8EAEE),
            onBackground = Color(0xFF101014),
            onSurface = Color(0xFF101014),
            onSurfaceVariant = if (hi) Color.Black else Color(0xFF3A3F4B),
            outline = if (hi) Color.Black.copy(alpha = 0.3f) else Color.Black.copy(alpha = 0.08f),
        )
    }
    val glass = GlassTokens(
        surface = if (dark) Color(0xFF1E1E25) else Color.White,
        surfaceStrong = if (dark) Color(0xFF26262E) else Color(0xFFE9EBEF),
        border = when {
            hi && dark -> Color.White.copy(alpha = 0.38f)
            hi -> Color.Black.copy(alpha = 0.3f)
            dark -> Color(0xFF34343D)
            else -> Color(0xFFDFE2E8)
        },
        highlight = if (dark) Color(0xFF2C2C34) else Color(0xFFF4F5F8),
        blur = settings.performanceMode != PerformanceMode.BATTERY_SAVER && settings.blurIntensity > 0.15f,
        intensity = settings.glassIntensity,
    )
    val animScale = if (settings.reduceMotion) 0f else settings.animationScale.coerceIn(0.25f, 2f)
    val typography = MaterialTheme.typography.copy(
        headlineLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 30.sp, letterSpacing = (-0.4).sp),
        headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 24.sp, letterSpacing = (-0.2).sp),
        titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
        titleMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 16.sp),
        bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp),
        bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 15.sp, lineHeight = 22.sp),
        labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, letterSpacing = 0.2.sp),
    )
    CompositionLocalProvider(LocalGlass provides glass, LocalAnimScale provides animScale) {
        MaterialTheme(colorScheme = scheme, typography = typography) {
            // Root Surface provides LocalContentColor, so default text stays readable
            // in both dark and light themes (fixes black-on-dark settings text).
            Surface(color = scheme.background, contentColor = scheme.onSurface, content = content)
        }
    }
}
