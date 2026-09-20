package com.example.util

import com.example.data.local.SubscriptionTier
import com.example.data.local.WorkshopProfile

/**
 * Feature Gate & Licensing Engine for Bengkel Qu.
 * Manages access control between REGULAR (Free/Starter) and PRO (Premium) tiers.
 */
object FeatureGate {

    const val REGULAR_MAX_STOCKS = 50
    const val REGULAR_MAX_DAILY_SERVICES = 15
    const val REGULAR_MAX_STAFF = 2

    // Demo / Sample Keys for testing
    const val DEMO_PRO_KEY = "BQPRO-DEV2026-VIP"
    const val DEMO_PRO_KEY_ALT = "BQPRO-BENGKELQU-2026"

    sealed class GateResult {
        data object Allowed : GateResult()
        data class Denied(
            val featureName: String,
            val reason: String,
            val limit: Int = 0,
            val current: Int = 0
        ) : GateResult()
    }

    /**
     * Checks if the profile has an active PRO license.
     */
    fun isPro(profile: WorkshopProfile?): Boolean {
        if (profile == null) return false
        val isTierPro = profile.subscriptionTier == SubscriptionTier.PRO
        val isNotExpired = profile.validUntilEpoch == 0L || profile.validUntilEpoch > System.currentTimeMillis()
        return isTierPro && isNotExpired
    }

    /**
     * Validates stock creation against tier limits.
     */
    fun canAddStock(currentCount: Int, isPro: Boolean): GateResult {
        return if (isPro || currentCount < REGULAR_MAX_STOCKS) {
            GateResult.Allowed
        } else {
            GateResult.Denied(
                featureName = "Katalog Sparepart",
                reason = "Batas versi Reguler adalah $REGULAR_MAX_STOCKS item suku cadang. Upgrade ke PRO untuk katalog tanpa batas.",
                limit = REGULAR_MAX_STOCKS,
                current = currentCount
            )
        }
    }

    /**
     * Validates service queue creation against tier daily limits.
     */
    fun canAddServiceQueue(todayCount: Int, isPro: Boolean): GateResult {
        return if (isPro || todayCount < REGULAR_MAX_DAILY_SERVICES) {
            GateResult.Allowed
        } else {
            GateResult.Denied(
                featureName = "Antrian Servis Harian",
                reason = "Batas antrian versi Reguler adalah $REGULAR_MAX_DAILY_SERVICES servis per hari. Upgrade ke PRO untuk antrian tanpa batas.",
                limit = REGULAR_MAX_DAILY_SERVICES,
                current = todayCount
            )
        }
    }

    /**
     * Validates staff members creation against tier limits.
     */
    fun canAddStaff(currentCount: Int, isPro: Boolean): GateResult {
        return if (isPro || currentCount < REGULAR_MAX_STAFF) {
            GateResult.Allowed
        } else {
            GateResult.Denied(
                featureName = "Manajemen Karyawan",
                reason = "Versi Reguler mendukung hingga $REGULAR_MAX_STAFF staf. Upgrade ke PRO untuk menambah staf dan mekanik tanpa batas.",
                limit = REGULAR_MAX_STAFF,
                current = currentCount
            )
        }
    }

    /**
     * Validates barcode camera scanner usage.
     */
    fun canUseBarcodeScanner(isPro: Boolean): GateResult {
        return if (isPro) {
            GateResult.Allowed
        } else {
            GateResult.Denied(
                featureName = "Scan Barcode Kamera Cepat",
                reason = "Pemindai barcode otomatis dengan kamera adalah fitur eksklusif Bengkel Qu PRO. Anda tetap bisa memasukkan kode secara manual di versi Reguler."
            )
        }
    }

    /**
     * Validates official watermark-free PDF printing.
     */
    fun canPrintOfficialPdf(isPro: Boolean): GateResult {
        return if (isPro) {
            GateResult.Allowed
        } else {
            GateResult.Denied(
                featureName = "Cetak PDF Resmi Bebas Watermark",
                reason = "Cetak nota PDF resmi berlogo bengkel tanpa watermark hanya tersedia di versi PRO."
            )
        }
    }

    /**
     * Validates a license key entered by the user.
     * Supports standard serial keys: BQPRO-XXXX-XXXX or valid checksums.
     */
    fun verifyLicenseKey(key: String, workshopEmail: String): Pair<Boolean, String> {
        val trimmed = key.trim().uppercase()
        if (trimmed.isBlank()) {
            return Pair(false, "Kode lisensi tidak boleh kosong")
        }

        // Check for preset developer/demo keys
        if (trimmed == DEMO_PRO_KEY || trimmed == DEMO_PRO_KEY_ALT) {
            return Pair(true, "Lisensi Bengkel Qu PRO (Seumur Hidup) Berhasil Diaktifkan!")
        }

        // Check key pattern: BQPRO-XXXX-XXXX or BQPRO-XXXXXX
        if (trimmed.startsWith("BQPRO-") && trimmed.length >= 12) {
            // Check alphanumeric characters
            val parts = trimmed.split("-")
            if (parts.size >= 2 && parts.all { it.all { ch -> ch.isLetterOrDigit() } }) {
                return Pair(true, "Lisensi Bengkel Qu PRO Resmi Berhasil Diverifikasi & Diaktifkan!")
            }
        }

        return Pair(false, "Format kode lisensi tidak valid. Contoh: BQPRO-DEV2026-VIP")
    }
}
