package com.notathermal.app.ui.pln

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notathermal.app.data.db.PlnCustomerEntity
import com.notathermal.app.ui.common.appViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlnCustomersScreen(onBack: () -> Unit) {
    val viewModel = appViewModel { container -> PlnCustomersViewModel(container.plnCustomerRepository) }
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<PlnCustomerEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pelanggan PLN") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { padding ->
        if (customers.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Belum ada pelanggan tersimpan", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Pelanggan akan otomatis tersimpan saat Anda membuat invoice Token PLN.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(customers, key = { it.id }) { c ->
                    ListItem(
                        headlineContent = { Text(c.customerName) },
                        supportingContent = { Text("No. Meter: ${c.meterNo}") },
                        trailingContent = {
                            IconButton(onClick = { pendingDelete = c }) {
                                Icon(Icons.Default.Delete, contentDescription = "Hapus")
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    pendingDelete?.let { c ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Hapus pelanggan?") },
            text = {
                Text(
                    "${c.customerName} (No. Meter ${c.meterNo}) akan dihapus dari daftar " +
                        "pelanggan tersimpan. Riwayat invoice tidak ikut terhapus."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(c.id)
                    pendingDelete = null
                }) { Text("Hapus") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Batal") }
            }
        )
    }
}
