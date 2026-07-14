package com.hipka.app.presentation.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

val CrimsonPrimary = Color(0xFF8A1638)
val NavyDark = Color(0xFF081734)
val BackgroundLight = Color(0xFFFFFFFF)
val SurfaceLight = Color(0xFFF5F7FA)


val CrimsonLight = Color(0xFFFFB4A9)
val CrimsonDark = Color(0xFF690005)
val BackgroundDark = Color(0xFF0B1120)
val SurfaceDark = Color(0xFF172033)

val LightColorScheme = lightColorScheme(
    primary = CrimsonPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD9),
    onPrimaryContainer = CrimsonDark,
    background = BackgroundLight,
    onBackground = NavyDark,
    surface = BackgroundLight,
    onSurface = NavyDark,
    surfaceVariant = SurfaceLight,
    onSurfaceVariant = NavyDark
)

val DarkColorScheme = darkColorScheme(
    primary = CrimsonLight,
    onPrimary = CrimsonDark,
    primaryContainer = CrimsonPrimary,
    onPrimaryContainer = Color.White,
    background = BackgroundDark,
    onBackground = Color.White,
    surface = SurfaceDark,
    onSurface = Color.White,
    surfaceVariant = NavyDark,
    onSurfaceVariant = Color.LightGray
)