package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.CustomerService
import com.example.data.local.PaymentMethod
import com.example.data.local.ServiceStatus
import com.example.ui.components.GlowingLockBadge
import com.example.ui.components.ProTrialPackagesDialog
import com.example.ui.components.TrialStatusBanner
import com.example.util.FeatureGate
import com.example.data.local.StockItem
import com.example.ui.BengkelScreen
import com.example.ui.BengkelViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun DashboardScreen(viewModel: BengkelViewModel) {
    val context = LocalContext.current
    val activeServices by viewModel.activeServices.collectAsStateWithLifecycle()
    val completedServices by viewModel.completedServices.collectAsStateWithLifecycle()
    val allStockItems by viewModel.allStockItems.collectAsStateWithLifecycle()
    val workshopProfile by viewModel.workshopProfile.collectAsStateWithLifecycle()
    val role by viewModel.activeUserRole.collectAsStateWithLifecycle()
    val syncNotification by viewModel.syncNotification.collectAsStateWithLifecycle()
    val isProUser by viewModel.isProUser.collectAsStateWithLifecycle()
    val trialStatus by viewModel.trialStatus.collectAsStateWithLifecycle()

    var showProDialog by remember { mutableStateOf(false) }

    val bengkelName = workshopProfile?.workshopName?.ifBlank { "BENGKELQU" } ?: "BENGKELQU"

    // 1. Current Queue Calculations
    val antrianCount = activeServices.count { it.status == ServiceStatus.ANTRIAN }
    val dikerjakanCount = activeServices.count { it.status == ServiceStatus.PROSES }
    val siapKasirCount = activeServices.count { it.status == ServiceStatus.SELESAI }
    val totalActiveQueue = activeServices.size

    // 2. Daily Revenue Calculations (Today)
    val now = System.currentTimeMillis()
    val startOfDay = remember(now) {
        Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val paidToday = remember(completedServices, startOfDay) {
        completedServices.filter { it.dateEpoch >= startOfDay }
    }

    val todayRevenue = remember(paidToday) { paidToday.sumOf { it.totalAmount } }
    val todayTransactionsCount = paidToday.size
    val todayCash = remember(paidToday) {
        paidToday.filter { it.paymentMethod == PaymentMethod.CASH }.sumOf { it.totalAmount }
    }
    val todayTransfer = remember(paidToday) {
        paidToday.filter { it.paymentMethod == PaymentMethod.TRANSFER_BANK }.sumOf { it.totalAmount }
    }
    val todayQris = remember(paidToday) {
        paidToday.filter { it.paymentMethod == PaymentMethod.QRIS }.sumOf { it.totalAmount }
    }
    val todayOjol = remember(paidToday) {
        paidToday.filter { it.paymentMethod == PaymentMethod.OJOL }.sumOf { it.totalAmount }
    }
    val avgPerCustomer = if (todayTransactionsCount > 0) todayRevenue / todayTransactionsCount else 0L

    // 3. Inventory Status Alerts Calculations
    val outOfStockItems = remember(allStockItems) { allStockItems.filter { it.qty <= 0 } }
    val lowStockItems = remember(allStockItems) { allStockItems.filter { it.qty in 1..3 } }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val isTabletOrExpanded = maxWidth >= 680.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Top Bar with Gradient Accent (Sync master icon is embedded here)
                DashboardHeader(
                    bengkelName = bengkelName,
                    role = role,
                    trialStatus = trialStatus,
                    onTrialClick = { showProDialog = true },
                    onSyncClick = { viewModel.syncMasterData(context) },
                    onLogoutClick = { viewModel.logout() }
                )

                // Status Trial 10 Hari / Lisensi PRO Banner
                TrialStatusBanner(
                    trialStatus = trialStatus,
                    onActivateClick = { showProDialog = true }
                )

                Spacer(modifier = Modifier.height(10.dp))

                // RESPONSIVE MAIN CONTENT
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 1100.dp)
                        .padding(horizontal = if (isTabletOrExpanded) 24.dp else 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (isTabletOrExpanded) {
                        // TABLET / EXPANDED MODE: 2 Columns Side-by-Side
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Left Column: Queue Counts
                            Box(modifier = Modifier.weight(1f)) {
                                QueueStatusCard(
                                    totalActiveQueue = totalActiveQueue,
                                    antrianCount = antrianCount,
                                    dikerjakanCount = dikerjakanCount,
                                    siapKasirCount = siapKasirCount,
                                    paidTodayCount = todayTransactionsCount,
                                    activeServices = activeServices,
                                    onViewQueue = { viewModel.navigateTo(BengkelScreen.SERVICE_QUEUE) },
                                    onOpenKasir = { viewModel.navigateTo(BengkelScreen.KASIR) }
                                )
                            }

                            // Right Column: Daily Revenue Summary
                            Box(modifier = Modifier.weight(1f)) {
                                DailyRevenueCard(
                                    todayRevenue = todayRevenue,
                                    todayTransactionsCount = todayTransactionsCount,
                                    todayCash = todayCash,
                                    todayTransfer = todayTransfer,
                                    todayQris = todayQris,
                                    todayOjol = todayOjol,
                                    avgPerCustomer = avgPerCustomer,
                                    onViewOmset = {
                                        if (isProUser) viewModel.navigateTo(BengkelScreen.OMSET)
                                        else showProDialog = true
                                    }
                                )
                            }
                        }

                        // Full Width: Inventory Status Alerts
                        InventoryAlertCard(
                            outOfStockItems = outOfStockItems,
                            lowStockItems = lowStockItems,
                            totalStockSku = allStockItems.size,
                            onManageStock = { viewModel.navigateTo(BengkelScreen.STOK) }
                        )

                        // Menu Grid 3 columns on wide screens
                        ResponsiveMenuGrid(
                            columns = 3,
                            isPro = isProUser,
                            trialStatus = trialStatus,
                            onNavigate = { viewModel.navigateTo(it) },
                            onOpenProDialog = { showProDialog = true }
                        )
                    } else {
                        // COMPACT / MOBILE PHONE MODE: Vertical Stack
                        // 1. Live Queue Counts Card
                        QueueStatusCard(
                            totalActiveQueue = totalActiveQueue,
                            antrianCount = antrianCount,
                            dikerjakanCount = dikerjakanCount,
                            siapKasirCount = siapKasirCount,
                            paidTodayCount = todayTransactionsCount,
                            activeServices = activeServices,
                            onViewQueue = { viewModel.navigateTo(BengkelScreen.SERVICE_QUEUE) },
                            onOpenKasir = { viewModel.navigateTo(BengkelScreen.KASIR) }
                        )

                        // 2. Summary of Daily Revenue Card
                        DailyRevenueCard(
                            todayRevenue = todayRevenue,
                            todayTransactionsCount = todayTransactionsCount,
                            todayCash = todayCash,
                            todayTransfer = todayTransfer,
                            todayQris = todayQris,
                            todayOjol = todayOjol,
                            avgPerCustomer = avgPerCustomer,
                            onViewOmset = {
                                if (isProUser) viewModel.navigateTo(BengkelScreen.OMSET)
                                else showProDialog = true
                            }
                        )

                        // 3. Inventory Status Alerts Card
                        InventoryAlertCard(
                            outOfStockItems = outOfStockItems,
                            lowStockItems = lowStockItems,
                            totalStockSku = allStockItems.size,
                            onManageStock = { viewModel.navigateTo(BengkelScreen.STOK) }
                        )

                        // 4. Quick Action Menu Grid (2 Columns)
                        ResponsiveMenuGrid(
                            columns = 2,
                            isPro = isProUser,
                            trialStatus = trialStatus,
                            onNavigate = { viewModel.navigateTo(it) },
                            onOpenProDialog = { showProDialog = true }
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    // Dialog Paket PRO & Aktivasi Serial Number
    if (showProDialog) {
        ProTrialPackagesDialog(
            trialStatus = trialStatus,
            workshopName = bengkelName,
            onDismiss = { showProDialog = false },
            onActivateKey = { key ->
                viewModel.activateProLicense(key = key, context = context) { success, msg ->
                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
                }
                showProDialog = false
            }
        )
    }

    // Dialog Sinkronisasi Notifikasi
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
                        modifier = Modifier.size(28.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "SINKRONISASI BERHASIL",
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
}

// ----------------------------------------------------------------------------
// COMPONENT: DASHBOARD TOP HEADER
// ----------------------------------------------------------------------------
@Composable
private fun DashboardHeader(
    bengkelName: String,
    role: String,
    trialStatus: FeatureGate.TrialStatus,
    onTrialClick: () -> Unit,
    onSyncClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.92f)
                    )
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.TwoWheeler,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = bengkelName.uppercase(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.2.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.White.copy(alpha = 0.25f))
                        .padding(horizontal = 8.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "MODE: $role",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                // Tombol Menu Trial 10 Hari (Logo Kecil & Status)
                Surface(
                    onClick = onTrialClick,
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFFFD54F),
                    shadowElevation = 2.dp,
                    modifier = Modifier.testTag("trial_menu_header_badge")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = "Menu Trial 10 Hari",
                            tint = Color(0xFF3E2723),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = when (trialStatus) {
                                is FeatureGate.TrialStatus.TrialActive -> "Trial: Sisa ${trialStatus.daysRemaining}H"
                                is FeatureGate.TrialStatus.ProActivated -> if (trialStatus.isLifetime) "PRO Lifetime" else "PRO: ${trialStatus.daysRemaining}H"
                                is FeatureGate.TrialStatus.Expired -> "Trial Habis"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF3E2723)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onSyncClick,
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        .testTag("sync_master_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudSync,
                        contentDescription = "Sinkronkan Master Data",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = onLogoutClick,
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color.White.copy(alpha = 0.2f), CircleShape)
                        .testTag("logout_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Keluar",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------
// COMPONENT 1: CURRENT QUEUE COUNTS & WORKLOAD
// ----------------------------------------------------------------------------
@Composable
private fun QueueStatusCard(
    totalActiveQueue: Int,
    antrianCount: Int,
    dikerjakanCount: Int,
    siapKasirCount: Int,
    paidTodayCount: Int,
    activeServices: List<CustomerService>,
    onViewQueue: () -> Unit,
    onOpenKasir: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("queue_counts_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFFF3E0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Engineering,
                            contentDescription = "Queue Icon",
                            tint = Color(0xFFE65100),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "STATUS ANTRIAN MOTOR",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Live Workload Bengkel Hari Ini",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Total Badge
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (totalActiveQueue > 0) Color(0xFFE65100) else Color(0xFF2E7D32))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$totalActiveQueue Motor Aktif",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 4-Pill Workload Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // 1. Antri (Menunggu)
                QueuePill(
                    label = "Antri",
                    count = antrianCount,
                    accentColor = Color(0xFFF57C00),
                    containerColor = Color(0xFFFFF8E1),
                    modifier = Modifier.weight(1f)
                )

                // 2. Dikerjakan (Mekanik)
                QueuePill(
                    label = "Servis",
                    count = dikerjakanCount,
                    accentColor = Color(0xFF1976D2),
                    containerColor = Color(0xFFE3F2FD),
                    modifier = Modifier.weight(1f)
                )

                // 3. Selesai (Di Kasir)
                QueuePill(
                    label = "Siap Kasir",
                    count = siapKasirCount,
                    accentColor = Color(0xFF7B1FA2),
                    containerColor = Color(0xFFF3E5F5),
                    modifier = Modifier.weight(1f)
                )

                // 4. Lunas Hari Ini
                QueuePill(
                    label = "Lunas",
                    count = paidTodayCount,
                    accentColor = Color(0xFF2E7D32),
                    containerColor = Color(0xFFE8F5E9),
                    modifier = Modifier.weight(1f)
                )
            }

            // Next In Line or Active Service Snippet
            val activeServing = activeServices.firstOrNull { it.status == ServiceStatus.PROSES }
                ?: activeServices.firstOrNull { it.status == ServiceStatus.ANTRIAN }

            if (activeServing != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFFF5F5F5))
                        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "#${activeServing.queueNumber}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "${activeServing.customerName} (${activeServing.plateNumber})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Mekanik: ${activeServing.mechanicName} • ${activeServing.status.name}",
                                fontSize = 10.sp,
                                color = Color.DarkGray
                            )
                        }
                    }
                    Text(
                        text = formatRupiah(activeServing.totalAmount),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1B5E20)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onViewQueue,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(Icons.Default.Engineering, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Buka Antrian", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onOpenKasir,
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(Icons.Default.PointOfSale, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Meja Kasir", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun QueuePill(
    label: String,
    count: Int,
    accentColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(containerColor)
            .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$count",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = accentColor
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor
        )
    }
}

