package com.womensafety.sos.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val LocalDarkTheme = staticCompositionLocalOf { false }

// ============================================================================
// BASE COLOR PALETTES (RAW CONSTANTS)
// ============================================================================

// Light Palette (Signature Jhumka Soft Blush & Pure White)
val JhumkaLightBackground = Color(0xFFFFF8FA)         // Clean soft blush backdrop
val JhumkaLightSurface = Color(0xFFFFFFFF)            // Pure crisp white card surface
val JhumkaLightSurfaceVariant = Color(0xFFFFF0F5)     // Soft blush card variant
val JhumkaLightSurfaceHighlight = Color(0xFFFFE8F0)   // Gentle highlight blush
val JhumkaLightBorder = Color(0xFFF7D8E2)             // Delicate rose-tinted border
val JhumkaLightBorderSubtle = Color(0x33F54278)       // 20% vibrant pink subtle outline
val JhumkaLightTextPrimary = Color(0xFF1E171B)        // Deep charcoal-plum text
val JhumkaLightTextSecondary = Color(0xFF6B5861)      // Elegant muted plum body text
val JhumkaLightTextTertiary = Color(0xFF9E8A93)       // Subtle hint & label text
val JhumkaLightPinkContainer = Color(0xFFFFE4EC)      // Soft pastel pink badge/chip fill

// Dark Palette (Sleek Obsidian & Rose Noir)
val JhumkaDarkBackground = Color(0xFF0F0B0E)          // Deep obsidian black with subtle rose undertone
val JhumkaDarkSurface = Color(0xFF1B1419)             // Dark plum-slate card surface
val JhumkaDarkSurfaceVariant = Color(0xFF261D23)      // Elevated surface for nested items
val JhumkaDarkSurfaceHighlight = Color(0xFF32252F)    // Highlighted surface
val JhumkaDarkBorder = Color(0xFF3B2A34)              // Muted rose-tinted dark border
val JhumkaDarkBorderSubtle = Color(0x44F54278)        // Pink subtle outline
val JhumkaDarkTextPrimary = Color(0xFFFDF0F4)         // Crisp ivory-rose text (high contrast)
val JhumkaDarkTextSecondary = Color(0xFFB8A2AC)       // Mauve-lavender secondary text
val JhumkaDarkTextTertiary = Color(0xFF85717B)        // Subtle tertiary text
val JhumkaDarkPinkContainer = Color(0xFF451525)       // Deep ruby container for pink badges

// Vibrant Pink Primary Accents (Consistent across themes)
val JhumkaPinkPrimary = Color(0xFFF54278)             // Signature vibrant pink accent
val JhumkaPinkPressed = Color(0xFFD8255B)             // Deeper pink when pressed
val JhumkaPinkLight = Color(0xFFFF759B)               // Radiant pink gradient highlight
val JhumkaPinkGradientStart = Color(0xFFFF5288)       // Gradient start for hero elements
val JhumkaPinkGradientEnd = Color(0xFFE02B65)         // Gradient end for hero elements

// High-Contrast Crimson Red (Active Emergency SOS State)
val JhumkaCrimsonRed = Color(0xFFD90429)              // High-contrast crimson for active SOS
val JhumkaCrimsonPressed = Color(0xFFAD0320)          // Deeper crimson pressed state
val JhumkaCrimsonContainer = Color(0xFFFFD9DF)        // Soft crimson container
val JhumkaCrimsonGradientStart = Color(0xFFEF233C)    // Radiant crimson gradient start
val JhumkaCrimsonGradientEnd = Color(0xFFB5021F)      // Intense crimson gradient end

// Functional Status & Accent Colors
val JhumkaSafetyGreen = Color(0xFF00A86B)             // Emerald green for verified/safe state
val JhumkaSafetyGreenPressed = Color(0xFF008755)
val JhumkaSafetyGreenContainer = Color(0xFFE6F7F0)

