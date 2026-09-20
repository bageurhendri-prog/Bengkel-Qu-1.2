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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EventNote
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.AttendanceRecord
import com.example.data.local.AttendanceStatus
import com.example.ui.BengkelScreen
import com.example.ui.BengkelViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun AbsenScreen(viewModel: BengkelViewModel) {
    val context = LocalContext.current
    val attendanceList by viewModel.attendanceList.collectAsStateWithLifecycle()
    val allAttendanceRecords by viewModel.allAttendanceRecords.collectAsStateWithLifecycle()

    var showAddStaffAttendanceDialog by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Hari Ini, 1: Rekap Bulanan

    val todayFormatted = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID")).format(Date())

    // Month options for filter
    val currentYear = Calendar.getInstance().get(Calendar.YEAR)
    val months = listOf(
        "Semua Bulan" to "",
        "Januari $currentYear" to "$currentYear-01",
        "Februari $currentYear" to "$currentYear-02",
        "Maret $currentYear" to "$currentYear-03",
        "April $currentYear" to "$currentYear-04",
        "Mei $currentYear" to "$currentYear-05",
        "Juni $currentYear" to "$currentYear-06",
        "Juli $currentYear" to "$currentYear-07",
        "Agustus $currentYear" to "$currentYear-08",
        "September $currentYear" to "$currentYear-09",
        "Oktober $currentYear" to "$currentYear-10",
        "November $currentYear" to "$currentYear-11",
        "Desember $currentYear" to "$currentYear-12"
    )

    val currentMonthPattern = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    var selectedMonthPair by remember {
        mutableStateOf(months.firstOrNull { it.second == currentMonthPattern } ?: months[0])
    }

    val filteredMonthlyRecords = remember(allAttendanceRecords, selectedMonthPair) {
        if (selectedMonthPair.second.isBlank()) {
            allAttendanceRecords
        } else {
            allAttendanceRecords.filter { it.dateString.startsWith(selectedMonthPair.second) }
        }
    }

    Scaffold(
        topBar = {
            BengkelTopBar(
                title = "ABSEN STAFF & MEKANIK",
                onBack = { viewModel.navigateTo(BengkelScreen.DASHBOARD) },
                actions = {
                    IconButton(onClick = { showAddStaffAttendanceDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah Absen", tint = Color.White)
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
            // Tabs: PRESENSI HARI INI vs REKAP BULANAN
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("HARI INI (${attendanceList.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("REKAP BULANAN", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                )
            }

            // Export Bar (Save Excel / Send WA)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    // Month Selector Chips
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("PILIH BULAN EKSPOR EXCEL:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(months) { pair ->
                            FilterChip(
                                selected = selectedMonthPair == pair,
                                onClick = { selectedMonthPair = pair },
                                label = { Text(pair.first, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Buttons: Simpan Excel & Kirim WA
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val listToExport = if (selectedTab == 0) attendanceList else filteredMonthlyRecords
                                viewModel.exportAttendanceExcel(
                                    context = context,
                                    monthLabel = selectedMonthPair.first,
                                    list = listToExport,
                                    sendViaWhatsApp = false
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SIMPAN EXCEL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                val listToExport = if (selectedTab == 0) attendanceList else filteredMonthlyRecords
                                viewModel.exportAttendanceExcel(
                                    context = context,
                                    monthLabel = selectedMonthPair.first,
                                    list = listToExport,
                                    sendViaWhatsApp = true
                                )
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
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

            if (selectedTab == 0) {
                // PRESENSI HARI INI
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        // Date Banner Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF2E7D32)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.EventNote,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "PRESENSI HARI INI",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color(0xFF1B5E20)
                                    )
                                    Text(
                                        text = todayFormatted,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = Color(0xFF2E7D32)
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "STATUS KEHADIRAN MEKANIK & STAFF",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (attendanceList.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Belum ada data presensi hari ini. Tekan (+) untuk menambah.", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    } else {
                        items(attendanceList) { record ->
                            AttendanceCard(
                                record = record,
                                onStatusChange = { newStatus ->
                                    viewModel.updateStaffAttendance(record.id, newStatus)
                                }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            } else {
                // REKAP BULANAN
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text(
                            text = "RIWAYAT PRESENSI: ${selectedMonthPair.first} (${filteredMonthlyRecords.size} Catatan)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (filteredMonthlyRecords.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Tidak ada catatan presensi pada bulan yang dipilih.", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                    } else {
                        items(filteredMonthlyRecords) { record ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = record.staffName,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = "Tgl: ${record.dateString} | Masuk: ${record.timeCheckIn}",
                                            fontSize = 11.sp,
                                            color = Color.Gray
                                        )
                                        if (record.notes.isNotBlank()) {
                                            Text(
                                                text = "Catatan: ${record.notes}",
                                                fontSize = 11.sp,
                                                color = Color.DarkGray
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                when (record.status) {
                                                    AttendanceStatus.MASUK -> Color(0xFFE8F5E9)
                                                    AttendanceStatus.IZIN -> Color(0xFFFFF3E0)
                                                    AttendanceStatus.SAKIT -> Color(0xFFFFEBEE)
                                                    AttendanceStatus.ALPA -> Color(0xFFFFCDD2)
                                                }
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = record.status.name,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when (record.status) {
                                                AttendanceStatus.MASUK -> Color(0xFF2E7D32)
                                                AttendanceStatus.IZIN -> Color(0xFFE65100)
                                                AttendanceStatus.SAKIT -> Color(0xFFC62828)
                                                AttendanceStatus.ALPA -> Color(0xFFB71C1C)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(20.dp))
                    }
                }
            }
        }
    }

    if (showAddStaffAttendanceDialog) {
        var name by remember { mutableStateOf("") }
        var status by remember { mutableStateOf(AttendanceStatus.MASUK) }

        AlertDialog(
            onDismissRequest = { showAddStaffAttendanceDialog = false },
            title = { Text("TAMBAH PRESENSI STAFF", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nama Staff / Mekanik") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Pilih Status Kehadiran:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AttendanceStatus.entries.forEach { st ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (status == st) MaterialTheme.colorScheme.primary else Color(0xFFEEEEEE))
                                    .clickable { status = st }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = st.name,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (status == st) Color.White else Color.Black
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.addStaffAttendanceRecord(name, status)
                            showAddStaffAttendanceDialog = false
                        }
                    }
                ) {
                    Text("SIMPAN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStaffAttendanceDialog = false }) {
                    Text("BATAL")
                }
            }
        )
    }
}

@Composable
fun AttendanceCard(
    record: AttendanceRecord,
    onStatusChange: (AttendanceStatus) -> Unit
) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFEEEEEE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.DarkGray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        // Matching diagram format: e.g. "PRAY: MASUK"
                        Text(
                            text = "${record.staffName}: ${record.status.name}",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (record.notes.isNotBlank()) {
                            Text(
                                text = "Ket: ${record.notes}",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when (record.status) {
                                AttendanceStatus.MASUK -> Color(0xFFE8F5E9)
                                AttendanceStatus.IZIN -> Color(0xFFFFF3E0)
                                AttendanceStatus.SAKIT -> Color(0xFFFFEBEE)
                                AttendanceStatus.ALPA -> Color(0xFFFFCDD2)
                            }
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = record.status.name,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (record.status) {
                            AttendanceStatus.MASUK -> Color(0xFF2E7D32)
                            AttendanceStatus.IZIN -> Color(0xFFE65100)
                            AttendanceStatus.SAKIT -> Color(0xFFC62828)
                            AttendanceStatus.ALPA -> Color(0xFFB71C1C)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Quick Status Switcher Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                AttendanceStatus.entries.forEach { st ->
                    val isSelected = record.status == st
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFF5F5F5)
                            )
                            .clickable { onStatusChange(st) }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = st.name,
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else Color.DarkGray
                        )
                    }
                }
            }
        }
    }
}
