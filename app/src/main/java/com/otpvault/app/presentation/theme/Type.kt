package com.otpvault.app.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val VaultTypography = Typography(
    headlineMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Bold, fontSize = 26.sp),
    titleLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontFamily = FontFamily.Default, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = FontFamily.Default, fontSize = 14.sp),
    labelLarge = TextStyle(fontFamily = FontFamily.Default, fontWeight = FontWeight.Medium, fontSize = 14.sp)
)
