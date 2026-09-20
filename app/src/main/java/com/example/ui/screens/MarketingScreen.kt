package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NavigateBefore
import androidx.compose.material.icons.filled.NavigateNext
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.CustomerService
import com.example.ui.BengkelScreen
import com.example.ui.BengkelViewModel
import com.example.ui.components.ProUpgradeDialog
import com.example.util.FeatureGate
import com.example.util.ReportExporter
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun MarketingScreen(viewModel: BengkelViewModel) {
    val context = LocalContext.current
    val profile by viewModel.workshopProfile.collectAsStateWithLifecycle()
    val completedServices by viewModel.completedServices.collectAsStateWithLifecycle()
    val activeServices by viewModel.activeServices.collectAsStateWithLifecycle()
    val allServices = remember(completedServices, activeServices) {
        completedServices + activeServices
    }

    val isPro = FeatureGate.isPro(profile)
    val hasProAccess = remember(profile) {
        FeatureGate.hasProAccess(context, profile)
    }

    var showProDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Loyal Customer, 1: WA Blast > 1 Bulan
    var showTemplateDialog by remember { mutableStateOf(false) }
    var showAutoBlastDialog by remember { mutableStateOf(false) }

    val rupiahFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("id", "ID")).apply {
            maximumFractionDigits = 0
        }
    }

    // 1. Calculate Loyal Customers (Aggregated by Customer)
    val loyalCustomers = remember(allServices) {
        // Group by Customer key (Phone or Name+Plate)
        val grouped = allServices.groupBy { service ->
            val cleanPhone = service.phoneNumber.trim().replace(Regex("[^0-9]"), "")
            if (cleanPhone.length >= 8) cleanPhone else "${service.customerName.trim().uppercase()}_${service.plateNumber.trim().uppercase()}"
        }

        grouped.map { (_, services) ->
            val first = services.first()
            val totalServices = services.size
            val totalSpending = services.sumOf { it.totalAmount }
            val lastDate = services.maxOfOrNull { it.dateEpoch } ?: 0L

            ReportExporter.LoyalCustomerData(
                rank = 0, // will be assigned after sorting
                name = first.customerName.ifBlank { "Pelanggan" },
                phone = first.phoneNumber.ifBlank { "-" },
                plateNumber = first.plateNumber.ifBlank { "-" },
                totalServices = totalServices,
                totalSpending = totalSpending,
                note = ""
            ) to lastDate
        }
            .sortedWith(compareByDescending<Pair<ReportExporter.LoyalCustomerData, Long>> { it.first.totalServices }
                .thenByDescending { it.first.totalSpending })
            .mapIndexed { index, (data, lastDate) ->
                val rank = index + 1
                val note = when (rank) {
                    1 -> "⭐ Top #1 Loyal VIP"
                    2 -> "🥈 Top #2 Loyal Gold"
                    3 -> "🥉 Top #3 Loyal Silver"
                    in 4..10 -> "Top 10 Loyal Customer"
                    in 11..20 -> "Top 20 Loyal Customer"
                    else -> "Pelanggan Setia"
                }
                data.copy(rank = rank, note = note) to lastDate
            }
    }

    // 2. Calculate WA Blast Customers (> 1 Month Since Last Service)
    val thirtyDaysMs = remember { 30L * 24L * 60L * 60L * 1000L }
    val now = remember { System.currentTimeMillis() }

    val waBlastCustomers = remember(allServices, now) {
        val grouped = allServices.groupBy { service ->
            val cleanPhone = service.phoneNumber.trim().replace(Regex("[^0-9]"), "")
            if (cleanPhone.length >= 8) cleanPhone else "${service.customerName.trim().uppercase()}_${service.plateNumber.trim().uppercase()}"
        }

        grouped.mapNotNull { (_, services) ->
            val first = services.first()
            val totalServices = services.size
            val totalSpending = services.sumOf { it.totalAmount }
            val lastDate = services.maxOfOrNull { it.dateEpoch } ?: 0L

            val diff = now - lastDate
            if (diff >= thirtyDaysMs && lastDate > 0L) {
                val days = (diff / (24L * 60L * 60L * 1000L)).toInt()
                val dateStr = SimpleDateFormat("dd/MM/yyyy", Locale("id", "ID")).format(Date(lastDate))
                val note = "Lewat $days hari (Perlu Servis & Ganti Oli)"

                ReportExporter.WaBlastCustomerData(
                    no = 0, // will assign index
                    lastDateStr = dateStr,
                    name = first.customerName.ifBlank { "Pelanggan" },
                    phone = first.phoneNumber.ifBlank { "-" },
                    plateNumber = first.plateNumber.ifBlank { "-" },
                    note = note,
                    totalServices = totalServices,
                    totalSpending = totalSpending,
                    daysSinceLastService = days
                )
            } else null
        }.sortedByDescending { it.daysSinceLastService }
            .mapIndexed { index, item -> item.copy(no = index + 1) }
    }

    Scaffold(
        topBar = {
            BengkelTopBar(
                title = "MARKETING (PRO)",
                onBack = { viewModel.navigateTo(BengkelScreen.DASHBOARD) }
            )
        }
    ) { padding ->
        if (!hasProAccess) {
            // PRO LOCKED VIEW
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFFEBEE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Fitur Pro Terkunci",
                        tint = Color(0xFFC62828),
                        modifier = Modifier.size(42.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = "Menu Marketing (Khusus PRO)",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC62828)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Fitur Ranking 20 Loyal Customer, Broadcast WA Blast pelanggan > 1 bulan servis, dan Ekspor Excel Marketing adalah fitur eksklusif Bengkel Qu PRO.",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Langganan Bulanan PRO Resmi: Hubungi Developer\nWA: 085714216556 / bageurhendri@gmail.com",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1565C0),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { showProDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(48.dp)
                ) {
                    Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AKTIFKAN SERIAL NUMBER PRO", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            // PRO UNLOCKED CONTENT
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Tab Selection: Loyal Customer vs WA Blast
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("RANKING 20 LOYAL", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("WA BLAST (>1 BULAN)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    )
                }

                if (selectedTab == 0) {
                    // TAB 0: RANKING 20 LOYAL CUSTOMER
                    LoyalCustomerTabContent(
                        loyalList = loyalCustomers.map { it.first },
                        rupiahFormat = rupiahFormat,
                        onExportExcel = { sendWa ->
                            viewModel.exportLoyalCustomerExcel(
                                context = context,
                                list = loyalCustomers.map { it.first },
                                sendViaWhatsApp = sendWa
                            )
                        },
                        onChatCustomer = { phone, name ->
                            val msg = "Halo Kak $name, salam dari ${profile?.workshopName ?: "Bengkel Qu"}! Terima kasih telah menjadi pelanggan setia kami. Kami selalu siap melayani perawatan kendaraan Anda."
                            ReportExporter.openDirectWhatsAppChat(context, phone, msg)
                        }
                    )
                } else {
                    // TAB 1: WA BLAST ALL CUSTOMERS > 1 MONTH
                    WaBlastTabContent(
                        bengkelName = profile?.workshopName ?: "Bengkel Qu",
                        blastList = waBlastCustomers,
                        rupiahFormat = rupiahFormat,
                        onExportExcel = { sendWa ->
                            viewModel.exportWaBlastExcel(
                                context = context,
                                list = waBlastCustomers,
                                sendViaWhatsApp = sendWa
                            )
                        },
                        onSendManualWa = { item ->
                            val msg = "Halo Kak ${item.name}, salam dari ${profile?.workshopName ?: "Bengkel Qu"}. Kendaraan Anda (${item.plateNumber}) sudah 1 bulan sejak servis terakhir pada ${item.lastDateStr}.\n\nKami sarankan untuk melakukan servis berkala dan ganti oli agar performa mesin tetap prima dan hemat bahan bakar. Silakan kunjungi bengkel kami atau balas pesan ini untuk reservasi antrian. Terima kasih!"
                            ReportExporter.openDirectWhatsAppChat(context, item.phone, msg)
                        },
                        onOpenTemplate = { showTemplateDialog = true },
                        onOpenAutoBlast = { showAutoBlastDialog = true }
                    )
                }
            }
        }
    }

    if (showProDialog) {
        ProUpgradeDialog(
            featureTitle = "MARKETING BENGKEL QU PRO",
            reasonText = "Fitur Loyal Customer Ranking, Broadcast Pengingat Servis WA Blast, dan Export Excel Marketing memerlukan lisensi Bengkel Qu PRO aktif.",
            onDismiss = { showProDialog = false },
            onActivateKey = { key ->
                viewModel.activateProLicense(key, context) { success, _ ->
                    if (success) showProDialog = false
                }
            }
        )
    }

    if (showTemplateDialog) {
        val clipboard = LocalClipboardManager.current
        val sampleTemplate = "Halo Kak [NAMA], salam dari ${profile?.workshopName ?: "Bengkel Qu"}. Kendaraan [PLAT] sudah 1 bulan sejak servis terakhir. Kami sarankan untuk cek berkala & ganti oli agar mesin tetap prima. Hubungi kami untuk reservasi antrian. Terima kasih!"

        AlertDialog(
            onDismissRequest = { showTemplateDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Chat, contentDescription = null, tint = Color(0xFF2E7D32))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Template Pesan WA Blast", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Format pesan pengingat otomatis untuk pelanggan yang sudah 1 bulan servis:",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, Color(0xFFC8E6C9)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = sampleTemplate,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(12.dp),
                            color = Color(0xFF1B5E20)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tips: Tombol 'KIRIM WA' pada setiap kartu pelanggan akan otomatis mengisi nama dan nomor plat pelanggan bersangkutan.",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        clipboard.setText(AnnotatedString(sampleTemplate))
                        Toast.makeText(context, "Template pesan disalin ke clipboard", Toast.LENGTH_SHORT).show()
                        showTemplateDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SALIN TEMPLATE")
                }
            },
            dismissButton = {
                TextButton(onClick = { showTemplateDialog = false }) {
                    Text("TUTUP")
                }
            }
        )
    }

    if (showAutoBlastDialog) {
        AutoWaBlastDialog(
            bengkelName = profile?.workshopName ?: "Bengkel Qu",
            blastList = waBlastCustomers,
            onDismiss = { showAutoBlastDialog = false },
            onSendWa = { item ->
                val msg = "Halo Kak ${item.name}, salam dari ${profile?.workshopName ?: "Bengkel Qu"}. Kendaraan Anda (${item.plateNumber}) sudah 1 bulan sejak servis terakhir pada ${item.lastDateStr}.\n\nKami sarankan untuk melakukan servis berkala dan ganti oli agar performa mesin tetap prima dan hemat bahan bakar. Silakan kunjungi bengkel kami atau balas pesan ini untuk reservasi antrian. Terima kasih!"
                ReportExporter.openDirectWhatsAppChat(context, item.phone, msg)
            }
        )
    }
}

