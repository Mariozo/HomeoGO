// # --- 1 ------ Header ----------------------------------------------------
// File: ui/theme/Theme.kt
// Module: HomeoGO
// Purpose: Central Compose theme (colors/typography/shapes) + dynamic color support
// Created: 17.sep.2025

package lv.mariozo.homeogo.ui.theme

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
import androidx.core.view.WindowCompat


private val LightColors = lightColorScheme(
    // TODO: pielāgo primārās krāsas savam zīmolam
)

private val DarkColors = darkColorScheme(
    // TODO: pielāgo primārās krāsas savam zīmolam
)

@Composable
fun HomeoGOTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    useDynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme =
        if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (useDarkTheme) dynamicDarkColorScheme(context)
            else dynamicLightColorScheme(context)
        } else {
            if (useDarkTheme) DarkColors else LightColors
        }

    // Edge-to-edge (status/navigation bar) — izvēles
    (context as? Activity)?.window?.let { window ->
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
