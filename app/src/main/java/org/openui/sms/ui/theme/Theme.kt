package org.openui.sms.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import org.openui.sms.data.AppSettings

data class AppCustomColors(
    val background: Color,
    val surface: Color,
    val card: Color,
    val cardElevated: Color,
    val divider: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val primaryBrand: Color,
    val onAccent: Color,
    val dialerGreen: Color,
    val actionRed: Color,
    val incomingBubble: Color,
    val isDark: Boolean,
    val isExpressive: Boolean
)

val LocalAppColors = staticCompositionLocalOf {
    AppCustomColors(
        background = OpenUIBlack,
        surface = OpenUISurface,
        card = OpenUICardBackground,
        cardElevated = OpenUIPillBackground,
        divider = OpenUIDivider,
        textPrimary = OpenUITextPrimary,
        textSecondary = OpenUITextSecondary,
        textMuted = OpenUITextSecondary,
        primaryBrand = Color(0xFF00A88F),
        onAccent = Color.White,
        dialerGreen = Color(0xFF00C853),
        actionRed = Color(0xFFFF5252),
        incomingBubble = OpenUIIncomingBubble,
        isDark = true,
        isExpressive = false
    )
}

object AppTheme {
    val colors: AppCustomColors
        @Composable
        get() = LocalAppColors.current
}

private val OpenUIDarkColorScheme = darkColorScheme(
    primary = Color(0xFF00A88F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF00A88F),
    onPrimaryContainer = Color.White,
    secondary = OpenUIAvatarBlue,
    onSecondary = Color.White,
    tertiary = OpenUIPillBackground,
    background = OpenUIBlack,
    onBackground = OpenUITextPrimary,
    surface = OpenUISurface,
    onSurface = OpenUITextPrimary,
    surfaceVariant = OpenUICardBackground,
    onSurfaceVariant = OpenUITextSecondary,
    outline = OpenUIDivider
)

private val OpenUILightColorScheme = lightColorScheme(
    primary = Color(0xFF008069),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF008069),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFF4A68D6),
    onSecondary = Color.White,
    tertiary = OpenUILightPill,
    background = OpenUILightBackground,
    onBackground = OpenUILightTextPrimary,
    surface = OpenUILightSurface,
    onSurface = OpenUILightTextPrimary,
    surfaceVariant = OpenUILightCard,
    onSurfaceVariant = OpenUILightTextSecondary,
    outline = OpenUILightDivider
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val appSettings = remember(context) { AppSettings.getInstance(context) }
    val themeMode by appSettings.themeMode.collectAsState()
    val designStyle by appSettings.designStyle.collectAsState()
    val monetEnabled by appSettings.monetEnabled.collectAsState()

    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        AppSettings.THEME_LIGHT -> false
        AppSettings.THEME_DARK -> true
        else -> systemDark
    }

    val isExpressive = designStyle == AppSettings.STYLE_MATERIAL3_EXPRESSIVE

    val expressiveScheme = rememberMonetColorScheme(isDark = isDark, enabled = monetEnabled)
    val colorScheme = when {
        isExpressive -> expressiveScheme
        isDark -> OpenUIDarkColorScheme
        else -> OpenUILightColorScheme
    }

    val appColors = if (isExpressive) {
        AppCustomColors(
            background = expressiveScheme.background,
            surface = expressiveScheme.surface,
            card = expressiveScheme.surfaceVariant,
            cardElevated = expressiveScheme.secondaryContainer,
            divider = expressiveScheme.outlineVariant,
            textPrimary = expressiveScheme.onSurface,
            textSecondary = expressiveScheme.onSurfaceVariant,
            textMuted = expressiveScheme.onSurfaceVariant.copy(alpha = 0.72f),
            primaryBrand = expressiveScheme.primary,
            onAccent = expressiveScheme.onPrimary,
            dialerGreen = expressiveScheme.tertiary,
            actionRed = expressiveScheme.error,
            incomingBubble = expressiveScheme.surfaceVariant,
            isDark = isDark,
            isExpressive = true
        )
    } else {
        if (isDark) {
            AppCustomColors(
                background = OpenUIBlack,
                surface = OpenUISurface,
                card = OpenUICardBackground,
                cardElevated = OpenUIPillBackground,
                divider = OpenUIDivider,
                textPrimary = OpenUITextPrimary,
                textSecondary = OpenUITextSecondary,
                textMuted = OpenUITextSecondary,
                primaryBrand = Color(0xFF00A88F),
                onAccent = Color.White,
                dialerGreen = Color(0xFF00C853),
                actionRed = Color(0xFFFF5252),
                incomingBubble = OpenUIIncomingBubble,
                isDark = true,
                isExpressive = false
            )
        } else {
            AppCustomColors(
                background = OpenUILightBackground,
                surface = OpenUILightSurface,
                card = OpenUILightCard,
                cardElevated = OpenUILightCardElevated,
                divider = OpenUILightDivider,
                textPrimary = OpenUILightTextPrimary,
                textSecondary = OpenUILightTextSecondary,
                textMuted = Color(0xFFA0A0A5),
                primaryBrand = Color(0xFF008069),
                onAccent = Color.White,
                dialerGreen = Color(0xFF00873D),
                actionRed = Color(0xFFD32F2F),
                incomingBubble = OpenUILightIncomingBubble,
                isDark = false,
                isExpressive = false
            )
        }
    }

    CompositionLocalProvider(LocalAppColors provides appColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
