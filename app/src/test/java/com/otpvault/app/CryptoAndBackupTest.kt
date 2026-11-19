package com.otpvault.app

import com.otpvault.app.core.crypto.PasswordKdf
import com.otpvault.app.data.backup.BackupManager
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import javax.crypto.KeyGenerator
import javax.crypto.spec.SecretKeySpec
import com.otpvault.app.core.crypto.CryptoManager
import com.otpvault.app.core.crypto.EncryptedData

/**
 * JVM unit tests for the pure-crypto paths. The Android Keystore path itself
 * is covered by instrumented tests (CryptoInstrumentedTest).
 */
class CryptoAndBackupTest {

    private fun aesKey(): javax.crypto.SecretKey {
        val kg = KeyGenerator.getInstance("AES")
        kg.init(256)
        return kg.generateKey()
    }

    @Test
    fun `aes gcm round trip with explicit key`() {
        val crypto = CryptoManager()
        val key = aesKey()
        val data = crypto.encryptWithKey("123456".toByteArray(), key)
        val plain = crypto.decryptWithKey(data, key)
        assertEquals("123456", plain?.let { String(it) })
    }

    @Test
    fun `wrong key fails decryption`() {
        val crypto = CryptoManager()
        val data = crypto.encryptWithKey("999999".toByteArray(), aesKey())
        assertNull(crypto.decryptWithKey(data, aesKey()))
    }

    @Test
    fun `iv is unique per encryption`() {
        val crypto = CryptoManager()
        val key = aesKey()
        val a = crypto.encryptWithKey("111111".toByteArray(), key)
        val b = crypto.encryptWithKey("111111".toByteArray(), key)
        assertFalse(a.iv.contentEquals(b.iv))
        assertFalse(a.cipherText.contentEquals(b.cipherText))
    }

    @Test
    fun `tampered ciphertext is rejected`() {
        val crypto = CryptoManager()
        val key = aesKey()
        val data = crypto.encryptWithKey("222222".toByteArray(), key)
        val tampered = EncryptedData(data.cipherText.copyOf().also { it[0] = (it[0] + 1).toByte() }, data.iv)
        assertNull(crypto.decryptWithKey(tampered, key))
    }

    @Test
    fun `pbkdf2 is deterministic per salt and differs across salts`() {
        val salt1 = PasswordKdf.newSalt()
        val salt2 = PasswordKdf.newSalt()
        val a = PasswordKdf.derive("123456".toCharArray(), salt1, 1_000)
        val b = PasswordKdf.derive("123456".toCharArray(), salt1, 1_000)
        val c = PasswordKdf.derive("123456".toCharArray(), salt2, 1_000)
        assertArrayEquals(a, b)
        assertFalse(a.contentEquals(c))
    }

    @Test
    fun `backup round trip and wrong password rejection`() {
        val manager = backupManager()
        val payload = "{\"version\":1}".toByteArray()
        val encrypted = manager.encryptBackup(payload, "strong-password".toCharArray())
        assertArrayEquals(payload, manager.decryptBackup(encrypted, "strong-password".toCharArray()))
        assertNull(manager.decryptBackup(encrypted, "wrong-password".toCharArray()))
    }

    @Test
    fun `corrupted backup is rejected`() {
        val manager = backupManager()
        val encrypted = manager.encryptBackup("hello".toByteArray(), "password123".toCharArray())
        val corrupted = encrypted.copyOf().also { it[it.size - 1] = (it[it.size - 1] + 1).toByte() }
        assertNull(manager.decryptBackup(corrupted, "password123".toCharArray()))
        assertNull(manager.decryptBackup(byteArrayOf(1, 2, 3), "password123".toCharArray()))
    }

    private fun backupManager(): BackupManager {
        // DAOs are unused by the pure encrypt/decrypt functions under test.
        return BackupManager(
            otpDao = FakeDaos.otpDao(),
            serviceDao = FakeDaos.serviceDao(),
            decryptLocal = { _, _ -> null },
            encryptLocal = { ByteArray(0) to ByteArray(0) }
        )
    }
}
