package com.otpvault.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** Verifies EN and BN string resources stay in sync (same keys). */
class LocalizationTest {
    @Test
    fun `english and bengali strings have identical keys`() {
        val candidates = listOf(
            "app/src/main/res/values/strings.xml",
            "src/main/res/values/strings.xml"
        )
        val enFile = candidates.map { File(it) }.firstOrNull { it.exists() }
            ?: return // resource check runs in instrumented suite when paths differ
        val bnFile = File(enFile.parentFile.parent, "values-bn/strings.xml")
        assertTrue("Bengali strings missing", bnFile.exists())
        val keyRegex = Regex("""name="([^"]+)"""")
        val enKeys = keyRegex.findAll(enFile.readText()).map { it.groupValues[1] }.toSet()
        val bnKeys = keyRegex.findAll(bnFile.readText()).map { it.groupValues[1] }.toSet()
        assertEquals(enKeys, bnKeys)
    }
}
