package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeveloperMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.FeatureGate

@Composable
fun ProFeatureBadge(
    isPro: Boolean,
    trialStatus: FeatureGate.TrialStatus? = null,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val (bgColor, textColor, label) = when {
        trialStatus is FeatureGate.TrialStatus.ProActivated || isPro -> {
            val remain = (trialStatus as? FeatureGate.TrialStatus.ProActivated)?.daysRemaining
            if (remain != null) {
                Triple(Color(0xFFFFB300), Color(0xFF3E2723), "PRO: SISA ${remain}H")
            } else {
                Triple(Color(0xFFFFB300), Color(0xFF3E2723), "PRO AKTIF")
            }
        }
        trialStatus is FeatureGate.TrialStatus.TrialActive -> {
            if (trialStatus.isUrgentWarning) {
                Triple(Color(0xFFFF7043), Color.White, "TRIAL: H-${trialStatus.daysRemaining}")
            } else {
                Triple(Color(0xFF26A69A), Color.White, "TRIAL 10 HARI")
            }
        }
        trialStatus is FeatureGate.TrialStatus.Expired -> {
            Triple(Color(0xFFE53935), Color.White, "TRIAL HABIS")
        }
        else -> {
            Triple(Color(0xFF78909C), Color.White, "REGULER")
        }
    }

    Surface(
        color = bgColor,
        shape = RoundedCornerShape(12.dp),
        shadowElevation = 2.dp,
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable { onClick() } else Modifier
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Icon(
                imageVector = if (isPro || trialStatus is FeatureGate.TrialStatus.ProActivated) {
                    Icons.Default.WorkspacePremium
                } else if (trialStatus is FeatureGate.TrialStatus.TrialActive) {
                    Icons.Default.Stars
                } else {
                    Icons.Default.Lock
                },
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}

/**
 * Visual lock icon shown on PRO-exclusive features.
 */
@Composable
fun ProLockIcon(
    isLocked: Boolean,
    modifier: Modifier = Modifier
) {
    if (isLocked) {
        Box(
            modifier = modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(Color(0xFFD32F2F)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Fitur Pro Terkunci",
                tint = Color.White,
                modifier = Modifier.size(13.dp)
            )
        }
    }
}

/**
 * Trial Banner showing info on H-1, H-2, H-3 or Expired status.
 */
@Composable
fun TrialStatusBanner(
    trialStatus: FeatureGate.TrialStatus,
    onActivateClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (trialStatus) {
        is FeatureGate.TrialStatus.TrialActive -> {
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .clickable(onClick = onActivateClick),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (trialStatus.isUrgentWarning) Color(0xFFFFF3E0) else Color(0xFFE8F5E9)
                ),
                border = BorderStroke(
                    1.dp,
                    if (trialStatus.isUrgentWarning) Color(0xFFFF9800) else Color(0xFF81C784)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (trialStatus.isUrgentWarning) Color(0xFFFFE0B2) else Color(0xFFC8E6C9)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (trialStatus.isUrgentWarning) Icons.Default.Warning else Icons.Default.Stars,
                            contentDescription = null,
                            tint = if (trialStatus.isUrgentWarning) Color(0xFFE65100) else Color(0xFF2E7D32),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (trialStatus.isUrgentWarning) {
                                "TRIAL PRO: SISA ${trialStatus.daysRemaining} HARI (H-${trialStatus.daysRemaining})"
                            } else {
                                "TRIAL PRO AKTIF: SISA ${trialStatus.daysRemaining} HARI"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (trialStatus.isUrgentWarning) Color(0xFFBF360C) else Color(0xFF1B5E20)
                        )
                        Text(
                            text = "Fitur kasir & data lokal selalu aktif. Sentuh untuk lihat paket PRO.",
                            fontSize = 10.sp,
                            color = Color.DarkGray
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = onActivateClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (trialStatus.isUrgentWarning) Color(0xFFE65100) else Color(0xFF2E7D32)
                        ),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("PAKET PRO", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        is FeatureGate.TrialStatus.Expired -> {
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .clickable(onClick = onActivateClick),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                border = BorderStroke(1.2.dp, Color(0xFFE53935))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFCDD2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFC62828), modifier = Modifier.size(18.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "MASA TRIAL 10 HARI TELAH BERAKHIR",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFFB71C1C)
                        )
                        Text(
                            text = "Kasir & data aman. Upgrade PRO mulai Rp1.000/hari.",
                            fontSize = 10.sp,
                            color = Color(0xFF4E342E)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Button(
                        onClick = onActivateClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("AKTIVASI", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        is FeatureGate.TrialStatus.ProActivated -> {
            // No warning needed for active Pro
        }
    }
}

/**
 * Comprehensive Pop-up Dialog for 10-Day Trial, Package Options (1, 3, 6, 12 months @ Rp1.000/hari),
 * Device-Bound ID, WhatsApp Developer Order, and Serial Number Activation.
 * Follows the Non-Blocking Principle.
 */
@Composable
fun ProTrialPackagesDialog(
    trialStatus: FeatureGate.TrialStatus,
    workshopName: String,
    onDismiss: () -> Unit,
    onActivateKey: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val deviceId = remember { FeatureGate.getDeviceId(context) }
    var selectedPlanCode by remember { mutableStateOf("30D") }
    var serialNumberInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    val selectedPlan = FeatureGate.PRO_PLANS.find { it.code == selectedPlanCode } ?: FeatureGate.PRO_PLANS[0]

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(Color(0xFFFFF8E1), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = Color(0xFFF57F17),
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "BENGKEL QU PRO",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        color = Color(0xFF1B5E20)
                    )
                    Text(
                        text = when (trialStatus) {
                            is FeatureGate.TrialStatus.TrialActive -> "Trial Otomatis 10 Hari (Sisa ${trialStatus.daysRemaining} Hari)"
                            is FeatureGate.TrialStatus.Expired -> "Masa Trial 10 Hari Selesai"
                            is FeatureGate.TrialStatus.ProActivated -> "Lisensi PRO Sedang Aktif"
                        },
                        fontSize = 11.sp,
                        color = if (trialStatus is FeatureGate.TrialStatus.Expired) Color(0xFFD32F2F) else Color(0xFF2E7D32),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Non-Blocking Principle Banner
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFA5D6A7))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Prinsip Non-Blocking: Fitur dasar Kasir, Antrian Servis, Nota Billing, dan Data Stok Lokal tetap 100% AMAN & AKTIF selamanya tanpa terkunci.",
                            fontSize = 11.sp,
                            color = Color(0xFF1B5E20),
                            lineHeight = 15.sp
                        )
                    }
                }

                // Header Paket
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "PILIHAN PAKET PRO (Rp 1.000/HARI):",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Grid of 4 Duration Plans
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    FeatureGate.PRO_PLANS.forEach { plan ->
                        val isSelected = selectedPlanCode == plan.code
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedPlanCode = plan.code },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) Color(0xFFF1F8E9) else Color(0xFFF5F5F5)
                            ),
                            border = BorderStroke(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) Color(0xFF2E7D32) else Color(0xFFE0E0E0)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) Color(0xFF2E7D32) else Color.LightGray),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = plan.title,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isSelected) Color(0xFF1B5E20) else Color.Black
                                        )
                                        Text(
                                            text = plan.badge,
                                            fontSize = 10.sp,
                                            color = if (isSelected) Color(0xFF388E3C) else Color.Gray,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                Text(
                                    text = plan.priceText,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = if (isSelected) Color(0xFF1B5E20) else Color.DarkGray
                                )
                            }
                        }
                    }
                }

                // Device ID Information Card (Device-Bound Security)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFDE7)),
                    border = BorderStroke(1.dp, Color(0xFFFFE082))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFFF57F17), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "ID PERANGKAT ANDA:",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = Color(0xFFE65100)
                                )
                            }
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(deviceId))
                                    Toast.makeText(context, "ID Perangkat berhasil disalin: $deviceId", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Salin ID", tint = Color(0xFFE65100), modifier = Modifier.size(16.dp))
                            }
                        }
                        Text(
                            text = deviceId,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFBF360C)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Serial Number dibuat mandiri oleh Developer terikat khusus perangkat ini agar aman dan tidak dapat dibajak.",
                            fontSize = 10.sp,
                            color = Color(0xFF5D4037)
                        )
                    }
                }

                // Tombol Ajukan Serial Number Email ke Developer (Sesuai Permintaan: Email Only, Hidden Nomor Developer)
                Button(
                    onClick = {
                        try {
                            val subject = "Aktivasi Lisensi BengkelQu PRO - $workshopName"
                            val body = "Yth. Developer BengkelQu (${FeatureGate.DEVELOPER_EMAIL}),\n\n" +
                                    "Saya bermaksud mengajukan aktivasi paket PRO untuk bengkel saya:\n" +
                                    "- Paket Dipilih: ${selectedPlan.title} (${selectedPlan.priceText} / ${selectedPlan.durationLabel})\n" +
                                    "- Nama Bengkel: $workshopName\n" +
                                    "- ID Perangkat (Device ID): $deviceId\n\n" +
                                    "Mohon dikirimkan Serial Number aktivasi resmi untuk ID Perangkat tersebut.\n\n" +
                                    "Terima kasih."
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:${FeatureGate.DEVELOPER_EMAIL}")
                                putExtra(Intent.EXTRA_SUBJECT, subject)
                                putExtra(Intent.EXTRA_TEXT, body)
                            }
                            context.startActivity(Intent.createChooser(intent, "Kirim Pengajuan Aktivasi via Email"))
                        } catch (e: Exception) {
                            clipboardManager.setText(AnnotatedString(FeatureGate.DEVELOPER_EMAIL))
                            Toast.makeText(context, "Email Developer disalin: ${FeatureGate.DEVELOPER_EMAIL}", Toast.LENGTH_LONG).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "AJUKAN SERIAL NUMBER VIA EMAIL",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Input Serial Number
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Sudah memiliki Serial Number? Masukkan di sini:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                    OutlinedTextField(
                        value = serialNumberInput,
                        onValueChange = {
                            serialNumberInput = it.uppercase()
                            errorMessage = ""
                        },
                        placeholder = { Text("Contoh: BQPRO-30D-XXXX-XXXX") },
                        leadingIcon = {
                            Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFF2E7D32))
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorMessage.isNotBlank()) {
                        Text(
                            text = errorMessage,
                            color = Color(0xFFD32F2F),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (serialNumberInput.isNotBlank()) {
                        onActivateKey(serialNumberInput.trim())
                    } else {
                        errorMessage = "Ketikkan Serial Number resmi Anda terlebih dahulu."
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("AKTIFKAN PRO", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("LANJUTKAN GRATIS", color = Color.Gray, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

/**
 * Backward compatibility dialogs wrapper
 */
@Composable
fun ProUpgradeDialog(
    featureTitle: String,
    reasonText: String,
    onDismiss: () -> Unit,
    onActivateKey: (String) -> Unit
) {
    val context = LocalContext.current
    val trialStatus = remember { FeatureGate.getTrialStatus(context, null) }
    ProTrialPackagesDialog(
        trialStatus = trialStatus,
        workshopName = "BENGKEL QU",
        onDismiss = onDismiss,
        onActivateKey = onActivateKey
    )
}

@Composable
fun ProFeatureComparisonDialog(
    isPro: Boolean = false,
    onDismiss: () -> Unit,
    onOpenActivation: () -> Unit = {},
    onActivateKey: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val trialStatus = remember { FeatureGate.getTrialStatus(context, null) }
    ProTrialPackagesDialog(
        trialStatus = trialStatus,
        workshopName = "BENGKEL QU",
        onDismiss = onDismiss,
        onActivateKey = onActivateKey
    )
}

/**
 * Tanda Kunci Bersinar (Glowing Lock Badge)
 * Efek visual dinamis dengan animasi pulsing halo & kilau emas untuk menu PRO (Pengaturan, Omset, Laporan, Marketing)
 */
@Composable
fun GlowingLockBadge(
    modifier: Modifier = Modifier,
    label: String = "PRO"
) {
    val infiniteTransition = rememberInfiniteTransition(label = "glowing_lock_transition")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_alpha"
    )
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow_scale"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = glowScale
                scaleY = glowScale
            }
            .clip(RoundedCornerShape(8.dp))
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFD54F).copy(alpha = glowAlpha),
                        Color(0xFFFF8F00).copy(alpha = 0.92f)
                    )
                )
            )
            .border(
                width = 1.2.dp,
                color = Color(0xFFFFF9C4).copy(alpha = glowAlpha),
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Terkunci PRO",
                tint = Color(0xFF3E2723),
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = label,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFF3E2723)
            )
        }
    }
}

