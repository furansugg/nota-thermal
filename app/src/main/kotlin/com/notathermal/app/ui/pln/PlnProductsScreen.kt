package com.notathermal.app.ui.pln

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notathermal.app.data.db.PlnProductEntity
import com.notathermal.app.data.prefs.AppSettings
import com.notathermal.app.data.prefs.SettingsRepository
import com.notathermal.app.ui.common.appViewModel
import com.notathermal.app.util.Format
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

private class PlnProductsCurrencyHolder(settings: SettingsRepository) : ViewModel() {
    val settings: StateFlow<AppSettings> = settings.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings.Default)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlnProductsScreen(onBack: () -> Unit) {
    val viewModel = appViewModel { container -> PlnProductsViewModel(container.plnProductRepository) }
    val currencyVm = appViewModel { container -> PlnProductsCurrencyHolder(container.settingsRepository) }
    val products by viewModel.products.collectAsStateWithLifecycle()
    val settings by currencyVm.settings.collectAsStateWithLifecycle()
    val currency = settings.currencySymbol

    var editing by remember { mutableStateOf<PlnProductEntity?>(null) }
    var showAdd by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<PlnProductEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Produk PLN") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Tambah Produk") }
            )
        }
    ) { padding ->
        if (products.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Belum ada produk PLN", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Tambahkan produk (mis. Token Listrik 50k) untuk mempercepat pembuatan invoice.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(vertical = 8.dp, horizontal = 0.dp)
            ) {
                items(products, key = { it.id }) { product ->
                    ListItem(
                        headlineContent = { Text(product.name) },
                        supportingContent = { Text("$currency ${Format.number(product.nominal)}") },
                        trailingContent = {
                            androidx.compose.foundation.layout.Row {
                                IconButton(onClick = { editing = product }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Ubah")
                                }
                                IconButton(onClick = { pendingDelete = product }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Hapus")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (showAdd) {
        ProductEditorDialog(
            title = "Tambah Produk PLN",
            initialName = "",
            initialNominal = "",
            currency = currency,
            onDismiss = { showAdd = false },
            onConfirm = { name, nominal ->
                viewModel.create(name, nominal)
                showAdd = false
            }
        )
    }

    editing?.let { p ->
        ProductEditorDialog(
            title = "Ubah Produk PLN",
            initialName = p.name,
            initialNominal = formatNominalForEdit(p.nominal),
            currency = currency,
            onDismiss = { editing = null },
            onConfirm = { name, nominal ->
                viewModel.update(p.id, name, nominal)
                editing = null
            }
        )
    }

    pendingDelete?.let { p ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Hapus produk?") },
            text = { Text("${p.name} akan dihapus dari daftar produk PLN.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(p.id)
                    pendingDelete = null
                }) { Text("Hapus") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Batal") }
            }
        )
    }
}

@Composable
private fun ProductEditorDialog(
    title: String,
    initialName: String,
    initialNominal: String,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (String, Double) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var nominal by remember { mutableStateOf(initialNominal) }
    val nominalValue = nominal.replace(',', '.').toDoubleOrNull() ?: 0.0
    val canSave = name.isNotBlank() && nominalValue > 0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nama produk") },
                    placeholder = { Text("Token Listrik 50k") },
                    singleLine = true
                )
                OutlinedTextField(
                    value = nominal,
                    onValueChange = { nominal = sanitizeNumber(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nominal ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim(), nominalValue) },
                enabled = canSave
            ) { Text("Simpan") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

private fun formatNominalForEdit(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

private fun sanitizeNumber(v: String): String {
    val cleaned = v.filter { it.isDigit() || it == '.' || it == ',' }
    val firstDot = cleaned.indexOfFirst { it == '.' || it == ',' }
    return if (firstDot < 0) cleaned else {
        val before = cleaned.substring(0, firstDot)
        val after = cleaned.substring(firstDot + 1).filter { it.isDigit() }
        if (after.isEmpty()) before + cleaned[firstDot] else "$before${cleaned[firstDot]}$after"
    }
}
