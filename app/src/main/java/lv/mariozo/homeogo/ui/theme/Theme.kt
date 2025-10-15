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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat

// Define placeholder colors - replace with your actual brand colors

private val LightColors = lightColorScheme(
    primary = Purple40,
    onPrimary = Color.White,
    primaryContainer = PurpleGrey80, // Adjusted for better contrast or use a specific light primary container
    onPrimaryContainer = PurpleGrey40, // Adjusted
    secondary = Pink40,
    onSecondary = Color.White,
    secondaryContainer = Pink80, // Adjusted
    onSecondaryContainer = PurpleGrey40, // Adjusted
    tertiary = Pink80, // Example, adjust as needed
    onTertiary = PurpleGrey40, // Example
    tertiaryContainer = Purple40, // Example
    onTertiaryContainer = Color.White, // Example
    error = Color(0xFFB00020),
    onError = Color.White,
    background = Color(0xFFFFFBFE),
    onBackground = Color(0xFF1C1B1F),
    surface = Color(0xFFFFFBFE),
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = PurpleGrey80, // Adjusted
    onSurfaceVariant = PurpleGrey40, // Adjusted
    outline = PurpleGrey40 // Adjusted
)

private val DarkColors = darkColorScheme(
    primary = Purple80,
    onPrimary = PurpleGrey40, // Adjusted for dark theme
    primaryContainer = Purple40, // Adjusted
    onPrimaryContainer = PurpleGrey80, // Adjusted
    secondary = Pink80, // Adjusted
    onSecondary = PurpleGrey40, // Adjusted
    secondaryContainer = Pink40, // Adjusted
    onSecondaryContainer = PurpleGrey80, // Adjusted
    tertiary = Pink40, // Example, adjust as needed
    onTertiary = PurpleGrey80, // Example
    tertiaryContainer = Purple80, // Example
    onTertiaryContainer = PurpleGrey40, // Example
    error = Color(0xFFCF6679),
    onError = Color.Black,
    background = Color(0xFF1C1B1F),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = PurpleGrey40, // Adjusted
    onSurfaceVariant = PurpleGrey80, // Adjusted
    outline = PurpleGrey80 // Adjusted
)

@Composable
fun HomeoGOTheme(
    useDarkTheme: Boolean = isSystemInDarkTheme(),
    useDynamicColor: Boolean = true, // Default to true to leverage dynamic color on S+
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

    // Edge-to-edge (status/navigation bar) — optional
    (context as? Activity)?.window?.let { window ->
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // You might also want to set status bar and navigation bar colors/icons
        // This requires more setup, potentially using Accompanist or custom side effects
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Assuming Typography is defined elsewhere (e.g., Type.kt)
        shapes = Shapes,       // Assuming Shapes is defined elsewhere (e.g., Shape.kt)
        content = content
    )
}