/**
 * Dialog Informasi Skema Teknis Generator Serial Number (Khusus Developer).
 * Menjelaskan formula derivasi kunci aman berbasis Device ID dan menyediakan live tester simulator.
 */
@Composable
fun DeveloperSchemaDialog(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val deviceId = remember { FeatureGate.getDeviceId(context) }
    var selectedPlanCode by remember { mutableStateOf("30D") }
    var generatedSerial by remember {
        mutableStateOf(FeatureGate.generateSerialNumber(deviceId, "30D"))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFFE8EAF6), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.DeveloperMode,
                        contentDescription = null,
                        tint = Color(0xFF283593),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "SKEMA SERIAL NUMBER",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = Color(0xFF1A237E)
                    )
                    Text(
                        text = "Teknis & Keamanan Offline Developer",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEDE7F6)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "🔐 Formula Kriptografi (Offline Binding):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = Color(0xFF4A148C)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Raw = Clean(DeviceID) + '#' + PlanCode + '#' + SecretSalt\n" +
                                    "Checksum = SHA-256(Raw).take(6 hex chars)\n" +
                                    "Format = BQPRO-[PAKET]-[DEVICE_ID]-[CHECKSUM]",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color(0xFF311B92)
                        )
                    }
                }

                Text(
                    text = "Alur Permintaan Resmi:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
                Text(
                    text = "• Seluruh permintaan SN HANYA via Email Developer (${FeatureGate.DEVELOPER_EMAIL}). Nomor WA disembunyikan/ditiadakan.\n" +
                            "• User mengirimkan Device ID mereka via tombol ajukan email.\n" +
                            "• Developer meng-generate SN dan membalas email pelanggan.",
                    fontSize = 11.sp,
                    color = Color.DarkGray
                )

                // Live Simulator Generator
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFE0E0E0))
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = "LIVE GENERATOR TESTER (DEVELOPER):",
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = Color(0xFF00695C)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Device ID Target: $deviceId", fontSize = 11.sp, fontFamily = FontFamily.Monospace)

                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            listOf("1D", "30D", "90D", "180D", "365D", "LIFE").forEach { code ->
                                val isSel = selectedPlanCode == code
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSel) Color(0xFF00695C) else Color(0xFFE0E0E0))
                                        .clickable {
                                            selectedPlanCode = code
                                            generatedSerial = FeatureGate.generateSerialNumber(deviceId, code)
                                        }
                                        .padding(horizontal = 6.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = code,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSel) Color.White else Color.Black
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Generated SN:",
                            fontSize = 10.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = generatedSerial,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            color = Color(0xFFBF360C)
                        )

                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = {
                                clipboardManager.setText(AnnotatedString(generatedSerial))
                                Toast.makeText(context, "Serial Number berhasil disalin: $generatedSerial", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00695C)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth().height(36.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("SALIN SERIAL NUMBER", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("TUTUP")
            }
        }
    )
}

