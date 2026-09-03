package com.example.physi_lock.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

// Only ~9 of Material3's ~20 color roles were set here originally -- every unset role
// (surfaceVariant, outline, error, the *Container roles) silently fell back to Material3's
// default purple/violet baseline, so any screen using a default-styled component (Switch,
// Chip, AlertDialog, ...) instead of a fully custom one showed off-brand purple accents
// against the rest of the app's olive/sage palette. Filling in the rest here fixes that
// everywhere at once, matching the same colors the custom-styled Admin components already use.
private val LightColorScheme = lightColorScheme(
    primary = DeepOlive,
    onPrimary = SurfaceLight,
    primaryContainer = CardCream,
    onPrimaryContainer = DeepOlive,
    secondary = SageAccent,
    onSecondary = OnSurfaceLight,
    secondaryContainer = TertiaryTan,
    onSecondaryContainer = DeepOlive,
    tertiary = Orchid,
    onTertiary = DeepOlive,
    tertiaryContainer = Orchid,
    onTertiaryContainer = DeepOlive,
    error = ErrorRed,
    onError = BackgroundLight,
    errorContainer = SignOutBackground,
    onErrorContainer = ErrorRed,
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = TertiaryTan,
    onSurfaceVariant = MutedText,
    outline = DeepOlive.copy(alpha = 0.25f),
    outlineVariant = DeepOlive.copy(alpha = 0.08f)
)

@Composable
fun PhysiLockTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography
    ) {
        // App targets SDK 36, where the OS enforces edge-to-edge by default -- without this,
        // every screen's own top content (e.g. Admin Console's header) draws underneath the
        // status bar instead of below it. Applied once here so it's uniform across all screens
        // rather than each one adding its own padding.
        Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
            content()
        }
    }
}