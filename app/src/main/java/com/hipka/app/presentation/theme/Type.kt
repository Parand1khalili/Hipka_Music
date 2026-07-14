package com.hipka.app.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val HipkaFontFamily = FontFamily.Default

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = HipkaFontFamily, fontWeight = FontWeight.Bold, fontSize = 36.sp, lineHeight = 44.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = HipkaFontFamily, fontWeight = FontWeight.Bold, fontSize = 28.sp, lineHeight = 36.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = HipkaFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 24.sp, lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontFamily = HipkaFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontFamily = HipkaFontFamily, fontWeight = FontWeight.Medium, fontSize = 16.sp, lineHeight = 24.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = HipkaFontFamily, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = HipkaFontFamily, fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = HipkaFontFamily, fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = HipkaFontFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = HipkaFontFamily, fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 16.sp
    )
)