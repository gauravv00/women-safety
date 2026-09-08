package com.womensafety.sos.ui.theme

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
    primary = AlertRed,
    onPrimary = TextPrimaryDark,
    primaryContainer = AlertRedPressed,
    onPrimaryContainer = TextPrimaryDark,
    secondary = SafetyGreen,
    onSecondary = TextPrimaryDark,
    secondaryContainer = SafetyGreenPressed,
    onSecondaryContainer = TextPrimaryDark,
    tertiary = TrustBlue,
    onTertiary = TextPrimaryDark,
    background = DarkBackground,
    onBackground = TextPrimaryDark,
    surface = DarkSurface,
    onSurface = TextPrimaryDark,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondaryDark,
    error = AlertRed,
    onError = TextPrimaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = AlertRed,
    onPrimary = TextPrimaryDark,
    primaryContainer = AlertRedPressed,
    onPrimaryContainer = TextPrimaryDark,
    secondary = SafetyGreen,
    onSecondary = TextPrimaryDark,
    secondaryContainer = SafetyGreenContainer,
    onSecondaryContainer = TextPrimaryLight,
    tertiary = TrustBlue,
    onTertiary = TextPrimaryDark,
    background = LightBackground,
    onBackground = TextPrimaryLight,
    surface = LightSurface,
    onSurface = TextPrimaryLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = TextSecondaryLight,
    error = AlertRed,
    onError = TextPrimaryDark
)

@Composable
fun WomenSafetyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
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
