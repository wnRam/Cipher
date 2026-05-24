package uz.angrykitten.spygame.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Dark — luxury spy thriller palette
val DarkBackground = Color(0xFF08090E)
val DarkSurface = Color(0xFF0F1018)
val DarkSurfaceElevated = Color(0xFF171923)
val DarkBorder = Color(0xFF2A2D3E)
val DarkDivider = Color(0xFF1E2030)
val DarkTextPrimary = Color(0xFFF0EEE8)
val DarkTextSecondary = Color(0xFF8A8A9A)

// Light
val LightBackground = Color(0xFFF4F3EF)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceElevated = Color(0xFFECEAE4)
val LightBorder = Color(0xFFD3CFC4)
val LightDivider = Color(0xFFE3DFD3)
val LightTextPrimary = Color(0xFF0C0C10)
val LightTextSecondary = Color(0xFF5A5A6A)

// Accents (shared, slightly tuned per theme via Theme.kt)
val Gold = Color(0xFFC8A96E)
val GoldBright = Color(0xFFE8C98A)
val GoldDeep = Color(0xFFA8894E)
val GoldDark = Color(0xFF8B6914)

val DangerRed = Color(0xFFE05555)
val SuccessGreen = Color(0xFF4CAF82)
val WarningAmber = Color(0xFFD9A441)

// Legacy aliases kept so older references still compile during the rebuild
val GoldAccent = Gold
val AlertRed = DangerRed
val NeonGreen = SuccessGreen
val InfoBlue = Color(0xFF7AA8D8)

val GoldGradient = Brush.horizontalGradient(
    colors = listOf(Gold, GoldDeep)
)

val GoldGradientVertical = Brush.verticalGradient(
    colors = listOf(GoldBright, Gold, GoldDeep)
)
