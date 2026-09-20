package com.example.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.example.data.local.StockItem
import java.text.NumberFormat
import java.util.Locale

@Composable
fun BarcodeScannerDialog(
    title: String = "SCAN BARCODE SPAREPART",
    registeredStocks: List<StockItem> = emptyList(),
    onBarcodeScanned: (String, StockItem?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    var manualCodeInput by remember { mutableStateOf("") }
    var showManualInput by remember { mutableStateOf(false) }
    var scannedResult by remember { mutableStateOf<Pair<String, StockItem?>?>(null) }
    var isFlashOn by remember { mutableStateOf(false) }

    fun triggerVibration() {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (vibrator != null && vibrator.hasVibrator()) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(100)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun handleCodeFound(code: String) {
        val matchedStock = registeredStocks.firstOrNull {
            it.barcode.equals(code, ignoreCase = true) ||
                    it.name.contains(code, ignoreCase = true) ||
                    (code.length >= 4 && it.barcode.contains(code))
        }
        triggerVibration()
        scannedResult = Pair(code, matchedStock)
    }

    // Laser Animation for scanner
    val infiniteTransition = rememberInfiniteTransition(label = "LaserTransition")
    val laserPosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "LaserPosition"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Tutup", tint = Color.White)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Arahkan kamera ke barcode sparepart",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                    }

                    IconButton(
                        onClick = { isFlashOn = !isFlashOn },
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                if (isFlashOn) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.2f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.FlashOn,
                            contentDescription = "Flashlight",
                            tint = if (isFlashOn) Color.Black else Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Viewfinder / Camera Frame
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF1E1E1E))
                        .border(2.dp, Color(0xFF4CAF50), RoundedCornerShape(20.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    // Camera Placeholder / Guidance Graphic
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = Color(0xFF81C784).copy(alpha = 0.6f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (hasCameraPermission) "Posisikan Barcode di Tengah Kotak" else "Izin Kamera Dibutuhkan",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        if (!hasCameraPermission) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
                            ) {
                                Text("Berikan Izin Kamera", fontSize = 12.sp)
                            }
                        }
                    }

                    // Animated Laser Scanning Line
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .height(3.dp)
                            .align(Alignment.TopCenter)
                            .padding(top = (laserPosition * 240).dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color.Transparent,
                                        Color(0xFF00E676),
                                        Color(0xFF69F0AE),
                                        Color(0xFF00E676),
                                        Color.Transparent
                                    )
                                )
                            )
                    )

                    // Corner brackets graphics for viewfinder
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp)
                    ) {
                        // Top-left
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .size(30.dp, 3.dp)
                                .background(Color(0xFF00E676))
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .size(3.dp, 30.dp)
                                .background(Color(0xFF00E676))
                        )

                        // Top-right
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(30.dp, 3.dp)
                                .background(Color(0xFF00E676))
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(3.dp, 30.dp)
                                .background(Color(0xFF00E676))
                        )

                        // Bottom-left
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .size(30.dp, 3.dp)
                                .background(Color(0xFF00E676))
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .size(3.dp, 30.dp)
                                .background(Color(0xFF00E676))
                        )

                        // Bottom-right
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(30.dp, 3.dp)
                                .background(Color(0xFF00E676))
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(3.dp, 30.dp)
                                .background(Color(0xFF00E676))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Result Card if scanned
                if (scannedResult != null) {
                    val (code, matchedStock) = scannedResult!!
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF263238)),
                        shape = RoundedCornerShape(16.dp),
                        border = borderFromColor(Color(0xFF4CAF50))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "BARCODE TERDETEKSI",
                                        color = Color(0xFF81C784),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                                Text(
                                    text = code,
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            if (matchedStock != null) {
                                Text(
                                    text = "${matchedStock.name} (${matchedStock.brand})",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = "Kualitas: ${matchedStock.quality} | Sisa Stok: ${matchedStock.qty} pcs",
                                    color = Color.LightGray,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = "Harga Jual: ${formatRupiahSimple(matchedStock.sellPrice)}",
                                    color = Color(0xFF66BB6A),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            } else {
                                Text(
                                    text = "Kode Barcode: $code (Belum terdaftar di stok)",
                                    color = Color.Yellow,
                                    fontSize = 12.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { scannedResult = null },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Scan Ulang", color = Color.White, fontSize = 12.sp)
                                }

                                Button(
                                    onClick = {
                                        onBarcodeScanned(code, matchedStock)
                                        onDismiss()
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("PILIH BARANG", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Quick Scan Tester Chips (Crucial for emulators without physical cameras)
                Text(
                    text = "PILIHAN BARCODE CEPAT (Klik untuk Uji Scan):",
                    color = Color.LightGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))

                val sampleStocks = if (registeredStocks.isNotEmpty()) registeredStocks else listOf(
                    StockItem(name = "OLI SHEL", brand = "Shell AX7", quality = "Original", modalPrice = 50000L, sellPrice = 65000L, qty = 12, barcode = "899100210036"),
                    StockItem(name = "KANVAS REM", brand = "AHM", quality = "Original", modalPrice = 14000L, sellPrice = 20000L, qty = 20, barcode = "899300410043"),
                    StockItem(name = "BUSI", brand = "NGK", quality = "Original", modalPrice = 15000L, sellPrice = 25000L, qty = 25, barcode = "899400510050"),
                    StockItem(name = "KOMSTIR", brand = "Aspira", quality = "Original", modalPrice = 60000L, sellPrice = 85000L, qty = 12, barcode = "899275310012"),
                    StockItem(name = "KLAKSON", brand = "Denso", quality = "OEM", modalPrice = 45000L, sellPrice = 65000L, qty = 6, barcode = "899275310029"),
                    StockItem(name = "VANBELT MATIC", brand = "Gates Power", quality = "Konsinyasi", modalPrice = 90000L, sellPrice = 120000L, qty = 10, barcode = "899500610067")
                )

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(sampleStocks) { stock ->
                        val code = if (stock.barcode.isNotBlank()) stock.barcode else "BRG-${stock.id}"
                        Card(
                            modifier = Modifier.clickable {
                                handleCodeFound(code)
                            },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF37474F)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                                Text(
                                    text = stock.name,
                                    color = Color.White,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = code,
                                    color = Color(0xFF80CBC4),
                                    fontSize = 10.sp
                                )
                                Text(
                                    text = formatRupiahSimple(stock.sellPrice),
                                    color = Color(0xFFA5D6A7),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Manual Input Toggle Button
                TextButton(
                    onClick = { showManualInput = !showManualInput },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF81C784))
                ) {
                    Icon(Icons.Default.Keyboard, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (showManualInput) "Tutup Input Manual" else "Ketik Nomor Barcode Manual", fontSize = 12.sp)
                }

                if (showManualInput) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = manualCodeInput,
                            onValueChange = { manualCodeInput = it },
                            placeholder = { Text("Masukkan Barcode / SKU...", color = Color.Gray) },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        )
                        Button(
                            onClick = {
                                if (manualCodeInput.isNotBlank()) {
                                    handleCodeFound(manualCodeInput.trim())
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("CARI", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

private fun borderFromColor(color: Color) = androidx.compose.foundation.BorderStroke(1.dp, color)

private fun formatRupiahSimple(amount: Long): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
    formatter.maximumFractionDigits = 0
    return formatter.format(amount)
}
