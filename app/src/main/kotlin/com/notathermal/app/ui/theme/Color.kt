package com.notathermal.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * Warm teal palette — primary teal that leans toward warm green/cyan, paired
 * with a soft amber accent and warm neutral surfaces. Tuned so light mode feels
 * like a fresh receipt paper (warm cream surface) and dark mode feels muted
 * (deep teal-leaning charcoal). Generated from key colors:
 *
 * - primary seed:   #00897B (warm teal)
 * - secondary seed: #B96B3C (warm amber/terracotta)
 * - tertiary seed:  #4F6D55 (warm sage)
 *
 * Tokens follow Material 3 conventions; values are hand-picked so neighbouring
 * surfaces have enough tonal separation without being noisy.
 */

// --- Light ---
private val LightPrimary = Color(0xFF006A60)
private val LightOnPrimary = Color(0xFFFFFFFF)
private val LightPrimaryContainer = Color(0xFF7AF7E5)
private val LightOnPrimaryContainer = Color(0xFF00201C)

private val LightSecondary = Color(0xFF8E4D27)
private val LightOnSecondary = Color(0xFFFFFFFF)
private val LightSecondaryContainer = Color(0xFFFFDBC8)
private val LightOnSecondaryContainer = Color(0xFF311300)

private val LightTertiary = Color(0xFF4A6358)
private val LightOnTertiary = Color(0xFFFFFFFF)
private val LightTertiaryContainer = Color(0xFFCCE9DA)
private val LightOnTertiaryContainer = Color(0xFF062017)

private val LightError = Color(0xFFB3261E)
private val LightOnError = Color(0xFFFFFFFF)
private val LightErrorContainer = Color(0xFFF9DEDC)
private val LightOnErrorContainer = Color(0xFF410E0B)

private val LightBackground = Color(0xFFFAF8F4)
private val LightOnBackground = Color(0xFF191C1B)
private val LightSurface = Color(0xFFFAF8F4)
private val LightOnSurface = Color(0xFF191C1B)
private val LightSurfaceVariant = Color(0xFFDAE5E1)
private val LightOnSurfaceVariant = Color(0xFF3F4946)
private val LightOutline = Color(0xFF6F7976)
private val LightOutlineVariant = Color(0xFFBEC9C5)
private val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
private val LightSurfaceContainerLow = Color(0xFFF4F1EC)
private val LightSurfaceContainer = Color(0xFFEEEBE5)
private val LightSurfaceContainerHigh = Color(0xFFE8E4DE)
private val LightSurfaceContainerHighest = Color(0xFFE2DED8)

// --- Dark ---
private val DarkPrimary = Color(0xFF5DDBCA)
private val DarkOnPrimary = Color(0xFF003731)
private val DarkPrimaryContainer = Color(0xFF005048)
private val DarkOnPrimaryContainer = Color(0xFF7AF7E5)

private val DarkSecondary = Color(0xFFFFB68C)
private val DarkOnSecondary = Color(0xFF522300)
private val DarkSecondaryContainer = Color(0xFF723612)
private val DarkOnSecondaryContainer = Color(0xFFFFDBC8)

private val DarkTertiary = Color(0xFFB1CDBE)
private val DarkOnTertiary = Color(0xFF1D352B)
private val DarkTertiaryContainer = Color(0xFF334B41)
private val DarkOnTertiaryContainer = Color(0xFFCCE9DA)

private val DarkError = Color(0xFFF2B8B5)
private val DarkOnError = Color(0xFF601410)
private val DarkErrorContainer = Color(0xFF8C1D18)
private val DarkOnErrorContainer = Color(0xFFF9DEDC)

private val DarkBackground = Color(0xFF101413)
private val DarkOnBackground = Color(0xFFE0E3E1)
private val DarkSurface = Color(0xFF101413)
private val DarkOnSurface = Color(0xFFE0E3E1)
private val DarkSurfaceVariant = Color(0xFF3F4946)
private val DarkOnSurfaceVariant = Color(0xFFBEC9C5)
private val DarkOutline = Color(0xFF899390)
private val DarkOutlineVariant = Color(0xFF3F4946)
private val DarkSurfaceContainerLowest = Color(0xFF0B0E0E)
private val DarkSurfaceContainerLow = Color(0xFF181C1B)
private val DarkSurfaceContainer = Color(0xFF1C201F)
private val DarkSurfaceContainerHigh = Color(0xFF262B2A)
private val DarkSurfaceContainerHighest = Color(0xFF313635)

internal val WarmTealLightColors = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    surfaceContainerLowest = LightSurfaceContainerLowest,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest
)

internal val WarmTealDarkColors = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    surfaceContainerLowest = DarkSurfaceContainerLowest,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest
)
