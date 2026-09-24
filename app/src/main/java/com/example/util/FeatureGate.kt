package com.example.util

import android.content.Context
import android.content.SharedPreferences
import com.example.data.local.SubscriptionTier
import com.example.data.local.WorkshopProfile
import java.security.MessageDigest
import kotlin.math.ceil

/**
 * =========================================================================
 * SKEMA & TEKNIS GENERATE SERIAL NUMBER DEVELOPER (OFFLINE SECURE)
 * =========================================================================
 * 1. Dasar: Device-Bound Cryptographic Signature.
 * 2. Parameter Input:
 *    - Device ID (contoh: "BQ-A1B2-C3D4")
 *    - Duration Code: "1D", "30D", "90D", "180D", "365D", atau "LIFE"
 *    - Developer Secret Salt: "BENGKELQU_SECRET_SALT_2026"
 * 3. Rumus Checksum Token:
 *    raw = Clean(DeviceId) + "#" + DurationCode + "#" + SecretSalt
 *    hash = SHA-256(raw)
 *    checksum = 6 karakter hex pertama dari hash (uppercase)
 * 4. Format Serial Number Resmi:
 *    BQPRO-[DURATION]-[CLEAN_DEVICE_ID]-[CHECKSUM]
 *    Contoh:
 *    - Paket 1 Hari   : BQPRO-1D-A1B2C3D4-9F8E2A
 *    - Paket 1 Bulan  : BQPRO-30D-A1B2C3D4-3B5C1D
 *    - Paket 3 Bulan  : BQPRO-90D-A1B2C3D4-A7E29F
 *    - Paket 6 Bulan  : BQPRO-180D-A1B2C3D4-8F123C
 *    - Paket 12 Bulan : BQPRO-365D-A1B2C3D4-E4567A
 *    - Lifetime       : BQPRO-LIFE-A1B2C3D4-D8812A
 * 5. Script Python untuk Developer PC:
 *    import hashlib
 *    def gen_sn(dev_id, plan):
 *        clean = dev_id.replace('-', '').replace('BQ', '').strip().upper()
 *        salt = "BENGKELQU_SECRET_SALT_2026"
 *        raw = f"{clean}#{plan}#{salt}".encode('utf-8')
 *        sig = hashlib.sha256(raw).hexdigest()[:6].upper()
 *        return f"BQPRO-{plan}-{clean}-{sig}"
 * =========================================================================
 */
object FeatureGate {

    private const val PREFS_NAME = "bengkel_qu_license_prefs"
    private const val KEY_TRIAL_START = "trial_start_epoch"
    private const val KEY_DEVICE_ID = "bengkel_device_id"
    private const val KEY_LAST_TRIAL_POPUP_DATE = "last_trial_popup_date"
    const val TRIAL_DURATION_DAYS = 10L
    const val ONE_DAY_MS = 24L * 60L * 60L * 1000L
    private const val TOTAL_TRIAL_MS = TRIAL_DURATION_DAYS * ONE_DAY_MS

    // Developer Contact - Official Email Only (Nomor WA disembunyikan/dihapus)
    const val DEVELOPER_EMAIL = "bageurhendri@gmail.com"
    private const val DEVELOPER_SECRET_SALT = "BENGKELQU_SECRET_SALT_2026"

    data class ProPlan(
        val id: String,
        val code: String, // 1D, 30D, 90D, 180D, 365D, LIFE
        val title: String,
        val durationLabel: String,
        val priceText: String,
        val days: Long,
        val badge: String,
        val description: String
    )

