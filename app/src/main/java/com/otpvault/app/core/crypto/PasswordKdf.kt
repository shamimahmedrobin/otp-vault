package com.otpvault.app.core.crypto

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * PBKDF2-HMAC-SHA256 key derivation used for the PIN verifier and for
 * backup-password keys. Pure JVM so it is unit-testable off-device.
 */
object PasswordKdf {
    const val ITERATIONS = 120_000
    const val SALT_BYTES = 16
    const val KEY_BITS = 256

    fun newSalt(): ByteArray = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }

    fun derive(password: CharArray, salt: ByteArray, iterations: Int = ITERATIONS): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    /** Constant-time comparison to avoid timing leaks on the verifier. */
    fun constantTimeEquals(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var diff = 0
        for (i in a.indices) diff = diff or (a[i].toInt() xor b[i].toInt())
        return diff == 0
    }
}