// ----------------------------------------------------------------------------
// COMPONENT 2: SUMMARY OF DAILY REVENUE
// ----------------------------------------------------------------------------
@Composable
private fun DailyRevenueCard(
    todayRevenue: Long,
    todayTransactionsCount: Int,
    todayCash: Long,
    todayTransfer: Long,
    todayQris: Long,
    todayOjol: Long,
    avgPerCustomer: Long,
    onViewOmset: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("daily_revenue_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE8F5E9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.MonetizationOn,
                            contentDescription = "Revenue Icon",
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "RINGKASAN OMSET HARI INI",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Pendapatan Bersih Kasir Lunas",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Date Chip
                val todayLabel = remember {
                    SimpleDateFormat("dd MMM", Locale("id", "ID")).format(Date())
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = todayLabel,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main Revenue Value Display
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(Color(0xFF1B5E20), Color(0xFF2E7D32))
                        )
                    )
                    .padding(16.dp)
            ) {
                Column {
                    Text(
                        text = "TOTAL OMSET HARI INI",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.85f),
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = formatRupiah(todayRevenue),
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$todayTransactionsCount Kendaraan Selesai",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Rata2: ${formatRupiah(avgPerCustomer)}/motor",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Payment Methods Breakdown (Cash vs Digital)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                RevenueBreakdownItem(
                    label = "CASH",
                    amount = todayCash,
                    icon = Icons.Default.Payments,
                    color = Color(0xFF2E7D32),
                    modifier = Modifier.weight(1f)
                )
                RevenueBreakdownItem(
                    label = "TRANSFER",
                    amount = todayTransfer,
                    icon = Icons.Default.CreditCard,
                    color = Color(0xFF1565C0),
                    modifier = Modifier.weight(1f)
                )
                RevenueBreakdownItem(
                    label = "QRIS",
                    amount = todayQris,
                    icon = Icons.Default.QrCode,
                    color = Color(0xFF6A1B9A),
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // View Details Button - Direct access
            OutlinedButton(
                onClick = onViewOmset,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Analisis Laba & Omset Lengkap", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun RevenueBreakdownItem(
    label: String,
    amount: Long,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFF9F9F9))
            .border(1.dp, Color(0xFFEAEAEA), RoundedCornerShape(8.dp))
            .padding(vertical = 6.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(12.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text(text = label, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = formatShortRupiah(amount),
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.Black
        )
    }
}

// ----------------------------------------------------------------------------
// COMPONENT 3: INVENTORY STATUS ALERTS
// ----------------------------------------------------------------------------
@Composable
private fun InventoryAlertCard(
    outOfStockItems: List<StockItem>,
    lowStockItems: List<StockItem>,
    totalStockSku: Int,
    onManageStock: () -> Unit
) {
    val totalAlerts = outOfStockItems.size + lowStockItems.size
    val isAlertActive = totalAlerts > 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("inventory_status_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isAlertActive) Color(0xFFFFFBF0) else Color(0xFFF6FBF6)
        ),
        border = BorderStroke(
            1.dp,
            if (isAlertActive) Color(0xFFFFCC80) else Color(0xFFA5D6A7)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isAlertActive) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isAlertActive) Icons.Default.Warning else Icons.Default.CheckCircle,
                            contentDescription = "Inventory Alert",
                            tint = if (isAlertActive) Color(0xFFD32F2F) else Color(0xFF2E7D32),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isAlertActive) "PERINGATAN STOK SPAREPART" else "STATUS INVENTORY AMAN",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isAlertActive) Color(0xFFB71C1C) else Color(0xFF1B5E20)
                        )
                        Text(
                            text = if (isAlertActive) "$totalAlerts item butuh restock segera" else "Semua $totalStockSku SKU sparepart tersedia",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Alert Count Badge
                if (isAlertActive) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFFD32F2F))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$totalAlerts Alert",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color(0xFF2E7D32))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Aman",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (isAlertActive) {
                // List of Critical Items Preview (Max 4 items)
                val previewList = (outOfStockItems + lowStockItems).take(4)

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    previewList.forEach { stock ->
                        val isZero = stock.qty <= 0
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .border(1.dp, Color(0xFFEEEEEE), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stock.name,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Black,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${stock.brand} • ${stock.quality}",
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (isZero) Color(0xFFFFCDD2) else Color(0xFFFFE0B2))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isZero) "HABIS (0)" else "Sisa ${stock.qty}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isZero) Color(0xFFB71C1C) else Color(0xFFE65100)
                                )
                            }
                        }
                    }

                    if (totalAlerts > 4) {
                        Text(
                            text = "+ ${totalAlerts - 4} barang menipis lainnya...",
                            fontSize = 11.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = onManageStock,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100))
                ) {
                    Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Kelola Stok / Order Sparepart", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Text(
                    text = "Persediaan sparepart fisik di gudang & etalase dalam kondisi cukup aman untuk melayani pelanggan hari ini.",
                    fontSize = 12.sp,
                    color = Color(0xFF2E7D32)
                )

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedButton(
                    onClick = onManageStock,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Inventory, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Buka Gudang Sparepart", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------
// COMPONENT 4: RESPONSIVE MENU GRID
// ----------------------------------------------------------------------------
@Composable
private fun ResponsiveMenuGrid(
    columns: Int,
    isPro: Boolean,
    trialStatus: FeatureGate.TrialStatus,
    onNavigate: (BengkelScreen) -> Unit,
    onOpenProDialog: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "MENU UTAMA OPERASIONAL",
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.5.sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = 4.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        val menuItems = listOf(
            MenuItemData(
                title = "Service & Antrian",
                subtitle = "Antrian & Mekanik",
                icon = Icons.Default.Engineering,
                destination = BengkelScreen.SERVICE_QUEUE
            ),
            MenuItemData(
                title = "Kasir & Billing",
                subtitle = "Bayar & Cetak Nota",
                icon = Icons.Default.PointOfSale,
                destination = BengkelScreen.KASIR
            ),
            MenuItemData(
                title = "Sparepart & Stok",
                subtitle = "Katalog & Fisik",
                icon = Icons.Default.Inventory,
                destination = BengkelScreen.STOK
            ),
            MenuItemData(
                title = "Trial 10 Hari",
                subtitle = when (trialStatus) {
                    is FeatureGate.TrialStatus.TrialActive -> "Sisa ${trialStatus.daysRemaining} Hari (Aktif)"
                    is FeatureGate.TrialStatus.ProActivated -> if (trialStatus.isLifetime) "Lisensi PRO Lifetime" else "PRO Resmi Aktif"
                    is FeatureGate.TrialStatus.Expired -> "Trial Berakhir • Upgrade"
                },
                icon = Icons.Default.WorkspacePremium,
                destination = BengkelScreen.DASHBOARD,
                isTrialAction = true
            ),
            MenuItemData(
                title = "Marketing WA",
                subtitle = "Promo & Reminder",
                icon = Icons.Default.Campaign,
                destination = BengkelScreen.MARKETING
            ),
            MenuItemData(
                title = "Omset & Keuangan",
                subtitle = "Laba & Pengeluaran",
                icon = Icons.Default.MonetizationOn,
                destination = BengkelScreen.OMSET
            ),
            MenuItemData(
                title = "Laporan Lengkap",
                subtitle = "Barang Datang & Reject",
                icon = Icons.Default.Assessment,
                destination = BengkelScreen.REPORT
            ),
            MenuItemData(
                title = "Absensi Staff",
                subtitle = "Absen & Gaji Karyawan",
                icon = Icons.Default.People,
                destination = BengkelScreen.ABSEN
            ),
            MenuItemData(
                title = "Pengaturan",
                subtitle = "Profil & Database",
                icon = Icons.Default.Settings,
                destination = BengkelScreen.PENGATURAN
            )
        )

        // Render as chunked rows according to column count for clean responsiveness
        val rows = menuItems.chunked(columns)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            rows.forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowItems.forEach { item ->
                        val isProMenu = item.destination in listOf(
                            BengkelScreen.PENGATURAN,
                            BengkelScreen.OMSET,
                            BengkelScreen.REPORT,
                            BengkelScreen.MARKETING
                        )
                        // Tanda Gembok Bersinar untuk menandai fitur-fitur berstatus PRO
                        val isOfficialPro = trialStatus is FeatureGate.TrialStatus.ProActivated
                        val showGlowingLock = isProMenu && !isOfficialPro

                        MenuGridCard(
                            item = item,
                            showGlowingLock = showGlowingLock,
                            onClick = {
                                if (item.isTrialAction) {
                                    onOpenProDialog()
                                } else if (!isPro && item.destination in listOf(
                                        BengkelScreen.OMSET,
                                        BengkelScreen.REPORT,
                                        BengkelScreen.MARKETING
                                    )
                                ) {
                                    onOpenProDialog()
                                } else {
                                    onNavigate(item.destination)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Fill remaining slots in last row if not full
                    if (rowItems.size < columns) {
                        repeat(columns - rowItems.size) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

private data class MenuItemData(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val destination: BengkelScreen,
    val isTrialAction: Boolean = false
)

@Composable
private fun MenuGridCard(
    item: MenuItemData,
    showGlowingLock: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isTrial = item.isTrialAction
    Card(
        modifier = modifier
            .height(105.dp)
            .clickable { onClick() }
            .testTag("menu_${item.title.lowercase().replace(" ", "_")}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isTrial) Color(0xFFFFFDF5) else MaterialTheme.colorScheme.surface
        ),
        border = when {
            isTrial -> BorderStroke(1.2.dp, Color(0xFFFFB300))
            showGlowingLock -> BorderStroke(1.2.dp, Color(0xFFFFB300))
            else -> BorderStroke(1.dp, Color(0xFFE8E8E8))
        },
        elevation = CardDefaults.cardElevation(defaultElevation = if (isTrial) 2.5.dp else 1.5.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isTrial) Color(0xFFFFF8E1) else MaterialTheme.colorScheme.primaryContainer
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = if (isTrial) Color(0xFFF57F17) else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = item.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isTrial) Color(0xFFB78103) else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.subtitle,
                        fontSize = 10.sp,
                        color = if (isTrial) Color(0xFF8D6E63) else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (showGlowingLock) {
                GlowingLockBadge(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                )
            } else if (isTrial) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFFFB300))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "10 HARI",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF3E2723)
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------
// UTILITY HELPERS
// ----------------------------------------------------------------------------
private fun formatShortRupiah(amount: Long): String {
    return when {
        amount >= 1_000_000 -> String.format(Locale("id", "ID"), "%.1f Jt", amount / 1_000_000.0)
        amount >= 1_000 -> "${amount / 1000} Rb"
        else -> "Rp $amount"
    }
}
