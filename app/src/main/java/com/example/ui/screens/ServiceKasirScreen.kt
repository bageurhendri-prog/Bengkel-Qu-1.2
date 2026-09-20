package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.CustomerService
import com.example.data.local.ServiceStatus
import com.example.ui.BengkelScreen
import com.example.ui.BengkelViewModel
import com.example.ui.components.ProFeatureBadge
import com.example.ui.components.ProUpgradeDialog
import com.example.util.FeatureGate

@Composable
fun ServiceKasirScreen(viewModel: BengkelViewModel) {
    val context = LocalContext.current
    val isPro by viewModel.isProUser.collectAsStateWithLifecycle()
    val activeServices by viewModel.activeServices.collectAsStateWithLifecycle()
    val completedServices by viewModel.completedServices.collectAsStateWithLifecycle()
    var showNewCustomerDialog by remember { mutableStateOf(false) }

    var customerDateFilter by remember { mutableStateOf("Semua Data") }
    val customerFilterOptions = listOf("Hari Ini", "7 Hari Terakhir", "Bulan Ini", "Semua Data")

    val allServices = remember(activeServices, completedServices) {
        activeServices + completedServices
    }

    val filteredCustomerServices = remember(allServices, customerDateFilter) {
        val now = System.currentTimeMillis()
        val cal = java.util.Calendar.getInstance()
        when (customerDateFilter) {
            "Hari Ini" -> {
                cal.timeInMillis = now
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                allServices.filter { it.dateEpoch >= cal.timeInMillis }
            }
            "7 Hari Terakhir" -> {
                val sevenDaysAgo = now - (7L * 24 * 60 * 60 * 1000)
                allServices.filter { it.dateEpoch >= sevenDaysAgo }
            }
            "Bulan Ini" -> {
                cal.timeInMillis = now
                cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
                cal.set(java.util.Calendar.HOUR_OF_DAY, 0)
                cal.set(java.util.Calendar.MINUTE, 0)
                cal.set(java.util.Calendar.SECOND, 0)
                allServices.filter { it.dateEpoch >= cal.timeInMillis }
            }
            else -> allServices
        }
    }

    // Pro Dialog States
    var showProUpgradeDialog by remember { mutableStateOf(false) }
    var proGateTitle by remember { mutableStateOf("") }
    var proGateReason by remember { mutableStateOf("") }

    val totalTodayServices = activeServices.size + completedServices.size

    Scaffold(
        topBar = {
            BengkelTopBar(
                title = "SERVICE & KASIR",
                onBack = { viewModel.navigateTo(BengkelScreen.DASHBOARD) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // New Customer Big Action Button (as on diagram: "NEW CUSTOMER")
            Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 6.dp)) {
                Button(
                    onClick = {
                        showNewCustomerDialog = true
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "NEW CUSTOMER",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            // Sub-modules shortcut row (Setoran & Pengeluaran as in diagram)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = { viewModel.navigateTo(BengkelScreen.SETORAN) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF388E3C),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("SETORAN", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { viewModel.navigateTo(BengkelScreen.PENGELUARAN) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFD84315),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("PENGELUARAN", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // EKSPOR DETAIL CUSTOMER (EXCEL & WA FORMAT) DENGAN FILTER TANGGAL
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DETAIL CUSTOMER ($customerDateFilter)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${filteredCustomerServices.size} Pelanggan",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(customerFilterOptions) { opt ->
                            FilterChip(
                                selected = customerDateFilter == opt,
                                onClick = { customerDateFilter = opt },
                                label = { Text(opt, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.exportCustomerDetailExcel(
                                    context = context,
                                    filterLabel = customerDateFilter,
                                    list = filteredCustomerServices,
                                    sendViaWhatsApp = false
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SIMPAN EXCEL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                viewModel.exportCustomerDetailExcel(
                                    context = context,
                                    filterLabel = customerDateFilter,
                                    list = filteredCustomerServices,
                                    sendViaWhatsApp = true
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366))
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SEND WA EXCEL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Antrian List Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "DAFTAR ANTRIAN AKTIF (${activeServices.size})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Pilih untuk buka nota",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (activeServices.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Engineering,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Tidak ada antrian servis saat ini",
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(activeServices) { item ->
                        CustomerQueueCard(
                            service = item,
                            onClick = { viewModel.selectCustomerService(item) }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }

    // Modal: NEW CUSTOMER (matching diagram fields: Nomor Antrian Otomatis, Nama, No HP, Plat Nomor, Keterangan, Kirim WA)
    if (showNewCustomerDialog) {
        var name by remember { mutableStateOf("") }
        var phone by remember { mutableStateOf("") }
        var plate by remember { mutableStateOf("") }
        var notes by remember { mutableStateOf("") }
        var sendWhatsApp by remember { mutableStateOf(true) }

        val nextQueue = (activeServices.maxOfOrNull { it.queueNumber } ?: 18) + 1

        AlertDialog(
            onDismissRequest = { showNewCustomerDialog = false },
            title = {
                Text(
                    text = "NEW CUSTOMER",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            },
            text = {
                Column {
                    // NOMOR ANTRIAN OTOMATIS banner
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "NOMOR ANTRIAN OTOMATIS: #$nextQueue",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("NAMA") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("NO HP") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = plate,
                        onValueChange = { plate = it },
                        label = { Text("PLAT NOMOR (misal: D 1234 BQ)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("KETERANGAN / KELUHAN") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { sendWhatsApp = !sendWhatsApp }
                    ) {
                        Checkbox(checked = sendWhatsApp, onCheckedChange = { sendWhatsApp = it })
                        Text(text = "KIRIM WA NOTIFIKASI", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addNewCustomer(
                            name = name,
                            phone = phone,
                            plate = plate,
                            notes = notes,
                            context = context,
                            sendWhatsApp = sendWhatsApp
                        )
                        showNewCustomerDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("SIMPAN ANTRIAN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewCustomerDialog = false }) {
                    Text("BATAL")
                }
            }
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
fun CustomerQueueCard(
    service: CustomerService,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Queue number badge (e.g. 16, 17, 18)
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${service.queueNumber}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${service.queueNumber}. ${service.customerName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (service.status == ServiceStatus.PROSES) Color(0xFFFFF3E0)
                                else Color(0xFFE8F5E9)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = service.status.name,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (service.status == ServiceStatus.PROSES) Color(0xFFE65100) else Color(0xFF2E7D32)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Plat: ${service.plateNumber} | Mekanik: ${service.mechanicName}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (service.notes.isNotBlank()) {
                    Text(
                        text = "Ket: ${service.notes}",
                        fontSize = 11.sp,
                        color = Color(0xFF757575)
                    )
                }

                Text(
                    text = "Total: ${formatRupiah(service.totalAmount)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color.Gray
            )
        }
    }
}
