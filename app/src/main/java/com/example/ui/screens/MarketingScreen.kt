package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.CustomerService
import com.example.ui.BengkelScreen
import com.example.ui.BengkelViewModel
import com.example.util.FeatureGate
import com.example.util.ReportExporter
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class MechanicPerformance(
    val rank: Int,
    val mechanicName: String,
    val totalMotor: Int,
    val totalOmset: Long,
    val totalJasa: Long,
    val estimasiKomisi: Long
)

data class WaBlastCustomerItem(
    val no: Int,
    val name: String,
    val phone: String,
    val plateNumber: String,
    val lastDateStr: String,
    val daysSinceLastService: Int,
    val lastServiceItems: String,
    val totalServices: Int,
    val totalSpending: Long,
    var isSent: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarketingScreen(viewModel: BengkelViewModel) {
    val context = LocalContext.current
    val profile by viewModel.workshopProfile.collectAsStateWithLifecycle()
    val completedServices by viewModel.completedServices.collectAsStateWithLifecycle()
    val activeServices by viewModel.activeServices.collectAsStateWithLifecycle()
    val staffMembers by viewModel.staffMembers.collectAsStateWithLifecycle()

    val allServices = remember(completedServices, activeServices) {
        completedServices + activeServices
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: 20 Loyal Customer, 1: Best Employee Mekanik, 2: WA Blast

    // Selected customer for draft correction popup
    var editingBlastCustomer by remember { mutableStateOf<WaBlastCustomerItem?>(null) }
    var showAutoBlastDialog by remember { mutableStateOf(false) }

    val rupiahFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
            maximumFractionDigits = 0
        }
    }

    // 1. Calculate 20 Loyal Customers
    val loyalCustomers = remember(allServices) {
        val grouped = allServices.groupBy { service ->
            val cleanPhone = service.phoneNumber.trim().replace(Regex("[^0-9]"), "")
            if (cleanPhone.length >= 8) cleanPhone else "${service.customerName.trim().uppercase()}_${service.plateNumber.trim().uppercase()}"
        }

        grouped.map { (_, services) ->
            val first = services.first()
            val totalServices = services.size
            val totalSpending = services.sumOf { it.totalAmount }

            ReportExporter.LoyalCustomerData(
                rank = 0,
                name = first.customerName.ifBlank { "Pelanggan" },
                phone = first.phoneNumber.ifBlank { "-" },
                plateNumber = first.plateNumber.ifBlank { "-" },
                totalServices = totalServices,
                totalSpending = totalSpending,
                note = ""
            )
        }
            .sortedWith(compareByDescending<ReportExporter.LoyalCustomerData> { it.totalServices }.thenByDescending { it.totalSpending })
            .take(20)
            .mapIndexed { index, data ->
                val rank = index + 1
                val note = when (rank) {
                    1 -> "⭐ Top #1 Loyal VIP"
                    2 -> "🥈 Top #2 Loyal Gold"
                    3 -> "🥉 Top #3 Loyal Silver"
                    in 4..10 -> "Top 10 Pelanggan Setia"
                    else -> "Top 20 Pelanggan Setia"
                }
                data.copy(rank = rank, note = note)
            }
    }

    // 2. Calculate Best Employee Performance Mekanik
    val mechanicPerformances = remember(allServices, staffMembers) {
        val mechanicList = if (staffMembers.any { it.role == "MEKANIK" }) {
            staffMembers.filter { it.role == "MEKANIK" }.map { it.name.trim().uppercase() }.distinct()
        } else {
            listOf("DAY", "AGUS", "DENI", "BUDI")
        }

        val allMechanics = (mechanicList + allServices.map { it.mechanicName.trim().uppercase() }).distinct()

        allMechanics.map { mechName ->
            val mechServices = allServices.filter { it.mechanicName.trim().equals(mechName, ignoreCase = true) }
            val count = mechServices.size
            val totalOmset = mechServices.sumOf { it.totalAmount }
            val jasaOmset = mechServices.sumOf { s ->
                s.items.filter { !it.isPart }.sumOf { it.price * it.qty }
            }
            // Estimasi komisi jasa 35%
            val komisi = (jasaOmset * 0.35).toLong()

            MechanicPerformance(
                rank = 0,
                mechanicName = mechName,
                totalMotor = count,
                totalOmset = totalOmset,
                totalJasa = jasaOmset,
                estimasiKomisi = komisi
            )
        }
            .sortedWith(compareByDescending<MechanicPerformance> { it.totalMotor }.thenByDescending { it.totalOmset })
            .mapIndexed { index, item -> item.copy(rank = index + 1) }
    }

    // 3. Calculate WA Blast Customers (> 1 Month Since Last Service)
    val thirtyDaysMs = remember { 30L * 24L * 60L * 60L * 1000L }
    val now = remember { System.currentTimeMillis() }

    val waBlastCustomers = remember(allServices, now) {
        val grouped = allServices.groupBy { service ->
            val cleanPhone = service.phoneNumber.trim().replace(Regex("[^0-9]"), "")
            if (cleanPhone.length >= 8) cleanPhone else "${service.customerName.trim().uppercase()}_${service.plateNumber.trim().uppercase()}"
        }

        grouped.mapNotNull { (_, services) ->
            val sortedByDate = services.sortedByDescending { it.dateEpoch }
            val latest = sortedByDate.first()
            val totalServices = services.size
            val totalSpending = services.sumOf { it.totalAmount }
            val lastDate = latest.dateEpoch

            val diff = now - lastDate
            if (diff >= thirtyDaysMs && lastDate > 0L) {
                val days = (diff / (24L * 60L * 60L * 1000L)).toInt()
                val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID")).format(Date(lastDate))

                val itemsSummary = if (latest.items.isNotEmpty()) {
                    latest.items.joinToString(", ") { it.name }
                } else {
                    "Servis Berkala & Ganti Oli"
                }

                WaBlastCustomerItem(
                    no = 0,
                    name = latest.customerName.ifBlank { "Pelanggan" },
                    phone = latest.phoneNumber.ifBlank { "-" },
                    plateNumber = latest.plateNumber.ifBlank { "-" },
                    lastDateStr = dateStr,
                    daysSinceLastService = days,
                    lastServiceItems = itemsSummary,
                    totalServices = totalServices,
                    totalSpending = totalSpending
                )
            } else null
        }.sortedByDescending { it.daysSinceLastService }
            .mapIndexed { index, item -> item.copy(no = index + 1) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("MARKETING & CRM (PRO)", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                        Text("20 Loyal Customer, Best Mekanik & WA Blast", fontSize = 11.sp, color = Color.White.copy(alpha = 0.85f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(BengkelScreen.DASHBOARD) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primary)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
                // Tab Selection
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("20 LOYAL", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("BEST MEKANIK", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("WA BLAST (${waBlastCustomers.size})", fontWeight = FontWeight.Bold, fontSize = 11.sp) }
                    )
                }

                when (selectedTab) {
                    0 -> LoyalCustomerView(
                        loyalList = loyalCustomers,
                        rupiahFormat = rupiahFormat,
                        onExportExcel = { sendWa ->
                            viewModel.exportLoyalCustomerExcel(context, loyalCustomers, sendWa)
                        },
                        onChatCustomer = { phone, name ->
                            val msg = "Halo Kak $name, salam dari ${profile?.workshopName ?: "Bengkel Qu"}! Terima kasih atas kepercayaannya selalu servis di bengkel kami."
                            ReportExporter.openDirectWhatsAppChat(context, phone, msg)
                        }
                    )
                    1 -> BestMechanicView(
                        performances = mechanicPerformances,
                        rupiahFormat = rupiahFormat
                    )
                    2 -> WaBlastView(
                        blastList = waBlastCustomers,
                        bengkelName = profile?.workshopName ?: "Bengkel Qu",
                        rupiahFormat = rupiahFormat,
                        onSelectForCorrection = { item -> editingBlastCustomer = item },
                        onOpenAutoBlast = { showAutoBlastDialog = true },
                        onExportExcel = { sendWa ->
                            viewModel.exportWaBlastExcel(
                                context,
                                waBlastCustomers.map {
                                    ReportExporter.WaBlastCustomerData(
                                        no = it.no,
                                        lastDateStr = it.lastDateStr,
                                        name = it.name,
                                        phone = it.phone,
                                        plateNumber = it.plateNumber,
                                        note = "Lewat ${it.daysSinceLastService} hari",
                                        totalServices = it.totalServices,
                                        totalSpending = it.totalSpending,
                                        daysSinceLastService = it.daysSinceLastService
                                    )
                                },
                                sendWa
                            )
                        }
                    )
                }
            }
        }

    // POP-UP DRAFT KOREKSI WA BERDASARKAN SERVIS TERAKHIR
    if (editingBlastCustomer != null) {
        val target = editingBlastCustomer!!
        val defaultMsg = remember(target) {
            "Halo Kak ${target.name}, salam dari ${profile?.workshopName ?: "Bengkel Qu"}!\n\nKendaraan Anda (${target.plateNumber}) terakhir servis pada tanggal ${target.lastDateStr} dengan pengerjaan: ${target.lastServiceItems}.\n\nSudah ${target.daysSinceLastService} hari sejak servis terakhir, kami sarankan untuk melakukan servis berkala dan ganti oli rutin agar tarikan mesin tetap bertenaga & awet. Silakan kunjungi bengkel kami atau balas pesan ini untuk reservasi antrian. Terima kasih!"
        }

        var draftMessage by remember { mutableStateOf(defaultMsg) }

        AlertDialog(
            onDismissRequest = { editingBlastCustomer = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Chat, contentDescription = null, tint = Color(0xFF2E7D32))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("DRAFT WA: ${target.name}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                ) {
                    Text(text = "Plat: ${target.plateNumber} • WA: ${target.phone}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Text(text = "Histori Servis Terakhir: ${target.lastDateStr}", fontSize = 11.sp, color = Color(0xFFC62828), fontWeight = FontWeight.SemiBold)
                    Text(text = "Item Terakhir: ${target.lastServiceItems}", fontSize = 11.sp, color = Color.DarkGray)

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "KOREKSI ISI PESAN WA SEBELUM KIRIM:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = draftMessage,
                        onValueChange = { draftMessage = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f, fill = false),
                        maxLines = 8,
                        shape = RoundedCornerShape(8.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        ReportExporter.openDirectWhatsAppChat(context, target.phone, draftMessage)
                        target.isSent = true
                        editingBlastCustomer = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SEND WA SEKARANG")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingBlastCustomer = null }) {
                    Text("BATAL")
                }
            }
        )
    }


}

// -------------------------------------------------------------
// TAB 1: 20 CUSTOMER LOYAL
// -------------------------------------------------------------
@Composable
private fun LoyalCustomerView(
    loyalList: List<ReportExporter.LoyalCustomerData>,
    rupiahFormat: NumberFormat,
    onExportExcel: (Boolean) -> Unit,
    onChatCustomer: (String, String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                border = BorderStroke(1.dp, Color(0xFFFFD54F))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("TOP 20 CUSTOMER LOYAL", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFE65100))
                    Text("Pelanggan paling sering berkunjung & total belanja tertinggi", fontSize = 11.sp, color = Color.DarkGray)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onExportExcel(false) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("SAVE EXCEL", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { onExportExcel(true) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("SEND WA EXCEL", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (loyalList.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Belum ada data pelanggan tercatat.", color = Color.Gray, fontSize = 13.sp)
                }
            }
        } else {
            items(loyalList) { customer ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    when (customer.rank) {
                                        1 -> Color(0xFFFFD700)
                                        2 -> Color(0xFFC0C0C0)
                                        3 -> Color(0xFFCD7F32)
                                        else -> Color(0xFFE0E0E0)
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "#${customer.rank}",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = if (customer.rank <= 3) Color.Black else Color.DarkGray
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(customer.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    customer.plateNumber,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1565C0),
                                    modifier = Modifier.background(Color(0xFFE3F2FD), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                            Text("Total: ${customer.totalServices}x Servis • Belanja: ${rupiahFormat.format(customer.totalSpending)}", fontSize = 11.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.SemiBold)
                            Text(customer.note, fontSize = 10.sp, color = Color(0xFFE65100), fontWeight = FontWeight.Bold)
                        }

                        if (customer.phone.isNotBlank() && customer.phone != "-") {
                            IconButton(onClick = { onChatCustomer(customer.phone, customer.name) }) {
                                Icon(Icons.Default.Chat, contentDescription = "Chat", tint = Color(0xFF2E7D32))
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 2: BEST EMPLOYEE PERFORMANCE MEKANIK
// -------------------------------------------------------------
@Composable
private fun BestMechanicView(
    performances: List<MechanicPerformance>,
    rupiahFormat: NumberFormat
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            val best = performances.firstOrNull()
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9C4)),
                border = BorderStroke(1.5.dp, Color(0xFFFBC02D))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("🏆 BEST MECHANIC OF THE MONTH", fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFFF57F17))
                            Text("Kinerja mekanik terbaik dengan unit servis terbanyak", fontSize = 11.sp, color = Color.DarkGray)
                        }
                        Text("JUARA 1", fontWeight = FontWeight.Black, fontSize = 16.sp, color = Color(0xFFF57F17))
                    }

                    if (best != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFFD54F)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color(0xFFE65100), modifier = Modifier.size(28.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(best.mechanicName, fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color.Black)
                                Text("${best.totalMotor} Unit Motor Diselesaikan", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF2E7D32))
                                Text("Omset: ${rupiahFormat.format(best.totalOmset)} • Estimasi Komisi: ${rupiahFormat.format(best.estimasiKomisi)}", fontSize = 11.sp, color = Color.DarkGray)
                            }
                        }
                    }
                }
            }
        }

        item {
            Text("PERINGKAT KINERJA SEMUA MEKANIK", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color.Gray, modifier = Modifier.padding(top = 8.dp))
        }

        items(performances) { mech ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                when (mech.rank) {
                                    1 -> Color(0xFFFFD700)
                                    2 -> Color(0xFFC0C0C0)
                                    3 -> Color(0xFFCD7F32)
                                    else -> Color(0xFFECEFF1)
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "#${mech.rank}",
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp,
                            color = if (mech.rank <= 3) Color.Black else Color.DarkGray
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(mech.mechanicName, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            "${mech.totalMotor} Motor Diservis • Omset Jasa: ${rupiahFormat.format(mech.totalJasa)}",
                            fontSize = 11.sp,
                            color = Color(0xFF1565C0),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Estimasi Bagi Hasil/Komisi: ${rupiahFormat.format(mech.estimasiKomisi)}",
                            fontSize = 11.sp,
                            color = Color(0xFF2E7D32),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 3: WA BLAST PENGINGAT SERVIS & LOG / DRAFT POPUP
// -------------------------------------------------------------
@Composable
private fun WaBlastView(
    blastList: List<WaBlastCustomerItem>,
    bengkelName: String,
    rupiahFormat: NumberFormat,
    onSelectForCorrection: (WaBlastCustomerItem) -> Unit,
    onOpenAutoBlast: () -> Unit,
    onExportExcel: (Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                border = BorderStroke(1.dp, Color(0xFFA5D6A7))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("WA BLAST PENGINGAT SERVIS", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1B5E20))
                            Text("Target pelanggan > 30 hari belum servis", fontSize = 11.sp, color = Color(0xFF2E7D32))
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF2E7D32))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("${blastList.size} Target", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onExportExcel(false) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("SAVE EXCEL", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = { onExportExcel(true) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20)),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("SEND WA EXCEL", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (blastList.isEmpty()) {
            item {
                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Semua pelanggan rutin servis dalam 30 hari terakhir!", color = Color.Gray, fontSize = 13.sp)
                }
            }
        } else {
            items(blastList) { item ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (item.isSent) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${item.no}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (item.isSent) Color(0xFF2E7D32) else Color(0xFFC62828)
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(item.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    item.plateNumber,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFC62828),
                                    modifier = Modifier.background(Color(0xFFFFEBEE), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                            Text("Terakhir: ${item.lastDateStr} (${item.daysSinceLastService} hari lalu)", fontSize = 11.sp, color = Color(0xFFD32F2F), fontWeight = FontWeight.SemiBold)
                            Text("Pengerjaan: ${item.lastServiceItems}", fontSize = 10.sp, color = Color.Gray, maxLines = 1)
                        }

                        Button(
                            onClick = { onSelectForCorrection(item) },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = if (item.isSent) Color(0xFF689F38) else Color(0xFF2E7D32)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (item.isSent) "SENT" else "KOREKSI & SEND", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
