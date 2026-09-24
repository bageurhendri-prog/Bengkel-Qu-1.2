package com.example.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Developer License Generator Utility for BengkelQu.
 * Used by the developer (Hendri - bageurhendri@gmail.com) to generate official,
 * device-bound Serial Numbers for clients across all plan durations.
 */
object DeveloperLicenseGenerator {

    data class GeneratedLicense(
        val planTitle: String,
        val durationLabel: String,
        val planCode: String,
        val serialNumber: String,
        val deviceId: String,
        val generatedAtFormatted: String
    )

    /**
     * Generates a single official Serial Number for a specific Device ID and Plan Code.
     * Supported codes: 1D, 30D, 90D, 180D, 365D, LIFE
     */
    fun generateLicense(deviceId: String, durationCode: String): GeneratedLicense {
        val cleanDev = deviceId.replace("-", "").replace("BQ", "").trim().uppercase()
        val sn = FeatureGate.generateSerialNumber(deviceId, durationCode)
        val plan = FeatureGate.PRO_PLANS.find { it.code.equals(durationCode, ignoreCase = true) }

        val planTitle = plan?.title ?: if (durationCode.equals("LIFE", ignoreCase = true)) "Paket Seumur Hidup (Lifetime)" else "Paket $durationCode"
        val durationLabel = plan?.durationLabel ?: if (durationCode.equals("LIFE", ignoreCase = true)) "Permanen / Seumur Hidup" else "$durationCode Hari"

        val now = SimpleDateFormat("dd MMMM yyyy, HH:mm", Locale("id", "ID")).format(Date())

        return GeneratedLicense(
            planTitle = planTitle,
            durationLabel = durationLabel,
            planCode = durationCode.uppercase(),
            serialNumber = sn,
            deviceId = deviceId.trim().uppercase(),
            generatedAtFormatted = now
        )
    }

    /**
     * Generates all standard licenses (1 Hari, 1, 3, 6, 12 Bulan, & Lifetime) for a client device ID.
     */
    fun generateAllPlansForDevice(deviceId: String): List<GeneratedLicense> {
        val codes = listOf("1D", "30D", "90D", "180D", "365D", "LIFE")
        return codes.map { generateLicense(deviceId, it) }
    }

    /**
     * Prepares an activation email reply template ready to be sent to the client.
     */
    fun formatActivationReply(workshopName: String, license: GeneratedLicense): String {
        return """
            Yth. Pemilik ${workshopName.ifBlank { "Bengkel" }},
            
            Terima kasih telah berlangganan BengkelQu PRO.
            Berikut adalah rincian lisensi resmi yang terikat pada perangkat Anda:
            
            - Paket: ${license.planTitle} (${license.durationLabel})
            - ID Perangkat: ${license.deviceId}
            - SERIAL NUMBER: ${license.serialNumber}
            - Tanggal Dibuat: ${license.generatedAtFormatted}
            
            CARA AKTIVASI:
            1. Buka aplikasi BengkelQu di perangkat Anda.
            2. Masuk ke Dashboard -> Tekan "PRO" atau Menu Pengaturan.
            3. Masukkan Serial Number di atas pada kolom "Masukkan Serial Number".
            4. Tekan "AKTIFKAN PRO".
            
            Salam hormat,
            Developer BengkelQu
            Email: ${FeatureGate.DEVELOPER_EMAIL}
        """.trimIndent()
    }
}
