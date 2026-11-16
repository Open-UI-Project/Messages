package org.openui.sms.ui.theme

import android.app.WallpaperManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Monet engine for the Material 3 Expressive design style.
 *
 * On Android 12+ the platform Monet implementation is used. On older devices the
 * dominant system-wallpaper color is read through [WallpaperManager] and turned
 * into a complete Material 3 tonal palette, so Material You keeps working
 * without any Google services or third-party libraries.
 */
object MonetEngine {

    /** Brand blue used when no wallpaper color can be resolved. */
    const val DEFAULT_SEED: Int = 0xFF2B5CD6.toInt()

    fun colorSchemeFor(context: Context, dark: Boolean): ColorScheme {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                return if (dark) {
                    dynamicDarkColorScheme(context)
                } else {
                    dynamicLightColorScheme(context)
                }
            } catch (_: Throwable) {
                // Fall through to the local engine below.
            }
        }
        return schemeFromSeed(resolveWallpaperSeed(context) ?: DEFAULT_SEED, dark)
    }

    fun schemeFromSeed(seed: Int, dark: Boolean): ColorScheme {
        val hsl = FloatArray(3)
        ColorUtils.colorToHSL(seed, hsl)
        val hue = hsl[0]
        val saturation = hsl[1].coerceIn(0.18f, 0.85f)
        val tertiaryHue = (hue + 60f) % 360f
        val neutralHue = hue

        return if (dark) {
            darkColorScheme(
                primary = tone(hue, saturation, 80f),
                onPrimary = tone(hue, saturation, 20f),
                primaryContainer = tone(hue, saturation, 30f),
                onPrimaryContainer = tone(hue, saturation, 90f),
                secondary = tone(hue, saturation * 0.34f, 80f),
                onSecondary = tone(hue, saturation * 0.34f, 20f),
                secondaryContainer = tone(hue, saturation * 0.34f, 30f),
                onSecondaryContainer = tone(hue, saturation * 0.34f, 90f),
                tertiary = tone(tertiaryHue, saturation * 0.5f, 80f),
                onTertiary = tone(tertiaryHue, saturation * 0.5f, 20f),
                tertiaryContainer = tone(tertiaryHue, saturation * 0.5f, 30f),
                onTertiaryContainer = tone(tertiaryHue, saturation * 0.5f, 90f),
                error = Color(0xFFFFB4AB),
                onError = Color(0xFF690005),
                errorContainer = Color(0xFF93000A),
                onErrorContainer = Color(0xFFFFDAD6),
                background = tone(neutralHue, 0.06f, 6f),
                onBackground = tone(neutralHue, 0.06f, 92f),
                surface = tone(neutralHue, 0.06f, 6f),
                onSurface = tone(neutralHue, 0.06f, 92f),
                surfaceVariant = tone(neutralHue, 0.10f, 30f),
                onSurfaceVariant = tone(neutralHue, 0.10f, 80f),
                outline = tone(neutralHue, 0.10f, 60f),
                outlineVariant = tone(neutralHue, 0.08f, 30f),
                inverseSurface = tone(neutralHue, 0.06f, 92f),
                inverseOnSurface = tone(neutralHue, 0.06f, 20f),
                inversePrimary = tone(hue, saturation, 40f)
            )
        } else {
            lightColorScheme(
                primary = tone(hue, saturation, 40f),
                onPrimary = Color.White,
                primaryContainer = tone(hue, saturation, 90f),
                onPrimaryContainer = tone(hue, saturation, 10f),
                secondary = tone(hue, saturation * 0.34f, 40f),
                onSecondary = Color.White,
                secondaryContainer = tone(hue, saturation * 0.34f, 90f),
                onSecondaryContainer = tone(hue, saturation * 0.34f, 10f),
                tertiary = tone(tertiaryHue, saturation * 0.5f, 40f),
                onTertiary = Color.White,
                tertiaryContainer = tone(tertiaryHue, saturation * 0.5f, 90f),
                onTertiaryContainer = tone(tertiaryHue, saturation * 0.5f, 10f),
                error = Color(0xFFBA1A1A),
                onError = Color.White,
                errorContainer = Color(0xFFFFDAD6),
                onErrorContainer = Color(0xFF410002),
                background = tone(neutralHue, 0.18f, 99f),
                onBackground = tone(neutralHue, 0.08f, 10f),
                surface = tone(neutralHue, 0.18f, 99f),
                onSurface = tone(neutralHue, 0.08f, 10f),
                surfaceVariant = tone(neutralHue, 0.22f, 90f),
                onSurfaceVariant = tone(neutralHue, 0.10f, 30f),
                outline = tone(neutralHue, 0.10f, 50f),
                outlineVariant = tone(neutralHue, 0.10f, 80f),
                inverseSurface = tone(neutralHue, 0.08f, 20f),
                inverseOnSurface = tone(neutralHue, 0.18f, 95f),
                inversePrimary = tone(hue, saturation, 80f)
            )
        }
    }

    private fun resolveWallpaperSeed(context: Context): Int? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O_MR1) return null
        return WallpaperSeedApi27.read(context)
    }

    private object WallpaperSeedApi27 {
        @RequiresApi(Build.VERSION_CODES.O_MR1)
        fun read(context: Context): Int? {
            return try {
                val colors = WallpaperManager.getInstance(context)
                    .getWallpaperColors(WallpaperManager.FLAG_SYSTEM) ?: return null
                val primary = colors.primaryColor.toArgb()
                val luminance = ColorUtils.calculateLuminance(primary)
                if (luminance < 0.04f || luminance > 0.96f) {
                    colors.secondaryColor?.toArgb() ?: primary
                } else {
                    primary
                }
            } catch (_: Throwable) {
                null
            }
        }
    }

    private fun tone(hue: Float, saturation: Float, tone: Float): Color {
        val lightness = (tone / 100f).coerceIn(0f, 1f)
        val chroma = (saturation * saturationBoost(tone)).coerceIn(0f, 1f)
        return Color(ColorUtils.HSLToColor(floatArrayOf(hue, chroma, lightness)))
    }

    /**
     * Material tonal palettes keep containers vivid and neutrals calm.
     * HSL lightness alone looks washed out, so saturation is gently scaled.
     */
    private fun saturationBoost(tone: Float): Float = when {
        tone <= 10f -> 1.05f
        tone <= 35f -> 1.10f
        tone <= 60f -> 1.0f
        tone <= 85f -> 0.85f
        else -> 0.70f
    }
}

/**
 * Remembers a Material You color scheme and recomputes it off the main thread.
 * When [enabled] is false the calm brand-blue fallback palette is used.
 */
@Composable
fun rememberMonetColorScheme(isDark: Boolean, enabled: Boolean): ColorScheme {
    val context = LocalContext.current
    val fallback = remember(isDark) {
        MonetEngine.schemeFromSeed(MonetEngine.DEFAULT_SEED, isDark)
    }
    val scheme by produceState(
        initialValue = fallback,
        isDark,
        enabled,
        context
    ) {
        value = if (enabled) {
            withContext(Dispatchers.Default) {
                MonetEngine.colorSchemeFor(context, isDark)
            }
        } else {
            fallback
        }
    }
    return scheme
}
