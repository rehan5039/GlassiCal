package com.rubex.glassical.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = RealmeOrange,
    secondary = GlassButtonDark,
    tertiary = OperatorGlassDark,
    background = RealmeDarkBg,
    surface = GlassButtonDark, // Base surface color
    onPrimary = TextWhite,
    onSecondary = TextWhite,
    onTertiary = TextWhite,
    onBackground = TextWhite,
    onSurface = TextWhite,
)

// Defining a light scheme that makes sense with the new glass variables
// Ideally we should have Light variants in Color.kt but I defined some like GlassTintLight
private val LightColorScheme = lightColorScheme(
    primary = RealmeOrange,
    secondary = GlassTintLight, // Use tint as secondary
    tertiary = OperatorGlassDark, // Keep operators darkish or define light op?
    // Let's use strict mapping.
    // In Color.kt I have: GlassTintLight, GlassBorderLight, RealmeLightBg
    background = RealmeLightBg,
    surface = RealmeLightBg,
    onPrimary = TextWhite,
    onSecondary = TextBlack,
    onTertiary = TextBlack,
    onBackground = TextBlack,
    onSurface = TextBlack,
)

@Composable
fun GlassiCalTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = false, // Disable to enforce Realme look
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

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MaterialTheme.typography, 
        content = content
    )
}