@Composable
private fun LoyalCustomerTabContent(
    loyalList: List<ReportExporter.LoyalCustomerData>,
    rupiahFormat: NumberFormat,
    onExportExcel: (Boolean) -> Unit,
    onChatCustomer: (String, String) -> Unit
) {
    val top20 = remember(loyalList) { loyalList.take(20) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Summary & Export Action Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
                border = BorderStroke(1.dp, Color(0xFFFFD54F))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "RANKING 20 LOYAL CUSTOMER",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFFE65100)
                            )
                            Text(
                                text = "Berdasarkan frekuensi servis terbanyak & total biaya",
                                fontSize = 11.sp,
                                color = Color(0xFF5D4037)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFFB300))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("Top 20", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Export File Excel (1-50)
                        Button(
                            onClick = { onExportExcel(false) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SIMPAN EXCEL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Send WA Excel
                        Button(
                            onClick = { onExportExcel(true) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("KIRIM WA (EXCEL)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "* Ekspor Excel mencakup rekap No 1 s/d 50 pelanggan loyal.",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        if (top20.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Belum ada riwayat transaksi servis pelanggan yang tercatat.",
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(top20) { item ->
                LoyalCustomerCard(
                    item = item,
                    rupiahFormat = rupiahFormat,
                    onChatClick = { onChatCustomer(item.phone, item.name) }
                )
            }
        }
    }
}

@Composable
private fun LoyalCustomerCard(
    item: ReportExporter.LoyalCustomerData,
    rupiahFormat: NumberFormat,
    onChatClick: () -> Unit
) {
    val rankColor = when (item.rank) {
        1 -> Color(0xFFFFB300) // Gold
        2 -> Color(0xFF78909C) // Silver
        3 -> Color(0xFF8D6E63) // Bronze
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank Badge Circle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(rankColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "#${item.rank}",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.plateNumber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF1565C0),
                        modifier = Modifier
                            .background(Color(0xFFE3F2FD), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Total: ${item.totalServices}x Servis",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                    Text(
                        text = "Biaya: ${rupiahFormat.format(item.totalSpending)}",
                        fontSize = 11.sp,
                        color = Color(0xFFE65100),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "WA: ${item.phone} • ${item.note}",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }

            if (item.phone.isNotBlank() && item.phone != "-") {
                Button(
                    onClick = onChatClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = "Chat WA", modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("WA", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun WaBlastTabContent(
    bengkelName: String,
    blastList: List<ReportExporter.WaBlastCustomerData>,
    rupiahFormat: NumberFormat,
    onExportExcel: (Boolean) -> Unit,
    onSendManualWa: (ReportExporter.WaBlastCustomerData) -> Unit,
    onOpenTemplate: () -> Unit,
    onOpenAutoBlast: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Summary & Actions Header Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
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
                            Text(
                                text = "PENGINGAT SERVIS (>1 BULAN)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF1B5E20)
                            )
                            Text(
                                text = "Pelanggan yang sudah 30+ hari tidak berkunjung ke bengkel",
                                fontSize = 11.sp,
                                color = Color(0xFF2E7D32)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2E7D32))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text("${blastList.size} Orang", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Row 1: AUTO WA BLAST & TEMPLATE
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = onOpenAutoBlast,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Icon(Icons.Default.Campaign, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("AUTO WA BLAST", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onOpenTemplate,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(0.9f)
                        ) {
                            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF2E7D32))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("TEMPLATE", fontSize = 10.sp, color = Color(0xFF2E7D32))
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Row 2: EXPORT EXCEL & KIRIM WA EXCEL
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Button(
                            onClick = { onExportExcel(false) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SIMPAN EXCEL", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onExportExcel(true) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("KIRIM WA (EXCEL)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        if (blastList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Semua pelanggan baru saja servis dalam 30 hari terakhir.",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color(0xFF2E7D32)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tidak ada pelanggan yang melewati batas 1 bulan tanpa servis.",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        } else {
            items(blastList) { item ->
                WaBlastCustomerCard(
                    item = item,
                    rupiahFormat = rupiahFormat,
                    onSendWa = { onSendManualWa(item) }
                )
            }
        }
    }
}

@Composable
private fun WaBlastCustomerCard(
    item: ReportExporter.WaBlastCustomerData,
    rupiahFormat: NumberFormat,
    onSendWa: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Index number Box
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFEBEE)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${item.no}",
                    color = Color(0xFFC62828),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = item.plateNumber,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFC62828),
                        modifier = Modifier
                            .background(Color(0xFFFFEBEE), RoundedCornerShape(4.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Terakhir: ${item.lastDateStr} (${item.daysSinceLastService} hari yang lalu)",
                    fontSize = 11.sp,
                    color = Color(0xFFD32F2F),
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "WA: ${item.phone} • Riwayat: ${item.totalServices}x (${rupiahFormat.format(item.totalSpending)})",
                    fontSize = 10.sp,
                    color = Color.Gray
                )
            }

            Button(
                onClick = onSendWa,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("KIRIM WA", fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun AutoWaBlastDialog(
    bengkelName: String,
    blastList: List<ReportExporter.WaBlastCustomerData>,
    onDismiss: () -> Unit,
    onSendWa: (ReportExporter.WaBlastCustomerData) -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var currentIndex by remember { mutableIntStateOf(0) }

    if (blastList.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Auto WA Blast", fontWeight = FontWeight.Bold) },
            text = { Text("Tidak ada pelanggan yang melewati batas 1 bulan tanpa servis.") },
            confirmButton = {
                Button(onClick = onDismiss) { Text("OK") }
            }
        )
        return
    }

    val currentCustomer = blastList.getOrElse(currentIndex) { blastList.first() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Campaign, contentDescription = null, tint = Color(0xFFE65100))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Asisten Auto WA Blast",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = Color(0xFFE65100)
                    )
                    Text(
                        text = "Target ${currentIndex + 1} dari ${blastList.size} Pelanggan",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFCC80)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentCustomer.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = currentCustomer.plateNumber,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFFE65100),
                                modifier = Modifier
                                    .background(Color(0xFFFFE0B2), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "WhatsApp: ${currentCustomer.phone}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Terakhir Servis: ${currentCustomer.lastDateStr} (${currentCustomer.daysSinceLastService} hari lalu)",
                            fontSize = 11.sp,
                            color = Color(0xFFD32F2F)
                        )
                    }
                }

                Text(
                    text = "Pesan Otomatis Siap Kirim:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray
                )

                val previewMessage = remember(currentCustomer, bengkelName) {
                    "Halo Kak ${currentCustomer.name}, salam dari $bengkelName. Kendaraan Anda (${currentCustomer.plateNumber}) sudah 1 bulan sejak servis terakhir pada ${currentCustomer.lastDateStr}.\n\nKami sarankan untuk melakukan servis berkala dan ganti oli agar performa mesin tetap prima dan hemat bahan bakar. Silakan kunjungi bengkel kami atau balas pesan ini untuk reservasi antrian. Terima kasih!"
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color(0xFFC8E6C9)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = previewMessage,
                        fontSize = 11.sp,
                        color = Color(0xFF1B5E20),
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(
                        onClick = {
                            val allPhones = blastList.mapNotNull {
                                val clean = it.phone.trim().replace(Regex("[^0-9]"), "")
                                if (clean.length >= 8) clean else null
                            }.joinToString("\n")
                            clipboard.setText(AnnotatedString(allPhones))
                            Toast.makeText(context, "${blastList.size} nomor WA disalin ke clipboard", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SALIN SEMUA NO WA", fontSize = 10.sp)
                    }
                }
            }
        },
        confirmButton = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = {
                        onSendWa(currentCustomer)
                        if (currentIndex < blastList.size - 1) {
                            currentIndex++
                        } else {
                            Toast.makeText(context, "Seluruh pelanggan target WA Blast telah diproses!", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (currentIndex < blastList.size - 1) "KIRIM WA & LANJUT" else "KIRIM WA (TERAKHIR)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = { if (currentIndex > 0) currentIndex-- },
                        enabled = currentIndex > 0,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.NavigateBefore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text("SEBELUMNYA", fontSize = 10.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            if (currentIndex < blastList.size - 1) {
                                currentIndex++
                            } else {
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(if (currentIndex < blastList.size - 1) "LEWATI" else "SELESAI", fontSize = 10.sp)
                        Icon(Icons.Default.NavigateNext, contentDescription = null, modifier = Modifier.size(16.dp))
                    }
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("TUTUP", color = Color.Gray)
            }
        }
    )
}
