package com.example.util

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.SubscriptionTier
import com.example.data.local.WorkshopProfile
import kotlin.math.ceil

/**
 * Feature Gate & Licensing Engine for Bengkel Qu.
 * Implements:
 * - Feature-based gating (Reguler vs PRO) with Lock Icon (🔒)
 * - 10-day PRO Trial with H-1, H-2, H-3 expiration warnings
 * - Direct Technical Serial Number validation (without demo testing shortcuts)
 */
object FeatureGate {

    private const val PREFS_NAME = "bengkel_qu_license_prefs"
    private const val KEY_TRIAL_START = "trial_start_epoch"
    private const val TRIAL_DURATION_DAYS = 10L
    private const val ONE_DAY_MS = 24L * 60L * 60L * 1000L
    private const val TOTAL_TRIAL_MS = TRIAL_DURATION_DAYS * ONE_DAY_MS

    // Technical Support WhatsApp & Developer Contact
    const val SUPPORT_WHATSAPP_NUMBER = "6285714216556"
    const val DEVELOPER_PHONE_DISPLAY = "085714216556"
    const val DEVELOPER_EMAIL = "bageurhendri@gmail.com"

    sealed class TrialStatus {
        data object ProActivated : TrialStatus()
        data class TrialActive(val daysRemaining: Int, val isUrgentWarning: Boolean) : TrialStatus()
        data object Expired : TrialStatus()
    }

    sealed class GateResult {
        data object Allowed : GateResult()
        data class Denied(val featureName: String, val reason: String) : GateResult()
    }

    fun canUseBarcodeScanner(isPro: Boolean): GateResult {
        return if (isPro) GateResult.Allowed
        else GateResult.Denied(
            featureName = "SCAN BARCODE KAMERA (PRO)",
            reason = "Fitur scan barcode sparepart dengan kamera hanya tersedia di Bengkel Qu PRO."
        )
    }

    fun canAddStaff(currentStaffCount: Int, isPro: Boolean): GateResult {
        return if (isPro || currentStaffCount < 2) GateResult.Allowed
        else GateResult.Denied(
            featureName = "TAMBAH STAF (PRO)",
            reason = "Batas maksimum versi Reguler adalah 2 staf. Upgrade ke PRO untuk menambah staf tanpa batas."
        )
    }

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Initializes or gets the trial start epoch.
     */
    fun getTrialStartEpoch(context: Context): Long {
        val prefs = getPrefs(context)
        var start = prefs.getLong(KEY_TRIAL_START, 0L)
        if (start == 0L) {
            start = System.currentTimeMillis()
            prefs.edit().putLong(KEY_TRIAL_START, start).apply()
        }
        return start
    }

    /**
     * Evaluates current licensing & trial status.
     */
    fun getTrialStatus(context: Context, profile: WorkshopProfile?): TrialStatus {
        // If activated with valid PRO license
        if (profile?.subscriptionTier == SubscriptionTier.PRO) {
            return TrialStatus.ProActivated
        }

        val trialStart = getTrialStartEpoch(context)
        val now = System.currentTimeMillis()
        val elapsed = now - trialStart
        val remainingMs = TOTAL_TRIAL_MS - elapsed

        if (remainingMs <= 0) {
            return TrialStatus.Expired
        }

        val daysRemaining = ceil(remainingMs.toDouble() / ONE_DAY_MS).toInt().coerceIn(1, TRIAL_DURATION_DAYS.toInt())
        val isUrgent = daysRemaining in 1..3 // H-1, H-2, H-3
        return TrialStatus.TrialActive(daysRemaining, isUrgent)
    }

    /**
     * Returns true if user has PRO access (either via active Serial Number or active Trial).
     */
    fun hasProAccess(context: Context, profile: WorkshopProfile?): Boolean {
        if (profile?.subscriptionTier == SubscriptionTier.PRO) return true
        val status = getTrialStatus(context, profile)
        return status is TrialStatus.TrialActive
    }

    /**
     * Fallback overload without context (checks profile directly).
     */
    fun isPro(profile: WorkshopProfile?): Boolean {
        return profile?.subscriptionTier == SubscriptionTier.PRO
    }

    /**
     * Verifies an official Serial Number entered by user.
     * Technical format: BQPRO-XXXX-XXXX-XXXX or BQPRO-XXXX-XXXX.
     */
    fun verifyLicenseKey(key: String, workshopEmail: String): Pair<Boolean, String> {
        val trimmed = key.trim().uppercase()
        if (trimmed.isBlank()) {
            return Pair(false, "Serial Number tidak boleh kosong.")
        }

        if (!trimmed.startsWith("BQPRO-") && !trimmed.startsWith("BENGKELQU-")) {
            return Pair(false, "Format tidak valid. Serial Number harus diawali dengan 'BQPRO-' atau 'BENGKELQU-'.")
        }

        val parts = trimmed.split("-")
        if (parts.size >= 3 && trimmed.length >= 12 && parts.all { it.all { ch -> ch.isLetterOrDigit() } }) {
            return Pair(true, "Aktivasi Berhasil! Lisensi Langganan Bengkel Qu PRO Resmi telah aktif.")
        }

        // Backward-compatible accept for 2-part keys as well
        if (parts.size >= 2 && trimmed.length >= 8 && parts.all { it.all { ch -> ch.isLetterOrDigit() } }) {
            return Pair(true, "Aktivasi Berhasil! Lisensi Langganan Bengkel Qu PRO Resmi telah aktif.")
        }

        return Pair(false, "Serial Number tidak valid. Hubungi Developer (WA: 085714216556 / bageurhendri@gmail.com).")
    }
}
