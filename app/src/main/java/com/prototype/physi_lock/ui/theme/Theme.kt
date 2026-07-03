package com.prototype.physi_lock.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryGreen,
    secondary = SecondarySage,
    tertiary = AccentLavender,
    background = PrimaryDark,
    surface = DeepOlive
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryGreen,
    secondary = SecondarySage,
    tertiary = AccentLavender,
    background = BackgroundLight,
    surface = TertiaryTan,
    onPrimary = BackgroundLight,
    onSecondary = PrimaryDark,
    onBackground = PrimaryDark,
    onSurface = PrimaryDark
)

@Composable
fun PhysiLockTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
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