val JhumkaTrustBlue = Color(0xFF2563EB)               // Clean sapphire blue for GPS tracking
val JhumkaTrustBlueLight = Color(0xFF60A5FA)
val JhumkaTrustBlueContainer = Color(0xFFEFF6FF)

val JhumkaWarningAmber = Color(0xFFF59E0B)            // Amber alert
val JhumkaWarningContainer = Color(0xFFFEF3C7)

// Typography Colors
val JhumkaTextOnDark = Color(0xFFFFFFFF)              // White text for SOS buttons & banners

// ============================================================================
// DYNAMIC COMPOSABLE GETTERS
// Automatically adapt between Light and Dark mode everywhere
// ============================================================================

val JhumkaBlushBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) JhumkaDarkBackground else JhumkaLightBackground

val JhumkaSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) JhumkaDarkSurface else JhumkaLightSurface

val JhumkaSurfaceVariant: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) JhumkaDarkSurfaceVariant else JhumkaLightSurfaceVariant

val JhumkaSurfaceHighlight: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) JhumkaDarkSurfaceHighlight else JhumkaLightSurfaceHighlight

val JhumkaBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) JhumkaDarkBorder else JhumkaLightBorder

val JhumkaBorderSubtle: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) JhumkaDarkBorderSubtle else JhumkaLightBorderSubtle

val JhumkaTextPrimary: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) JhumkaDarkTextPrimary else JhumkaLightTextPrimary

val JhumkaTextSecondary: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) JhumkaDarkTextSecondary else JhumkaLightTextSecondary

val JhumkaTextTertiary: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) JhumkaDarkTextTertiary else JhumkaLightTextTertiary

val JhumkaPinkContainer: Color
    @Composable
    @ReadOnlyComposable
    get() = if (LocalDarkTheme.current) JhumkaDarkPinkContainer else JhumkaLightPinkContainer

// ============================================================================
// BACKWARDS-COMPATIBILITY ALIASES
// ============================================================================

val AlertRed = JhumkaCrimsonRed
val AlertRedPressed = JhumkaCrimsonPressed
val AlertRedGradientStart = JhumkaCrimsonGradientStart
val AlertRedGradientEnd = JhumkaCrimsonGradientEnd

val SafetyGreen = JhumkaSafetyGreen
val SafetyGreenPressed = JhumkaSafetyGreenPressed
val SafetyGreenContainer = JhumkaSafetyGreenContainer

val TrustBlue = JhumkaTrustBlue
val TrustBlueLight = JhumkaTrustBlueLight
val TrustBlueContainer = JhumkaTrustBlueContainer

val WarningYellow = JhumkaWarningAmber

val DarkBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = JhumkaBlushBackground

val DarkSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = JhumkaSurface

val DarkSurfaceVariant: Color
    @Composable
    @ReadOnlyComposable
    get() = JhumkaSurfaceVariant

val DarkSurfaceHigh: Color
    @Composable
    @ReadOnlyComposable
    get() = JhumkaSurfaceHighlight

val DarkCardBorder: Color
    @Composable
    @ReadOnlyComposable
    get() = JhumkaBorder

val LightBackground: Color
    @Composable
    @ReadOnlyComposable
    get() = JhumkaBlushBackground

val LightSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = JhumkaSurface

val LightSurfaceVariant: Color
    @Composable
    @ReadOnlyComposable
    get() = JhumkaSurfaceVariant

val TextPrimaryDark: Color
    @Composable
    @ReadOnlyComposable
    get() = JhumkaTextPrimary

val TextSecondaryDark: Color
    @Composable
    @ReadOnlyComposable
    get() = JhumkaTextSecondary

val TextTertiaryDark: Color
    @Composable
    @ReadOnlyComposable
    get() = JhumkaTextTertiary

val TextPrimaryLight: Color
    @Composable
    @ReadOnlyComposable
    get() = JhumkaTextPrimary

val TextSecondaryLight: Color
    @Composable
    @ReadOnlyComposable
    get() = JhumkaTextSecondary
