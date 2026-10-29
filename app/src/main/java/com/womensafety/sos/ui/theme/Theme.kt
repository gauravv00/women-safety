package com.womensafety.sos.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val JhumkaLightColorScheme = lightColorScheme(
    primary = JhumkaPinkPrimary,
    onPrimary = JhumkaTextOnDark,
    primaryContainer = JhumkaLightPinkContainer,
    onPrimaryContainer = JhumkaPinkPressed,

    secondary = JhumkaPinkLight,
    onSecondary = JhumkaTextOnDark,
    secondaryContainer = JhumkaLightPinkContainer,
    onSecondaryContainer = JhumkaLightTextPrimary,

    tertiary = JhumkaTrustBlue,
    onTertiary = JhumkaTextOnDark,
    tertiaryContainer = JhumkaTrustBlueContainer,
    onTertiaryContainer = JhumkaTrustBlue,

    background = JhumkaLightBackground,
    onBackground = JhumkaLightTextPrimary,

    surface = JhumkaLightSurface,
    onSurface = JhumkaLightTextPrimary,
    surfaceVariant = JhumkaLightSurfaceVariant,
    onSurfaceVariant = JhumkaLightTextSecondary,

    error = JhumkaCrimsonRed,
    onError = JhumkaTextOnDark,
    errorContainer = JhumkaCrimsonContainer,
    onErrorContainer = JhumkaCrimsonPressed,

    outline = JhumkaLightBorder,
    outlineVariant = JhumkaLightBorderSubtle
)

private val JhumkaDarkColorScheme = darkColorScheme(
    primary = JhumkaPinkPrimary,
    onPrimary = JhumkaTextOnDark,
    primaryContainer = JhumkaDarkPinkContainer,
    onPrimaryContainer = JhumkaPinkLight,

    secondary = JhumkaPinkLight,
    onSecondary = JhumkaTextOnDark,
    secondaryContainer = JhumkaDarkPinkContainer,
    onSecondaryContainer = JhumkaDarkTextPrimary,

    tertiary = JhumkaTrustBlueLight,
    onTertiary = JhumkaTextOnDark,
    tertiaryContainer = Color(0xFF1E293B),
    onTertiaryContainer = JhumkaTrustBlueLight,

    background = JhumkaDarkBackground,
    onBackground = JhumkaDarkTextPrimary,

    surface = JhumkaDarkSurface,
    onSurface = JhumkaDarkTextPrimary,
    surfaceVariant = JhumkaDarkSurfaceVariant,
    onSurfaceVariant = JhumkaDarkTextSecondary,

    error = JhumkaCrimsonRed,
    onError = JhumkaTextOnDark,
    errorContainer = Color(0xFF5A0B1A),
    onErrorContainer = JhumkaCrimsonContainer,

    outline = JhumkaDarkBorder,
    outlineVariant = JhumkaDarkBorderSubtle
)

@Composable
fun WomenSafetyTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) JhumkaDarkColorScheme else JhumkaLightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalDarkTheme provides darkTheme
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}

@Composable
fun JhumkaTheme(
    darkTheme: Boolean = LocalDarkTheme.current,
    content: @Composable () -> Unit
) {
    WomenSafetyTheme(darkTheme = darkTheme, content = content)
}
