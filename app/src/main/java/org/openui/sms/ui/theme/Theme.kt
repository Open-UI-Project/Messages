package org.openui.sms.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color

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

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> OpenUIDarkColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

