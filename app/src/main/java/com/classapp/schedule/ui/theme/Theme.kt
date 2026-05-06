package com.classapp.schedule.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = AppPrimary,
    background = AppBackground,
    surface = AppSurface,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextTertiary,
    outline = DividerColor,
    outlineVariant = DividerColor,
    surfaceVariant = AppBackground,
    onPrimary = AppSurface,
    primaryContainer = AppPrimaryDark,
    secondaryContainer = NarrowTagBg,
)

@Composable
fun ClassAppTheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = LightColorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }

    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
