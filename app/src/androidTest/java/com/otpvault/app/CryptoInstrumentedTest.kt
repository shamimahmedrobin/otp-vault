package com.otpvault.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.otpvault.app.core.crypto.CryptoManager
import com.otpvault.app.core.crypto.EncryptedData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/** Keystore-backed encryption can only run on a device / emulator. */
@RunWith(AndroidJUnit4::class)
class CryptoInstrumentedTest {
    @Test fun keystoreRoundTrip() {
        val crypto = CryptoManager("test_alias")
        val enc = crypto.encrypt("654321")
        assertEquals("654321", crypto.decrypt(enc))
        val tampered = EncryptedData(enc.cipherText.copyOf().also { it[0] = (it[0] + 1).toByte() }, enc.iv)
        assertNull(crypto.decrypt(tampered))
        crypto.deleteKey()
    }
}
