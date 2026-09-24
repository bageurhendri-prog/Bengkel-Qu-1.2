package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ApprovalStatus
import com.example.data.local.CashDeposit
import com.example.data.local.ExpenseItem
import com.example.data.local.PaymentMethod
import com.example.data.local.ServiceStatus
import com.example.ui.BengkelScreen
import com.example.ui.BengkelViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportScreen(viewModel: BengkelViewModel) {
    val context = LocalContext.current
    val rejects by viewModel.rejectItems.collectAsStateWithLifecycle()
    val incoming by viewModel.incomingStocks.collectAsStateWithLifecycle()
    val expenses by viewModel.expenseItems.collectAsStateWithLifecycle()
    val cashDeposits by viewModel.cashDeposits.collectAsStateWithLifecycle()
    val completedServices by viewModel.completedServices.collectAsStateWithLifecycle()
    val activeRole by viewModel.activeUserRole.collectAsStateWithLifecycle()
    val workshopProfile by viewModel.workshopProfile.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }
    // 0: Closingan Kasir & Metode Bayar
    // 1: Setoran Bos (Cash/TF + Approval Tgl/Jam)
    // 2: Peti Cash (Kas Besar + Kas Kecil/Rembes)
    // 3: Approval Barang & Ekspor

    // Dialog state
    var showAjukanSetoranDialog by remember { mutableStateOf(false) }
    var showAjukanKasKecilDialog by remember { mutableStateOf(false) }

    val isBossOrAdmin = activeRole == "ADMIN" || activeRole == "OWNER"

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "LAPORAN & REPORT KASIR",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Closingan, Setoran Bos, Peti Cash & Approval",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.85f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateTo(BengkelScreen.DASHBOARD) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Tab Navigation
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 12.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("CLOSINGAN & BAYAR", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("SETORAN BOS (APPROVAL)", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("PETI CASH (BESAR/KECIL)", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("BARANG & EKSPOR", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                )
            }

            // Tab Content
            when (selectedTab) {
                0 -> ClosinganTabContent(
                    completedServices = completedServices,
                    workshopName = workshopProfile?.workshopName ?: "BENGKEL QU",
                    context = context
                )
                1 -> SetoranBosTabContent(
                    deposits = cashDeposits,
                    isBoss = isBossOrAdmin,
                    onAjukanClick = { showAjukanSetoranDialog = true },
                    onApprove = { id, approved -> viewModel.approveSetoranKeBos(id, approved, context) }
                )
                2 -> PetiCashTabContent(
                    deposits = cashDeposits,
                    expenses = expenses,
                    isBoss = isBossOrAdmin,
                    onAjukanPengajuan = { showAjukanKasKecilDialog = true },
                    onApproveExpense = { id, approved -> viewModel.approveKasKecilRembes(id, approved, context) }
                )
                3 -> BarangDanEksporTabContent(
                    rejects = rejects,
                    incoming = incoming,
                    onApproveReject = { id, approved -> viewModel.approveRejectItem(id, approved) },
                    onApproveIncoming = { id, approved -> viewModel.approveIncomingStock(id, approved) },
                    onExportCustomer = { sendWa -> viewModel.exportCustomerData(context, sendWa) },
                    onExportStock = { sendWa -> viewModel.exportStockData(context, sendWa) }
                )
            }
        }
    }

    // DIALOG AJUKAN SETORAN KE BOS (CASH / TF)
    if (showAjukanSetoranDialog) {
        var depositType by remember { mutableStateOf("CASH") }
        var amountStr by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAjukanSetoranDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Send, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AJUKAN SETORAN KE BOS", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Pilih jenis setoran omset kasir ke Bos/Owner:", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = depositType == "CASH",
                            onClick = { depositType = "CASH" },
                            label = { Text("CASH (Tunai)", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = depositType == "TRANSFER_BANK",
                            onClick = { depositType = "TRANSFER_BANK" },
                            label = { Text("TRANSFER (TF)", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Nominal Setoran (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Catatan / Rekening Tujuan (Opsional)") },
                        singleLine = false,
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = amountStr.toLongOrNull() ?: 0L
                        if (amount <= 0) {
                            Toast.makeText(context, "Masukkan nominal setoran yang valid!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.submitSetoranKeBos(depositType, amount, notes, context)
                        showAjukanSetoranDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("KIRIM PENGAJUAN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAjukanSetoranDialog = false }) {
                    Text("BATAL")
                }
            }
        )
    }

    // DIALOG AJUKAN KAS KECIL / REMBES
    if (showAjukanKasKecilDialog) {
        var category by remember { mutableStateOf("KAS_KECIL") } // KAS_KECIL atau REMBES
        var itemName by remember { mutableStateOf("") }
        var amountStr by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAjukanKasKecilDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Receipt, contentDescription = null, tint = Color(0xFFE65100))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PENGAJUAN DANA / REMBES", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("Pengajuan dana kas kecil atau penggantian rembesan operasional bengkel:", fontSize = 12.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = category == "KAS_KECIL",
                            onClick = { category = "KAS_KECIL" },
                            label = { Text("KAS KECIL", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = category == "REMBES",
                            onClick = { category = "REMBES" },
                            label = { Text("REMBESAN", fontWeight = FontWeight.Bold) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = itemName,
                        onValueChange = { itemName = it },
                        label = { Text("Keperluan / Nama Barang") },
                        placeholder = { Text("Contoh: Bensin Operasional, Air Galon, Baut") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = { amountStr = it },
                        label = { Text("Nominal Pengajuan (Rp)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = amountStr.toLongOrNull() ?: 0L
                        if (itemName.isBlank() || amount <= 0) {
                            Toast.makeText(context, "Lengkapi nama keperluan dan nominal!", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        viewModel.submitPengajuanKasKecil(itemName, amount, category, context)
                        showAjukanKasKecilDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
                ) {
                    Text("AJUKAN DANA")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAjukanKasKecilDialog = false }) {
                    Text("BATAL")
                }
            }
        )
    }
}

// -------------------------------------------------------------
// TAB 1: CLOSINGAN & METODE BAYAR (Cash / TF / QRIS / Ojol)
// -------------------------------------------------------------
@Composable
private fun ClosinganTabContent(
    completedServices: List<com.example.data.local.CustomerService>,
    workshopName: String,
    context: Context
) {
    val todayStr = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date())

    val totalCash = completedServices.filter { it.paymentMethod == PaymentMethod.CASH }.sumOf { it.totalAmount }
    val totalTf = completedServices.filter { it.paymentMethod == PaymentMethod.TRANSFER_BANK }.sumOf { it.totalAmount }
    val totalQris = completedServices.filter { it.paymentMethod == PaymentMethod.QRIS }.sumOf { it.totalAmount }
    val totalOjol = completedServices.filter { it.paymentMethod == PaymentMethod.OJOL }.sumOf { it.totalAmount }
    val grandTotal = totalCash + totalTf + totalQris + totalOjol

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Grand Total Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "RINGKASAN CLOSINGAN HARI INI", fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f), fontWeight = FontWeight.Bold)
                Text(text = todayStr, fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(10.dp))
                Text(text = formatRupiah(grandTotal), fontSize = 28.sp, fontWeight = FontWeight.Black, color = Color.White)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = "Total Transaksi: ${completedServices.size} Nota Lunas", fontSize = 12.sp, color = Color.White)
            }
        }

        // Breakdown per Metode Bayar
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "RINCIAN PER METODE PEMBAYARAN:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(10.dp))

                MetodeBayarRow(label = "💵 Setoran Cash (Tunai)", amount = totalCash, color = Color(0xFF2E7D32))
                MetodeBayarRow(label = "🏦 Transfer Bank (TF)", amount = totalTf, color = Color(0xFF1565C0))
                MetodeBayarRow(label = "📱 QRIS / Barcode", amount = totalQris, color = Color(0xFF6A1B9A))
                MetodeBayarRow(label = "🛵 Ojol / EDC / Lainnya", amount = totalOjol, color = Color(0xFFE65100))

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "TOTAL OMSET CLOSING", fontWeight = FontWeight.Black, fontSize = 13.sp)
                    Text(text = formatRupiah(grandTotal), fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color(0xFF1B5E20))
                }
            }
        }

        // Tombol Kirim WA ke Bos
        Button(
            onClick = {
                val report = """
                    *REPORT CLOSINGAN KASIR RESMI*
                    Bengkel: $workshopName
                    Tanggal: $todayStr
                    --------------------------------
                    • Setoran CASH (Tunai) : ${formatRupiah(totalCash)}
                    • Transfer Bank (TF)  : ${formatRupiah(totalTf)}
                    • QRIS / Barcode      : ${formatRupiah(totalQris)}
                    • Ojol / EDC          : ${formatRupiah(totalOjol)}
                    --------------------------------
                    *GRAND TOTAL OMSET    : ${formatRupiah(grandTotal)}*
                    *TOTAL TRANSAKSI      : ${completedServices.size} Pelanggan*
                    ================================
                    Laporan closingan kasir siap disetor ke Bos.
                """.trimIndent()

                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, report)
                }
                context.startActivity(Intent.createChooser(intent, "Kirim Closingan via WhatsApp"))
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
        ) {
            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("KIRIM REPORT CLOSINGAN KE BOS (WA)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

@Composable
private fun MetodeBayarRow(label: String, amount: Long, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Text(text = formatRupiah(amount), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

// -------------------------------------------------------------
// TAB 2: SETORAN BOS (CASH / TF + APPROVAL BOS TGL/JAM)
// -------------------------------------------------------------
@Composable
private fun SetoranBosTabContent(
    deposits: List<CashDeposit>,
    isBoss: Boolean,
    onAjukanClick: () -> Unit,
    onApprove: (Long, Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "SETORAN CASH / TF KE BOS", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(text = "Approval Bos disertai tanggal & jam resmi", fontSize = 11.sp, color = Color.Gray)
            }

            Button(
                onClick = onAjukanClick,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("SETOR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (deposits.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Belum ada riwayat setoran ke Bos.", color = Color.Gray, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(deposits) { dep ->
                    SetoranItemCard(
                        deposit = dep,
                        isBoss = isBoss,
                        onApprove = { onApprove(dep.id, it) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SetoranItemCard(
    deposit: CashDeposit,
    isBoss: Boolean,
    onApprove: (Boolean) -> Unit
) {
    val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(deposit.dateEpoch))
    val approvedDateStr = if (deposit.approvedAtEpoch > 0) {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(deposit.approvedAtEpoch))
    } else null

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (deposit.depositType == "CASH") Color(0xFFE8F5E9) else Color(0xFFE3F2FD)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (deposit.depositType == "CASH") Icons.Default.Money else Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = if (deposit.depositType == "CASH") Color(0xFF2E7D32) else Color(0xFF1565C0),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Setoran ${deposit.depositType}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(text = "Tgl: $dateStr", fontSize = 10.sp, color = Color.Gray)
                    }
                }

                StatusBadge(deposit.approvalStatus)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = formatRupiah(deposit.totalAmount),
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF1B5E20)
            )

            if (deposit.notes.isNotBlank()) {
                Text(text = "Catatan: ${deposit.notes}", fontSize = 11.sp, color = Color.DarkGray)
            }

            // Approval info tgl / jam
            if (deposit.approvalStatus == ApprovalStatus.DISETUJUI && approvedDateStr != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "✓ Di-approve Bos: $approvedDateStr (${deposit.approvedBy})",
                    fontSize = 11.sp,
                    color = Color(0xFF2E7D32),
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Tombol Approve untuk Bos
            if (isBoss && deposit.approvalStatus == ApprovalStatus.PENDING) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onApprove(true) },
                        modifier = Modifier.weight(1f).height(36.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Text("SETUJUI (APPROVE BOS)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { onApprove(false) },
                        modifier = Modifier.weight(0.7f).height(36.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("KOREKSI", fontSize = 10.sp, color = Color.Red)
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 3: PETI CASH (KAS BESAR & KAS KECIL / PENGAJUAN / REMBES)
// -------------------------------------------------------------
@Composable
private fun PetiCashTabContent(
    deposits: List<CashDeposit>,
    expenses: List<ExpenseItem>,
    isBoss: Boolean,
    onAjukanPengajuan: () -> Unit,
    onApproveExpense: (Long, Boolean) -> Unit
) {
    val totalKasBesar = deposits
        .filter { it.approvalStatus == ApprovalStatus.DISETUJUI }
        .sumOf { it.totalAmount }

    val totalKasKecilKeluar = expenses
        .filter { it.status == ApprovalStatus.DISETUJUI }
        .sumOf { it.amount }

    val sisaPetiCash = (totalKasBesar - totalKasKecilKeluar).coerceAtLeast(0L)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Ringkasan Peti Cash
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "PETI CASH BENGKEL (KAS BESAR + KAS KECIL)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFE8F5E9), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text("KAS BESAR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B5E20))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(formatRupiah(totalKasBesar), fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color(0xFF1B5E20))
                            Text("Total setoran lunas", fontSize = 10.sp, color = Color.Gray)
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(Color(0xFFFFF3E0), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Column {
                            Text("KAS KECIL (REMBES)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE65100))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(formatRupiah(totalKasKecilKeluar), fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color(0xFFE65100))
                            Text("Total terpakai", fontSize = 10.sp, color = Color.Gray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Sisa Kas Peti Cash:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(text = formatRupiah(sisaPetiCash), fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF2E7D32))
                    }
                }
            }
        }

        // Header Pengajuan Kas Kecil & Tombol
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = "PENGAJUAN KAS KECIL & REMBES", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(text = "Operasional bensin, galon, konsumsi, rembesan", fontSize = 11.sp, color = Color.Gray)
            }

            Button(
                onClick = onAjukanPengajuan,
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("AJUKAN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }

        if (expenses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Belum ada pengajuan kas kecil atau rembesan.", color = Color.Gray, fontSize = 12.sp)
            }
        } else {
            expenses.forEach { exp ->
                val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(exp.dateEpoch))
                val approvedStr = if (exp.approvedAtEpoch > 0) SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(exp.approvedAtEpoch)) else null

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(exp.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("${exp.category} • $dateStr", fontSize = 10.sp, color = Color.Gray)
                            }
                            StatusBadge(exp.status)
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = formatRupiah(exp.amount),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFC62828)
                        )

                        if (exp.status == ApprovalStatus.DISETUJUI && approvedStr != null) {
                            Text(text = "✓ Disetujui Bos: $approvedStr", fontSize = 10.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.SemiBold)
                        }

                        if (isBoss && exp.status == ApprovalStatus.PENDING) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onApproveExpense(exp.id, true) },
                                    modifier = Modifier.weight(1f).height(34.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                                ) {
                                    Text("SETUJUI", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                OutlinedButton(
                                    onClick = { onApproveExpense(exp.id, false) },
                                    modifier = Modifier.weight(0.7f).height(34.dp),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("KOREKSI", fontSize = 10.sp, color = Color.Red)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------
// TAB 4: BARANG REJECT, BARANG DATANG & EKSPOR EXCEL
// -------------------------------------------------------------
@Composable
private fun BarangDanEksporTabContent(
    rejects: List<com.example.data.local.RejectItem>,
    incoming: List<com.example.data.local.IncomingStock>,
    onApproveReject: (Long, Boolean) -> Unit,
    onApproveIncoming: (Long, Boolean) -> Unit,
    onExportCustomer: (Boolean) -> Unit,
    onExportStock: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // SECTION 1: BARANG REJECT
        ReportApprovalSection(
            title = "PENGAJUAN BARANG REJECT (OLI BOCOR / RUSAK)",
            headerColor = Color(0xFFD32F2F)
        ) {
            if (rejects.isEmpty()) {
                Text("Tidak ada pengajuan barang reject.", fontSize = 12.sp, color = Color.Gray)
            } else {
                rejects.forEach { item ->
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = "${item.itemName} (${item.qty} pcs)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(text = "Alasan: ${item.reason}", fontSize = 11.sp, color = Color.Red)
                            }
                            if (item.status == ApprovalStatus.PENDING) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = { onApproveReject(item.id, true) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("SETUJUI", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { onApproveReject(item.id, false) },
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("KOREKSI", fontSize = 10.sp, color = Color.Red)
                                    }
                                }
                            } else {
                                StatusBadge(item.status)
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFEEEEEE))
                    }
                }
            }
        }

        // SECTION 2: BARANG DATANG
        ReportApprovalSection(
            title = "BARANG DATANG DARI SUPPLIER",
            headerColor = Color(0xFF2E7D32)
        ) {
            if (incoming.isEmpty()) {
                Text("Tidak ada data barang datang.", fontSize = 12.sp, color = Color.Gray)
            } else {
                incoming.forEach { item ->
                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = item.itemName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text(text = "Jumlah: ${item.qty} pcs", fontSize = 11.sp, color = Color.DarkGray)
                            }
                            if (item.status == ApprovalStatus.PENDING) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Button(
                                        onClick = { onApproveIncoming(item.id, true) },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("SETUJUI", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { onApproveIncoming(item.id, false) },
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text("KOREKSI", fontSize = 10.sp, color = Color.Red)
                                    }
                                }
                            } else {
                                StatusBadge(item.status)
                            }
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = Color(0xFFEEEEEE))
                    }
                }
            }
        }

        // SECTION 3: EKSPOR EXCEL (DETAIL CUSTOMER & DETAIL STOK)
        ExportCard(
            title = "EKSPOR DETAIL CUSTOMER",
            description = "Ekspor riwayat pelanggan & servis motor ke format Excel (CSV)",
            onSaveExcel = { onExportCustomer(false) },
            onSendWhatsApp = { onExportCustomer(true) }
        )

        ExportCard(
            title = "EKSPOR DETAIL STOK",
            description = "Ekspor katalog stok sparepart & valuasi aset ke format Excel (CSV)",
            onSaveExcel = { onExportStock(false) },
            onSendWhatsApp = { onExportStock(true) }
        )
    }
}

@Composable
fun ReportApprovalSection(
    title: String,
    headerColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerColor)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White
                )
            }
            Column(modifier = Modifier.padding(14.dp)) {
                content()
            }
        }
    }
}

@Composable
fun StatusBadge(status: ApprovalStatus) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                when (status) {
                    ApprovalStatus.DISETUJUI -> Color(0xFFE8F5E9)
                    ApprovalStatus.KOREKSI -> Color(0xFFFFEBEE)
                    else -> Color(0xFFFFF3E0)
                }
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = status.name,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = when (status) {
                ApprovalStatus.DISETUJUI -> Color(0xFF2E7D32)
                ApprovalStatus.KOREKSI -> Color(0xFFC62828)
                else -> Color(0xFFE65100)
            }
        )
    }
}

@Composable
fun ExportCard(
    title: String,
    description: String,
    onSaveExcel: () -> Unit,
    onSendWhatsApp: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onSaveExcel,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "SAVE EXCEL", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onSendWhatsApp,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "SEND WA EXCEL", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
