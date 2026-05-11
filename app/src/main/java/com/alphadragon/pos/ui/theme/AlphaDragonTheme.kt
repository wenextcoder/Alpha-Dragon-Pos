package com.alphadragon.pos.ui.theme

import android.app.Activity
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Brand palette
val BrandRed = Color(0xFFB71C1C)
val BrandRedDark = Color(0xFF7F0000)
val BrandRedLight = Color(0xFFE53935)

val SurfaceBlack = Color(0xFF0D0D0D)
val SurfaceDark = Color(0xFF1A1A1A)
val SurfaceContainer = Color(0xFF242424)
val SurfaceContainerHigh = Color(0xFF2E2E2E)

val OnSurfacePrimary = Color(0xFFFFFFFF)
val OnSurfaceSecondary = Color(0xFFB0B0B0)
val OnSurfaceTertiary = Color(0xFF707070)

val SuccessGreen = Color(0xFF2E7D32)
val WarningAmber = Color(0xFFF57C00)
val ErrorRed = Color(0xFFCF6679)
val Outline = Color(0xFF3A3A3A)

private val AlphaDragonDarkColorScheme = darkColorScheme(
    primary = BrandRed,
    onPrimary = Color.White,
    primaryContainer = BrandRedDark,
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFE8B4B0),
    onSecondary = Color(0xFF4A1515),
    secondaryContainer = Color(0xFF652B2B),
    onSecondaryContainer = Color(0xFFFFDAD6),
    tertiary = Color(0xFFFFB77C),
    onTertiary = Color(0xFF4A2800),
    background = SurfaceBlack,
    onBackground = OnSurfacePrimary,
    surface = SurfaceDark,
    onSurface = OnSurfacePrimary,
    surfaceVariant = SurfaceContainer,
    onSurfaceVariant = OnSurfaceSecondary,
    surfaceContainer = SurfaceContainer,
    surfaceContainerHigh = SurfaceContainerHigh,
    outline = Outline,
    outlineVariant = Color(0xFF2A2A2A),
    error = ErrorRed,
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6)
)

@Composable
fun AlphaDragonTheme(content: @Composable () -> Unit) {
    val colorScheme = AlphaDragonDarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = SurfaceBlack.toArgb()
            window.navigationBarColor = SurfaceBlack.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AlphaDragonTypography,
        shapes = AlphaDragonShapes,
        content = content
    )
}
