package com.example.ui.screens

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import com.example.data.local.StockItem
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.BengkelScreen
import com.example.ui.BengkelViewModel
import com.example.ui.components.BarcodeScannerDialog
import com.example.util.FeatureGate

@Composable
fun ServiceDetailScreen(viewModel: BengkelViewModel) {
    val context = LocalContext.current
    val isPro by viewModel.isProUser.collectAsStateWithLifecycle()
    val service by viewModel.selectedCustomerService.collectAsStateWithLifecycle()
    val allStocks by viewModel.allStockItems.collectAsStateWithLifecycle()
    val staffMembers by viewModel.staffMembers.collectAsStateWithLifecycle()

    var showAddPartDialog by remember { mutableStateOf(false) }
    var showAddServiceDialog by remember { mutableStateOf(false) }
    var showEditNotesDialog by remember { mutableStateOf(false) }
    var showPrintPdfDialog by remember { mutableStateOf(false) }
    var showBarcodeScanner by remember { mutableStateOf(false) }

    if (service == null) {
        viewModel.navigateTo(BengkelScreen.SERVICE_QUEUE)
        return
    }

    val currentService = service!!

    Scaffold(
        topBar = {
            BengkelTopBar(
                title = "NOTA: #${currentService.queueNumber} ${currentService.customerName.uppercase()}",
                onBack = { viewModel.navigateTo(BengkelScreen.SERVICE_QUEUE) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Customer Header Card (matching diagram: "16. Budi")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${currentService.queueNumber}. ${currentService.customerName}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = currentService.status.name,
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(text = "No. HP: ${currentService.phoneNumber}", fontSize = 13.sp)
                    Text(text = "Plat Nomor: ${currentService.plateNumber}", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "Ket: ${currentService.notes.ifBlank { "-" }}",
                        fontSize = 13.sp,
                        color = Color(0xFF616161)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Mekanik selector row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "MEKANIK:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        val mechanics = listOf("DAY", "PRAY", "BUL", "MAN", "YULI")
                        mechanics.forEach { mech ->
                            val isSelected = currentService.mechanicName.equals(mech, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFEEEEEE))
                                    .clickable {
                                        viewModel.updateMechanicAndDiscount(mech, currentService.discount, currentService.notes)
                                    }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = mech,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else Color.Black
                                )
                            }
                        }
                    }
                }
            }

            // SECTION: TAMBAH BELANJA / RINCIAN SPAREPART (matching diagram)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TAMBAH BELANJA (SPAREPART)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Button(
                                onClick = {
                                    showBarcodeScanner = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("SCAN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { showAddPartDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("+ BELANJA", fontSize = 11.sp)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val spareparts = currentService.items.filter { it.isPart }
                    if (spareparts.isEmpty()) {
                        Text(
                            text = "Belum ada sparepart yang ditambahkan.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )
                    } else {
                        spareparts.forEachIndexed { _, part ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "• ${part.name}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = formatRupiah(part.price * part.qty),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                IconButton(
                                    onClick = {
                                        val idx = currentService.items.indexOf(part)
                                        viewModel.removeServiceItemFromCustomer(idx)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Hapus",
                                        tint = Color.Red.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // SECTION: JASA SERVICE (matching diagram: "service ringan 35000")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "JASA SERVIS",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        OutlinedButton(
                            onClick = { showAddServiceDialog = true },
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("+ JASA", fontSize = 11.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    val services = currentService.items.filter { !it.isPart }
                    if (services.isEmpty()) {
                        Text(
                            text = "Belum ada jasa servis yang ditambahkan.",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    } else {
                        services.forEachIndexed { _, svc ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "• ${svc.name}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = formatRupiah(svc.price * svc.qty),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                IconButton(
                                    onClick = {
                                        val idx = currentService.items.indexOf(svc)
                                        viewModel.removeServiceItemFromCustomer(idx)
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Hapus",
                                        tint = Color.Red.copy(alpha = 0.7f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                    // Discount row (matching diagram: "discount 0")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "discount", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (currentService.discount > 0) "-${formatRupiah(currentService.discount)}" else "0",
                                fontSize = 14.sp,
                                color = if (currentService.discount > 0) Color.Red else Color.Gray
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            TextButton(onClick = { showEditNotesDialog = true }) {
                                Text("Ubah", fontSize = 11.sp)
                            }
                        }
                    }

                    // TOTAL TAGIHAN (matching diagram: "total tagihan 120000")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE8F5E9))
                            .border(1.dp, Color(0xFF4CAF50), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "total tagihan",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = Color(0xFF1B5E20)
                            )
                            Text(
                                text = formatRupiah(currentService.totalAmount),
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                color = Color(0xFF1B5E20)
                            )
                        }
                    }
                }
            }

            // ACTION BUTTONS (Sesuai Permintaan: Bayar diganti SELESAI AUTO BILL SEND WA & MASUK KASIR)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Tombol SELESAI (KIRIM BILL WA & MASUK KASIR)
                Button(
                    onClick = { viewModel.finishServiceAndSendToKasir(context) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1B5E20),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "SELESAI (KIRIM BILL WA & MASUK KASIR)",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Button CETAK PDF
                    Button(
                        onClick = { showPrintPdfDialog = true },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF37474F),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "CETAK PDF", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    // Button KIRIM WA TAGIHAN
                    Button(
                        onClick = { viewModel.sendBillViaWhatsApp(context, currentService) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E7D32),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "KIRIM WA", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Modal: Tambah Belanja Sparepart (HARUS DARI STOK SPAREPART, TIDAK BOLEH INPUT BEBAS/STOK KOSONG)
    if (showAddPartDialog) {
        var searchQuery by remember { mutableStateOf("") }
        var selectedStockItem by remember { mutableStateOf<StockItem?>(null) }

        val filteredStocks = allStocks.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.brand.contains(searchQuery, ignoreCase = true) ||
            it.barcode.contains(searchQuery, ignoreCase = true)
        }

        AlertDialog(
            onDismissRequest = { showAddPartDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Inventory2, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("TAMBAH BELANJA DARI STOK", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Sparepart wajib terdaftar di katalog & memiliki sisa stok fisik.",
                        fontSize = 11.sp,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // Input Cari Barang di Stok
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari nama part / barcode / brand...", fontSize = 12.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    if (filteredStocks.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFFFF3E0), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Barang tidak ditemukan di stok!",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFFE65100)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Silakan tambah barang baru atau terima barang datang di menu Stok.",
                                    fontSize = 10.sp,
                                    color = Color.DarkGray
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 260.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredStocks) { stock ->
                                val isSelected = selectedStockItem?.id == stock.id
                                val isOutOfStock = stock.qty <= 0

                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = !isOutOfStock) {
                                            selectedStockItem = stock
                                        },
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = when {
                                            isOutOfStock -> Color(0xFFFFEBEE)
                                            isSelected -> MaterialTheme.colorScheme.primaryContainer
                                            else -> Color(0xFFF5F5F5)
                                        }
                                    ),
                                    border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = stock.name,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isOutOfStock) Color(0xFFC62828) else Color.Black
                                            )
                                            Text(
                                                text = "${stock.brand} • ${stock.quality}",
                                                fontSize = 10.sp,
                                                color = Color.Gray
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = formatRupiah(stock.sellPrice),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFF1B5E20)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(if (isOutOfStock) Color(0xFFD32F2F) else Color(0xFF388E3C))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = if (isOutOfStock) "STOK KOSONG" else "Stok: ${stock.qty}",
                                                    color = Color.White,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Shortcut ke menu Stok jika stok kosong
                    OutlinedButton(
                        onClick = {
                            showAddPartDialog = false
                            viewModel.navigateTo(BengkelScreen.STOK)
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth().height(36.dp)
                    ) {
                        Icon(Icons.Default.AddBusiness, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("TAMBAH STOK DI MENU SPAREPART", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val item = selectedStockItem
                        if (item != null) {
                            if (item.qty <= 0) {
                                Toast.makeText(context, "Stok barang ini kosong! Tambah stok di menu Sparepart terlebih dahulu.", Toast.LENGTH_LONG).show()
                            } else {
                                viewModel.addPartFromStock(currentService.id, item, context)
                                showAddPartDialog = false
                            }
                        } else {
                            Toast.makeText(context, "Pilih sparepart dari daftar stok terlebih dahulu", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("PASANG PART")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPartDialog = false }) {
                    Text("BATAL")
                }
            }
        )
    }

    // Modal: Tambah Jasa Service
    if (showAddServiceDialog) {
        val standardServices = listOf(
            "service ringan" to 35000L,
            "service lengkap" to 75000L,
            "service cvt" to 40000L,
            "tune up injeksi" to 45000L,
            "ganti oli & filter" to 15000L,
            "bongkar pasang ban" to 20000L
        )
        var svcName by remember { mutableStateOf("service cvt") }
        var svcPriceStr by remember { mutableStateOf("40000") }

        AlertDialog(
            onDismissRequest = { showAddServiceDialog = false },
            title = { Text("TAMBAH JASA SERVIS", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column {
                    Text("Pilihan Jasa Umum:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    standardServices.forEach { (name, price) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    svcName = name
                                    svcPriceStr = price.toString()
                                }
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(name, fontSize = 13.sp)
                            Text(formatRupiah(price), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = svcName,
                        onValueChange = { svcName = it },
                        label = { Text("Nama Jasa") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = svcPriceStr,
                        onValueChange = { svcPriceStr = it },
                        label = { Text("Tarif Jasa (Rp)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val price = svcPriceStr.toLongOrNull() ?: 0L
                        if (svcName.isNotBlank() && price > 0L) {
                            viewModel.addServiceItemToCustomer(
                                itemId = "svc_${System.currentTimeMillis()}",
                                name = svcName,
                                price = price,
                                isPart = false
                            )
                            showAddServiceDialog = false
                        }
                    }
                ) {
                    Text("SIMPAN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddServiceDialog = false }) {
                    Text("BATAL")
                }
            }
        )
    }

    // Modal: Edit Diskon & Keterangan
    if (showEditNotesDialog) {
        var discStr by remember { mutableStateOf(currentService.discount.toString()) }
        var notesVal by remember { mutableStateOf(currentService.notes) }

        AlertDialog(
            onDismissRequest = { showEditNotesDialog = false },
            title = { Text("UBAH DISKON & KETERANGAN", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column {
                    OutlinedTextField(
                        value = discStr,
                        onValueChange = { discStr = it },
                        label = { Text("Diskon (Rp)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = notesVal,
                        onValueChange = { notesVal = it },
                        label = { Text("Keterangan (misal: Ket chek cvt bulan Oktober)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val d = discStr.toLongOrNull() ?: 0L
                        viewModel.updateMechanicAndDiscount(currentService.mechanicName, d, notesVal)
                        showEditNotesDialog = false
                    }
                ) {
                    Text("SIMPAN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNotesDialog = false }) {
                    Text("BATAL")
                }
            }
        )
    }

    // Modal: CETAK PDF / NOTA PREVIEW
    if (showPrintPdfDialog) {
        AlertDialog(
            onDismissRequest = { showPrintPdfDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Print, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PREVIEW NOTA CETAK", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFFBFBFB))
                        .border(1.dp, Color(0xFFDDDDDD), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Text(
                        text = "================================",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color.DarkGray
                    )
                    Text(
                        text = "       BENGKEL QU MOTOR        ",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "   Jl. Otomotif No. 88, Bandung ",
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
                    Text(
                        text = "No. Antrian : #${currentService.queueNumber}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Nama        : ${currentService.customerName}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Plat        : ${currentService.plateNumber}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Mekanik     : ${currentService.mechanicName}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "Ket         : ${currentService.notes}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "--------------------------------",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color.DarkGray
                    )
                    currentService.items.forEach { item ->
                        val prefix = if (item.isPart) "P:" else "S:"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "$prefix ${item.name}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                            Text(
                                text = formatRupiah(item.price * item.qty),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                    }
                    if (currentService.discount > 0) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Diskon", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                            Text(text = "-${formatRupiah(currentService.discount)}", fontFamily = FontFamily.Monospace, fontSize = 11.sp)
                        }
                    }
                    Text(
                        text = "================================",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color.DarkGray
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "TOTAL",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formatRupiah(currentService.totalAmount),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "================================",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Terima kasih atas kunjungannya!",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "[✓ NOTA RESMI - BENGKEL QU]",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    Toast.makeText(context, "Dokumen Nota PDF siap dicetak / disimpan", Toast.LENGTH_SHORT).show()
                    showPrintPdfDialog = false
                }) {
                    Text("CETAK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPrintPdfDialog = false }) {
                    Text("TUTUP")
                }
            }
        )
    }

    if (showBarcodeScanner) {
        BarcodeScannerDialog(
            title = "SCAN BARCODE SPAREPART NOTA",
            registeredStocks = allStocks,
            onBarcodeScanned = { code, matchedStock ->
                if (matchedStock != null) {
                    viewModel.addServiceItemToCustomer(
                        itemId = matchedStock.id.toString(),
                        name = "${matchedStock.name} (${matchedStock.brand})",
                        price = matchedStock.sellPrice,
                        isPart = true
                    )
                    Toast.makeText(
                        context,
                        "Berhasil menambahkan ${matchedStock.name} (${formatRupiah(matchedStock.sellPrice)}) ke nota!",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        "Barcode $code belum terdaftar di stok sparepart",
                        Toast.LENGTH_LONG
                    ).show()
                }
            },
            onDismiss = { showBarcodeScanner = false }
        )
    }


}
