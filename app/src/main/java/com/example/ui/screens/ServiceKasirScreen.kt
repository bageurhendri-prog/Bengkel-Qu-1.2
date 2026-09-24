package com.example.ui.screens

import android.content.Intent
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
import androidx.compose.material.icons.filled.Print
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
import androidx.compose.material3.HorizontalDivider
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
import com.example.data.local.ServiceItemDetail
import com.example.data.local.ServiceStatus
import com.example.ui.BengkelScreen
import com.example.ui.BengkelViewModel
import com.example.util.FeatureGate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ServiceKasirScreen(viewModel: BengkelViewModel) {
    val context = LocalContext.current
    val isPro by viewModel.isProUser.collectAsStateWithLifecycle()
    val activeServices by viewModel.activeServices.collectAsStateWithLifecycle()
    val completedServices by viewModel.completedServices.collectAsStateWithLifecycle()
    val workshopProfile by viewModel.workshopProfile.collectAsStateWithLifecycle()
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

            Spacer(modifier = Modifier.height(10.dp))

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
        var printMechanicSpk by remember { mutableStateOf(true) }
        var showSpkPreviewDialog by remember { mutableStateOf(false) }
        var savedCustomerForSpk by remember { mutableStateOf<CustomerService?>(null) }

        val nextQueue = (activeServices.maxOfOrNull { it.queueNumber } ?: 0) + 1

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
                        label = { Text("NAMA PELANGGAN") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("NO WHATSAPP PELANGGAN") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = plate,
                        onValueChange = { plate = it },
                        label = { Text("PLAT NOMOR MOTOR") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("KELUHAN / INSTRUKSI SERVIS") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { sendWhatsApp = !sendWhatsApp }
                    ) {
                        Checkbox(checked = sendWhatsApp, onCheckedChange = { sendWhatsApp = it })
                        Text(text = "SEND WA CUSTOMER (TIKET ANTRIAN)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { printMechanicSpk = !printMechanicSpk }
                    ) {
                        Checkbox(checked = printMechanicSpk, onCheckedChange = { printMechanicSpk = it })
                        Text(text = "PRINT BUAT MEKANIK (SPK KERJA)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val created = CustomerService(
                            queueNumber = nextQueue,
                            customerName = name.ifBlank { "Pelanggan #$nextQueue" },
                            phoneNumber = phone,
                            plateNumber = plate,
                            notes = notes,
                            mechanicName = "MEKANIK",
                            status = ServiceStatus.ANTRIAN,
                            items = listOf(ServiceItemDetail(name = "Jasa Pengecekan / Servis", price = 35000L, isPart = false)),
                            totalAmount = 35000L
                        )

                        viewModel.addNewCustomer(
                            name = name,
                            phone = phone,
                            plate = plate,
                            notes = notes,
                            context = context,
                            sendWhatsApp = sendWhatsApp
                        )

                        showNewCustomerDialog = false

                        if (printMechanicSpk) {
                            savedCustomerForSpk = created
                            showSpkPreviewDialog = true
                        }
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

        // DIALOG CETAK SPK BUAT MEKANIK
        if (showSpkPreviewDialog && savedCustomerForSpk != null) {
            val spk = savedCustomerForSpk!!
            val profile = workshopProfile
            val timeNow = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())

            AlertDialog(
                onDismissRequest = { showSpkPreviewDialog = false },
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Print, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SPK KERJA MEKANIK", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFFFFDE7), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFFFD54F), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(text = "SURAT PERINTAH KERJA (SPK)", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = Color(0xFFE65100))
                        Text(text = "Bengkel: ${profile?.workshopName?.ifBlank { "Bengkel Saya" } ?: "Bengkel Saya"}", fontSize = 11.sp, color = Color.DarkGray)
                        Text(text = "Waktu Masuk: $timeNow", fontSize = 10.sp, color = Color.Gray)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        Text(text = "NO. ANTRIAN: #${spk.queueNumber}", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color.Red)
                        Text(text = "Nama Pelanggan : ${spk.customerName}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        Text(text = "No. HP Pelanggan: ${spk.phoneNumber.ifBlank { "-" }}", fontSize = 12.sp)
                        Text(text = "Plat Nomor     : ${spk.plateNumber.ifBlank { "-" }}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text(text = "Keluhan/Kendala: ${spk.notes.ifBlank { "Pengecekan umum" }}", fontSize = 12.sp)
                        HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                        Text(text = "Tanda Tangan Mekanik: ______________", fontSize = 11.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            try {
                                val printText = """
                                    ================================
                                    SPK KERJA MEKANIK
                                    ${profile?.workshopName?.ifBlank { "Bengkel Saya" } ?: "Bengkel Saya"}
                                    Waktu: $timeNow
                                    --------------------------------
                                    NO. ANTRIAN : #${spk.queueNumber}
                                    PELANGGAN   : ${spk.customerName}
                                    NO. HP      : ${spk.phoneNumber}
                                    PLAT MOTOR  : ${spk.plateNumber}
                                    KELUHAN     : ${spk.notes}
                                    --------------------------------
                                    Instruksi: Cek kondisi & pastikan
                                    part diambil dari stok resmi.
                                    ================================
                                """.trimIndent()

                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, printText)
                                }
                                context.startActivity(Intent.createChooser(shareIntent, "Cetak / Bagikan SPK Mekanik"))
                            } catch (e: Exception) {
                                Toast.makeText(context, "Mencetak SPK Mekanik...", Toast.LENGTH_SHORT).show()
                            }
                            showSpkPreviewDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("CETAK SPK")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showSpkPreviewDialog = false }) {
                        Text("TUTUP")
                    }
                }
            )
        }
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
