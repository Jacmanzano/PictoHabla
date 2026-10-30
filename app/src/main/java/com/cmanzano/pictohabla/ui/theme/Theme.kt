package com.cmanzano.pictohabla.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.cmanzano.pictohabla.core.designsystem.PictoColors

private val NormalColorScheme = lightColorScheme(
    primary = PictoColors.Primary,
    onPrimary = PictoColors.OnPrimary,
    primaryContainer = PictoColors.PrimaryContainer,
    onPrimaryContainer = PictoColors.OnPrimaryContainer,

    secondary = PictoColors.Secondary,
    onSecondary = PictoColors.OnSecondary,
    secondaryContainer = PictoColors.SecondaryContainer,
    onSecondaryContainer = PictoColors.OnSecondaryContainer,

    background = PictoColors.BackgroundLight,
    onBackground = PictoColors.OnBackgroundLight,
    surface = PictoColors.SurfaceLight,
    onSurface = PictoColors.OnSurfaceLight,
    surfaceVariant = PictoColors.SurfaceVariantLight,
    onSurfaceVariant = PictoColors.OnSurfaceVariantLight,

    error = PictoColors.Error,
    onError = PictoColors.OnError,
    errorContainer = PictoColors.ErrorContainer,
    onErrorContainer = PictoColors.OnErrorContainer
)

private val HighContrastColorScheme = darkColorScheme(
    primary = PictoColors.HighlightHighContrast,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF333333),
    onPrimaryContainer = Color.White,

    secondary = Color.White,
    onSecondary = Color.Black,
    secondaryContainer = Color(0xFF222222),
    onSecondaryContainer = Color.White,

    background = PictoColors.BackgroundHighContrast,
    onBackground = PictoColors.OnBackgroundHighContrast,
    surface = PictoColors.SurfaceHighContrast,
    onSurface = PictoColors.OnSurfaceHighContrast,
    surfaceVariant = PictoColors.SurfaceVariantHighContrast,
    onSurfaceVariant = Color.White,

    error = Color(0xFFFF5252),
    onError = Color.Black
)

@Composable
fun PictoHablaTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) HighContrastColorScheme else NormalColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = colorScheme.surface.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
