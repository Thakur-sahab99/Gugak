package com.example.ui.theme

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
    primary = GugakPrimaryDark,
    onPrimary = GugakOnPrimaryDark,
    primaryContainer = GugakPrimaryContainerDark,
    onPrimaryContainer = GugakOnPrimaryContainerDark,
    secondary = GugakSecondaryDark,
    onSecondary = GugakOnSecondaryDark,
    secondaryContainer = GugakSecondaryContainerDark,
    onSecondaryContainer = GugakOnSecondaryContainerDark,
    tertiary = GugakTertiaryDark,
    tertiaryContainer = GugakTertiaryContainerDark,
    onTertiaryContainer = GugakOnTertiaryContainerDark,
    background = GugakBackgroundDark,
    onBackground = GugakOnBackgroundDark,
    surface = GugakSurfaceDark,
    onSurface = GugakOnSurfaceDark,
    surfaceVariant = GugakSurfaceVariantDark,
    onSurfaceVariant = GugakOnSurfaceVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = GugakPrimaryLight,
    onPrimary = GugakOnPrimaryLight,
    primaryContainer = GugakPrimaryContainerLight,
    onPrimaryContainer = GugakOnPrimaryContainerLight,
    secondary = GugakSecondaryLight,
    onSecondary = GugakOnSecondaryLight,
    secondaryContainer = GugakSecondaryContainerLight,
    onSecondaryContainer = GugakOnSecondaryContainerLight,
    tertiary = GugakTertiaryLight,
    tertiaryContainer = GugakTertiaryContainerLight,
    onTertiaryContainer = GugakOnTertiaryContainerLight,
    background = GugakBackgroundLight,
    onBackground = GugakOnBackgroundLight,
    surface = GugakSurfaceLight,
    onSurface = GugakOnSurfaceLight,
    surfaceVariant = GugakSurfaceVariantLight,
    onSurfaceVariant = GugakOnSurfaceVariantLight
)

@Composable
fun GugakTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
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
