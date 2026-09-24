package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.CustomerService
import com.example.data.local.PaymentMethod
import com.example.data.local.ServiceItemDetail
import com.example.data.local.ServiceStatus
import com.example.data.local.StockItem
import com.example.data.local.WorkshopProfile
import com.example.ui.BengkelScreen
import com.example.ui.BengkelViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KasirScreen(
    viewModel: BengkelViewModel
) {
    val context = LocalContext.current
    val customerServices: List<CustomerService> by viewModel.customerServices.collectAsStateWithLifecycle()
    val allStocks: List<StockItem> by viewModel.stockList.collectAsStateWithLifecycle()
    val workshopProfile: WorkshopProfile? by viewModel.workshopProfile.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Menunggu Bayar, 1: Sudah Lunas, 2: Semua
    var searchQuery by remember { mutableStateOf("") }

    // Dialog state
    var payingCustomer by remember { mutableStateOf<CustomerService?>(null) }
    var correctingCustomer by remember { mutableStateOf<CustomerService?>(null) }
    var printReceiptCustomer by remember { mutableStateOf<CustomerService?>(null) }
    var showClosinganDialog by remember { mutableStateOf(false) }

    // Filter daftar customer
    val waitingPaymentList = customerServices.filter { it.status == ServiceStatus.SELESAI }
    val paidList = customerServices.filter { it.status == ServiceStatus.DIBAYAR }

    val displayedList = remember(selectedTab, customerServices, searchQuery) {
        val baseList = when (selectedTab) {
            0 -> waitingPaymentList
            1 -> paidList
            else -> customerServices.filter { it.status == ServiceStatus.SELESAI || it.status == ServiceStatus.DIBAYAR }
        }
        if (searchQuery.isBlank()) {
            baseList
        } else {
            baseList.filter {
                it.customerName.contains(searchQuery, ignoreCase = true) ||
                it.plateNumber.contains(searchQuery, ignoreCase = true) ||
                it.phoneNumber.contains(searchQuery, ignoreCase = true) ||
                it.queueNumber.toString().contains(searchQuery)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "KASIR & BILLING",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${waitingPaymentList.size} Menunggu Pembayaran",
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
                actions = {
                    // Tombol Laporan Closingan Kasir
                    IconButton(onClick = { showClosinganDialog = true }) {
                        Icon(Icons.Default.Assessment, contentDescription = "Closingan", tint = Color.White)
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
            // Header Action Banner: Closingan Button & Search
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Cari antrian, nama, plat...", fontSize = 12.sp) },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        )

                        Button(
                            onClick = { showClosinganDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
                        ) {
                            Icon(Icons.Default.Summarize, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("CLOSINGAN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Tab Row
                    TabRow(
                        selectedTabIndex = selectedTab,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("MENUNGGU BAYAR (${waitingPaymentList.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("LUNAS (${paidList.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("SEMUA", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }

            // Customer List
            if (displayedList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.ReceiptLong,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = Color.LightGray
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = if (selectedTab == 0) "Tidak ada antrian yang menunggu pembayaran." else "Belum ada transaksi di tab ini.",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Nota yang diselesaikan di meja mekanik akan otomatis masuk ke sini.",
                            fontSize = 12.sp,
                            color = Color.DarkGray
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(displayedList) { service ->
                        KasirCustomerCard(
                            service = service,
                            onPayClick = { payingCustomer = service },
                            onCorrectClick = { correctingCustomer = service },
                            onSendBillClick = { viewModel.sendBillViaWhatsApp(context, service) },
                            onPrintReceiptClick = { printReceiptCustomer = service },
                            onSendLunasWaClick = { viewModel.sendReceiptViaWhatsApp(context, service) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(30.dp))
                    }
                }
            }
        }
    }

    // MODAL BAYAR KASIR (Cash / TF / QRIS / Ojol)
    if (payingCustomer != null) {
        val cust = payingCustomer!!
        var selectedMethod by remember { mutableStateOf(PaymentMethod.CASH) }
        var cashGivenStr by remember { mutableStateOf(cust.totalAmount.toString()) }
        var sendLunasWa by remember { mutableStateOf(true) }
        var printReceiptAfterPay by remember { mutableStateOf(true) }

        val cashGiven = cashGivenStr.toLongOrNull() ?: 0L
        val change = (cashGiven - cust.totalAmount).coerceAtLeast(0L)

        AlertDialog(
            onDismissRequest = { payingCustomer = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Payment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("BAYAR NOTA #${cust.queueNumber}", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(text = "Pelanggan: ${cust.customerName} (${cust.plateNumber})", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(text = "Total Tagihan:", fontSize = 12.sp, color = Color.Gray)
                    Text(text = formatRupiah(cust.totalAmount), fontWeight = FontWeight.Black, fontSize = 20.sp, color = Color(0xFF1B5E20))

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(text = "METODE PEMBAYARAN:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PaymentMethodChip(
                            label = "CASH",
                            selected = selectedMethod == PaymentMethod.CASH,
                            onClick = { selectedMethod = PaymentMethod.CASH },
                            modifier = Modifier.weight(1f)
                        )
                        PaymentMethodChip(
                            label = "TRANSFER",
                            selected = selectedMethod == PaymentMethod.TRANSFER_BANK,
                            onClick = { selectedMethod = PaymentMethod.TRANSFER_BANK },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        PaymentMethodChip(
                            label = "QRIS",
                            selected = selectedMethod == PaymentMethod.QRIS,
                            onClick = { selectedMethod = PaymentMethod.QRIS },
                            modifier = Modifier.weight(1f)
                        )
                        PaymentMethodChip(
                            label = "OJOL/EDC",
                            selected = selectedMethod == PaymentMethod.OJOL,
                            onClick = { selectedMethod = PaymentMethod.OJOL },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (selectedMethod == PaymentMethod.CASH) {
                        OutlinedTextField(
                            value = cashGivenStr,
                            onValueChange = { cashGivenStr = it },
                            label = { Text("Uang Tunai Diterima (Rp)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Kembalian:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                Text(text = formatRupiah(change), fontSize = 15.sp, fontWeight = FontWeight.Black, color = Color(0xFF2E7D32))
                            }
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFE3F2FD), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = "Pembayaran $selectedMethod telah diverifikasi masuk ke rekening/akun kasir.",
                                fontSize = 12.sp,
                                color = Color(0xFF0D47A1)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { sendLunasWa = !sendLunasWa }
                    ) {
                        Checkbox(checked = sendLunasWa, onCheckedChange = { sendLunasWa = it })
                        Text(text = "Auto Send WA Struk Lunas ke Customer", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { printReceiptAfterPay = !printReceiptAfterPay }
                    ) {
                        Checkbox(checked = printReceiptAfterPay, onCheckedChange = { printReceiptAfterPay = it })
                        Text(text = "Tampilkan Preview Cetak Struk Kasir", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val currentCust = cust
                        viewModel.processKasirPayment(
                            serviceId = currentCust.id,
                            method = selectedMethod,
                            amountPaid = if (selectedMethod == PaymentMethod.CASH) cashGiven else currentCust.totalAmount,
                            context = context
                        )

                        if (sendLunasWa) {
                            viewModel.sendReceiptViaWhatsApp(context, currentCust)
                        }

                        payingCustomer = null

                        if (printReceiptAfterPay) {
                            printReceiptCustomer = currentCust.copy(status = ServiceStatus.DIBAYAR, paymentMethod = selectedMethod)
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
                ) {
                    Text("PROSES LUNAS")
                }
            },
            dismissButton = {
                TextButton(onClick = { payingCustomer = null }) {
                    Text("BATAL")
                }
            }
        )
    }

    // MODAL KOREKSI NOTA (Tambah Part dari Stok / Hapus Part / Ubah Diskon)
    if (correctingCustomer != null) {
        val target = correctingCustomer!!
        var showAddPartInner by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { correctingCustomer = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.EditNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("KOREKSI NOTA #${target.queueNumber}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 400.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(text = "Pelanggan: ${target.customerName}", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(text = "Koreksi item part / jasa sebelum customer membayar di kasir.", fontSize = 11.sp, color = Color.Gray)

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(text = "DAFTAR ITEM NOTA:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))

                    target.items.forEachIndexed { index, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(text = item.name, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = if (item.isPart) "Sparepart • ${formatRupiah(item.price)}" else "Jasa Servis • ${formatRupiah(item.price)}",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                            IconButton(
                                onClick = {
                                    viewModel.removePartFromService(target.id, item, context)
                                    // Refresh local ref
                                    val updated = customerServices.find { it.id == target.id }
                                    correctingCustomer = updated
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color.Red, modifier = Modifier.size(16.dp))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Tombol Tambah Part dari Stok
                    Button(
                        onClick = { showAddPartInner = true },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("TAMBAH PART DARI STOK", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp))
                            .padding(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Total Saat Ini:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(text = formatRupiah(target.totalAmount), fontWeight = FontWeight.Black, fontSize = 14.sp, color = Color(0xFF1B5E20))
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { correctingCustomer = null }) {
                    Text("SELESAI KOREKSI")
                }
            }
        )

        // Sub-dialog pilih dari stok saat koreksi
        if (showAddPartInner) {
            var searchPartQuery by remember { mutableStateOf("") }
            val availableStocks = allStocks.filter {
                it.qty > 0 &&
                (it.name.contains(searchPartQuery, ignoreCase = true) || it.brand.contains(searchPartQuery, ignoreCase = true))
            }

            AlertDialog(
                onDismissRequest = { showAddPartInner = false },
                title = { Text("PILIH PART DARI STOK FISIK", fontWeight = FontWeight.Bold, fontSize = 15.sp) },
                text = {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = searchPartQuery,
                            onValueChange = { searchPartQuery = it },
                            placeholder = { Text("Cari part...", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (availableStocks.isEmpty()) {
                            Text(text = "Tidak ada sparepart dengan sisa stok fisik!", fontSize = 11.sp, color = Color.Red)
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 200.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                items(availableStocks) { stock ->
                                    Card(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                viewModel.addPartFromStock(target.id, stock, context)
                                                showAddPartInner = false
                                                val updated = customerServices.find { it.id == target.id }
                                                correctingCustomer = updated
                                            },
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(stock.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                                Text("Sisa Stok: ${stock.qty}", fontSize = 10.sp, color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold)
                                            }
                                            Text(formatRupiah(stock.sellPrice), fontSize = 12.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAddPartInner = false }) {
                        Text("TUTUP")
                    }
                }
            )
        }
    }

    // MODAL CETAK NOTA KASIR / STRUK PREVIEW
    if (printReceiptCustomer != null) {
        val receipt = printReceiptCustomer!!
        val profile = workshopProfile
        val timeNow = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

        AlertDialog(
            onDismissRequest = { printReceiptCustomer = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Print, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("STRUK PEMBAYARAN KASIR", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFAFAFA), RoundedCornerShape(8.dp))
                        .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = "================================",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color.DarkGray
                    )
                    Text(
                        text = (profile?.workshopName ?: "BENGKEL QU").uppercase(),
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = profile?.address ?: "Jl. Otomotif No. 88, Bandung",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "Telp: ${profile?.phone ?: "0812-XXXX-XXXX"}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "================================",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color.DarkGray
                    )
                    Text(text = "No. Antrian : #${receipt.queueNumber}", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    Text(text = "Nama        : ${receipt.customerName}", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    Text(text = "Plat Motor  : ${receipt.plateNumber}", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    Text(text = "Mekanik     : ${receipt.mechanicName}", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    Text(text = "Waktu Bayar : $timeNow", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                    Text(text = "Metode      : ${receipt.paymentMethod}", fontFamily = FontFamily.Monospace, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Text(
                        text = "--------------------------------",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    receipt.items.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = item.name.take(18),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                            Text(
                                text = formatRupiah(item.price * item.qty),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (receipt.discount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Diskon", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                            Text(text = "-${formatRupiah(receipt.discount)}", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.Red)
                        }
                    }
                    Text(
                        text = "--------------------------------",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "TOTAL LUNAS", fontFamily = FontFamily.Monospace, fontSize = 12.sp, fontWeight = FontWeight.Black)
                        Text(text = formatRupiah(receipt.totalAmount), fontFamily = FontFamily.Monospace, fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF1B5E20))
                    }
                    Text(
                        text = "================================",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color.DarkGray
                    )
                    Text(
                        text = "   TERIMA KASIH ATAS KUNJUNGANNYA  ",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 9.sp,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            val printText = """
                                ================================
                                ${(profile?.workshopName ?: "BENGKEL QU").uppercase()}
                                ${profile?.address ?: "Jl. Otomotif No. 88, Bandung"}
                                Telp: ${profile?.phone ?: "-"}
                                ================================
                                No. Antrian : #${receipt.queueNumber}
                                Nama        : ${receipt.customerName}
                                Plat Motor  : ${receipt.plateNumber}
                                Mekanik     : ${receipt.mechanicName}
                                Waktu       : $timeNow
                                Status      : LUNAS (${receipt.paymentMethod})
                                --------------------------------
                                ${receipt.items.joinToString("\n") { "${it.name} - ${formatRupiah(it.price)}" }}
                                --------------------------------
                                TOTAL       : ${formatRupiah(receipt.totalAmount)}
                                ================================
                                Terima kasih atas kunjungan Anda!
                            """.trimIndent()

                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, printText)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Cetak Struk Kasir / Printer"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Membuka printer thermal kasir...", Toast.LENGTH_SHORT).show()
                        }
                        printReceiptCustomer = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("CETAK STRUK")
                }
            },
            dismissButton = {
                TextButton(onClick = { printReceiptCustomer = null }) {
                    Text("TUTUP")
                }
            }
        )
    }

    // MODAL REPORT CLOSINGAN KASIR
    if (showClosinganDialog) {
        val todayStr = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID")).format(Date())
        val paidToday = paidList

        val totalCash = paidToday.filter { it.paymentMethod == PaymentMethod.CASH }.sumOf { it.totalAmount }
        val totalTf = paidToday.filter { it.paymentMethod == PaymentMethod.TRANSFER_BANK }.sumOf { it.totalAmount }
        val totalQris = paidToday.filter { it.paymentMethod == PaymentMethod.QRIS }.sumOf { it.totalAmount }
        val totalOjol = paidToday.filter { it.paymentMethod == PaymentMethod.OJOL }.sumOf { it.totalAmount }
        val grandTotal = totalCash + totalTf + totalQris + totalOjol

        AlertDialog(
            onDismissRequest = { showClosinganDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PointOfSale, contentDescription = null, tint = Color(0xFF1B5E20))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("REPORT CLOSINGAN KASIR", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(text = "Tanggal: $todayStr", fontSize = 12.sp, color = Color.Gray)
                    Text(text = "Total Transaksi Lunas: ${paidToday.size} Pelanggan", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

                    Spacer(modifier = Modifier.height(10.dp))

                    ClosinganRowItem(label = "Setoran CASH (Tunai)", amount = totalCash, color = Color(0xFF2E7D32))
                    ClosinganRowItem(label = "Transfer Bank (TF)", amount = totalTf, color = Color(0xFF1565C0))
                    ClosinganRowItem(label = "QRIS / Barcode", amount = totalQris, color = Color(0xFF6A1B9A))
                    ClosinganRowItem(label = "Ojol / EDC / Lainnya", amount = totalOjol, color = Color(0xFFE65100))

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFE8F5E9), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "TOTAL OMSET CLOSINGAN", fontWeight = FontWeight.Black, fontSize = 13.sp, color = Color(0xFF1B5E20))
                            Text(text = formatRupiah(grandTotal), fontWeight = FontWeight.Black, fontSize = 17.sp, color = Color(0xFF1B5E20))
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val reportText = """
                            *REPORT CLOSINGAN KASIR*
                            Bengkel: ${workshopProfile?.workshopName ?: "BENGKEL QU"}
                            Tanggal: $todayStr
                            --------------------------------
                            • Total Transaksi : ${paidToday.size} Motor
                            • Setoran CASH   : ${formatRupiah(totalCash)}
                            • Transfer Bank  : ${formatRupiah(totalTf)}
                            • QRIS / Barcode : ${formatRupiah(totalQris)}
                            • Ojol / EDC     : ${formatRupiah(totalOjol)}
                            --------------------------------
                            *GRAND TOTAL OMSET: ${formatRupiah(grandTotal)}*
                            ================================
                            Laporan closingan resmi Kasir Bengkel Qu.
                        """.trimIndent()

                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, reportText)
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Kirim Laporan Closingan"))
                        showClosinganDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SEND WA KE BOS")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClosinganDialog = false }) {
                    Text("TUTUP")
                }
            }
        )
    }
}

@Composable
private fun ClosinganRowItem(label: String, amount: Long, color: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = Color.DarkGray)
        Text(text = formatRupiah(amount), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun PaymentMethodChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else Color(0xFFF0F0F0))
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (selected) Color.White else Color.Black
        )
    }
}

@Composable
private fun KasirCustomerCard(
    service: CustomerService,
    onPayClick: () -> Unit,
    onCorrectClick: () -> Unit,
    onSendBillClick: () -> Unit,
    onPrintReceiptClick: () -> Unit,
    onSendLunasWaClick: () -> Unit
) {
    val isPaid = service.status == ServiceStatus.DIBAYAR

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Antrian & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (isPaid) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "#${service.queueNumber}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isPaid) Color(0xFF2E7D32) else Color(0xFFE65100)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = service.customerName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${service.plateNumber} • HP: ${service.phoneNumber.ifBlank { "-" }}",
                            fontSize = 11.sp,
                            color = Color.Gray
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isPaid) Color(0xFF2E7D32) else Color(0xFFF57C00))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isPaid) "LUNAS (${service.paymentMethod})" else "MENUNGGU BAYAR",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Rincian Item Singkat
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF9F9F9), RoundedCornerShape(8.dp))
                    .padding(8.dp)
            ) {
                service.items.take(3).forEach { itm ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "• ${itm.name}", fontSize = 11.sp, color = Color.DarkGray)
                        Text(text = formatRupiah(itm.price * itm.qty), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                if (service.items.size > 3) {
                    Text(text = "+ ${service.items.size - 3} item lainnya...", fontSize = 10.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tagihan
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "Mekanik: ${service.mechanicName}", fontSize = 11.sp, color = Color.Gray)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "Total: ", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Text(text = formatRupiah(service.totalAmount), fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFF1B5E20))
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            if (!isPaid) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Koreksi
                    OutlinedButton(
                        onClick = onCorrectClick,
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("KOREKSI", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    // Send Bill WA
                    OutlinedButton(
                        onClick = onSendBillClick,
                        modifier = Modifier.weight(1.2f).height(38.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF2E7D32))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SEND BILL WA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                    }

                    // Bayar
                    Button(
                        onClick = onPayClick,
                        modifier = Modifier.weight(1.3f).height(38.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B5E20))
                    ) {
                        Icon(Icons.Default.Payment, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("BAYAR", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onPrintReceiptClick,
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("CETAK STRUK", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onSendLunasWaClick,
                        modifier = Modifier.weight(1f).height(38.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("SEND WA LUNAS", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
