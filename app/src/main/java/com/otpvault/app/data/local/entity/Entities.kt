package com.otpvault.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * OTP entry. The OTP itself, phone and note are stored ONLY as AES-GCM
 * ciphertext (otpEncrypted/phoneEncrypted/noteEncrypted) with per-field IVs.
 * Plaintext OTPs are never written to the database, logs or backups.
 */
@Entity(
    tableName = "otp_entries",
    foreignKeys = [
        ForeignKey(
            entity = ServiceEntity::class,
            parentColumns = ["id"],
            childColumns = ["serviceId"],
            onDelete = ForeignKey.RESTRICT // protect OTP records when a service is removed
        )
    ],
    indices = [Index("serviceId"), Index("expiresAt")]
)
data class OTPEntry(
    @PrimaryKey val id: String,
    val serviceId: String,
    val phoneEncrypted: ByteArray?,
    val phoneIv: ByteArray?,
    val otpEncrypted: ByteArray,
    val otpIv: ByteArray,
    val noteEncrypted: ByteArray?,
    val noteIv: ByteArray?,
    val createdAt: Long,
    val expiresAt: Long,
    val isUsed: Boolean = false,
    val usedAt: Long? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is OTPEntry) return false
        return id == other.id && serviceId == other.serviceId &&
            createdAt == other.createdAt && expiresAt == other.expiresAt && isUsed == other.isUsed
    }
    override fun hashCode(): Int = id.hashCode()
}

@Entity(tableName = "services")
data class ServiceEntity(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val icon: String,
    val isCustom: Boolean,
    val createdAt: Long
)

@Entity(tableName = "app_preferences")
data class AppPreference(
    @PrimaryKey val key: String,
    val value: String
)
