package com.snowi.snuzznoise.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.snowi.snuzznoise.R
val FigtreeFontFamily = FontFamily(
    Font(R.font.figtree_regular, FontWeight.Normal),
    Font(R.font.figtree_bold, FontWeight.Bold)
)
val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal)
)

val AppTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = FigtreeFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = FigtreeFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FigtreeFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 48.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FigtreeFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp
    )
)