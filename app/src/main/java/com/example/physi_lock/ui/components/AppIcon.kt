package com.example.physi_lock.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.example.physi_lock.ui.theme.Nunito

/** Real launcher icon for [packageName] (`PackageManager.getApplicationIcon`), cached per
 *  package for the composable's lifetime. Returns null when the icon can't be resolved --
 *  e.g. an Admin-added manual app (see AdminCategoriesSection) that has no real installed
 *  package behind its AppCategory row, or an app that's since been uninstalled. Callers fall
 *  back to an honest initial-letter avatar in that case, not a guessed icon. */
@Composable
fun rememberAppIconBitmap(packageName: String): ImageBitmap? {
    val context = LocalContext.current
    return remember(packageName) {
        runCatching {
            context.packageManager.getApplicationIcon(packageName).toBitmap().asImageBitmap()
        }.getOrNull()
    }
}

/** Replaces the colored-circle/initial-letter and guessed-emoji placeholders that stood in
 *  for real app icons across the app's various app pickers (App Lock Rules, Focus Mode
 *  Blocked Apps, Whitelist Manager, Study App Allowlist, Admin Categories) until 2026-09-04.
 *  [backgroundColor]/[contentColor] still drive the fallback avatar's look (and remain visible
 *  as a colored surface behind icons with transparency), so each picker's existing
 *  on/off-state color coding is preserved. */
@Composable
fun AppIconAvatar(
    packageName: String,
    appName: String,
    backgroundColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    fontSize: TextUnit = 15.sp,
    shape: Shape = CircleShape
) {
    val bitmap = rememberAppIconBitmap(packageName)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(
                text = appName.take(1).uppercase().ifEmpty { "?" },
                fontFamily = Nunito,
                fontWeight = FontWeight.Bold,
                fontSize = fontSize,
                color = contentColor
            )
        }
    }
}
