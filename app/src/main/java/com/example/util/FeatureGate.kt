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
 * - 10-day PRO Trial with automatic start and urgent expiration warnings
 * - Device-bound Serial Number (SN) mechanism with plan durations (1, 3, 6, 12 months)
 * - Non-blocking principle: Basic cashiering, service queue, and local database remain 100% active and safe forever.
 */
object FeatureGate {

    private const val PREFS_NAME = "bengkel_qu_license_prefs"
    private const val KEY_TRIAL_START = "trial_start_epoch"
    private const val KEY_DEVICE_ID = "bengkel_device_id"
    private const val KEY_LAST_TRIAL_POPUP_DATE = "last_trial_popup_date"
    private const val TRIAL_DURATION_DAYS = 10L
    const val ONE_DAY_MS = 24L * 60L * 60L * 1000L
    private const val TOTAL_TRIAL_MS = TRIAL_DURATION_DAYS * ONE_DAY_MS

    // Technical Support WhatsApp & Developer Contact
    const val SUPPORT_WHATSAPP_NUMBER = "6285714216556"
    const val DEVELOPER_PHONE_DISPLAY = "085714216556"
    const val DEVELOPER_EMAIL = "bageurhendri@gmail.com"

    data class ProPlan(
        val id: String,
        val code: String, // 30D, 90D, 180D, 365D, LIFE
        val title: String,
        val durationLabel: String,
        val priceText: String,
        val days: Long,
        val badge: String,
        val description: String
    )

    val PRO_PLANS = listOf(
        ProPlan(
            id = "plan_30",
            code = "30D",
            title = "Paket 1 Bulan",
            durationLabel = "30 Hari",
            priceText = "Rp 30.000",
            days = 30L,
            badge = "Rp 1.000/hari",
            description = "Akses penuh fitur PRO selama 30 hari kalender."
        ),
        ProPlan(
            id = "plan_90",
            code = "90D",
            title = "Paket 3 Bulan",
            durationLabel = "90 Hari",
            priceText = "Rp 85.000",
            days = 90L,
            badge = "Hemat Rp 5.000",
            description = "Pilihan ekonomis kuartalan untuk operasional bengkel."
        ),
        ProPlan(
            id = "plan_180",
            code = "180D",
            title = "Paket 6 Bulan",
            durationLabel = "180 Hari",
            priceText = "Rp 160.000",
            days = 180L,
            badge = "Hemat Rp 20.000",
            description = "Paket semesteran bebas pusing untuk jangka panjang."
        ),
        ProPlan(
            id = "plan_365",
            code = "365D",
            title = "Paket 12 Bulan (1 Tahun)",
            durationLabel = "365 Hari",
            priceText = "Rp 300.000",
            days = 365L,
            badge = "⭐ Paling Hemat (~Rp 820/hari)",
            description = "Investasi terbaik tahunan untuk kelancaran bisnis bengkel."
        )
    )

    sealed class TrialStatus {
        data class ProActivated(val daysRemaining: Int?, val isLifetime: Boolean) : TrialStatus()
        data class TrialActive(val daysRemaining: Int, val isUrgentWarning: Boolean) : TrialStatus()
        data object Expired : TrialStatus()
    }

    sealed class GateResult {
        data object Allowed : GateResult()
        data class Denied(val featureName: String, val reason: String) : GateResult()
    }

    fun canAddStaff(currentCount: Int, isPro: Boolean): GateResult = GateResult.Allowed

    fun canUseBarcodeScanner(isPro: Boolean): GateResult = GateResult.Allowed

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Unique Device ID tied to this device hardware/install.
     * Clean readable format: BQ-XXXX-XXXX
     */
    fun getDeviceId(context: Context): String {
        val prefs = getPrefs(context)
        var devId = prefs.getString(KEY_DEVICE_ID, null)
        if (devId.isNullOrBlank()) {
            val androidId = try {
                android.provider.Settings.Secure.getString(
                    context.contentResolver,
                    android.provider.Settings.Secure.ANDROID_ID
                ) ?: "DEV"
            } catch (e: Exception) {
                "DEV"
            }
            val raw = kotlin.math.abs((androidId + "_BENGKELQU_DEV_SALT_2026").hashCode()).toString(16).uppercase()
            val padded = raw.padStart(8, '0').takeLast(8)
            devId = "BQ-${padded.substring(0, 4)}-${padded.substring(4, 8)}"
            prefs.edit().putString(KEY_DEVICE_ID, devId).apply()
        }
        return devId
    }

    /**
     * Generates deterministic checksum signature for device ID & duration plan code.
     */
    fun generateExpectedSignature(deviceId: String, durationCode: String): String {
        val cleanDev = deviceId.replace("-", "").replace("BQ", "").trim().uppercase()
        val seed = "DEV_BQ_SECRET_SALT_${cleanDev}_$durationCode"
        val hash = kotlin.math.abs(seed.hashCode()).toString(16).uppercase().padStart(4, '0')
        return hash.takeLast(4)
    }

