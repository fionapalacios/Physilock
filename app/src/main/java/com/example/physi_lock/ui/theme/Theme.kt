package com.example.physi_lock.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

// Map prototype palette into Material3 color schemes
private val DarkColorScheme = darkColorScheme(
    primary = SageAccent,
    onPrimary = OnSurfaceDark,
    secondary = Orchid,
    onSecondary = OnSurfaceDark,
    background = SurfaceDark,
    surface = SurfaceDark,
    onBackground = OnSurfaceDark,
    onSurface = OnSurfaceDark,
    tertiary = DeepOlive
)

private val LightColorScheme = lightColorScheme(
    primary = DeepOlive,
    onPrimary = SurfaceLight,
    secondary = SageAccent,
    onSecondary = OnSurfaceLight,
    background = SurfaceLight,
    surface = SurfaceLight,
    onBackground = OnSurfaceLight,
    onSurface = OnSurfaceLight,
    tertiary = Orchid

    /* Other defaults can be added as needed
    */
)

@Composable
fun PhysiLockTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Off by default so the brand palette (DeepOlive/SageAccent/Orchid) always renders,
    // instead of being silently replaced by the device's wallpaper-derived Material You colors.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}