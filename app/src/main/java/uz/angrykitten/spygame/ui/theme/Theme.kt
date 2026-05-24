package uz.angrykitten.spygame.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val OnGoldDark = Color(0xFF1A130A)
private val PureWhite = Color(0xFFFFFFFF)

private val DarkColorScheme = darkColorScheme(
    primary = Gold,
    onPrimary = OnGoldDark,
    primaryContainer = GoldDeep,
    onPrimaryContainer = DarkTextPrimary,
    secondary = GoldBright,
    onSecondary = OnGoldDark,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = DarkTextSecondary,
    surfaceContainer = DarkSurfaceElevated,
    surfaceContainerHigh = DarkSurfaceElevated,
    outline = DarkBorder,
    outlineVariant = DarkDivider,
    error = DangerRed,
    onError = PureWhite
)

private val LightColorScheme = lightColorScheme(
    primary = GoldDark,
    onPrimary = PureWhite,
    primaryContainer = GoldBright,
    onPrimaryContainer = LightTextPrimary,
    secondary = Gold,
    onSecondary = LightTextPrimary,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceElevated,
    onSurfaceVariant = LightTextSecondary,
    surfaceContainer = LightSurfaceElevated,
    surfaceContainerHigh = LightSurfaceElevated,
    outline = LightBorder,
    outlineVariant = LightDivider,
    error = DangerRed,
    onError = PureWhite
)

@Composable
fun CipherTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = CipherTypography,
        content = content
    )
}

// Back-compat alias so existing previews keep working.
@Composable
fun SpyGameTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) = CipherTheme(darkTheme = darkTheme, content = content)
