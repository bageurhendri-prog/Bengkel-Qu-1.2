package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.ApprovalStatus
import com.example.ui.BengkelScreen
import com.example.ui.BengkelViewModel

@Composable
fun ReportScreen(viewModel: BengkelViewModel) {
    val context = LocalContext.current
    val rejects by viewModel.rejectItems.collectAsStateWithLifecycle()
    val incoming by viewModel.incomingStocks.collectAsStateWithLifecycle()
    val expenses by viewModel.expenseItems.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            BengkelTopBar(
                title = "REPORT & PERSETUJUAN",
                onBack = { viewModel.navigateTo(BengkelScreen.DASHBOARD) }
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // SECTION 1: PENGAJUAN BARANG REJECT (matching diagram: OLI BOCOR + SETUJUI / KOREKSI)
            ReportApprovalSection(
                title = "PENGAJUAN BARANG REJECT",
                headerColor = Color(0xFFD32F2F)
            ) {
                if (rejects.isEmpty()) {
                    Text("Tidak ada pengajuan barang reject.", fontSize = 12.sp, color = Color.Gray)
                } else {
                    rejects.forEach { item ->
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${item.itemName} (${item.qty} pcs)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Alasan: ${item.reason}",
                                        fontSize = 12.sp,
                                        color = Color.Red
                                    )
                                }

                                if (item.status == ApprovalStatus.PENDING) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Button(
                                            onClick = { viewModel.approveRejectItem(item.id, true) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("SETUJUI", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        OutlinedButton(
                                            onClick = { viewModel.approveRejectItem(item.id, false) },
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("KOREKSI", fontSize = 11.sp, color = Color(0xFFC62828))
                                        }
                                    }
                                } else {
                                    StatusBadge(item.status)
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFFEEEEEE))
                        }
                    }
                }
            }

            // SECTION 2: BARANG DATANG (matching diagram: KOMSTIR 12, KLAKSON 6, OLI SHEL 12 + SETUJUI / KOREKSI)
            ReportApprovalSection(
                title = "BARANG DATANG",
                headerColor = Color(0xFF2E7D32)
            ) {
                if (incoming.isEmpty()) {
                    Text("Tidak ada data barang datang.", fontSize = 12.sp, color = Color.Gray)
                } else {
                    incoming.forEach { item ->
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.itemName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = "Jumlah: ${item.qty} pcs",
                                        fontSize = 12.sp,
                                        color = Color.DarkGray
                                    )
                                }

                                if (item.status == ApprovalStatus.PENDING) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Button(
                                            onClick = { viewModel.approveIncomingStock(item.id, true) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("SETUJUI", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        OutlinedButton(
                                            onClick = { viewModel.approveIncomingStock(item.id, false) },
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("KOREKSI", fontSize = 11.sp, color = Color(0xFFC62828))
                                        }
                                    }
                                } else {
                                    StatusBadge(item.status)
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFFEEEEEE))
                        }
                    }
                }
            }

            // SECTION 3: BELANJA / PENGELUARAN (matching diagram: AIR GALON 30000, BENSIN 20000, TOTAL 50000 + SETUJUI / KOREKSI)
            ReportApprovalSection(
                title = "BELANJA / OPERASIONAL",
                headerColor = Color(0xFFE65100)
            ) {
                if (expenses.isEmpty()) {
                    Text("Tidak ada data belanja diajukan.", fontSize = 12.sp, color = Color.Gray)
                } else {
                    expenses.forEach { exp ->
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = exp.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = formatRupiah(exp.amount),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                if (exp.status == ApprovalStatus.PENDING) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Button(
                                            onClick = { viewModel.approveExpenseItem(exp.id, true) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("SETUJUI", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                        OutlinedButton(
                                            onClick = { viewModel.approveExpenseItem(exp.id, false) },
                                            shape = RoundedCornerShape(6.dp),
                                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                        ) {
                                            Text("KOREKSI", fontSize = 11.sp, color = Color(0xFFC62828))
                                        }
                                    }
                                } else {
                                    StatusBadge(exp.status)
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), color = Color(0xFFEEEEEE))
                        }
                    }

                    val total = expenses.sumOf { it.amount }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "TOTAL", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = formatRupiah(total), fontWeight = FontWeight.Black, fontSize = 15.sp, color = Color(0xFFE65100))
                    }
                }
            }

            // SECTION 4: DETAIL CUSTOMER (matching diagram: SAVE EXCEL | SEND WA EXCEL)
            ExportCard(
                title = "DETAIL CUSTOMER",
                description = "Ekspor riwayat pelanggan & servis motor ke Excel (CSV)",
                onSaveExcel = { viewModel.exportCustomerData(context, sendViaWhatsApp = false) },
                onSendWhatsApp = { viewModel.exportCustomerData(context, sendViaWhatsApp = true) }
            )

            // SECTION 5: DETAIL STOK (matching diagram: SAVE EXCEL | SEND WA EXCEL)
            ExportCard(
                title = "DETAIL STOK",
                description = "Ekspor daftar harga modal, harga jual, & qty stok ke Excel (CSV)",
                onSaveExcel = { viewModel.exportStockData(context, sendViaWhatsApp = false) },
                onSendWhatsApp = { viewModel.exportStockData(context, sendViaWhatsApp = true) }
            )
        }
    }
}

@Composable
fun ReportApprovalSection(
    title: String,
    headerColor: Color,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerColor)
                    .padding(horizontal = 16.dp, vertical = 10.dp)
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
            }
            Column(modifier = Modifier.padding(14.dp)) {
                content()
            }
        }
    }
}

@Composable
fun StatusBadge(status: ApprovalStatus) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(
                when (status) {
                    ApprovalStatus.DISETUJUI -> Color(0xFFE8F5E9)
                    ApprovalStatus.KOREKSI -> Color(0xFFFFEBEE)
                    else -> Color(0xFFFFF3E0)
                }
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = status.name,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = when (status) {
                ApprovalStatus.DISETUJUI -> Color(0xFF2E7D32)
                ApprovalStatus.KOREKSI -> Color(0xFFC62828)
                else -> Color(0xFFE65100)
            }
        )
    }
}

@Composable
fun ExportCard(
    title: String,
    description: String,
    onSaveExcel: () -> Unit,
    onSendWhatsApp: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons matching diagram: "SAVE EXCEL" | "SEND WA EXCEL"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onSaveExcel,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2E7D32),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "SAVE EXCEL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onSendWhatsApp,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1B5E20),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = "SEND WA EXCEL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
