package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Stars
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.FeatureGate

@Composable
fun ProFeatureBadge(
    isPro: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Surface(
        color = if (isPro) Color(0xFFFFB300) else Color(0xFF78909C),
        shape = RoundedCornerShape(12.dp),
        shadowElevation = if (isPro) 2.dp else 0.dp,
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable { onClick() } else Modifier
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Icon(
                imageVector = if (isPro) Icons.Default.WorkspacePremium else Icons.Default.Stars,
                contentDescription = null,
                tint = if (isPro) Color(0xFF3E2723) else Color.White,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isPro) "PRO VIP" else "REGULER",
                color = if (isPro) Color(0xFF3E2723) else Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
fun ProUpgradeDialog(
    featureTitle: String,
    reasonText: String,
    onDismiss: () -> Unit,
    onActivateKey: (String) -> Unit
) {
    val context = LocalContext.current
    var licenseKeyInput by remember { mutableStateOf("") }
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
                        .size(36.dp)
                        .background(Color(0xFFFFF8E1), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.WorkspacePremium,
                        contentDescription = null,
                        tint = Color(0xFFF57F17),
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "FITUR BENGKEL QU PRO",
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
                    Text(
                        text = reasonText,
                        fontSize = 12.sp,
                        color = Color(0xFFBF360C),
                        modifier = Modifier.padding(10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Keunggulan Versi PRO:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    BenefitRow("Stok Sparepart Tanpa Batas (Reguler: maks 50)")
                    BenefitRow("Antrian Servis Tanpa Batas (Reguler: maks 15/hari)")
                    BenefitRow("Scan Barcode Kamera Cepat di Kasir & Stok")
                    BenefitRow("Cetak Nota PDF Resmi Bebas Watermark")
                    BenefitRow("Manajemen Karyawan Tanpa Batas (Reguler: maks 2)")
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Input Lisensi
                Text(
                    text = "Aktivasi Kode Lisensi PRO:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Spacer(modifier = Modifier.height(4.dp))

                OutlinedTextField(
                    value = licenseKeyInput,
                    onValueChange = {
                        licenseKeyInput = it.uppercase()
                        errorMessage = ""
                    },
                    placeholder = { Text("Contoh: BQPRO-DEV2026-VIP") },
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

                Spacer(modifier = Modifier.height(8.dp))

                // Tombol Bantuan Demo
                OutlinedButton(
                    onClick = {
                        licenseKeyInput = FeatureGate.DEMO_PRO_KEY
                        Toast.makeText(context, "Kunci Demo PRO dimasukkan!", Toast.LENGTH_SHORT).show()
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Gunakan Kunci Pengujian: ${FeatureGate.DEMO_PRO_KEY}", fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (licenseKeyInput.isNotBlank()) {
                        onActivateKey(licenseKeyInput)
                    } else {
                        errorMessage = "Masukkan kode lisensi atau tekan tombol kunci pengujian di atas."
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57F17)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("AKTIFKAN PRO", fontWeight = FontWeight.Bold)
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
    isPro: Boolean,
    onDismiss: () -> Unit,
    onOpenActivation: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.WorkspacePremium, contentDescription = null, tint = Color(0xFFF57F17))
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("SKEMA REGULER VS PRO", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text("Spesifikasi Fitur Bengkel Qu", fontSize = 11.sp, color = Color.Gray)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
            ) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isPro) Color(0xFFFFF8E1) else Color(0xFFECEFF1)
                    ),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, if (isPro) Color(0xFFFFB300) else Color(0xFFB0BEC5)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isPro) "STATUS: BENGKEL QU PRO (AKTIF)" else "STATUS: BENGKEL QU REGULER",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (isPro) Color(0xFFE65100) else Color(0xFF37474F)
                            )
                            Text(
                                text = if (isPro) "Semua fitur tidak terbatas telah aktif seumur hidup." else "Versi gratis untuk bengkel permulaan.",
                                fontSize = 11.sp,
                                color = Color.DarkGray
                            )
                        }
                        ProFeatureBadge(isPro = isPro)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Tabel Perbandingan
                ComparisonItem("Kapasitas Stok Sparepart", "Maks 50 Item", "Tanpa Batas (Unlimited)")
                ComparisonItem("Antrian Servis Harian", "Maks 15 / hari", "Tanpa Batas (Unlimited)")
                ComparisonItem("Jumlah Staf & Mekanik", "Maks 2 Karyawan", "Tanpa Batas (Unlimited)")
                ComparisonItem("Scan Barcode Kamera", "Input Manual", "Scanner Kamera Cepat")
                ComparisonItem("Cetak Nota PDF", "Dengan Watermark", "Resmi Bebas Watermark")
                ComparisonItem("Backup / Ekspor Data", "JSON Standar", "JSON + Cloud Storage")
                ComparisonItem("Dukungan Multi-Cabang", "1 Lokasi", "Siap Skala Multi-Bengkel")
            }
        },
        confirmButton = {
            if (!isPro) {
                Button(
                    onClick = {
                        onDismiss()
                        onOpenActivation()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF57F17)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("AKTIFKAN PRO")
                }
            } else {
                Button(onClick = onDismiss) {
                    Text("TUTUP")
                }
            }
        },
        dismissButton = {
            if (!isPro) {
                TextButton(onClick = onDismiss) {
                    Text("TUTUP")
                }
            }
        }
    )
}

@Composable
private fun BenefitRow(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            Icons.Default.Check,
            contentDescription = null,
            tint = Color(0xFF2E7D32),
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text, fontSize = 11.sp, color = Color.DarkGray)
    }
}

@Composable
private fun ComparisonItem(
    title: String,
    regularVal: String,
    proVal: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.5.dp, Color(0xFFE0E0E0)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = Color.Black)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Reguler:", fontSize = 9.sp, color = Color.Gray)
                    Text(regularVal, fontSize = 11.sp, color = Color(0xFF546E7A))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("PRO:", fontSize = 9.sp, color = Color(0xFFE65100), fontWeight = FontWeight.Bold)
                    Text(proVal, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                }
            }
        }
    }
}
