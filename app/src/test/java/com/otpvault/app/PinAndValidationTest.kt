package com.otpvault.app

import com.otpvault.app.core.security.PinManager
import com.otpvault.app.core.security.PinRateLimiter
import com.otpvault.app.core.util.OtpValidator
import com.otpvault.app.core.util.PhoneMask
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinAndValidationTest {

    private class MapStore : PinManager.PinStore {
        val map = mutableMapOf<String, String>()
        override fun read(key: String): String? = map[key]
        override fun write(key: String, value: String) { map[key] = value }
        override fun clear() = map.clear()
    }

    @Test
    fun `pin verifier accepts correct pin only and never stores raw pin`() {
        val store = MapStore()
        val manager = PinManager(store)
        manager.setPin("246810")
        assertTrue(manager.isPinSet())
        assertTrue(manager.verifyPin("246810"))
        assertFalse(manager.verifyPin("000000"))
        assertFalse(store.map.values.any { it.contains("246810") })
    }

    @Test
    fun `rate limiter locks after five failures and resets on success`() {
        var now = 1_000_000L
        val limiter = PinRateLimiter { now }
        repeat(4) { limiter.onFailure() }
        assertTrue(limiter.check() is PinRateLimiter.Result.Allowed)
        limiter.onFailure()
        val locked = limiter.check()
        assertTrue(locked is PinRateLimiter.Result.Locked)
        assertEquals(30, (locked as PinRateLimiter.Result.Locked).remainingSeconds)
        now += 31_000
        assertTrue(limiter.check() is PinRateLimiter.Result.Allowed)
        limiter.onSuccess()
        assertEquals(0, limiter.failedAttempts())
    }

    @Test
    fun `otp validation bounds`() {
        assertFalse(OtpValidator.isValid("123"))
        assertTrue(OtpValidator.isValid("1234"))
        assertTrue(OtpValidator.isValid("123456"))
        assertTrue(OtpValidator.isValid("AB12CD"))
        assertFalse(OtpValidator.isValid("12345678901"))
        assertFalse(OtpValidator.isValid("12 34"))
    }

    @Test
    fun `phone validation and masking`() {
        assertTrue(PhoneMask.isValid("01712345678"))
        assertTrue(PhoneMask.isValid("+8801712345678"))
        assertTrue(PhoneMask.isValid(""))
        assertFalse(PhoneMask.isValid("abc"))
        assertEquals("017••••••78", PhoneMask.mask("01712345678"))
    }
}
