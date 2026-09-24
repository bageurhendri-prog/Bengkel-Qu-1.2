package com.example.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.local.AttendanceRecord
import com.example.data.local.CustomerService
import com.example.data.local.StockItem
import java.io.File
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReportExporter {

    private val rupiahFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
        maximumFractionDigits = 0
    }

    private fun formatRupiah(amount: Long): String {
        return rupiahFormat.format(amount)
    }

    private fun formatDate(epoch: Long): String {
        return SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID")).format(Date(epoch))
    }

    /**
     * Creates a temporary file in cache and gets its FileProvider content URI.
     */
    fun createExportFile(context: Context, fileName: String, content: String): Uri? {
        return try {
            val exportDir = File(context.cacheDir, "exports")
            if (!exportDir.exists()) {
                exportDir.mkdirs()
            }
            val file = File(exportDir, fileName)
            FileOutputStream(file).use { out ->
                // UTF-8 BOM for Excel compatibility with Indonesian/special characters
                out.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                out.write(content.toByteArray(Charsets.UTF_8))
            }
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Shares export file and/or text summary via WhatsApp or standard chooser.
     */
    fun shareReport(
        context: Context,
        title: String,
        fileName: String,
        csvContent: String,
        waSummaryText: String,
        sendViaWhatsApp: Boolean = false
    ) {
        try {
            val fileUri = createExportFile(context, fileName, csvContent)
            val intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, waSummaryText)
                if (fileUri != null) {
                    putExtra(Intent.EXTRA_STREAM, fileUri)
                    type = "text/csv"
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } else {
                    type = "text/plain"
                }
                if (sendViaWhatsApp) {
                    setPackage("com.whatsapp")
                }
            }
            context.startActivity(Intent.createChooser(intent, "Bagikan $title"))
        } catch (e: Exception) {
            // If WhatsApp package is not installed or error occurs, fallback to general share
            try {
                val fallbackIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_SUBJECT, title)
                    putExtra(Intent.EXTRA_TEXT, waSummaryText)
                    type = "text/plain"
                }
                context.startActivity(Intent.createChooser(fallbackIntent, "Bagikan $title"))
            } catch (err: Exception) {
                Toast.makeText(context, "Gagal membagikan laporan: ${err.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Builds Omset Report Excel CSV and WhatsApp text.
     */
    fun buildOmsetReport(
        bengkelName: String,
        filterLabel: String,
        services: List<CustomerService>
    ): Pair<String, String> {
        val totalRevenue = services.sumOf { it.totalAmount }
        val totalUnits = services.size

        var totalJasa = 0L
        var totalPart = 0L
        services.forEach { s ->
            s.items.forEach { item ->
                val cost = item.price * item.qty
                if (item.isPart) totalPart += cost else totalJasa += cost
            }
        }

        // CSV String
        val csv = StringBuilder()
        csv.appendLine("\"LAPORAN OMSET BENGKEL - $bengkelName\"")
        csv.appendLine("\"Periode:\",\"$filterLabel\"")
        csv.appendLine("\"Total Omset:\",\"${formatRupiah(totalRevenue)}\"")
        csv.appendLine("\"Total Unit Kendaraan:\",\"$totalUnits Unit\"")
        csv.appendLine("\"Total Pendapatan Jasa:\",\"${formatRupiah(totalJasa)}\"")
        csv.appendLine("\"Total Penjualan Sparepart:\",\"${formatRupiah(totalPart)}\"")
        csv.appendLine("")
        csv.appendLine("\"No\",\"Tanggal\",\"No Antrian\",\"Pelanggan\",\"No Telepon\",\"Plat Nomor\",\"Mekanik\",\"Rincian Item Jasa & Part\",\"Total Biaya (Rp)\"")

        services.forEachIndexed { idx, s ->
            val itemsStr = s.items.joinToString("; ") { "${it.name} (${it.qty}x)" }
            val tgl = formatDate(s.dateEpoch)
            csv.appendLine("\"${idx + 1}\",\"$tgl\",\"#${s.queueNumber}\",\"${s.customerName}\",\"${s.phoneNumber}\",\"${s.plateNumber}\",\"${s.mechanicName}\",\"$itemsStr\",\"${s.totalAmount}\"")
        }

        // WhatsApp Summary
        val wa = StringBuilder()
        wa.appendLine("📊 *LAPORAN OMSET - $bengkelName*")
        wa.appendLine("📅 Periode: $filterLabel")
        wa.appendLine("━━━━━━━━━━━━━━━━━━━")
        wa.appendLine("💰 *Total Omset:* ${formatRupiah(totalRevenue)}")
        wa.appendLine("🏍️ *Total Unit:* $totalUnits Kendaraan")
        wa.appendLine("🔧 *Omset Jasa:* ${formatRupiah(totalJasa)}")
        wa.appendLine("📦 *Omset Sparepart:* ${formatRupiah(totalPart)}")
        wa.appendLine("━━━━━━━━━━━━━━━━━━━")
        wa.appendLine("*Ringkasan Transaksi:*")
        services.take(15).forEachIndexed { index, item ->
            wa.appendLine("${index + 1}. #${item.queueNumber} ${item.customerName} (${item.plateNumber}) - ${formatRupiah(item.totalAmount)}")
        }
        if (services.size > 15) {
            wa.appendLine("... dan ${services.size - 15} transaksi lainnya terlampir di file Excel.")
        }
        wa.appendLine("\n_Laporan diekspor otomatis dari Aplikasi Bengkel Qu_")

        return Pair(csv.toString(), wa.toString())
    }

    /**
     * Builds Stock Report Excel CSV and WhatsApp text.
     */
    fun buildStockReport(
        bengkelName: String,
        filterLabel: String,
        stocks: List<StockItem>
    ): Pair<String, String> {
        val totalItems = stocks.size
        val totalQty = stocks.sumOf { it.qty }
        val totalModalValue = stocks.sumOf { it.modalPrice * it.qty }
        val totalSellValue = stocks.sumOf { it.sellPrice * it.qty }
        val potentialProfit = totalSellValue - totalModalValue

        val csv = StringBuilder()
        csv.appendLine("\"LAPORAN INVENTARIS STOK SPAREPART - $bengkelName\"")
        csv.appendLine("\"Periode/Filter:\",\"$filterLabel\"")
        csv.appendLine("\"Total Jenis Sparepart:\",\"$totalItems Item\"")
        csv.appendLine("\"Total Stok Fisik:\",\"$totalQty Pcs\"")
        csv.appendLine("\"Total Nilai Modal:\",\"${formatRupiah(totalModalValue)}\"")
        csv.appendLine("\"Total Estimasi Nilai Jual:\",\"${formatRupiah(totalSellValue)}\"")
        csv.appendLine("")
        csv.appendLine("\"No\",\"Barcode / SKU\",\"Nama Sparepart\",\"Merk\",\"Kualitas\",\"Status\",\"Harga Modal (Rp)\",\"Harga Jual (Rp)\",\"Stok (Pcs)\",\"Total Nilai Modal (Rp)\",\"Total Nilai Jual (Rp)\"")

        stocks.forEachIndexed { idx, item ->
            val totalModalItem = item.modalPrice * item.qty
            val totalJualItem = item.sellPrice * item.qty
            csv.appendLine("\"${idx + 1}\",\"${item.barcode}\",\"${item.name}\",\"${item.brand}\",\"${item.quality}\",\"${item.status.name}\",\"${item.modalPrice}\",\"${item.sellPrice}\",\"${item.qty}\",\"$totalModalItem\",\"$totalJualItem\"")
        }

        val wa = StringBuilder()
        wa.appendLine("📦 *LAPORAN STOK SPAREPART - $bengkelName*")
        wa.appendLine("📅 Keterangan: $filterLabel")
        wa.appendLine("━━━━━━━━━━━━━━━━━━━")
        wa.appendLine("🔢 *Total Item:* $totalItems Macam")
        wa.appendLine("📦 *Total Fisik:* $totalQty Pcs")
        wa.appendLine("💵 *Total Aset Modal:* ${formatRupiah(totalModalValue)}")
        wa.appendLine("💰 *Estimasi Nilai Jual:* ${formatRupiah(totalSellValue)}")
        wa.appendLine("📈 *Potensi Keuntungan:* ${formatRupiah(potentialProfit)}")
        wa.appendLine("━━━━━━━━━━━━━━━━━━━")
        wa.appendLine("*Daftar Stok (Ringkasan):*")
        stocks.take(15).forEachIndexed { i, item ->
            wa.appendLine("${i + 1}. ${item.name} (${item.brand}) - Sisa: ${item.qty} pcs @ ${formatRupiah(item.sellPrice)}")
        }
        if (stocks.size > 15) {
            wa.appendLine("... dan ${stocks.size - 15} item sparepart lainnya terlampir di file Excel.")
        }
        wa.appendLine("\n_Laporan diekspor otomatis dari Aplikasi Bengkel Qu_")

        return Pair(csv.toString(), wa.toString())
    }

    /**
     * Builds Customer Detail Report Excel CSV and WhatsApp text.
     */
    fun buildCustomerDetailReport(
        bengkelName: String,
        filterLabel: String,
        services: List<CustomerService>
    ): Pair<String, String> {
        val totalCust = services.size
        val totalRevenue = services.sumOf { it.totalAmount }

        val csv = StringBuilder()
        csv.appendLine("\"REKAP DATA DETAIL CUSTOMER - $bengkelName\"")
        csv.appendLine("\"Periode:\",\"$filterLabel\"")
        csv.appendLine("\"Total Customer:\",\"$totalCust Orang\"")
        csv.appendLine("\"Total Transaksi:\",\"${formatRupiah(totalRevenue)}\"")
        csv.appendLine("")
        csv.appendLine("\"No\",\"Tanggal\",\"No Antrian\",\"Nama Pelanggan\",\"No Telepon\",\"Plat Nomor\",\"Keluhan / Catatan\",\"Mekanik\",\"Rincian Jasa & Sparepart\",\"Total Biaya (Rp)\",\"Status Pembayaran\"")

        services.forEachIndexed { idx, s ->
            val itemsStr = s.items.joinToString("; ") { "${it.name} (${it.qty}x) [${if (it.isPart) "Part" else "Jasa"}]" }
            val tgl = formatDate(s.dateEpoch)
            csv.appendLine("\"${idx + 1}\",\"$tgl\",\"#${s.queueNumber}\",\"${s.customerName}\",\"${s.phoneNumber}\",\"${s.plateNumber}\",\"${s.notes}\",\"${s.mechanicName}\",\"$itemsStr\",\"${s.totalAmount}\",\"${s.status.name}\"")
        }

        val wa = StringBuilder()
        wa.appendLine("📋 *REKAP DATA DETAIL CUSTOMER - $bengkelName*")
        wa.appendLine("📅 Periode: $filterLabel")
        wa.appendLine("━━━━━━━━━━━━━━━━━━━")
        wa.appendLine("👥 *Total Pelanggan:* $totalCust Orang")
        wa.appendLine("💰 *Total Transaksi:* ${formatRupiah(totalRevenue)}")
        wa.appendLine("━━━━━━━━━━━━━━━━━━━")
        services.take(10).forEachIndexed { i, s ->
            wa.appendLine("${i + 1}. *#${s.queueNumber} ${s.customerName}* (${s.plateNumber})")
            wa.appendLine("   📱 HP: ${s.phoneNumber.ifBlank { "-" }} | Mekanik: ${s.mechanicName}")
            wa.appendLine("   🔧 Keluhan: ${s.notes.ifBlank { "-" }}")
            wa.appendLine("   💵 Total: ${formatRupiah(s.totalAmount)} [${s.status.name}]")
        }
        if (services.size > 10) {
            wa.appendLine("... dan ${services.size - 10} pelanggan lainnya tercatat di file Excel terlampir.")
        }
        wa.appendLine("\n_Laporan diekspor otomatis dari Aplikasi Bengkel Qu_")

        return Pair(csv.toString(), wa.toString())
    }

    /**
     * Builds Attendance (Absen) Report Excel CSV and WhatsApp text.
     */
    fun buildAttendanceReport(
        bengkelName: String,
        monthLabel: String,
        records: List<AttendanceRecord>
    ): Pair<String, String> {
        val totalRecords = records.size
        val masukCount = records.count { it.status.name == "MASUK" }
        val izinCount = records.count { it.status.name == "IZIN" }
        val sakitCount = records.count { it.status.name == "SAKIT" }
        val alpaCount = records.count { it.status.name == "ALPA" }

        // Per staff counts
        val staffSummary = records.groupBy { it.staffName }

        val csv = StringBuilder()
        csv.appendLine("\"LAPORAN PRESENSI & ABSENSI KARYAWAN - $bengkelName\"")
        csv.appendLine("\"Bulan:\",\"$monthLabel\"")
        csv.appendLine("\"Total Presensi:\",\"$totalRecords Catatan\"")
        csv.appendLine("\"Total Hadir/Masuk:\",\"$masukCount\"")
        csv.appendLine("\"Total Izin:\",\"$izinCount\"")
        csv.appendLine("\"Total Sakit:\",\"$sakitCount\"")
        csv.appendLine("\"Total Alpa:\",\"$alpaCount\"")
        csv.appendLine("")
        csv.appendLine("\"--- REKAP PER KARYAWAN ---\"")
        csv.appendLine("\"Nama Karyawan\",\"Total Hadir\",\"Total Izin\",\"Total Sakit\",\"Total Alpa\"")
        staffSummary.forEach { (name, list) ->
            val m = list.count { it.status.name == "MASUK" }
            val i = list.count { it.status.name == "IZIN" }
            val s = list.count { it.status.name == "SAKIT" }
            val a = list.count { it.status.name == "ALPA" }
            csv.appendLine("\"$name\",\"$m\",\"$i\",\"$s\",\"$a\"")
        }
        csv.appendLine("")
        csv.appendLine("\"--- DETAIL RINCIAN ABSENSI ---\"")
        csv.appendLine("\"No\",\"Tanggal\",\"Nama Karyawan / Mekanik\",\"Status Kehadiran\",\"Jam Masuk\",\"Keterangan\"")

        records.forEachIndexed { idx, r ->
            csv.appendLine("\"${idx + 1}\",\"${r.dateString}\",\"${r.staffName}\",\"${r.status.name}\",\"${r.timeCheckIn}\",\"${r.notes}\"")
        }

        val wa = StringBuilder()
        wa.appendLine("👥 *LAPORAN PRESENSI STAFF & MEKANIK*")
        wa.appendLine("🏢 Bengkel: $bengkelName")
        wa.appendLine("📅 Bulan: $monthLabel")
        wa.appendLine("━━━━━━━━━━━━━━━━━━━")
        wa.appendLine("✅ *Masuk:* $masukCount | 📝 *Izin:* $izinCount")
        wa.appendLine("🏥 *Sakit:* $sakitCount | ❌ *Alpa:* $alpaCount")
        wa.appendLine("━━━━━━━━━━━━━━━━━━━")
        wa.appendLine("*Rekap Kehadiran Karyawan:*")
        staffSummary.forEach { (name, list) ->
            val m = list.count { it.status.name == "MASUK" }
            val i = list.count { it.status.name == "IZIN" }
            val s = list.count { it.status.name == "SAKIT" }
            val a = list.count { it.status.name == "ALPA" }
            wa.appendLine("• *${name}*: Hadir $m, Izin $i, Sakit $s, Alpa $a")
        }
        wa.appendLine("\n_Rincian tanggal dan jam masuk terlampir dalam file Excel._")
        wa.appendLine("_Laporan diekspor otomatis dari Aplikasi Bengkel Qu_")

        return Pair(csv.toString(), wa.toString())
    }

    data class LoyalCustomerData(
        val rank: Int,
        val name: String,
        val phone: String,
        val plateNumber: String,
        val totalServices: Int,
        val totalSpending: Long,
        val note: String
    )

    data class WaBlastCustomerData(
        val no: Int,
        val lastDateStr: String,
        val name: String,
        val phone: String,
        val plateNumber: String,
        val note: String,
        val totalServices: Int,
        val totalSpending: Long,
        val daysSinceLastService: Int
    )

    fun openDirectWhatsAppChat(context: Context, phoneNumber: String, message: String) {
        try {
            var cleanPhone = phoneNumber.replace(Regex("[^0-9]"), "")
            if (cleanPhone.startsWith("0")) {
                cleanPhone = "62" + cleanPhone.substring(1)
            } else if (!cleanPhone.startsWith("62")) {
                cleanPhone = "62$cleanPhone"
            }
            val uri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(message)}")
            val intent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Tidak dapat membuka WhatsApp", Toast.LENGTH_SHORT).show()
        }
    }

    fun buildLoyalCustomerReport(
        bengkelName: String,
        list: List<LoyalCustomerData>
    ): Pair<String, String> {
        val csv = StringBuilder()
        csv.appendLine("\"No\",\"Nama\",\"No WA\",\"No Plat\",\"Total Service\",\"Total Biaya\",\"Ket\"")
        list.take(50).forEach { item ->
            csv.appendLine("\"${item.rank}\",\"${item.name}\",\"${item.phone}\",\"${item.plateNumber}\",\"${item.totalServices}x\",\"${formatRupiah(item.totalSpending)}\",\"${item.note}\"")
        }

        val wa = StringBuilder()
        wa.appendLine("⭐ *RANKING LOYAL CUSTOMER - $bengkelName*")
        wa.appendLine("📊 Total Pelanggan: ${list.size}")
        wa.appendLine("━━━━━━━━━━━━━━━━━━━")
        list.take(20).forEach { item ->
            wa.appendLine("${item.rank}. *${item.name}* (${item.plateNumber})")
            wa.appendLine("   • Servis: ${item.totalServices}x | Biaya: ${formatRupiah(item.totalSpending)}")
            wa.appendLine("   • WA: ${item.phone} | Status: ${item.note}")
        }
        wa.appendLine("━━━━━━━━━━━━━━━━━━━")
        wa.appendLine("_File rekap Excel lengkap (No 1-50) terlampir._")
        wa.appendLine("_Aplikasi Bengkel Qu Marketing Engine_")

        return Pair(csv.toString(), wa.toString())
    }

    fun buildWaBlastReport(
        bengkelName: String,
        list: List<WaBlastCustomerData>
    ): Pair<String, String> {
        val csv = StringBuilder()
        csv.appendLine("\"No\",\"Tgl\",\"Nama\",\"No WA\",\"Plat\",\"Ket\",\"Total Service\",\"Total Biaya\"")
        list.forEach { item ->
            csv.appendLine("\"${item.no}\",\"${item.lastDateStr}\",\"${item.name}\",\"${item.phone}\",\"${item.plateNumber}\",\"${item.note}\",\"${item.totalServices}x\",\"${formatRupiah(item.totalSpending)}\"")
        }

        val wa = StringBuilder()
        wa.appendLine("📢 *LAPORAN WA BLAST PELANGGAN (>1 BULAN) - $bengkelName*")
        wa.appendLine("👥 Total Pelanggan Perlu Servis: ${list.size} orang")
        wa.appendLine("━━━━━━━━━━━━━━━━━━━")
        list.take(20).forEach { item ->
            wa.appendLine("${item.no}. *${item.name}* - ${item.plateNumber}")
            wa.appendLine("   • Tgl Terakhir: ${item.lastDateStr} (${item.note})")
            wa.appendLine("   • No WA: ${item.phone}")
            wa.appendLine("   • Total Riwayat: ${item.totalServices}x (${formatRupiah(item.totalSpending)})")
        }
        if (list.size > 20) {
            wa.appendLine("...dan ${list.size - 20} pelanggan lainnya.")
        }
        wa.appendLine("━━━━━━━━━━━━━━━━━━━")
        wa.appendLine("_File Excel data lengkap terlampir._")
        wa.appendLine("_Aplikasi Bengkel Qu Marketing Engine_")

        return Pair(csv.toString(), wa.toString())
    }
}
