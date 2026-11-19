package com.otpvault.app.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.otpvault.app.core.preferences.ThemeMode

val DeepNavy = Color(0xFF182C50)
val VaultBlue = Color(0xFF4776E6)
val VaultBackground = Color(0xFFF6F8FC)
val VaultSurface = Color(0xFFFFFFFF)
val VaultSuccess = Color(0xFF16A36A)
val VaultWarning = Color(0xFFE99A30)
val VaultError = Color(0xFFD94A4A)

private val LightColors = lightColorScheme(
    primary = DeepNavy,
    secondary = VaultBlue,
    background = VaultBackground,
    surface = VaultSurface,
    error = VaultError
)

private val DarkColors = darkColorScheme(
    primary = VaultBlue,
    secondary = VaultBlue,
    background = Color(0xFF0D1526),
    surface = Color(0xFF182C50),
    error = VaultError
)

@Composable
fun OtpVaultTheme(mode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val dark = when (mode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = VaultTypography,
        content = content
    )
}
