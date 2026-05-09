package com.notathermal.app.ui.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notathermal.app.ui.common.appViewModel
import com.notathermal.app.util.Format

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceFormScreen(
    onBack: () -> Unit,
    onSaved: (Long) -> Unit
) {
    val viewModel = appViewModel { container ->
        InvoiceFormViewModel(container.invoiceRepository, container.settingsRepository)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val currency = settings.currencySymbol

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Buat Invoice") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.save(onSaved) },
                        enabled = state.canSave
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Simpan")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SectionHeader("Daftar Item") }
            items(state.items, key = { it.id }) { draft ->
                ItemCard(
                    draft = draft,
                    currency = currency,
                    onChange = { transform -> viewModel.updateItem(draft.id, transform) },
                    onRemove = { viewModel.removeItem(draft.id) },
                    canRemove = state.items.size > 1
                )
            }
            item {
                OutlinedButton(
                    onClick = { viewModel.addItem() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Tambah Item")
                }
            }
            item { SectionHeader("Pelanggan & Catatan") }
            item {
                OutlinedTextField(
                    value = state.customerName,
                    onValueChange = { v -> viewModel.updateField { it.copy(customerName = v) } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nama pelanggan (opsional)") },
                    singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    value = state.cashierName,
                    onValueChange = { v -> viewModel.updateField { it.copy(cashierName = v) } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nama kasir (opsional)") },
                    singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    value = state.note,
                    onValueChange = { v -> viewModel.updateField { it.copy(note = v) } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Catatan (opsional)") },
                    minLines = 2
                )
            }
            item { SectionHeader("Diskon, Pajak, Pembayaran") }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = state.discount,
                        onValueChange = { v -> viewModel.updateField { it.copy(discount = sanitizeNumber(v)) } },
                        modifier = Modifier.weight(1f),
                        label = { Text("Diskon ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = state.taxPercent,
                        onValueChange = { v -> viewModel.updateField { it.copy(taxPercent = sanitizeNumber(v)) } },
                        modifier = Modifier.weight(1f),
                        label = { Text("Pajak (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
            }
            item { PaymentMethodPicker(state.paymentMethod) { v -> viewModel.updateField { it.copy(paymentMethod = v) } } }
            item {
                OutlinedTextField(
                    value = state.paymentReceived,
                    onValueChange = { v -> viewModel.updateField { it.copy(paymentReceived = sanitizeNumber(v)) } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Diterima ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
            item { TotalsCard(state, currency) }
            state.error?.let { msg ->
                item { Text(msg, color = MaterialTheme.colorScheme.error) }
            }
            item {
                Button(
                    onClick = { viewModel.save(onSaved) },
                    enabled = state.canSave,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (state.saving) "Menyimpan…" else "Simpan & Lihat")
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

private fun sanitizeNumber(v: String): String {
    val cleaned = v.filter { it.isDigit() || it == '.' || it == ',' }
    val firstDot = cleaned.indexOfFirst { it == '.' || it == ',' }
    return if (firstDot < 0) cleaned else {
        val before = cleaned.substring(0, firstDot)
        val after = cleaned.substring(firstDot + 1).filter { it.isDigit() }
        if (after.isEmpty()) before + cleaned[firstDot] else "$before${cleaned[firstDot]}$after"
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun ItemCard(
    draft: ItemDraft,
    currency: String,
    onChange: ((ItemDraft) -> ItemDraft) -> Unit,
    onRemove: () -> Unit,
    canRemove: Boolean
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = { v -> onChange { it.copy(name = v) } },
                    modifier = Modifier.weight(1f),
                    label = { Text("Nama item") },
                    singleLine = true
                )
                if (canRemove) {
                    IconButton(onClick = onRemove) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus")
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = draft.quantity,
                    onValueChange = { v -> onChange { it.copy(quantity = sanitizeNumber(v)) } },
                    modifier = Modifier.weight(1f),
                    label = { Text("Qty") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = draft.price,
                    onValueChange = { v -> onChange { it.copy(price = sanitizeNumber(v)) } },
                    modifier = Modifier.weight(1.4f),
                    label = { Text("Harga") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = draft.discount,
                    onValueChange = { v -> onChange { it.copy(discount = sanitizeNumber(v)) } },
                    modifier = Modifier.weight(1f),
                    label = { Text("Diskon") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
            Text(
                "Subtotal: $currency ${Format.number(draft.subtotal)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PaymentMethodPicker(selected: String, onSelect: (String) -> Unit) {
    val methods = listOf("TUNAI", "QRIS", "DEBIT", "KREDIT", "TRANSFER")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        methods.forEach { method ->
            FilterChip(
                selected = selected == method,
                onClick = { onSelect(method) },
                label = { Text(method) }
            )
        }
    }
}

@Composable
private fun TotalsCard(state: FormState, currency: String) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TotalLine("Subtotal", currency, state.subtotal)
            if (state.discountValue > 0) TotalLine("Diskon", currency, -state.discountValue)
            if (state.taxPercentValue > 0) TotalLine("Pajak ${Format.number(state.taxPercentValue)}%", currency, state.taxAmount)
            Spacer(Modifier.height(4.dp))
            TotalLine("TOTAL", currency, state.total, bold = true)
            if (state.paymentReceivedValue > 0) {
                TotalLine("Diterima", currency, state.paymentReceivedValue)
                TotalLine("Kembali", currency, state.change, bold = true)
            }
        }
    }
}

@Composable
private fun TotalLine(label: String, currency: String, value: Double, bold: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(
            label,
            modifier = Modifier.weight(1f),
            style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
        Text(
            "$currency ${Format.number(value)}",
            style = if (bold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal
        )
    }
}
