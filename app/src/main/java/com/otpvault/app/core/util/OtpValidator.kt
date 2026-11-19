package com.otpvault.app.core.util

object OtpValidator {
    /** OTPs are 4-10 chars; numeric by default, alphanumeric allowed when requested. */
    fun isValid(code: String, allowAlphanumeric: Boolean = true): Boolean {
        val c = code.trim()
        if (c.length !in 4..10) return false
        return if (allowAlphanumeric) c.all { it.isLetterOrDigit() } else c.all { it.isDigit() }
    }

    fun mask(code: String): String = "•".repeat(code.length.coerceIn(4, 10))
}
