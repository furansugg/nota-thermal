package com.notathermal.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * App-wide theme. Defaults to the hand-tuned warm teal palette so the app
 * looks consistent across devices regardless of wallpaper. Set
 * `dynamicColor = true` if the user opts into Material You / device-tinted
 * colors (Android 12+ only).
 */
@Composable
fun NotaThermalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> WarmTealDarkColors
        else -> WarmTealLightColors
    }
    MaterialTheme(
        colorScheme = colors,
        typography = NotaTypography,
        shapes = NotaShapes,
        content = content
    )
}
