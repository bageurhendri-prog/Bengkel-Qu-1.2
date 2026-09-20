package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ApprovalStatus
import com.example.data.local.AttendanceRecord
import com.example.data.local.AttendanceStatus
import com.example.data.local.BengkelDatabase
import com.example.data.local.CashDeposit
import com.example.data.local.CustomerService
import com.example.data.local.ExpenseItem
import com.example.data.local.IncomingStock
import com.example.data.local.RejectItem
import com.example.data.local.ServiceItemDetail
import com.example.data.local.ServiceStatus
import com.example.data.local.StaffMember
import com.example.data.local.StockItem
import com.example.data.local.StockStatus
import com.example.data.local.SubscriptionTier
import com.example.data.local.WorkshopProfile
import com.example.data.repository.BengkelRepository
import com.example.ui.theme.AppColorTheme
import com.example.util.FeatureGate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class BengkelScreen {
    LOGIN,
    DASHBOARD,
    SERVICE_QUEUE,
    SERVICE_DETAIL,
    SETORAN,
    PENGELUARAN,
    STOK,
    ABSEN,
    OMSET,
    REPORT,
    MARKETING,
    PENGATURAN
}

enum class FontSizePreference(val scale: Float, val label: String) {
    NORMAL(1.0f, "Normal"),
    BESAR(1.15f, "Besar"),
    EKSTRA_BESAR(1.3f, "Ekstra Besar")
}

class BengkelViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: BengkelRepository

    init {
        val database = BengkelDatabase.getDatabase(application, viewModelScope)
        repository = BengkelRepository(database.bengkelDao())
    }

    // Navigation & UI Configuration State
    private val _currentScreen = MutableStateFlow(BengkelScreen.LOGIN)
    val currentScreen: StateFlow<BengkelScreen> = _currentScreen.asStateFlow()

    private val _selectedTheme = MutableStateFlow(AppColorTheme.BENGKEL_GREEN)
    val selectedTheme: StateFlow<AppColorTheme> = _selectedTheme.asStateFlow()
    val activeTheme: StateFlow<AppColorTheme> = _selectedTheme.asStateFlow()

    private val _fontSizePreference = MutableStateFlow(FontSizePreference.NORMAL)
    val fontSizePreference: StateFlow<FontSizePreference> = _fontSizePreference.asStateFlow()

    private val _fontSizeScale = MutableStateFlow(1.0f)
    val fontSizeScale: StateFlow<Float> = _fontSizeScale.asStateFlow()

    fun setColorTheme(theme: AppColorTheme) {
        _selectedTheme.value = theme
    }

    fun setFontSizeScale(scale: Float) {
        _fontSizeScale.value = scale
        val pref = when {
            scale > 1.2f -> FontSizePreference.EKSTRA_BESAR
            scale > 1.05f -> FontSizePreference.BESAR
            else -> FontSizePreference.NORMAL
        }
        _fontSizePreference.value = pref
    }

    // Authentication State
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _activeUserRole = MutableStateFlow("ADMIN")
    val activeUserRole: StateFlow<String> = _activeUserRole.asStateFlow()

    // Selected Customer Service for detail viewing / billing
    private val _selectedCustomerService = MutableStateFlow<CustomerService?>(null)
    val selectedCustomerService: StateFlow<CustomerService?> = _selectedCustomerService.asStateFlow()

    // Search query for Stock
    private val _stockSearchQuery = MutableStateFlow("")
    val stockSearchQuery: StateFlow<String> = _stockSearchQuery.asStateFlow()

    // Reactive Data Flows
    val activeServices: StateFlow<List<CustomerService>> = repository.activeServices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedServices: StateFlow<List<CustomerService>> = repository.completedServices
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allStockItems: StateFlow<List<StockItem>> = repository.allStockItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val incomingStocks: StateFlow<List<IncomingStock>> = repository.incomingStocks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val rejectItems: StateFlow<List<RejectItem>> = repository.rejectItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenseItems: StateFlow<List<ExpenseItem>> = repository.expenseItems
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val attendanceList: StateFlow<List<AttendanceRecord>> = repository.getAttendanceByDate(todayStr)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allAttendanceRecords: StateFlow<List<AttendanceRecord>> = repository.allAttendanceRecords
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cashDeposits: StateFlow<List<CashDeposit>> = repository.cashDeposits
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val workshopProfile: StateFlow<WorkshopProfile?> = repository.workshopProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isProUser: StateFlow<Boolean> = repository.workshopProfile
        .map { profile -> FeatureGate.isPro(profile) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val staffMembers: StateFlow<List<StaffMember>> = repository.staffMembers
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Navigation Functions ---
    fun navigateTo(screen: BengkelScreen) {
        _currentScreen.value = screen
    }

    fun selectCustomerService(service: CustomerService) {
        _selectedCustomerService.value = service
        _currentScreen.value = BengkelScreen.SERVICE_DETAIL
    }

    // --- Authentication ---
    fun login(username: String, role: String = "ADMIN") {
        _isLoggedIn.value = true
        _activeUserRole.value = role
        _currentScreen.value = BengkelScreen.DASHBOARD
    }

    fun logout() {
        _isLoggedIn.value = false
        _currentScreen.value = BengkelScreen.LOGIN
    }

    // --- Theme & Font Settings ---
    fun setTheme(theme: AppColorTheme) {
        _selectedTheme.value = theme
    }

    fun setFontSize(size: FontSizePreference) {
        _fontSizePreference.value = size
    }

    // --- Customer Service & Queue Operations ---
    fun addNewCustomer(name: String, phone: String, plate: String, notes: String, context: Context, sendWhatsApp: Boolean = false) {
        viewModelScope.launch {
            val nextQueue = repository.getNextQueueNumber()
            val newService = CustomerService(
                queueNumber = nextQueue,
                customerName = name.ifBlank { "Customer #$nextQueue" },
                phoneNumber = phone,
                plateNumber = plate.uppercase(),
                notes = notes,
                mechanicName = "DAY",
                status = ServiceStatus.ANTRIAN,
                items = listOf(
                    ServiceItemDetail(
                        id = "svc_${System.currentTimeMillis()}",
                        name = "service ringan",
                        price = 35000L,
                        isPart = false
                    )
                ),
                totalAmount = 35000L
            )
            repository.insertCustomerService(newService)
            Toast.makeText(context, "Antrian #$nextQueue untuk $name berhasil dibuat", Toast.LENGTH_SHORT).show()

            if (sendWhatsApp && phone.isNotBlank()) {
                sendQueueNotificationWhatsApp(context, newService)
            }
        }
    }

    fun addServiceItemToCustomer(itemId: String, name: String, price: Long, isPart: Boolean) {
        val current = _selectedCustomerService.value ?: return
        val updatedItems = current.items + ServiceItemDetail(
            id = itemId,
            name = name,
            price = price,
            qty = 1,
            isPart = isPart
        )
        val gross = updatedItems.sumOf { it.price * it.qty }
        val net = (gross - current.discount).coerceAtLeast(0L)
        val updated = current.copy(items = updatedItems, totalAmount = net)
        _selectedCustomerService.value = updated
        viewModelScope.launch {
            repository.updateCustomerService(updated)
        }
    }

    fun removeServiceItemFromCustomer(index: Int) {
        val current = _selectedCustomerService.value ?: return
        if (index in current.items.indices) {
            val updatedItems = current.items.toMutableList().also { it.removeAt(index) }
            val gross = updatedItems.sumOf { it.price * it.qty }
            val net = (gross - current.discount).coerceAtLeast(0L)
            val updated = current.copy(items = updatedItems, totalAmount = net)
            _selectedCustomerService.value = updated
            viewModelScope.launch {
                repository.updateCustomerService(updated)
            }
        }
    }

    fun updateMechanicAndDiscount(mechanic: String, discount: Long, notes: String) {
        val current = _selectedCustomerService.value ?: return
        val gross = current.items.sumOf { it.price * it.qty }
        val net = (gross - discount).coerceAtLeast(0L)
        val updated = current.copy(
            mechanicName = mechanic,
            discount = discount,
            notes = notes,
            totalAmount = net
        )
        _selectedCustomerService.value = updated
        viewModelScope.launch {
            repository.updateCustomerService(updated)
        }
    }

    fun payAndCompleteService(context: Context) {
        val current = _selectedCustomerService.value ?: return
        val updated = current.copy(
            status = ServiceStatus.DIBAYAR,
            dateEpoch = System.currentTimeMillis()
        )
        _selectedCustomerService.value = updated
        viewModelScope.launch {
            repository.updateCustomerService(updated)
            Toast.makeText(context, "Pembayaran berhasil dicatat untuk antrian #${updated.queueNumber}", Toast.LENGTH_SHORT).show()
            _currentScreen.value = BengkelScreen.SERVICE_QUEUE
        }
    }

    // --- WhatsApp & Invoicing ---
    fun sendBillViaWhatsApp(context: Context, service: CustomerService) {
        val profile = workshopProfile.value
        val workshopName = profile?.workshopName ?: "BENGKEL QU"
        val phoneBengkel = profile?.phone ?: ""

        val rupiah = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        val sb = StringBuilder()
        sb.appendLine("*NOTA BENGKEL - $workshopName*")
        sb.appendLine("--------------------------------")
        sb.appendLine("No. Antrian : #${service.queueNumber}")
        sb.appendLine("Nama        : ${service.customerName}")
        sb.appendLine("Plat No     : ${service.plateNumber}")
        sb.appendLine("Mekanik     : ${service.mechanicName}")
        sb.appendLine("Keterangan  : ${service.notes}")
        sb.appendLine("--------------------------------")
        sb.appendLine("*RINCIAN BELANJA & JASA:*")

        service.items.forEach { item ->
            val type = if (item.isPart) "[Part]" else "[Jasa]"
            sb.appendLine("- $type ${item.name}: ${rupiah.format(item.price * item.qty)}")
        }

        if (service.discount > 0) {
            sb.appendLine("Diskon: -${rupiah.format(service.discount)}")
        }
        sb.appendLine("--------------------------------")
        sb.appendLine("*TOTAL TAGIHAN: ${rupiah.format(service.totalAmount)}*")
        sb.appendLine("--------------------------------")
        sb.appendLine("Terima kasih atas kunjungan Anda di $workshopName!")
        if (phoneBengkel.isNotBlank()) sb.appendLine("Hubungi kami: $phoneBengkel")

        val message = sb.toString()
        val formattedPhone = formatPhoneNumber(service.phoneNumber)

        try {
            val intent = Intent(Intent.ACTION_VIEW)
            val uri = if (formattedPhone.isNotEmpty()) {
                Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(message)}")
            } else {
                Uri.parse("https://api.whatsapp.com/send?text=${Uri.encode(message)}")
            }
            intent.data = uri
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to general share
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, message)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Kirim Tagihan via WhatsApp"))
        }
    }

    private fun sendQueueNotificationWhatsApp(context: Context, service: CustomerService) {
        val profile = workshopProfile.value
        val workshopName = profile?.workshopName ?: "BENGKEL QU"
        val message = """
            *TIKET ANTRIAN $workshopName*
            --------------------------------
            No. Antrian : *#${service.queueNumber}*
            Nama        : ${service.customerName}
            Plat Motor  : ${service.plateNumber}
            Keluhan     : ${service.notes}
            --------------------------------
            Sepeda motor Anda sedang dalam antrian servis kami. Kami akan memberitahu saat pengerjaan selesai.
        """.trimIndent()

        val formattedPhone = formatPhoneNumber(service.phoneNumber)
        try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(message)}")
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Membuka WhatsApp...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatPhoneNumber(phone: String): String {
        val cleaned = phone.replace("[^0-9]".toRegex(), "")
        return when {
            cleaned.startsWith("08") -> "628" + cleaned.substring(2)
            cleaned.startsWith("62") -> cleaned
            cleaned.startsWith("+62") -> cleaned.substring(1)
            else -> cleaned
        }
    }

    // --- Stock Operations ---
    fun updateStockSearch(query: String) {
        _stockSearchQuery.value = query
    }

    fun addNewStockItem(
        name: String,
        brand: String,
        quality: String,
        status: StockStatus,
        modal: Long,
        jual: Long,
        qty: Int,
        barcode: String = "",
        context: Context
    ) {
        viewModelScope.launch {
            repository.insertStockItem(
                StockItem(
                    name = name.uppercase(),
                    brand = brand,
                    quality = quality,
                    status = status,
                    modalPrice = modal,
                    sellPrice = jual,
                    qty = qty,
                    barcode = barcode
                )
            )
            Toast.makeText(context, "Item $name berhasil ditambahkan ke Stok", Toast.LENGTH_SHORT).show()
        }
    }

    fun addIncomingStock(itemName: String, qty: Int, context: Context) {
        viewModelScope.launch {
            repository.insertIncomingStock(
                IncomingStock(
                    itemName = itemName.uppercase(),
                    qty = qty,
                    status = ApprovalStatus.PENDING
                )
            )
            Toast.makeText(context, "Penerimaan $itemName ($qty unit) dicatat", Toast.LENGTH_SHORT).show()
        }
    }

    fun addRejectItem(itemName: String, reason: String, qty: Int, context: Context) {
        viewModelScope.launch {
            repository.insertRejectItem(
                RejectItem(
                    itemName = itemName.uppercase(),
                    reason = reason.uppercase(),
                    qty = qty,
                    status = ApprovalStatus.PENDING
                )
            )
            Toast.makeText(context, "Pengajuan reject $itemName diajukan", Toast.LENGTH_SHORT).show()
        }
    }

    // --- Attendance Operations ---
    fun updateStaffAttendance(id: Long, newStatus: AttendanceStatus) {
        viewModelScope.launch {
            repository.updateAttendanceStatus(id, newStatus)
        }
    }

    fun addStaffAttendanceRecord(name: String, status: AttendanceStatus) {
        viewModelScope.launch {
            repository.insertOrUpdateAttendance(
                AttendanceRecord(
                    staffName = name.uppercase(),
                    dateString = todayStr,
                    status = status,
                    timeCheckIn = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
                )
            )
        }
    }

    // --- Setoran Kasir Operations ---
    fun submitCashDeposit(
        c100k: Int,
        c50k: Int,
        c20k: Int,
        c10k: Int,
        c5k: Int,
        c2k: Int,
        c1k: Int,
        c500: Int,
        context: Context
    ) {
        val total = (c100k * 100000L) +
                (c50k * 50000L) +
                (c20k * 20000L) +
                (c10k * 10000L) +
                (c5k * 5000L) +
                (c2k * 2000L) +
                (c1k * 1000L) +
                (c500 * 500L)

        viewModelScope.launch {
            repository.insertCashDeposit(
                CashDeposit(
                    count100k = c100k,
                    count50k = c50k,
                    count20k = c20k,
                    count10k = c10k,
                    count5k = c5k,
                    count2k = c2k,
                    count1k = c1k,
                    count500 = c500,
                    totalAmount = total
                )
            )
            Toast.makeText(context, "Setoran senilai Rp $total berhasil disimpan!", Toast.LENGTH_LONG).show()
            _currentScreen.value = BengkelScreen.DASHBOARD
        }
    }

    // --- Pengeluaran / Expense Operations ---
    fun addExpense(name: String, amount: Long, context: Context) {
        viewModelScope.launch {
            repository.insertExpenseItem(
                ExpenseItem(
                    name = name.uppercase(),
                    amount = amount,
                    status = ApprovalStatus.PENDING
                )
            )
            Toast.makeText(context, "Pengeluaran $name berhasil diajukan", Toast.LENGTH_SHORT).show()
        }
    }

    // --- Report Approvals ---
    fun approveRejectItem(id: Long, isApproved: Boolean) {
        viewModelScope.launch {
            repository.updateRejectItemStatus(id, if (isApproved) ApprovalStatus.DISETUJUI else ApprovalStatus.KOREKSI)
        }
    }

    fun approveIncomingStock(id: Long, isApproved: Boolean) {
        viewModelScope.launch {
            repository.updateIncomingStockStatus(id, if (isApproved) ApprovalStatus.DISETUJUI else ApprovalStatus.KOREKSI)
        }
    }

    fun approveExpenseItem(id: Long, isApproved: Boolean) {
        viewModelScope.launch {
            repository.updateExpenseItemStatus(id, if (isApproved) ApprovalStatus.DISETUJUI else ApprovalStatus.KOREKSI)
        }
    }

    // --- Export / Save Excel & Send WA Excel ---
    fun exportCustomerData(context: Context, sendViaWhatsApp: Boolean = false) {
        viewModelScope.launch {
            val list = repository.completedServices
            val completed = completedServices.value + activeServices.value
            val csvBuilder = StringBuilder()
            csvBuilder.appendLine("No,Antrian,Nama,No HP,Plat Nomor,Mekanik,Total,Status")
            completed.forEachIndexed { index, item ->
                csvBuilder.appendLine("${index + 1},#${item.queueNumber},${item.customerName},${item.phoneNumber},${item.plateNumber},${item.mechanicName},${item.totalAmount},${item.status}")
            }
            val text = csvBuilder.toString()
            shareExportData(context, "Data_Customer_BengkelQu.csv", text, sendViaWhatsApp)
        }
    }

    fun exportStockData(context: Context, sendViaWhatsApp: Boolean = false) {
        viewModelScope.launch {
            val stocks = allStockItems.value
            val csvBuilder = StringBuilder()
            csvBuilder.appendLine("No,Nama Barang,Brand,Kualitas,Status,Harga Modal,Harga Jual,Stok Qty")
            stocks.forEachIndexed { index, item ->
                csvBuilder.appendLine("${index + 1},${item.name},${item.brand},${item.quality},${item.status},${item.modalPrice},${item.sellPrice},${item.qty}")
            }
            val text = csvBuilder.toString()
            shareExportData(context, "Data_Stok_BengkelQu.csv", text, sendViaWhatsApp)
        }
    }

    private fun shareExportData(context: Context, title: String, content: String, sendViaWhatsApp: Boolean) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, content)
                if (sendViaWhatsApp) {
                    setPackage("com.whatsapp")
                }
            }
            context.startActivity(Intent.createChooser(intent, "Bagikan $title"))
        } catch (e: Exception) {
            val fallback = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_TEXT, content)
            }
            context.startActivity(Intent.createChooser(fallback, "Bagikan $title"))
        }
    }

    // --- Workshop Profile & Staff Updates ---
    fun updateWorkshopProfile(name: String, owner: String, phone: String, email: String, address: String) {
        viewModelScope.launch {
            repository.updateWorkshopProfile(
                WorkshopProfile(
                    workshopName = name,
                    ownerName = owner,
                    email = email,
                    phone = phone,
                    address = address
                )
            )
        }
    }

    fun updateProfile(name: String, owner: String, email: String, phone: String, context: Context) {
        viewModelScope.launch {
            repository.updateWorkshopProfile(
                WorkshopProfile(
                    workshopName = name,
                    ownerName = owner,
                    email = email,
                    phone = phone
                )
            )
            Toast.makeText(context, "Profil bengkel berhasil diperbarui", Toast.LENGTH_SHORT).show()
        }
    }

    fun addNewStaff(name: String, role: String, phone: String) {
        viewModelScope.launch {
            val upperName = name.trim().uppercase()
            repository.insertStaffMember(
                StaffMember(
                    name = upperName,
                    role = role.trim().uppercase(),
                    phone = phone.trim()
                )
            )
            // Auto masuk absen hari ini
            repository.insertOrUpdateAttendance(
                AttendanceRecord(
                    staffName = upperName,
                    dateString = todayStr,
                    status = AttendanceStatus.MASUK,
                    timeCheckIn = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
                    notes = "Otomatis masuk dari Tambah Staff"
                )
            )
        }
    }

    fun deleteStaff(staff: StaffMember) {
        viewModelScope.launch {
            repository.deleteStaffMember(staff)
            // Auto hapus dari riwayat absen
            repository.deleteAttendanceByStaffName(staff.name)
        }
    }

    fun addStaffMember(name: String, role: String, phone: String, context: Context) {
        viewModelScope.launch {
            val upperName = name.trim().uppercase()
            repository.insertStaffMember(
                StaffMember(
                    name = upperName,
                    role = role.trim().uppercase(),
                    phone = phone.trim()
                )
            )
            // Auto masuk absen hari ini
            repository.insertOrUpdateAttendance(
                AttendanceRecord(
                    staffName = upperName,
                    dateString = todayStr,
                    status = AttendanceStatus.MASUK,
                    timeCheckIn = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date()),
                    notes = "Otomatis masuk dari Tambah Staff"
                )
            )
            Toast.makeText(context, "Staff $upperName ($role) ditambahkan & otomatis masuk absen", Toast.LENGTH_SHORT).show()
        }
    }

    fun resetDemoData(context: Context) {
        viewModelScope.launch {
            BengkelDatabase.populateInitialData(BengkelDatabase.getDatabase(context, viewModelScope).bengkelDao())
            Toast.makeText(context, "Data demo Bengkel Qu berhasil di-reset", Toast.LENGTH_SHORT).show()
        }
    }

    fun syncMasterData(context: Context) {
        viewModelScope.launch {
            repository.syncMasterData()
            Toast.makeText(context, "Sinkronisasi Master Selesai! Katalog sparepart dan data master diperbarui.", Toast.LENGTH_SHORT).show()
        }
    }

    fun resetAllDataToZero(context: Context) {
        viewModelScope.launch {
            repository.resetAllToZero()
            Toast.makeText(context, "Reset Berhasil: Transaksi, omset, stok, kasir, antrian, absen, dan pengajuan telah kembali ke 0.", Toast.LENGTH_LONG).show()
        }
    }

    // --- Export Reports Excel & WhatsApp ---
    fun exportOmsetExcel(context: Context, filterLabel: String, list: List<CustomerService>, sendViaWhatsApp: Boolean) {
        val bName = workshopProfile.value?.workshopName ?: "BENGKEL QU"
        val (csv, wa) = com.example.util.ReportExporter.buildOmsetReport(bName, filterLabel, list)
        com.example.util.ReportExporter.shareReport(
            context = context,
            title = "Laporan Omset - $filterLabel",
            fileName = "Laporan_Omset_${System.currentTimeMillis()}.csv",
            csvContent = csv,
            waSummaryText = wa,
            sendViaWhatsApp = sendViaWhatsApp
        )
    }

    fun exportStockExcel(context: Context, filterLabel: String, list: List<StockItem>, sendViaWhatsApp: Boolean) {
        val bName = workshopProfile.value?.workshopName ?: "BENGKEL QU"
        val (csv, wa) = com.example.util.ReportExporter.buildStockReport(bName, filterLabel, list)
        com.example.util.ReportExporter.shareReport(
            context = context,
            title = "Laporan Stok Sparepart",
            fileName = "Laporan_Stok_${System.currentTimeMillis()}.csv",
            csvContent = csv,
            waSummaryText = wa,
            sendViaWhatsApp = sendViaWhatsApp
        )
    }

    fun exportCustomerDetailExcel(context: Context, filterLabel: String, list: List<CustomerService>, sendViaWhatsApp: Boolean) {
        val bName = workshopProfile.value?.workshopName ?: "BENGKEL QU"
        val (csv, wa) = com.example.util.ReportExporter.buildCustomerDetailReport(bName, filterLabel, list)
        com.example.util.ReportExporter.shareReport(
            context = context,
            title = "Rekap Detail Customer - $filterLabel",
            fileName = "Detail_Customer_${System.currentTimeMillis()}.csv",
            csvContent = csv,
            waSummaryText = wa,
            sendViaWhatsApp = sendViaWhatsApp
        )
    }

    fun exportAttendanceExcel(context: Context, monthLabel: String, list: List<AttendanceRecord>, sendViaWhatsApp: Boolean) {
        val bName = workshopProfile.value?.workshopName ?: "BENGKEL QU"
        val (csv, wa) = com.example.util.ReportExporter.buildAttendanceReport(bName, monthLabel, list)
        com.example.util.ReportExporter.shareReport(
            context = context,
            title = "Laporan Absensi Karyawan - $monthLabel",
            fileName = "Laporan_Absen_${System.currentTimeMillis()}.csv",
            csvContent = csv,
            waSummaryText = wa,
            sendViaWhatsApp = sendViaWhatsApp
        )
    }

    fun exportLoyalCustomerExcel(
        context: Context,
        list: List<com.example.util.ReportExporter.LoyalCustomerData>,
        sendViaWhatsApp: Boolean
    ) {
        val bName = workshopProfile.value?.workshopName ?: "BENGKEL QU"
        val (csv, wa) = com.example.util.ReportExporter.buildLoyalCustomerReport(bName, list)
        com.example.util.ReportExporter.shareReport(
            context = context,
            title = "Ranking Loyal Customer - $bName",
            fileName = "Loyal_Customer_${System.currentTimeMillis()}.csv",
            csvContent = csv,
            waSummaryText = wa,
            sendViaWhatsApp = sendViaWhatsApp
        )
    }

    fun exportWaBlastExcel(
        context: Context,
        list: List<com.example.util.ReportExporter.WaBlastCustomerData>,
        sendViaWhatsApp: Boolean
    ) {
        val bName = workshopProfile.value?.workshopName ?: "BENGKEL QU"
        val (csv, wa) = com.example.util.ReportExporter.buildWaBlastReport(bName, list)
        com.example.util.ReportExporter.shareReport(
            context = context,
            title = "Laporan WA Blast Pelanggan 1 Bulan - $bName",
            fileName = "WA_Blast_1Bulan_${System.currentTimeMillis()}.csv",
            csvContent = csv,
            waSummaryText = wa,
            sendViaWhatsApp = sendViaWhatsApp
        )
    }

    // --- Backup, Restore & Reset Methods ---
    suspend fun getBackupJson(): String {
        return repository.createBackupJson()
    }

    fun shareBackup(context: Context) {
        viewModelScope.launch {
            try {
                val json = repository.createBackupJson()
                val dateTag = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                val sendIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_SUBJECT, "Backup Database Bengkel Qu - $dateTag")
                    putExtra(Intent.EXTRA_TEXT, json)
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(sendIntent, "Bagikan / Simpan Backup Bengkel Qu")
                shareIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(shareIntent)
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal membagikan backup: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun restoreBackup(context: Context, jsonString: String, onFinished: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            try {
                val payload = com.example.data.local.BackupRestoreManager.parseBackupJson(jsonString)
                repository.restoreBackup(payload)
                val summary = "Berhasil memulihkan ${payload.stockList.size} sparepart, ${payload.serviceList.size} antrian servis, ${payload.staffList.size} staf!"
                Toast.makeText(context, summary, Toast.LENGTH_LONG).show()
                onFinished(true, summary)
            } catch (e: Exception) {
                val err = "Gagal memulihkan: Format backup tidak valid (${e.localizedMessage})"
                Toast.makeText(context, err, Toast.LENGTH_LONG).show()
                onFinished(false, err)
            }
        }
    }

    fun resetTransactionsOnly(context: Context) {
        viewModelScope.launch {
            repository.resetTransactionsOnly()
            Toast.makeText(context, "Riwayat transaksi & antrian servis berhasil dibersihkan!", Toast.LENGTH_SHORT).show()
        }
    }

    fun resetFactoryDefaults(context: Context) {
        viewModelScope.launch {
            repository.resetToFactoryDefaults()
            Toast.makeText(context, "Database berhasil di-reset total ke data awal!", Toast.LENGTH_SHORT).show()
        }
    }

    fun findStockByBarcode(barcode: String, onResult: (StockItem?) -> Unit) {
        viewModelScope.launch {
            val item = repository.findStockByBarcode(barcode)
            onResult(item)
        }
    }

    // --- Pro License Operations ---
    fun activateProLicense(key: String, context: Context, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val currentProfile = workshopProfile.value ?: WorkshopProfile()
            val (isValid, message) = FeatureGate.verifyLicenseKey(key, currentProfile.email)
            if (isValid) {
                val updatedProfile = currentProfile.copy(
                    subscriptionTier = SubscriptionTier.PRO,
                    licenseKey = key.trim().uppercase(),
                    validUntilEpoch = 0L
                )
                repository.updateWorkshopProfile(updatedProfile)
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                onResult(true, message)
            } else {
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                onResult(false, message)
            }
        }
    }

    fun deactivateProLicense(context: Context) {
        viewModelScope.launch {
            val currentProfile = workshopProfile.value ?: WorkshopProfile()
            val updatedProfile = currentProfile.copy(
                subscriptionTier = SubscriptionTier.REGULAR,
                licenseKey = "",
                validUntilEpoch = 0L
            )
            repository.updateWorkshopProfile(updatedProfile)
            Toast.makeText(context, "Beralih ke mode Bengkel Qu REGULER", Toast.LENGTH_SHORT).show()
        }
    }
}
