package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WorkspacePremium
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
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.BengkelScreen
import com.example.ui.BengkelViewModel
import com.example.ui.components.ProFeatureBadge
import com.example.ui.components.ProFeatureComparisonDialog
import com.example.ui.components.ProUpgradeDialog
import com.example.ui.theme.AppColorTheme
import com.example.util.FeatureGate
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PengaturanScreen(viewModel: BengkelViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    val profile by viewModel.workshopProfile.collectAsStateWithLifecycle()
    val isPro by viewModel.isProUser.collectAsStateWithLifecycle()
    val staffList by viewModel.staffMembers.collectAsStateWithLifecycle()
    val activeTheme by viewModel.activeTheme.collectAsStateWithLifecycle()
    val fontSizeScale by viewModel.fontSizeScale.collectAsStateWithLifecycle()
    val allStocks by viewModel.allStockItems.collectAsStateWithLifecycle()
    val completedServices by viewModel.completedServices.collectAsStateWithLifecycle()

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showAddStaffDialog by remember { mutableStateOf(false) }

    // Pro Licensing States
    var showProComparisonDialog by remember { mutableStateOf(false) }
    var showProActivationDialog by remember { mutableStateOf(false) }

    // Backup & Restore & Reset States
    var showBackupDialog by remember { mutableStateOf(false) }
    var showRestoreDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var resetActionType by remember { mutableStateOf("TRANSAKSI") } // "TRANSAKSI" or "FACTORY"

    var backupJsonContent by remember { mutableStateOf("") }
    var restoreJsonInput by remember { mutableStateOf("") }
    var restoreParsedPayload by remember { mutableStateOf<com.example.data.local.BackupPayload?>(null) }
    var restoreValidationMsg by remember { mutableStateOf("") }

    // File saver for Backup JSON
    val backupFileSaver = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null && backupJsonContent.isNotBlank()) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { output ->
                    output.write(backupJsonContent.toByteArray())
                }
                Toast.makeText(context, "File cadangan (.json) berhasil disimpan!", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal menyimpan file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // File picker for Restore JSON
    val restoreFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val content = inputStream?.bufferedReader()?.use { it.readText() } ?: ""
                if (content.isNotBlank()) {
                    restoreJsonInput = content
                    try {
                        val payload = com.example.data.local.BackupRestoreManager.parseBackupJson(content)
                        restoreParsedPayload = payload
                        restoreValidationMsg = "File Valid: ${payload.metadata.dateFormatted} | ${payload.stockList.size} Sparepart, ${payload.serviceList.size} Servis, ${payload.staffList.size} Staff"
                    } catch (e: Exception) {
                        restoreParsedPayload = null
                        restoreValidationMsg = "Format file tidak valid: ${e.localizedMessage}"
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "Gagal membaca file: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val hasProAccess = remember(profile) {
        FeatureGate.hasProAccess(context, profile)
    }

    Scaffold(
        topBar = {
            BengkelTopBar(
                title = "PENGATURAN APLIKASI",
                onBack = { viewModel.navigateTo(BengkelScreen.DASHBOARD) }
            )
        }
    ) { padding ->
        if (!hasProAccess) {
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
                    text = "Pengaturan (Khusus PRO)",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFC62828)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Masa trial 10 hari telah selesai atau serial number PRO belum aktif. Menu Pengaturan, Kelola Staff, dan Reset Data hanya dapat diakses pada versi Bengkel Qu PRO.",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { showProActivationDialog = true },
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.background)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // SECTION: PANDUAN TEKNIS SERIAL NUMBER
                var inputDirectKey by remember { mutableStateOf("") }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F8E9)),
                    border = BorderStroke(1.dp, Color(0xFF81C784)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFF2E7D32))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PANDUAN TEKNIS SERIAL NUMBER",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF1B5E20)
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Aktivasi Serial Number sistem Langganan Bulanan PRO. Buka seluruh fitur eksklusif tanpa batasan.",
                            fontSize = 12.sp,
                            color = Color(0xFF33691E)
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Format SN: BENGKELQU-PRO-XXXX atau BQPRO-XXXX-XXXX\nHubungi Developer: 085714216556 / bageurhendri@gmail.com",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20)
                        )

                        if (!isPro) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = inputDirectKey,
                                onValueChange = { inputDirectKey = it.uppercase() },
                                label = { Text("Masukkan Serial Number PRO") },
                                placeholder = { Text("BENGKELQU-PRO-XXXX-XXXX") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Button(
                                onClick = {
                                    if (inputDirectKey.isNotBlank()) {
                                        viewModel.activateProLicense(inputDirectKey.trim(), context) { success, _ ->
                                            if (success) {
                                                inputDirectKey = ""
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("AKTIFKAN SERIAL NUMBER SEKARANG", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFF2E7D32))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("STATUS: PRO LIFETIME AKTIF", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            // SECTION: STATUS LISENSI & FITUR (REGULER vs PRO)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isPro) Color(0xFFFFFDE7) else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(1.dp, if (isPro) Color(0xFFFFD54F) else Color(0xFFCFD8DC)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.WorkspacePremium,
                                contentDescription = null,
                                tint = if (isPro) Color(0xFFF57F17) else Color(0xFF78909C)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "STATUS LISENSI APLIKASI",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (isPro) Color(0xFFE65100) else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        ProFeatureBadge(
                            isPro = isPro,
                            onClick = { showProComparisonDialog = true }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = if (isPro) {
                            "Bengkel Qu PRO Aktif (Seumur Hidup). Semua fitur dan kuota operasional berjalan tanpa batasan."
                        } else {
                            "Versi Reguler (Starter Gratis). Kuota maksimal 50 sparepart, 15 antrian/hari, dan 2 staf."
                        },
                        fontSize = 12.sp,
                        color = if (isPro) Color(0xFF5D4037) else Color.DarkGray
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Quota indicator boxes
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Stok Quota
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isPro) Color(0xFFFFF8E1) else Color(0xFFF5F5F5)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Stok Barang", fontSize = 10.sp, color = Color.Gray)
                                Text(
                                    text = if (isPro) "${allStocks.size} (Unlimited)" else "${allStocks.size} / 50",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (allStocks.size >= 50 && !isPro) Color.Red else Color.Black
                                )
                            }
                        }

                        // Antrian Quota
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isPro) Color(0xFFFFF8E1) else Color(0xFFF5F5F5)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Antrian/Hari", fontSize = 10.sp, color = Color.Gray)
                                Text(
                                    text = if (isPro) "${completedServices.size} (Unlimited)" else "${completedServices.size} / 15",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color.Black
                                )
                            }
                        }

                        // Barcode Scan Quota
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isPro) Color(0xFFFFF8E1) else Color(0xFFF5F5F5)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text("Scan Barcode", fontSize = 10.sp, color = Color.Gray)
                                Text(
                                    text = if (isPro) "Aktif (PRO)" else "Manual",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (isPro) Color(0xFF2E7D32) else Color(0xFF757575)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showProComparisonDialog = true },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Bandingkan Fitur", fontSize = 11.sp)
                        }

                        if (!isPro) {
                            Button(
                                onClick = { showProActivationDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57F17)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Aktivasi PRO", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        } else {
                            OutlinedButton(
                                onClick = { viewModel.deactivateProLicense(context) },
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Uji Mode Reguler", fontSize = 11.sp, color = Color.Gray)
                            }
                        }
                    }
                }
            }

            // SECTION: PROFIL BENGKEL (matching diagram: "PROFIL")
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Store, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "PROFIL BENGKEL",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        TextButton(onClick = { showEditProfileDialog = true }) {
                            Text("Ubah Profil", fontSize = 12.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Nama Bengkel: ${profile?.workshopName ?: "BENGKEL QU"}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(text = "Pemilik: ${profile?.ownerName ?: "Hendri"}", fontSize = 13.sp)
                    Text(text = "No. Telepon: ${profile?.phone ?: "085714216556"}", fontSize = 13.sp)
                    Text(text = "Email: ${profile?.email ?: "bageurhendri@gmail.com"}", fontSize = 13.sp)
                    Text(text = "Alamat: ${profile?.address ?: "Jl. Otomotif No. 88, Bandung"}", fontSize = 12.sp, color = Color.Gray)
                }
            }

            // SECTION: STAFF & MEKANIK (matching diagram: "STAFF")
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.People, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "STAFF & MEKANIK (${staffList.size})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        IconButton(onClick = {
                            val gate = FeatureGate.canAddStaff(staffList.size, isPro)
                            if (gate is FeatureGate.GateResult.Denied) {
                                Toast.makeText(context, gate.reason, Toast.LENGTH_LONG).show()
                                showProComparisonDialog = true
                            } else {
                                showAddStaffDialog = true
                            }
                        }) {
                            Icon(Icons.Default.Add, contentDescription = "Tambah Staff", tint = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    staffList.forEach { staff ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(30.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = staff.name.take(1),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(text = staff.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text(text = "Peran: ${staff.role}", fontSize = 11.sp, color = Color.Gray)
                                }
                            }

                            IconButton(
                                onClick = { viewModel.deleteStaff(staff) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Hapus", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }

            // SECTION: TEMA WARNA UI (matching diagram: "SETTING WARNA UI")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SETTING WARNA UI",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        ThemeColorTile(
                            name = "Hijau Qu",
                            color = Color(0xFF2E7D32),
                            isSelected = activeTheme == AppColorTheme.BENGKEL_GREEN,
                            onClick = { viewModel.setColorTheme(AppColorTheme.BENGKEL_GREEN) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeColorTile(
                            name = "Emerald",
                            color = Color(0xFF00897B),
                            isSelected = activeTheme == AppColorTheme.EMERALD_RACING,
                            onClick = { viewModel.setColorTheme(AppColorTheme.EMERALD_RACING) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeColorTile(
                            name = "Teal",
                            color = Color(0xFF0097A7),
                            isSelected = activeTheme == AppColorTheme.OCEAN_TEAL,
                            onClick = { viewModel.setColorTheme(AppColorTheme.OCEAN_TEAL) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeColorTile(
                            name = "Slate Pro",
                            color = Color(0xFF455A64),
                            isSelected = activeTheme == AppColorTheme.DARK_SLATE,
                            onClick = { viewModel.setColorTheme(AppColorTheme.DARK_SLATE) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // SECTION: UKURAN HURUF (matching diagram: "SETTING HURUF")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.FormatSize, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "SETTING HURUF",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FontSizeOptionChip(
                            label = "Normal (100%)",
                            isSelected = fontSizeScale == 1.0f,
                            onClick = { viewModel.setFontSizeScale(1.0f) },
                            modifier = Modifier.weight(1f)
                        )
                        FontSizeOptionChip(
                            label = "Besar (115%)",
                            isSelected = fontSizeScale == 1.15f,
                            onClick = { viewModel.setFontSizeScale(1.15f) },
                            modifier = Modifier.weight(1f)
                        )
                        FontSizeOptionChip(
                            label = "Ekstra (130%)",
                            isSelected = fontSizeScale == 1.3f,
                            onClick = { viewModel.setFontSizeScale(1.3f) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // SECTION: MANAJEMEN DATA (BACKUP, RESTORE, RESET)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "MANAJEMEN DATA (BACKUP, RESTORE, RESET)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Amankan database bengkel Anda dengan mencadangkan atau memulihkan data sewaktu-waktu.",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Tombol Backup
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                backupJsonContent = viewModel.getBackupJson()
                                showBackupDialog = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1976D2)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("CADANGKAN DATA (BACKUP)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Tombol Restore
                    OutlinedButton(
                        onClick = {
                            restoreJsonInput = ""
                            restoreParsedPayload = null
                            restoreValidationMsg = ""
                            showRestoreDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.CloudDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("PULIHKAN DATA (RESTORE)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Tombol Reset
                    OutlinedButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFD32F2F))
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, tint = Color(0xFFD32F2F), modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("RESET DATABASE", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFD32F2F))
                    }
                }
            }
        }
    }
}

    // Modal: Ubah Profil
    if (showEditProfileDialog) {
        var bengkelName by remember { mutableStateOf(profile?.workshopName ?: "BENGKEL QU") }
        var ownerName by remember { mutableStateOf(profile?.ownerName ?: "Hendri") }
        var phone by remember { mutableStateOf(profile?.phone ?: "085714216556") }
        var email by remember { mutableStateOf(profile?.email ?: "bageurhendri@gmail.com") }
        var address by remember { mutableStateOf(profile?.address ?: "Jl. Otomotif No. 88, Bandung") }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("UBAH PROFIL BENGKEL", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column {
                    OutlinedTextField(
                        value = bengkelName,
                        onValueChange = { bengkelName = it },
                        label = { Text("Nama Bengkel") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = ownerName,
                        onValueChange = { ownerName = it },
                        label = { Text("Nama Pemilik") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Nomor HP") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Alamat") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateWorkshopProfile(bengkelName, ownerName, phone, email, address)
                        showEditProfileDialog = false
                    }
                ) {
                    Text("SIMPAN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("BATAL")
                }
            }
        )
    }

    // Modal: Tambah Staff
    if (showAddStaffDialog) {
        var sName by remember { mutableStateOf("") }
        var sRole by remember { mutableStateOf("MEKANIK") }
        var sPhone by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddStaffDialog = false },
            title = { Text("TAMBAH ANGGOTA STAFF", fontWeight = FontWeight.Bold, fontSize = 16.sp) },
            text = {
                Column {
                    OutlinedTextField(
                        value = sName,
                        onValueChange = { sName = it },
                        label = { Text("Nama Staff") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = sPhone,
                        onValueChange = { sPhone = it },
                        label = { Text("Nomor HP") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Pilih Peran:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("ADMIN", "KASIR", "MEKANIK").forEach { r ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (sRole == r) MaterialTheme.colorScheme.primary else Color(0xFFEEEEEE))
                                    .clickable { sRole = r }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = r,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (sRole == r) Color.White else Color.Black
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (sName.isNotBlank()) {
                            viewModel.addNewStaff(sName, sRole, sPhone)
                            showAddStaffDialog = false
                        }
                    }
                ) {
                    Text("TAMBAH")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddStaffDialog = false }) {
                    Text("BATAL")
                }
            }
        )
    }

    // Modal: Cadangkan Data (Backup)
    if (showBackupDialog) {
        AlertDialog(
            onDismissRequest = { showBackupDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, tint = Color(0xFF1976D2))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("CADANGKAN DATABASE", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Data saat ini yang akan dicadangkan:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD)),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("• Total Sparepart: ${allStocks.size} item", fontSize = 12.sp)
                            Text("• Riwayat Servis / Nota: ${completedServices.size} transaksi", fontSize = 12.sp)
                            Text("• Karyawan Bengkel: ${staffList.size} orang", fontSize = 12.sp)
                            Text("• Format: File JSON Standar", fontSize = 11.sp, color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Opsi 1: Bagikan via WhatsApp / Drive / Bluetooth
                    Button(
                        onClick = {
                            viewModel.shareBackup(context)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("BAGIKAN (WHATSAPP / DRIVE / EMAIL)", fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Opsi 2: Simpan File ke Storage HP
                    OutlinedButton(
                        onClick = {
                            val timeTag = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
                            backupFileSaver.launch("bengkel_qu_backup_$timeTag.json")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SIMPAN FILE (.JSON) KE HP", fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Opsi 3: Salin Teks
                    OutlinedButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(backupJsonContent))
                            Toast.makeText(context, "Kode backup disalin ke papan klip!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("SALIN TEKS KODE CADANGAN", fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showBackupDialog = false }) {
                    Text("SELESAI")
                }
            }
        )
    }

    // Modal: Pulihkan Data (Restore)
    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PULIHKAN DATABASE", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column {
                    Text(
                        text = "Pilih file backup (.json) atau tempel teks cadangan untuk memulihkan seluruh data:",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            restoreFilePicker.launch("application/json")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("PILIH FILE CADANGAN (.JSON)", fontSize = 12.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = restoreJsonInput,
                        onValueChange = {
                            restoreJsonInput = it
                            if (it.isNotBlank()) {
                                try {
                                    val p = com.example.data.local.BackupRestoreManager.parseBackupJson(it)
                                    restoreParsedPayload = p
                                    restoreValidationMsg = "Data Valid: ${p.stockList.size} Sparepart, ${p.serviceList.size} Servis, ${p.staffList.size} Staf"
                                } catch (e: Exception) {
                                    restoreParsedPayload = null
                                    restoreValidationMsg = "Format belum sesuai: ${e.localizedMessage}"
                                }
                            } else {
                                restoreParsedPayload = null
                                restoreValidationMsg = ""
                            }
                        },
                        label = { Text("Atau Tempel Teks Cadangan JSON di Sini") },
                        placeholder = { Text("{\"metadata\": { ... }}") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                        maxLines = 4
                    )

                    if (restoreValidationMsg.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = restoreValidationMsg,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (restoreParsedPayload != null) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Catatan: Memulihkan database akan menimpa data yang ada saat ini dengan data dari cadangan.",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val payload = restoreParsedPayload
                        if (payload != null) {
                            viewModel.restoreBackup(context, restoreJsonInput) { success, _ ->
                                if (success) {
                                    showRestoreDialog = false
                                }
                            }
                        } else {
                            Toast.makeText(context, "Silakan masukkan file atau kode cadangan yang valid", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = restoreParsedPayload != null,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                ) {
                    Text("PULIHKAN SEKARANG")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("BATAL")
                }
            }
        )
    }

    // Modal: Pilihan Reset
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD32F2F))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("PILIH JENIS RESET", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFD32F2F))
                }
            },
            text = {
                Column {
                    Text(
                        text = "Silakan tentukan bagian data yang ingin Anda bersihkan:",
                        fontSize = 12.sp,
                        color = Color.DarkGray
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Opsi 1: Reset Transaksi Saja
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { resetActionType = "TRANSAKSI" },
                        colors = CardDefaults.cardColors(
                            containerColor = if (resetActionType == "TRANSAKSI") Color(0xFFFFF3E0) else Color(0xFFFAFAFA)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (resetActionType == "TRANSAKSI") 2.dp else 1.dp,
                            color = if (resetActionType == "TRANSAKSI") Color(0xFFF57C00) else Color(0xFFE0E0E0)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "1. Reset Transaksi & Antrian Saja (Disarankan)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFFE65100)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Menghapus riwayat transaksi servis, pengeluaran kasir, dan setor tunai. Data master stok sparepart dan daftar staf tetap aman.",
                                fontSize = 11.sp,
                                color = Color.DarkGray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Opsi 2: Reset Total ke 0 (Sesuai Permintaan User)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { resetActionType = "RESET_ALL_ZERO" },
                        colors = CardDefaults.cardColors(
                            containerColor = if (resetActionType == "RESET_ALL_ZERO") Color(0xFFFFEBEE) else Color(0xFFFAFAFA)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (resetActionType == "RESET_ALL_ZERO") 2.dp else 1.dp,
                            color = if (resetActionType == "RESET_ALL_ZERO") Color(0xFFD32F2F) else Color(0xFFE0E0E0)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "2. Reset Semua Kembali ke 0 (Total Reset)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFFC62828)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Absen hapus, omset hapus, stok hapus, kasir hapus, selesai, antrian, absen, pengajuan reject, report persetujuan hapus, semua kembali ke 0.",
                                fontSize = 11.sp,
                                color = Color.DarkGray
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Opsi 3: Reset Pabrik Total (Data Demo)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { resetActionType = "FACTORY" },
                        colors = CardDefaults.cardColors(
                            containerColor = if (resetActionType == "FACTORY") Color(0xFFFFEBEE) else Color(0xFFFAFAFA)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (resetActionType == "FACTORY") 2.dp else 1.dp,
                            color = if (resetActionType == "FACTORY") Color(0xFFD32F2F) else Color(0xFFE0E0E0)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "3. Reset ke Data Awal Demo Bawaan",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFFC62828)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Menghapus semua data dan memulihkan kembali data demo bawaan bengkel.",
                                fontSize = 11.sp,
                                color = Color.DarkGray
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showResetDialog = false
                        showResetConfirmDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("LANJUTKAN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("BATAL")
                }
            }
        )
    }

    // Modal: Konfirmasi Pengamanan Reset
    if (showResetConfirmDialog) {
        var confirmText by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null, tint = Color(0xFFD32F2F))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("KONFIRMASI PENGHAPUSAN", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFFD32F2F))
                }
            },
            text = {
                Column {
                    Text(
                        text = when (resetActionType) {
                            "TRANSAKSI" -> "Anda akan menghapus seluruh data riwayat transaksi kasir dan servis antrian."
                            "RESET_ALL_ZERO" -> "Absen hapus, omset hapus, stok hapus, kasir hapus, servis selesai, antrian, pengajuan reject, report persetujuan hapus, semua kembali ke 0."
                            else -> "Anda akan mereset database total ke konfigurasi awal bawaan demo."
                        },
                        fontSize = 12.sp,
                        color = Color.Black
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Ketik kata \"RESET\" untuk konfirmasi:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = confirmText,
                        onValueChange = { confirmText = it },
                        placeholder = { Text("RESET") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (confirmText.trim().equals("RESET", ignoreCase = true)) {
                            when (resetActionType) {
                                "TRANSAKSI" -> viewModel.resetTransactionsOnly(context)
                                "RESET_ALL_ZERO" -> viewModel.resetAllDataToZero(context)
                                else -> viewModel.resetFactoryDefaults(context)
                            }
                            showResetConfirmDialog = false
                        } else {
                            Toast.makeText(context, "Ketik RESET untuk mengonfirmasi", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = confirmText.trim().equals("RESET", ignoreCase = true),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
                ) {
                    Text("YA, BERSIHKAN SEKARANG")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetConfirmDialog = false }) {
                    Text("BATAL")
                }
            }
        )
    }

    // PRO Licensing Dialogs
    if (showProActivationDialog) {
        ProUpgradeDialog(
            featureTitle = "AKTIVASI LISENSI BENGKEL QU PRO",
            reasonText = "Buka seluruh potensi operasional bengkel Anda dengan versi PRO: tanpa batas stok, antrian servis unlimited, scan barcode kamera cepat, dan cetak nota PDF resmi.",
            onDismiss = { showProActivationDialog = false },
            onActivateKey = { key ->
                viewModel.activateProLicense(key, context) { success, _ ->
                    if (success) {
                        showProActivationDialog = false
                    }
                }
            }
        )
    }

    if (showProComparisonDialog) {
        ProFeatureComparisonDialog(
            isPro = isPro,
            onDismiss = { showProComparisonDialog = false },
            onOpenActivation = { showProActivationDialog = true }
        )
    }
}

@Composable
fun ThemeColorTile(
    name: String,
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF5F5F5))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) color else Color.Transparent,
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = name, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun FontSizeOptionChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color(0xFFF5F5F5))
            .border(
                width = if (isSelected) 1.5.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black
        )
    }
}