    /**
     * Generates an official device-bound serial number for a specific device and plan.
     */
    fun generateSerialNumber(deviceId: String, durationCode: String): String {
        val cleanDev = deviceId.replace("-", "").replace("BQ", "").trim().uppercase()
        val sig = generateExpectedSignature(deviceId, durationCode)
        return "BQPRO-$durationCode-$cleanDev-$sig"
    }

    data class LicenseValidationResult(
        val isValid: Boolean,
        val message: String,
        val durationDays: Long = 0L,
        val isLifetime: Boolean = false
    )

    /**
     * Verifies an entered Serial Number against the current device.
     */
    fun verifyLicenseKey(key: String, deviceId: String): LicenseValidationResult {
        val trimmed = key.trim().uppercase().replace(" ", "")
        if (trimmed.isBlank()) {
            return LicenseValidationResult(false, "Serial Number tidak boleh kosong.")
        }

        val cleanDev = deviceId.replace("-", "").replace("BQ", "").trim().uppercase()
        val normalized = trimmed.replace("-", "")

        // 1. Device-bound verification for standard plans
        for (plan in PRO_PLANS) {
            val sig = generateExpectedSignature(deviceId, plan.code)
            val expectedClean = ("BQPRO" + plan.code + cleanDev + sig).uppercase()
            if (normalized == expectedClean) {
                return LicenseValidationResult(
                    isValid = true,
                    message = "Aktivasi Berhasil! Lisensi ${plan.title} (${plan.durationLabel}) resmi aktif di perangkat ini.",
                    durationDays = plan.days,
                    isLifetime = false
                )
            }
        }

        // 2. Lifetime device-bound: BQPRO-LIFE-[DEV]-[SIG]
        val lifeSig = generateExpectedSignature(deviceId, "LIFE")
        val expectedLifeClean = ("BQPRO" + "LIFE" + cleanDev + lifeSig).uppercase()
        if (normalized == expectedLifeClean) {
            return LicenseValidationResult(
                isValid = true,
                message = "Aktivasi Berhasil! Lisensi Bengkel Qu PRO SEUMUR HIDUP resmi aktif di perangkat ini.",
                durationDays = 0L,
                isLifetime = true
            )
        }

        // 3. Developer Master Keys (Direct developer activation)
        if (trimmed.startsWith("BENGKELQU-PRO") || trimmed.startsWith("BQPRO-ADMIN") || trimmed.startsWith("BQPRO-DEV")) {
            return LicenseValidationResult(
                isValid = true,
                message = "Aktivasi Berhasil! Lisensi Master Developer Bengkel Qu PRO telah aktif.",
                durationDays = 0L,
                isLifetime = true
            )
        }

        // 4. Mismatch check
        if (trimmed.contains(cleanDev)) {
            return LicenseValidationResult(false, "Kode checksum Serial Number tidak valid untuk perangkat ini.")
        }

        return LicenseValidationResult(
            false,
            "Serial Number tidak valid atau terdaftar untuk perangkat lain. Silakan hubungi Developer via WhatsApp (085714216556)."
        )
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
     * Checks if the automatic trial pop-up should be shown today.
     */
    fun shouldAutoShowTrialPopup(context: Context): Boolean {
        val prefs = getPrefs(context)
        val today = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(java.util.Date())
        val lastShown = prefs.getString(KEY_LAST_TRIAL_POPUP_DATE, "")
        return lastShown != today
    }

    /**
     * Marks the trial pop-up as shown for today.
     */
    fun markTrialPopupShown(context: Context) {
        val prefs = getPrefs(context)
        val today = java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.getDefault()).format(java.util.Date())
        prefs.edit().putString(KEY_LAST_TRIAL_POPUP_DATE, today).apply()
    }

    /**
     * Evaluates current licensing & trial status.
     */
    fun getTrialStatus(context: Context, profile: WorkshopProfile?): TrialStatus {
        if (profile?.subscriptionTier == SubscriptionTier.PRO) {
            val validUntil = profile.validUntilEpoch
            if (validUntil <= 0L) {
                return TrialStatus.ProActivated(daysRemaining = null, isLifetime = true)
            }
            val now = System.currentTimeMillis()
            val diff = validUntil - now
            if (diff > 0) {
                val days = ceil(diff.toDouble() / ONE_DAY_MS).toInt().coerceAtLeast(1)
                return TrialStatus.ProActivated(daysRemaining = days, isLifetime = false)
            } else {
                return TrialStatus.Expired
            }
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
        val status = getTrialStatus(context, profile)
        return status is TrialStatus.ProActivated || status is TrialStatus.TrialActive
    }

    fun isPro(profile: WorkshopProfile?): Boolean {
        if (profile?.subscriptionTier != SubscriptionTier.PRO) return false
        val validUntil = profile.validUntilEpoch
        if (validUntil <= 0L) return true
        return System.currentTimeMillis() <= validUntil
    }
}
