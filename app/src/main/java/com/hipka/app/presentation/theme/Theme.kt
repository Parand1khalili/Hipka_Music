package com.hipka.app.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider


object HipkaTheme {
    val dimens: Dimens
        @Composable
        get() = LocalDimens.current
}

@Composable
fun HipkaTheme(
    darkTheme: Boolean,
    dimens: Dimens = Dimens(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalDimens provides dimens) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = HipkaShapes,
            content = content
        )
    }
}