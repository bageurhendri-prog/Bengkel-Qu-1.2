package com.example.ui.screens

import android.widget.Toast
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
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
import com.example.ui.components.ProFeatureBadge
import com.example.ui.components.ProUpgradeDialog
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

    // Pro Dialog States
    var showProUpgradeDialog by remember { mutableStateOf(false) }
    var proGateTitle by remember { mutableStateOf("") }
    var proGateReason by remember { mutableStateOf("") }

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
                                    val gate = FeatureGate.canUseBarcodeScanner(isPro)
                                    if (gate is FeatureGate.GateResult.Denied) {
                                        proGateTitle = gate.featureName
                                        proGateReason = gate.reason
                                        showProUpgradeDialog = true
                                    } else {
                                        showBarcodeScanner = true
                                    }
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

            // ACTION BUTTONS (matching diagram: BAYAR, CETAK PDF, KIRIM WA / KIRIM TAGIHAN)
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Button BAYAR
                Button(
                    onClick = { viewModel.payAndCompleteService(context) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Payment, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "BAYAR (SELESAIKAN)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 1.sp
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

                    // Button KIRIM WA (matching diagram)
                    Button(
                        onClick = { viewModel.sendBillViaWhatsApp(context, currentService) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2E7D32),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "KIRIM WA", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Modal: Tambah Belanja Sparepart (select from stock or type new)
    if (showAddPartDialog) {
        var selectedStockName by remember { mutableStateOf("") }
        var partPriceStr by remember { mutableStateOf("") }
        var customPartName by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddPartDialog = false },
            title = { Text("TAMBAH BELANJA SPAREPART", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column {
                    Text("Pilih dari Stok:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        allStocks.take(5).forEach { stock ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedStockName = stock.name.lowercase()
                                        partPriceStr = stock.sellPrice.toString()
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedStockName == stock.name.lowercase())
                                        MaterialTheme.colorScheme.primaryContainer else Color(0xFFF5F5F5)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(stock.name, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                    Text(formatRupiah(stock.sellPrice), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Atau Tulis Sparepart Baru:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = customPartName,
                        onValueChange = {
                            customPartName = it
                            selectedStockName = it
                        },
                        label = { Text("Nama Sparepart (misal: oli shel, kanvas)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = partPriceStr,
                        onValueChange = { partPriceStr = it },
                        label = { Text("Harga (Rp)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = selectedStockName.ifBlank { customPartName }
                        val price = partPriceStr.toLongOrNull() ?: 0L
                        if (name.isNotBlank() && price > 0L) {
                            viewModel.addServiceItemToCustomer(
                                itemId = "part_${System.currentTimeMillis()}",
                                name = name,
                                price = price,
                                isPart = true
                            )
                            showAddPartDialog = false
                        } else {
                            Toast.makeText(context, "Masukkan nama dan harga valid", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("TAMBAH")
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

                    if (isPro) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "[✓ NOTA RESMI - BENGKEL QU PRO]",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF2E7D32)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "*** [VERSI REGULER - FREE WATERMARK] ***",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFD32F2F)
                        )
                        Text(
                            text = "Upgrade ke PRO untuk nota resmi tanpa watermark",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 9.sp,
                            color = Color.Gray
                        )
                    }
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

    if (showProUpgradeDialog) {
        ProUpgradeDialog(
            featureTitle = proGateTitle,
            reasonText = proGateReason,
            onDismiss = { showProUpgradeDialog = false },
            onActivateKey = { key ->
                viewModel.activateProLicense(key, context) { success, _ ->
                    if (success) {
                        showProUpgradeDialog = false
                    }
                }
            }
        )
    }
}
