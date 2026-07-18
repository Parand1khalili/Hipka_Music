package com.hipka.app.presentation.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class Dimens(
    val spaceXXS: Dp = 2.dp,
    val spaceXS: Dp = 4.dp,
    val spaceS: Dp = 8.dp,
    val spaceM: Dp = 16.dp,
    val spaceL: Dp = 24.dp,
    val spaceXL: Dp = 32.dp,
    val spaceXXL: Dp = 48.dp,

    val cornerS: Dp = 8.dp,
    val cornerM: Dp = 16.dp,
    val cornerL: Dp = 24.dp,

    val bottomNavHeight: Dp = 72.dp,
    val miniPlayerHeight: Dp = 64.dp,

    val albumCoverS: Dp = 48.dp,
    val albumCoverM: Dp = 120.dp,
    val albumCoverL: Dp = 280.dp,

    val visualizerHeight: Dp = 64.dp
)

val LocalDimens = staticCompositionLocalOf { Dimens() }