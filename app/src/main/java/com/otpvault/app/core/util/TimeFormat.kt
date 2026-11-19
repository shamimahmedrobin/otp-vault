package com.otpvault.app.core.util

import java.text.DateFormat
import java.util.Date
import java.util.Locale

object TimeFormat {
    fun countdown(remainingMs: Long): String {
        if (remainingMs <= 0) return "0:00"
        val totalSeconds = remainingMs / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        else String.format(Locale.US, "%d:%02d", minutes, seconds)
    }

    fun dateTime(epochMs: Long, locale: Locale = Locale.getDefault()): String =
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale).format(Date(epochMs))

    fun isExpiringSoon(expiresAt: Long, now: Long = System.currentTimeMillis()): Boolean =
        expiresAt > now && expiresAt - now <= 60_000L
}