    val PRO_PLANS = listOf(
        ProPlan(
            id = "plan_1",
            code = "1D",
            title = "Paket 1 Hari",
            durationLabel = "1 Hari",
            priceText = "Rp 1.000",
            days = 1L,
            badge = "Rp 1.000/hari",
            description = "Akses penuh fitur PRO selama 1 hari kalender."
        ),
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
            priceText = "Rp 80.000",
            days = 90L,
            badge = "Hemat Rp 10.000",
            description = "Pilihan ekonomis kuartalan untuk operasional bengkel."
        ),
        ProPlan(
            id = "plan_180",
            code = "180D",
            title = "Paket 6 Bulan",
            durationLabel = "180 Hari",
            priceText = "Rp 150.000",
            days = 180L,
            badge = "Hemat Rp 30.000",
            description = "Paket semesteran bebas pusing untuk jangka panjang."
        ),
        ProPlan(
            id = "plan_365",
            code = "365D",
            title = "Paket 12 Bulan (1 Tahun)",
            durationLabel = "365 Hari",
            priceText = "Rp 275.000",
            days = 365L,
            badge = "⭐ Paling Hemat (~Rp 750/hari)",
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

    // Prinsip Non-Blocking: Fitur dasar tetap 100% aktif dan tanpa batasan
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
            val raw = kotlin.math.abs((androidId + "_BENGKELQU_SALT").hashCode()).toString(16).uppercase()
            val padded = raw.padStart(8, '0').takeLast(8)
            devId = "BQ-${padded.substring(0, 4)}-${padded.substring(4, 8)}"
            prefs.edit().putString(KEY_DEVICE_ID, devId).apply()
        }
        return devId
    }

    /**
     * Generates deterministic checksum signature for device ID & duration plan code.
     * Uses SHA-256 and takes first 6 uppercase hex characters.
     */
    fun generateExpectedSignature(deviceId: String, durationCode: String): String {
        val cleanDev = deviceId.replace("-", "").replace("BQ", "").trim().uppercase()
        val rawInput = "$cleanDev#$durationCode#$DEVELOPER_SECRET_SALT"
        val bytes = MessageDigest.getInstance("SHA-256").digest(rawInput.toByteArray(Charsets.UTF_8))
        return bytes.take(3).joinToString("") { "%02X".format(it) }
    }

    /**
     * Generates an official device-bound serial number for a specific device and plan.
     * Format: BQPRO-[DURATION]-[CLEAN_DEV_ID]-[CHECKSUM]
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

        // 1. Device-bound verification for standard plans (1D, 30D, 90D, 180D, 365D)
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
            "Serial Number tidak valid untuk perangkat ini. Silakan ajukan aktivasi resmi ke Developer via Email ($DEVELOPER_EMAIL)."
        )
    }

    /**
     * Initializes or gets the trial start epoch.
     * Trial 10 hari langsung aktif sejak aplikasi diinstall.
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
     * 1. PRO License in database active? -> ProActivated
     * 2. Within 10 days of first install? -> TrialActive (with remaining days)
     * 3. Else -> Expired (Fitur dasar tetap 100% aktif tanpa batasan)
     */
    fun getTrialStatus(context: Context, profile: WorkshopProfile?): TrialStatus {
        val now = System.currentTimeMillis()
        if (profile?.subscriptionTier == SubscriptionTier.PRO) {
            val isLifetime = profile.validUntilEpoch == 0L
            val days = if (isLifetime) null else {
                val diff = profile.validUntilEpoch - now
                if (diff > 0) (diff / ONE_DAY_MS).toInt() + 1 else 0
            }
            if (isLifetime || (days != null && days > 0)) {
                return TrialStatus.ProActivated(daysRemaining = days, isLifetime = isLifetime)
            }
        }

        // Automatic 10-day trial starting on install date
        val trialStart = getTrialStartEpoch(context)
        val elapsedMs = (now - trialStart).coerceAtLeast(0L)
        val elapsedDays = elapsedMs / ONE_DAY_MS
        val remainingDays = (TRIAL_DURATION_DAYS - elapsedDays).coerceAtLeast(0L).toInt()

        return if (remainingDays > 0) {
            TrialStatus.TrialActive(
                daysRemaining = remainingDays,
                isUrgentWarning = remainingDays <= 3
            )
        } else {
            TrialStatus.Expired
        }
    }

    /**
     * Returns true if user has PRO access (either active Trial or active PRO License).
     */
    fun hasProAccess(context: Context, profile: WorkshopProfile?): Boolean {
        val status = getTrialStatus(context, profile)
        return status is TrialStatus.ProActivated || status is TrialStatus.TrialActive
    }

    fun isPro(context: Context, profile: WorkshopProfile?): Boolean {
        return hasProAccess(context, profile)
    }
}

