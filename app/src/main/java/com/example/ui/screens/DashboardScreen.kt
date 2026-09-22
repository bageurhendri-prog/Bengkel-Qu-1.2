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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.BengkelScreen
import com.example.ui.BengkelViewModel
import com.example.ui.components.ProFeatureBadge
import com.example.ui.components.ProLockIcon
import com.example.ui.components.ProTrialPackagesDialog
import com.example.ui.components.TrialStatusBanner
import com.example.util.FeatureGate

@Composable
fun DashboardScreen(viewModel: BengkelViewModel) {
    val context = LocalContext.current
    val activeServices by viewModel.activeServices.collectAsStateWithLifecycle()
    val completedServices by viewModel.completedServices.collectAsStateWithLifecycle()
    val workshopProfile by viewModel.workshopProfile.collectAsStateWithLifecycle()
    val role by viewModel.activeUserRole.collectAsStateWithLifecycle()
    val isPro by viewModel.isProUser.collectAsStateWithLifecycle()
    val syncNotification by viewModel.syncNotification.collectAsStateWithLifecycle()

    val selesaiCount = completedServices.size
    val serviceCount = activeServices.size
    val bengkelName = workshopProfile?.workshopName ?: "BENGKELQU"

    val trialStatus = remember(workshopProfile) {
        FeatureGate.getTrialStatus(context, workshopProfile)
    }
    val hasProAccess = remember(workshopProfile) {
        FeatureGate.hasProAccess(context, workshopProfile)
    }

    var showProDialog by remember { mutableStateOf(false) }

    // Auto-popup 10-day trial on first install / launch
    LaunchedEffect(Unit) {
        if (!isPro && FeatureGate.shouldAutoShowTrialPopup(context)) {
            showProDialog = true
            FeatureGate.markTrialPopupShown(context)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Top Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primary)
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = bengkelName.replace(" ", "").uppercase(),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.5.sp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Mode: $role",
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.85f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            ProFeatureBadge(
                                isPro = isPro,
                                trialStatus = trialStatus,
                                onClick = {
                                    if (!isPro) {
                                        showProDialog = true
                                    }
                                }
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = { viewModel.syncMasterData(context) },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Sync,
                                contentDescription = "Sinkronkan Master Data",
                                tint = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { viewModel.logout() },
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Keluar",
                                tint = Color.White
                            )
                        }
                    }
                }
            }

            // Trial Info Banner (shows on H-1, H-2, H-3 or when Expired)
            TrialStatusBanner(
                trialStatus = trialStatus,
                onActivateClick = {
                    showProDialog = true
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // TOP COUNTER TILES (matching diagram: "selesai 15", "service 3")
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Selesai Box
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFE8F5E9))
                            .border(1.5.dp, Color(0xFF4CAF50), RoundedCornerShape(12.dp))
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF2E7D32),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "selesai",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B5E20)
                            )
                        }
                        Text(
                            text = "$selesaiCount",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF2E7D32)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Service Box (Active)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFFF3E0))
                            .border(1.5.dp, Color(0xFFFFA000), RoundedCornerShape(12.dp))
                            .clickable { viewModel.navigateTo(BengkelScreen.SERVICE_QUEUE) }
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Engineering,
                                contentDescription = null,
                                tint = Color(0xFFE65100),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "service",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE65100)
                            )
                        }
                        Text(
                            text = "$serviceCount",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFE65100)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // MAIN MENU BUTTONS - Urutan Baru: Service & Antrian, Kasir & Barcode, Sparepart & Stok, Marketing, Omset, Report, Pengaturan
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Service & Antrian
                DashboardMenuButton(
                    title = "Service & Antrian",
                    subtitle = "Antrian Kendaraan Masuk & Status Pengerjaan Servis",
                    icon = Icons.Default.Engineering,
                    hasProBadge = false,
                    isLocked = false,
                    onClick = { viewModel.navigateTo(BengkelScreen.SERVICE_QUEUE) }
                )

                // 2. Kasir & Barcode
                DashboardMenuButton(
                    title = "Kasir & Barcode",
                    subtitle = "Billing Pembayaran, Scan Barcode, Setoran & Pengeluaran",
                    icon = Icons.Default.PointOfSale,
                    hasProBadge = false,
                    isLocked = false,
                    onClick = { viewModel.navigateTo(BengkelScreen.SERVICE_QUEUE) }
                )

                // 3. Sparepart & Stok
                DashboardMenuButton(
                    title = "Sparepart & Stok",
                    subtitle = "Katalog Suku Cadang, Stok Minimum & Ekspor Excel",
                    icon = Icons.Default.Inventory,
                    hasProBadge = false,
                    isLocked = false,
                    onClick = { viewModel.navigateTo(BengkelScreen.STOK) }
                )

                // 4. Marketing (PRO)
                DashboardMenuButton(
                    title = "Marketing",
                    subtitle = "Ranking 20 Loyal Customer & Reminder Servis WA",
                    icon = Icons.Default.Campaign,
                    hasProBadge = true,
                    isLocked = !hasProAccess,
                    onLockedClick = { showProDialog = true },
                    onClick = { viewModel.navigateTo(BengkelScreen.MARKETING) }
                )

                // 5. Omset (PRO)
                DashboardMenuButton(
                    title = "Omset",
                    subtitle = "Laporan Omset Keuangan & Laba Bersih Bengkel",
                    icon = Icons.Default.MonetizationOn,
                    hasProBadge = true,
                    isLocked = !hasProAccess,
                    onLockedClick = { showProDialog = true },
                    onClick = { viewModel.navigateTo(BengkelScreen.OMSET) }
                )

                // 6. Report (PRO)
                DashboardMenuButton(
                    title = "Report",
                    subtitle = "Persetujuan Reject, Barang Datang & Belanja Toko",
                    icon = Icons.Default.Assessment,
                    hasProBadge = true,
                    isLocked = !hasProAccess,
                    onLockedClick = { showProDialog = true },
                    onClick = { viewModel.navigateTo(BengkelScreen.REPORT) }
                )

                // 7. Pengaturan (PRO)
                DashboardMenuButton(
                    title = "Pengaturan",
                    subtitle = "Profil Bengkel, Manajemen Staff & Backup Restore",
                    icon = Icons.Default.Settings,
                    hasProBadge = true,
                    isLocked = !hasProAccess,
                    onLockedClick = { showProDialog = true },
                    onClick = { viewModel.navigateTo(BengkelScreen.PENGATURAN) }
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    // Notifikasi Pop-up Kecil Ketika Sinkronisasi Master Selesai
    if (syncNotification != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissSyncNotification() },
            icon = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8F5E9)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(30.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "SINKRONISASI SELESAI",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color(0xFF1B5E20)
                )
            },
            text = {
                Text(
                    text = syncNotification ?: "",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.dismissSyncNotification() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("OK, MENGERTI")
                }
            }
        )
    }

    // Dialog Aktivasi Serial Number & Paket PRO (Rp1.000/hari, 1 Bulan, 3 Bulan, 6 Bulan, 12 Bulan)
    if (showProDialog) {
        ProTrialPackagesDialog(
            trialStatus = trialStatus,
            workshopName = workshopProfile?.workshopName ?: "BENGKEL QU",
            onDismiss = { showProDialog = false },
            onActivateKey = { key ->
                viewModel.activateProLicense(key, context) { success, _ ->
                    if (success) {
                        showProDialog = false
                    }
                }
            }
        )
    }
}

@Composable
fun DashboardMenuButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    hasProBadge: Boolean = false,
    isLocked: Boolean = false,
    onLockedClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                if (isLocked && onLockedClick != null) {
                    onLockedClick()
                } else {
                    onClick()
                }
            },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLocked) Color(0xFFFFF8F8) else MaterialTheme.colorScheme.surface
        ),
        border = if (isLocked) BorderStroke(1.dp, Color(0xFFFFCDD2)) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isLocked) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.primaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isLocked) Color(0xFFC62828) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (isLocked) Color(0xFFC62828) else MaterialTheme.colorScheme.onSurface
                    )
                    if (hasProBadge) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isLocked) Color(0xFFFFCDD2) else Color(0xFFFFD54F))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.WorkspacePremium,
                                    contentDescription = null,
                                    tint = if (isLocked) Color(0xFFB71C1C) else Color(0xFFE65100),
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = "PRO",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (isLocked) Color(0xFFB71C1C) else Color(0xFFE65100)
                                )
                            }
                        }
                    }
                }
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = if (isLocked) Color(0xFF757575) else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        if (isLocked) Color(0xFFFFEBEE) else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isLocked) Icons.Default.Lock else Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = if (isLocked) Color(0xFFC62828) else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
