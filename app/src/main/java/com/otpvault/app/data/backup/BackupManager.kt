package com.otpvault.app.data.backup

import com.otpvault.app.core.crypto.PasswordKdf
import com.otpvault.app.data.local.dao.OtpDao
import com.otpvault.app.data.local.dao.ServiceDao
import com.otpvault.app.data.local.entity.OTPEntry
import com.otpvault.app.data.local.entity.ServiceEntity
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Encrypted local backup.
 * Format (binary): magic "OTPV1" | salt(16) | iv(12) | AES-256-GCM ciphertext.
 * Key = PBKDF2-HMAC-SHA256(password, salt, 120k). The GCM tag authenticates
 * the whole payload, so corrupted / tampered backups are rejected.
 * Payload JSON contains the already-Keystore-encrypted OTP fields re-encrypted
 * for portability: OTP/phone/note are decrypted locally, serialized, then the
 * whole file is encrypted with the backup key. Plaintext never touches disk.
 * Android Keystore keys themselves are NEVER exported; on restore, values are
 * re-encrypted with the device's fresh local key.
 */
class BackupManager(
    private val otpDao: OtpDao,
    private val serviceDao: ServiceDao,
    private val decryptLocal: (ByteArray, ByteArray) -> String?,
    private val encryptLocal: (String) -> Pair<ByteArray, ByteArray>
) {
    suspend fun exportJson(): String {
        val root = JSONObject()
        val services = JSONArray()
        // services are read through a one-shot snapshot of OTP-linked + custom rows
        val entries = otpDao.getAllOnce()
        val serviceRows = serviceDao.getAllOnce()
        for (s in serviceRows) {
            services.put(JSONObject().apply {
                put("id", s.id); put("name", s.name); put("category", s.category)
                put("icon", s.icon); put("isCustom", s.isCustom); put("createdAt", s.createdAt)
            })
        }
        root.put("services", services)
        val otps = JSONArray()
        for (e in entries) {
            val code = decryptLocal(e.otpEncrypted, e.otpIv) ?: continue
            otps.put(JSONObject().apply {
                put("id", e.id); put("serviceId", e.serviceId)
                put("phone", e.phoneEncrypted?.let { c -> e.phoneIv?.let { iv -> decryptLocal(c, iv) } } ?: JSONObject.NULL)
                put("code", code)
                put("note", e.noteEncrypted?.let { c -> e.noteIv?.let { iv -> decryptLocal(c, iv) } } ?: JSONObject.NULL)
                put("createdAt", e.createdAt); put("expiresAt", e.expiresAt)
                put("isUsed", e.isUsed); put("usedAt", e.usedAt ?: JSONObject.NULL)
            })
        }
        root.put("otps", otps)
        root.put("version", 1)
        return root.toString()
    }

    suspend fun importJson(json: String): Int {
        val root = JSONObject(json)
        require(root.optInt("version", -1) == 1) { "Unsupported backup version" }
        val services = root.getJSONArray("services")
        val seenServiceIds = mutableSetOf<String>()
        for (i in 0 until services.length()) {
            val s = services.getJSONObject(i)
            val id = s.getString("id")
            if (!seenServiceIds.add(id)) continue
            if (serviceDao.getById(id) == null) {
                serviceDao.insert(
                    ServiceEntity(
                        id = id, name = s.getString("name"), category = s.optString("category", "Other"),
                        icon = s.optString("icon", "apps"), isCustom = s.optBoolean("isCustom", false),
                        createdAt = s.optLong("createdAt", System.currentTimeMillis())
                    )
                )
            }
        }
        val otps = root.getJSONArray("otps")
        var restored = 0
        val seenIds = mutableSetOf<String>()
        for (i in 0 until otps.length()) {
            val o = otps.getJSONObject(i)
            val id = o.getString("id")
            if (!seenIds.add(id)) continue // prevent duplicates
            if (otpDao.getById(id) != null) continue
            val code = o.getString("code")
            if (code.isBlank() || code.length > 10) continue // malformed record guard
            val (otpC, otpIv) = encryptLocal(code)
            val phone = if (o.isNull("phone")) null else o.getString("phone")
            val phoneEnc = phone?.let { encryptLocal(it) }
            val note = if (o.isNull("note")) null else o.getString("note")
            val noteEnc = note?.let { encryptLocal(it) }
            otpDao.insert(
                OTPEntry(
                    id = id, serviceId = o.getString("serviceId"),
                    phoneEncrypted = phoneEnc?.first, phoneIv = phoneEnc?.second,
                    otpEncrypted = otpC, otpIv = otpIv,
                    noteEncrypted = noteEnc?.first, noteIv = noteEnc?.second,
                    createdAt = o.getLong("createdAt"), expiresAt = o.getLong("expiresAt"),
                    isUsed = o.optBoolean("isUsed", false),
                    usedAt = if (o.isNull("usedAt")) null else o.getLong("usedAt")
                )
            )
            restored++
        }
        return restored
    }

    fun encryptBackup(plain: ByteArray, password: CharArray): ByteArray {
        val salt = PasswordKdf.newSalt()
        val keyBytes = PasswordKdf.derive(password, salt)
        val key = SecretKeySpec(keyBytes, "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plain)
        val out = ByteArrayOutputStream()
        out.write(MAGIC); out.write(salt); out.write(iv); out.write(cipherText)
        return out.toByteArray()
    }

    /** Returns null when the password is wrong or the file is corrupted. */
    fun decryptBackup(data: ByteArray, password: CharArray): ByteArray? {
        return try {
            if (data.size < MAGIC.size + 16 + 12 + 16) return null
            if (!data.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)) return null
            val salt = data.copyOfRange(MAGIC.size, MAGIC.size + 16)
            val iv = data.copyOfRange(MAGIC.size + 16, MAGIC.size + 28)
            val cipherText = data.copyOfRange(MAGIC.size + 28, data.size)
            val key = SecretKeySpec(PasswordKdf.derive(password, salt), "AES")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, iv))
            cipher.doFinal(cipherText)
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        val MAGIC = "OTPV1".toByteArray(Charsets.US_ASCII)
        fun encodeBase64(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)
    }
}
