package com.otpvault.app.core.security

import com.otpvault.app.core.crypto.PasswordKdf
import java.util.Base64

/**
 * Stores ONLY a salted PBKDF2 verifier of the 6-digit PIN.
 * The raw PIN is never persisted anywhere.
 */
class PinManager(private val store: PinStore) {

    interface PinStore {
        fun read(key: String): String?
        fun write(key: String, value: String)
        fun clear()
    }

    fun isPinSet(): Boolean = store.read(KEY_VERIFIER) != null

    fun setPin(pin: String) {
        require(pin.length == PIN_LENGTH && pin.all { it.isDigit() }) { "PIN must be 6 digits" }
        val salt = PasswordKdf.newSalt()
        val hash = PasswordKdf.derive(pin.toCharArray(), salt)
        store.write(KEY_SALT, Base64.getEncoder().encodeToString(salt))
        store.write(KEY_VERIFIER, Base64.getEncoder().encodeToString(hash))
    }

    fun verifyPin(pin: String): Boolean {
        val saltB64 = store.read(KEY_SALT) ?: return false
        val verifierB64 = store.read(KEY_VERIFIER) ?: return false
        val salt = Base64.getDecoder().decode(saltB64)
        val expected = Base64.getDecoder().decode(verifierB64)
        val actual = PasswordKdf.derive(pin.toCharArray(), salt)
        return PasswordKdf.constantTimeEquals(expected, actual)
    }

    fun clear() = store.clear()

    companion object {
        const val PIN_LENGTH = 6
        private const val KEY_SALT = "pin_salt"
        private const val KEY_VERIFIER = "pin_verifier"
    }
}
