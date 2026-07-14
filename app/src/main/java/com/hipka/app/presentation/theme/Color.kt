package com.hipka.app.presentation.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Brand palette sampled directly from the approved mockup:
//  - Wine/crimson: play button, active bottom-nav tab, "See all" links, progress fill
//  - Navy: top bar + hero card background
// Nothing outside this file should ever write a hex value — go through
// MaterialTheme.colorScheme (see Theme.kt) instead.

// Wine / crimson (primary)
val Wine20 = Color(0xFF460B1D)
val Wine30 = Color(0xFF7B1433)
val Wine40 = Color(0xFF871638)
val Wine80 = Color(0xFFEC8DAA)
val Wine90 = Color(0xFFF0DBE1)

// Navy (secondary)
val Navy10 = Color(0xFF061023)
val Navy20 = Color(0xFF081733)
val Navy30 = Color(0xFF0B2047)
val Navy40 = Color(0xFF123473)
val Navy80 = Color(0xFFB2C1DB)
val Navy95 = Color(0xFFEAEEF5)

// Error — kept a warm orange-red rather than another wine tone, so
// destructive/error states stay visually distinct from the brand accent.
val Error40 = Color(0xFFC43C28)
val Error80 = Color(0xFFECABA2)
val ErrorDark20 = Color(0xFF3A0A02)

// Neutrals
val Neutral05 = Color(0xFF040C1A)
val NeutralSurfaceDark = Color(0xFF0A1C3E)
val Neutral10 = Color(0xFF07142C)
val Neutral90 = Color(0xFFEEEFF1)
val Neutral95 = Color(0xFFF1F2F4)
val Neutral99 = Color(0xFFFDFDFD)
val OnSurfaceVariantLight = Color(0xFFA3A6B1) // sampled caption/subtitle gray (e.g. artist names)
val OnSurfaceVariantDark = Color(0xFFB4B6BB)

val LightColorScheme = lightColorScheme(
    primary = Wine40,
    onPrimary = Color.White,
    primaryContainer = Wine90,
    onPrimaryContainer = Wine20,
    secondary = Navy30,
    onSecondary = Color.White,
    secondaryContainer = Navy95,
    onSecondaryContainer = Navy10,
    error = Error40,
    onError = Color.White,
    background = Neutral99,
    onBackground = Neutral10,
    surface = Neutral99,
    onSurface = Neutral10,
    surfaceVariant = Neutral95,
    onSurfaceVariant = OnSurfaceVariantLight
)

val DarkColorScheme = darkColorScheme(
    primary = Wine80,
    onPrimary = Wine20,
    primaryContainer = Wine30,
    onPrimaryContainer = Wine90,
    secondary = Navy80,
    onSecondary = Navy20,
    secondaryContainer = Navy40,
    onSecondaryContainer = Navy95,
    error = Error80,
    onError = ErrorDark20,
    background = Neutral05,
    onBackground = Neutral90,
    surface = NeutralSurfaceDark,
    onSurface = Neutral90,
    surfaceVariant = Navy20,
    onSurfaceVariant = OnSurfaceVariantDark
)
