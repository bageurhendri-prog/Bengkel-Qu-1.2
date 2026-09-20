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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.data.local.ApprovalStatus
import com.example.data.local.StockItem
import com.example.data.local.StockStatus
import com.example.ui.BengkelScreen
import com.example.ui.BengkelViewModel
import com.example.ui.components.BarcodeScannerDialog
import com.example.ui.components.ProFeatureBadge
import com.example.ui.components.ProUpgradeDialog
import com.example.util.FeatureGate

@Composable
fun StokScreen(viewModel: BengkelViewModel) {
    val context = LocalContext.current
    val isPro by viewModel.isProUser.collectAsStateWithLifecycle()
    val allStocks by viewModel.allStockItems.collectAsStateWithLifecycle()
    val incomingStocks by viewModel.incomingStocks.collectAsStateWithLifecycle()
    val rejectItems by viewModel.rejectItems.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }

    var showAddStockDialog by remember { mutableStateOf(false) }
    var showAddIncomingDialog by remember { mutableStateOf(false) }
    var showAddRejectDialog by remember { mutableStateOf(false) }
    var showBarcodeScanner by remember { mutableStateOf(false) }
    var barcodeForNewItem by remember { mutableStateOf("") }

    // Pro Gate Dialog States
    var showProUpgradeDialog by remember { mutableStateOf(false) }
    var proGateTitle by remember { mutableStateOf("") }
    var proGateReason by remember { mutableStateOf("") }

    val filteredStocks = if (searchQuery.isBlank()) allStocks else {
        allStocks.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
                    it.brand.contains(searchQuery, ignoreCase = true) ||
                    it.barcode.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            BengkelTopBar(
                title = "STOK & INVENTARIS",
                onBack = { viewModel.navigateTo(BengkelScreen.DASHBOARD) },
                actions = {
                    IconButton(
                        onClick = {
                            val gate = FeatureGate.canUseBarcodeScanner(isPro)
                            if (gate is FeatureGate.GateResult.Denied) {
                                proGateTitle = gate.featureName
                                proGateReason = gate.reason
                                showProUpgradeDialog = true
                            } else {
                                barcodeForNewItem = ""
                                showBarcodeScanner = true
                            }
                        }
                    ) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan Barcode", tint = Color.White)
                    }
                    IconButton(
                        onClick = {
                            val gate = FeatureGate.canAddStock(allStocks.size, isPro)
                            if (gate is FeatureGate.GateResult.Denied) {
                                proGateTitle = gate.featureName
                                proGateReason = gate.reason
                                showProUpgradeDialog = true
                            } else {
                                barcodeForNewItem = ""
                                showAddStockDialog = true
                            }
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah Barang", tint = Color.White)
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Search Bar & Scan Barcode (matching diagram: "CARI")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Cari Barang / Barcode...") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Cancel, contentDescription = "Clear", tint = Color.Gray)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = {
                        val gate = FeatureGate.canUseBarcodeScanner(isPro)
                        if (gate is FeatureGate.GateResult.Denied) {
                            proGateTitle = gate.featureName
                            proGateReason = gate.reason
                            showProUpgradeDialog = true
                        } else {
                            barcodeForNewItem = ""
                            showBarcodeScanner = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(54.dp)
                ) {
                    Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("SCAN", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            // Quota / Tier Status Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Kapasitas Katalog: ",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = if (isPro) "${allStocks.size} (Unlimited PRO)" else "${allStocks.size} / 50 (Reguler)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (allStocks.size >= 50 && !isPro) Color.Red else MaterialTheme.colorScheme.primary
                    )
                }
                ProFeatureBadge(
                    isPro = isPro,
                    onClick = {
                        if (!isPro) {
                            proGateTitle = "Fitur Bengkel Qu PRO"
                            proGateReason = "Tingkatkan ke PRO untuk mengelola katalog sparepart tanpa batasan 50 item dan scan barcode kamera otomatis."
                            showProUpgradeDialog = true
                        }
                    }
                )
            }
            Spacer(modifier = Modifier.height(4.dp))

            // Tab Row: DAFTAR STOK | BARANG DATANG | PENGAJUAN REJECT (matching diagram)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("STOK (${allStocks.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("BARANG DATANG", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("PENGAJUAN REJECT", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
            }

            when (selectedTab) {
                0 -> {
                    // STOK LIST (matching diagram: NAMA BARANG, BRAND, kualitas, STATUS: JUAL PUTUS/KONSINYASI, HARGA MODAL, HARGA JUAL, QTY)
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "KATALOG SPAREPART",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                OutlinedButton(
                                    onClick = { showAddStockDialog = true },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("+ BARANG", fontSize = 11.sp)
                                }
                            }
                        }

                        items(filteredStocks) { item ->
                            StockItemCard(item)
                        }
                    }
                }
                1 -> {
                    // BARANG DATANG (matching diagram: KOMSTIR 12, KLAKSON 6, OLI SHEL 12 + TAMBAH)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PENERIMAAN BARANG DATANG",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Button(
                                onClick = { showAddIncomingDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("TAMBAH", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(incomingStocks) { incoming ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.LocalShipping,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = incoming.itemName,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp
                                                )
                                                Text(
                                                    text = "Jumlah: ${incoming.qty} Unit",
                                                    fontSize = 13.sp,
                                                    color = Color.DarkGray
                                                )
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (incoming.status == ApprovalStatus.DISETUJUI) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = incoming.status.name,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (incoming.status == ApprovalStatus.DISETUJUI) Color(0xFF2E7D32) else Color(0xFFE65100)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                2 -> {
                    // PENGAJUAN BARANG REJECT (matching diagram: OLI BOCOR 1 + AJUKAN)
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "PENGAJUAN BARANG REJECT",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Button(
                                onClick = { showAddRejectDialog = true },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("AJUKAN", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(rejectItems) { reject ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(14.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.ReportProblem,
                                                contentDescription = null,
                                                tint = Color(0xFFD32F2F)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column {
                                                Text(
                                                    text = reject.itemName,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp
                                                )
                                                Text(
                                                    text = "Alasan: ${reject.reason} (${reject.qty} Pcs)",
                                                    fontSize = 13.sp,
                                                    color = Color.DarkGray
                                                )
                                            }
                                        }

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(
                                                    if (reject.status == ApprovalStatus.DISETUJUI) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = reject.status.name,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (reject.status == ApprovalStatus.DISETUJUI) Color(0xFF2E7D32) else Color(0xFFC62828)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal: Tambah Stok Baru (matching all diagram fields: Nama Barang, Brand, Kualitas, Status: Jual Putus/Konsinyasi, Harga Modal, Harga Jual, Qty)
    if (showAddStockDialog) {
        var name by remember { mutableStateOf("") }
        var brand by remember { mutableStateOf("") }
        var quality by remember { mutableStateOf("Original") }
        var barcode by remember(barcodeForNewItem) { mutableStateOf(barcodeForNewItem) }
        var isKonsinyasi by remember { mutableStateOf(false) }
        var modalPriceStr by remember { mutableStateOf("") }
        var sellPriceStr by remember { mutableStateOf("") }
        var qtyStr by remember { mutableStateOf("10") }

        AlertDialog(
            onDismissRequest = { showAddStockDialog = false },
            title = { Text("TAMBAH ITEM SPAREPART", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("NAMA BARANG") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = brand,
                        onValueChange = { brand = it },
                        label = { Text("BRAND (misal: Shell, Aspira, AHM)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = quality,
                        onValueChange = { quality = it },
                        label = { Text("KUALITAS (Original, OEM, Aftermarket)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    // Barcode field + Scan Button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = barcode,
                            onValueChange = { barcode = it },
                            label = { Text("KODE BARCODE (OPSIONAL)") },
                            placeholder = { Text("Contoh: 8991234567") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = {
                                showBarcodeScanner = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SCAN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // STATUS: JUAL PUTUS / KONSINYASI (matching diagram)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { isKonsinyasi = false },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (!isKonsinyasi) MaterialTheme.colorScheme.primary else Color(0xFFE0E0E0),
                                contentColor = if (!isKonsinyasi) Color.White else Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("JUAL PUTUS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { isKonsinyasi = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isKonsinyasi) MaterialTheme.colorScheme.primary else Color(0xFFE0E0E0),
                                contentColor = if (isKonsinyasi) Color.White else Color.Black
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("KONSINYASI", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = modalPriceStr,
                            onValueChange = { modalPriceStr = it },
                            label = { Text("HARGA MODAL") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = sellPriceStr,
                            onValueChange = { sellPriceStr = it },
                            label = { Text("HARGA JUAL") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = qtyStr,
                        onValueChange = { qtyStr = it },
                        label = { Text("QTY / JUMLAH STOK") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val modal = modalPriceStr.toLongOrNull() ?: 0L
                        val jual = sellPriceStr.toLongOrNull() ?: 0L
                        val qty = qtyStr.toIntOrNull() ?: 0
                        if (name.isNotBlank() && jual > 0L) {
                            viewModel.addNewStockItem(
                                name = name,
                                brand = brand.ifBlank { "Generic" },
                                quality = quality,
                                status = if (isKonsinyasi) StockStatus.KONSINYASI else StockStatus.JUAL_PUTUS,
                                modal = modal,
                                jual = jual,
                                qty = qty,
                                barcode = barcode,
                                context = context
                            )
                            barcodeForNewItem = ""
                            showAddStockDialog = false
                        } else {
                            Toast.makeText(context, "Lengkapi data dengan benar", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("SIMPAN")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    barcodeForNewItem = ""
                    showAddStockDialog = false
                }) {
                    Text("BATAL")
                }
            }
        )
    }

    // Modal: Tambah Barang Datang (matching diagram: "BARANG DATANG" + "TAMBAH")
    if (showAddIncomingDialog) {
        var inName by remember { mutableStateOf("KOMSTIR") }
        var inQtyStr by remember { mutableStateOf("12") }

        AlertDialog(
            onDismissRequest = { showAddIncomingDialog = false },
            title = { Text("CATAT BARANG DATANG", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column {
                    OutlinedTextField(
                        value = inName,
                        onValueChange = { inName = it },
                        label = { Text("Nama Barang (misal: KOMSTIR, KLAKSON, OLI SHEL)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = inQtyStr,
                        onValueChange = { inQtyStr = it },
                        label = { Text("Jumlah Qty") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val q = inQtyStr.toIntOrNull() ?: 0
                        if (inName.isNotBlank() && q > 0) {
                            viewModel.addIncomingStock(inName, q, context)
                            showAddIncomingDialog = false
                        }
                    }
                ) {
                    Text("TAMBAH")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddIncomingDialog = false }) {
                    Text("BATAL")
                }
            }
        )
    }

    // Modal: Pengajuan Barang Reject (matching diagram: "PENGAJUAN BARANG REJECT" + "AJUKAN")
    if (showAddRejectDialog) {
        var rejName by remember { mutableStateOf("OLI SHEL") }
        var rejReason by remember { mutableStateOf("BOCOR") }
        var rejQtyStr by remember { mutableStateOf("1") }

        AlertDialog(
            onDismissRequest = { showAddRejectDialog = false },
            title = { Text("AJUKAN BARANG REJECT", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column {
                    OutlinedTextField(
                        value = rejName,
                        onValueChange = { rejName = it },
                        label = { Text("Nama Barang (misal: OLI SHEL)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rejReason,
                        onValueChange = { rejReason = it },
                        label = { Text("Alasan Reject (misal: BOCOR, PECAH, RUSAK PABRIK)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rejQtyStr,
                        onValueChange = { rejQtyStr = it },
                        label = { Text("Jumlah (Pcs)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val q = rejQtyStr.toIntOrNull() ?: 1
                        if (rejName.isNotBlank() && rejReason.isNotBlank()) {
                            viewModel.addRejectItem(rejName, rejReason, q, context)
                            showAddRejectDialog = false
                        }
                    }
                ) {
                    Text("AJUKAN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddRejectDialog = false }) {
                    Text("BATAL")
                }
            }
        )
    }

    if (showBarcodeScanner) {
        BarcodeScannerDialog(
            title = if (showAddStockDialog) "SCAN BARCODE UNTUK ITEM BARU" else "CARI SPAREPART VIA BARCODE",
            registeredStocks = allStocks,
            onBarcodeScanned = { code, matchedStock ->
                if (showAddStockDialog) {
                    barcodeForNewItem = code
                    Toast.makeText(context, "Barcode $code berhasil dipindai!", Toast.LENGTH_SHORT).show()
                } else {
                    searchQuery = code
                    if (matchedStock != null) {
                        Toast.makeText(context, "Ditemukan: ${matchedStock.name} (Stok: ${matchedStock.qty})", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Barcode $code belum ada di katalog stok", Toast.LENGTH_LONG).show()
                    }
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

@Composable
fun StockItemCard(item: StockItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = item.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Brand: ${item.brand} | Kualitas: ${item.quality}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (item.barcode.isNotBlank()) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.QrCodeScanner,
                                contentDescription = null,
                                modifier = Modifier.size(13.dp),
                                tint = Color(0xFF1976D2)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = item.barcode,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF1976D2),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (item.status == StockStatus.JUAL_PUTUS) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (item.status == StockStatus.JUAL_PUTUS) "JUAL PUTUS" else "KONSINYASI",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.status == StockStatus.JUAL_PUTUS) Color(0xFF2E7D32) else Color(0xFFE65100)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(text = "Modal: ${formatRupiah(item.modalPrice)}", fontSize = 12.sp, color = Color.Gray)
                    Text(text = "Jual: ${formatRupiah(item.sellPrice)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (item.qty <= 3) Color(0xFFFFEBEE) else Color(0xFFE8F5E9))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "Qty: ${item.qty} pcs",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (item.qty <= 3) Color(0xFFC62828) else Color(0xFF2E7D32)
                    )
                }
            }
        }
    }
}
