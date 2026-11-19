package com.otpvault.app.core.util

/** Phone masking + validation helpers (pure, unit-testable). */
object PhoneMask {
    /** Keeps the first 3 and last 2 digits visible: 01712****67 */
    fun mask(phone: String?): String {
        if (phone.isNullOrBlank()) return ""
        val digits = phone.trim()
        if (digits.length <= 5) return "•".repeat(digits.length)
        val head = digits.take(3)
        val tail = digits.takeLast(2)
        return head + "•".repeat(digits.length - 5) + tail
    }

    /** Accepts BD numbers (01XXXXXXXXX, +8801XXXXXXXXX) and general international + numbers. */
    fun isValid(phone: String): Boolean {
        if (phone.isBlank()) return true // optional field
        val cleaned = phone.replace(" ", "").replace("-", "")
        if (cleaned.startsWith("+")) {
            val digits = cleaned.drop(1)
            return digits.length in 7..15 && digits.all { it.isDigit() }
        }
        if (cleaned.startsWith("01")) return cleaned.length == 11 && cleaned.all { it.isDigit() }
        return cleaned.length in 7..15 && cleaned.all { it.isDigit() }
    }
}
