package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.BengkelScreen
import com.example.ui.BengkelViewModel

@Composable
fun SetoranScreen(viewModel: BengkelViewModel) {
    val context = LocalContext.current
    val recentDeposits by viewModel.cashDeposits.collectAsStateWithLifecycle()

    var c100k by remember { mutableStateOf(15) }
    var c50k by remember { mutableStateOf(8) }
    var c20k by remember { mutableStateOf(4) }
    var c10k by remember { mutableStateOf(1) }
    var c5k by remember { mutableStateOf(1) }
    var c2k by remember { mutableStateOf(2) }
    var c1k by remember { mutableStateOf(1) }
    var c500 by remember { mutableStateOf(0) }

    val currentTotal = (c100k * 100000L) +
            (c50k * 50000L) +
            (c20k * 20000L) +
            (c10k * 10000L) +
            (c5k * 5000L) +
            (c2k * 2000L) +
            (c1k * 1000L) +
            (c500 * 500L)

    Scaffold(
        topBar = {
            BengkelTopBar(
                title = "SETORAN KASIR",
                onBack = { viewModel.navigateTo(BengkelScreen.SERVICE_QUEUE) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Live Total Banner Card (matching diagram: "CASH 2000000", "TOTAL 2000000")
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TOTAL CASH SETORAN",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = formatRupiah(currentTotal),
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Denomination Table (matching diagram: 100000, 50000, 20000, 10000, 5000, 2000, 1000, 500)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "HITUNG PECAHAN UANG KAS",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    DenominationRow(nominal = 100000, count = c100k, onCountChange = { c100k = it })
                    DenominationRow(nominal = 50000, count = c50k, onCountChange = { c50k = it })
                    DenominationRow(nominal = 20000, count = c20k, onCountChange = { c20k = it })
                    DenominationRow(nominal = 10000, count = c10k, onCountChange = { c10k = it })
                    DenominationRow(nominal = 5000, count = c5k, onCountChange = { c5k = it })
                    DenominationRow(nominal = 2000, count = c2k, onCountChange = { c2k = it })
                    DenominationRow(nominal = 1000, count = c1k, onCountChange = { c1k = it })
                    DenominationRow(nominal = 500, count = c500, onCountChange = { c500 = it })

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "TOTAL", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            text = formatRupiah(currentTotal),
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Button SETOR (matching diagram)
            Button(
                onClick = {
                    if (currentTotal <= 0L) {
                        Toast.makeText(context, "Jumlah setoran harus lebih dari Rp 0", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.submitCashDeposit(
                            c100k, c50k, c20k, c10k, c5k, c2k, c1k, c500, context
                        )
                    }
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
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "SETOR",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun DenominationRow(
    nominal: Long,
    count: Int,
    onCountChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.width(90.dp)) {
            Text(
                text = "Rp ${nominal / 1000}k",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { if (count > 0) onCountChange(count - 1) },
                modifier = Modifier
                    .size(28.dp)
                    .background(Color(0xFFEEEEEE), RoundedCornerShape(6.dp))
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Kurang", modifier = Modifier.size(16.dp))
            }

            Text(
                text = "$count",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 12.dp)
            )

            IconButton(
                onClick = { onCountChange(count + 1) },
                modifier = Modifier
                    .size(28.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(6.dp))
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Tambah",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Text(
            text = formatRupiah(nominal * count),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.DarkGray
        )
    }
}
