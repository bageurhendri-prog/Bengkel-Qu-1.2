package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
        isPro || trialStatus is FeatureGate.TrialStatus.ProActivated -> {
            Triple(Color(0xFFFFB300), Color(0xFF3E2723), "PRO AKTIF")
        }
        trialStatus is FeatureGate.TrialStatus.TrialActive -> {
            if (trialStatus.isUrgentWarning) {
                Triple(Color(0xFFFF7043), Color.White, "TRIAL: H-${trialStatus.daysRemaining}")
            } else {
                Triple(Color(0xFF26A69A), Color.White, "TRIAL 10 HARI")
            }
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
            if (trialStatus.isUrgentWarning) {
                Card(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    border = BorderStroke(1.5.dp, Color(0xFFFF9800))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFFFE0B2)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFE65100))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "MASA TRIAL PRO TERSISA ${trialStatus.daysRemaining} HARI (H-${trialStatus.daysRemaining})",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color(0xFFBF360C)
                            )
                            Text(
                                text = "Fitur Laporan Omset & Pengaturan akan terkunci setelah masa trial habis.",
                                fontSize = 11.sp,
                                color = Color(0xFF5D4037)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = onActivateClick,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE65100)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text("AKTIVASI", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        is FeatureGate.TrialStatus.Expired -> {
            Card(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                border = BorderStroke(1.5.dp, Color(0xFFE53935))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFFCDD2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFFC62828))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "TRIAL 10 HARI TELAH BERAKHIR",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color(0xFFB71C1C)
                        )
                        Text(
                            text = "Fitur Laporan Omset & Pengaturan Pro kini terkunci 🔒. Masukkan Serial Number untuk membuka.",
                            fontSize = 11.sp,
                            color = Color(0xFF4E342E)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onActivateClick,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("INPUT SN", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        is FeatureGate.TrialStatus.ProActivated -> {
            // No warning banner needed when official PRO is active
        }
    }
}

/**
 * Direct Technical Serial Number Activation Dialog.
 * Does NOT contain demo keys/shortcuts. Directly provides technical instructions and activation.
 */
@Composable
fun ProUpgradeDialog(
    featureTitle: String,
    reasonText: String,
    onDismiss: () -> Unit,
    onActivateKey: (String) -> Unit
) {
    val context = LocalContext.current
    var serialNumberInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFFFFF8E1), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFFF57F17),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "AKTIVASI LISENSI PRO",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = Color(0xFFE65100)
                    )
                    Text(
                        text = featureTitle,
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, Color(0xFFFFCC80)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text(
                            text = reasonText,
                            fontSize = 12.sp,
                            color = Color(0xFFBF360C),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Panduan Teknis Serial Number
                Text(
                    text = "PANDUAN TEKNIS SERIAL NUMBER:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "1. Lisensi PRO menggunakan sistem langganan bulanan.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B5E20)
                        )
                        Text(
                            text = "2. Format resmi: BQPRO-XXXX-XXXX atau BENGKELQU-PRO-XXXX",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.DarkGray
                        )
                        Text(
                            text = "3. Hubungi Developer langsung: WA 085714216556 / bageurhendri@gmail.com untuk aktivasi & perpanjangan bulanan.",
                            fontSize = 11.sp,
                            color = Color(0xFF0D47A1),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Masukkan Serial Number:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = serialNumberInput,
                    onValueChange = {
                        serialNumberInput = it.uppercase()
                        errorMessage = ""
                    },
                    placeholder = { Text("Contoh: BQPRO-2026-8899-KQU") },
                    leadingIcon = {
                        Icon(Icons.Default.Key, contentDescription = null, tint = Color(0xFFF57F17))
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = errorMessage,
                        color = Color(0xFFD32F2F),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tombol Bantuan Teknis WhatsApp Langsung
                Button(
                    onClick = {
                        try {
                            val url = "https://wa.me/${FeatureGate.SUPPORT_WHATSAPP_NUMBER}?text=Halo%20Admin%20BengkelQu,%20saya%20ingin%20aktivasi%20Serial%20Number%20PRO"
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Tidak dapat membuka WhatsApp", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Hubungi Bantuan Teknis (WhatsApp)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
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
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57F17)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("AKTIFKAN SEKARANG", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("NANTI SAJA")
            }
        }
    )
}

@Composable
fun ProFeatureComparisonDialog(
    isPro: Boolean = false,
    onDismiss: () -> Unit,
    onOpenActivation: () -> Unit = {},
    onActivateKey: (String) -> Unit = {}
) {
    ProUpgradeDialog(
        featureTitle = "PERBANDINGAN FITUR REGULER vs PRO",
        reasonText = "Versi PRO memberikan kebebasan operasional seumur hidup: kuota stok sparepart tanpa batas, antrian servis unlimited, scan barcode kamera cepat, laporan omset lengkap, kirim WA excel, dan manajemen staf penuh.",
        onDismiss = onDismiss,
        onActivateKey = { key ->
            onActivateKey(key)
            onOpenActivation()
        }
    )
}
