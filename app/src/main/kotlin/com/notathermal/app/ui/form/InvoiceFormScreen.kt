package com.notathermal.app.ui.form

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notathermal.app.data.db.PaymentMethod
import com.notathermal.app.ui.common.appViewModel
import com.notathermal.app.ui.components.SectionHeader as SharedSectionHeader
import com.notathermal.app.util.Format
import kotlinx.coroutines.launch

private val PAYMENT_METHODS = listOf(PaymentMethod.TUNAI, PaymentMethod.HUTANG)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceFormScreen(
    onBack: () -> Unit,
    onSaved: (Long) -> Unit
) {
    val viewModel = appViewModel { container ->
        InvoiceFormViewModel(
            container.invoiceRepository,
            container.settingsRepository,
            container.aiInvoiceParser
        )
    }
    val holder = viewModel.holder
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val currency = settings.currencySymbol
    val onSave = remember(viewModel, onSaved) { { viewModel.save(onSaved) } }

    var showAiSheet by remember { mutableStateOf(false) }

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
                    IconButton(onClick = { showAiSheet = true }) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Isi otomatis dengan AI")
                    }
                    IconButton(onClick = onSave, enabled = holder.canSave) {
                        Icon(Icons.Default.Save, contentDescription = "Simpan")
                    }
                }
            )
        }
    ) { padding ->
        if (showAiSheet) {
            AiInputSheet(
                apiKey = settings.geminiApiKey,
                onDismiss = { showAiSheet = false },
                onApply = { text, onResult ->
                    viewModel.applyAi(settings.geminiApiKey, text) { result ->
                        result.onSuccess { showAiSheet = false }
                        onResult(result.exceptionOrNull()?.message)
                    }
                }
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { SharedSectionHeader("Daftar Item", icon = Icons.Default.Inventory2) }
            items(holder.items, key = { it.id }) { itemHolder ->
                ItemCard(
                    itemHolder = itemHolder,
                    currency = currency,
                    canRemove = holder.items.size > 1,
                    onRemove = { holder.removeItem(itemHolder.id) }
                )
            }
            item {
                OutlinedButton(
                    onClick = { holder.addItem() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Tambah Item")
                }
            }
            item { SharedSectionHeader("Pelanggan & Catatan", icon = Icons.Default.Person) }
            item {
                OutlinedTextField(
                    value = holder.customerName,
                    onValueChange = { holder.customerName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nama pelanggan (opsional)") },
                    singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    value = holder.cashierName,
                    onValueChange = { holder.cashierName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nama kasir (opsional)") },
                    singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    value = holder.note,
                    onValueChange = { holder.note = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Catatan (opsional)") },
                    minLines = 2
                )
            }
            item { SharedSectionHeader("Diskon, Pajak, Pembayaran", icon = Icons.Default.Payments) }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = holder.discount,
                        onValueChange = { holder.discount = sanitizeNumber(it) },
                        modifier = Modifier.weight(1f),
                        label = { Text("Diskon ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = holder.taxPercent,
                        onValueChange = { holder.taxPercent = sanitizeNumber(it) },
                        modifier = Modifier.weight(1f),
                        label = { Text("Pajak (%)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
            }
            item {
                PaymentMethodPicker(
                    selected = holder.paymentMethod,
                    onSelect = { holder.paymentMethod = it }
                )
            }
            if (holder.paymentMethod == PaymentMethod.TUNAI) {
                item {
                    OutlinedTextField(
                        value = holder.paymentReceived,
                        onValueChange = { holder.paymentReceived = sanitizeNumber(it) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Diterima ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
            }
            item { TotalsCard(holder, currency) }
            holder.error?.let { msg ->
                item { Text(msg, color = MaterialTheme.colorScheme.error) }
            }
            item {
                Button(
                    onClick = onSave,
                    enabled = holder.canSave,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (holder.saving) "Menyimpan…" else "Simpan & Lihat",
                        style = MaterialTheme.typography.titleMedium
                    )
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
private fun ItemCard(
    itemHolder: ItemDraftHolder,
    currency: String,
    canRemove: Boolean,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = itemHolder.name,
                    onValueChange = { itemHolder.name = it },
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
                    value = itemHolder.quantity,
                    onValueChange = { itemHolder.quantity = sanitizeNumber(it) },
                    modifier = Modifier.weight(1f),
                    label = { Text("Qty") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = itemHolder.price,
                    onValueChange = { itemHolder.price = sanitizeNumber(it) },
                    modifier = Modifier.weight(1.4f),
                    label = { Text("Harga") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                OutlinedTextField(
                    value = itemHolder.discount,
                    onValueChange = { itemHolder.discount = sanitizeNumber(it) },
                    modifier = Modifier.weight(1f),
                    label = { Text("Diskon") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
            Text(
                "Subtotal: $currency ${Format.number(itemHolder.subtotal)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun PaymentMethodPicker(selected: String, onSelect: (String) -> Unit) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        PAYMENT_METHODS.forEach { method ->
            FilterChip(
                selected = selected == method,
                onClick = { onSelect(method) },
                label = { Text(method) }
            )
        }
    }
}

@Composable
private fun TotalsCard(holder: InvoiceFormStateHolder, currency: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            TotalLine("Subtotal", currency, holder.subtotal)
            if (holder.discountValue > 0) TotalLine("Diskon", currency, -holder.discountValue)
            if (holder.taxPercentValue > 0) {
                TotalLine("Pajak ${Format.number(holder.taxPercentValue)}%", currency, holder.taxAmount)
            }
            Spacer(Modifier.height(4.dp))
            TotalLine("TOTAL", currency, holder.total, bold = true)
            if (holder.paymentMethod == PaymentMethod.HUTANG) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "BELUM LUNAS / HUTANG",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            } else if (holder.paymentReceivedValue > 0) {
                TotalLine("Diterima", currency, holder.paymentReceivedValue)
                TotalLine("Kembali", currency, holder.change, bold = true)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiInputSheet(
    apiKey: String,
    onDismiss: () -> Unit,
    onApply: (text: String, onResult: (String?) -> Unit) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var text by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val speechLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val transcripts = result.data?.getStringArrayListExtra(
                android.speech.RecognizerIntent.EXTRA_RESULTS
            )
            val transcript = transcripts?.firstOrNull().orEmpty()
            if (transcript.isNotBlank()) {
                text = if (text.isBlank()) transcript else "$text $transcript"
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(8.dp))
                Text("Isi otomatis dengan AI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            }
            Text(
                "Ketik atau ucapkan apa saja, mis. \"3 indomie 5000, 2 teh botol 4500, untuk pak budi\".",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Deskripsi invoice") },
                minLines = 3,
                maxLines = 6
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AssistChip(
                    onClick = {
                        runCatching {
                            val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                putExtra(
                                    android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                                    android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                                )
                                putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "id-ID")
                                putExtra(android.speech.RecognizerIntent.EXTRA_PROMPT, "Sebutkan item invoice")
                            }
                            speechLauncher.launch(intent)
                        }.onFailure {
                            error = "Speech-to-text tidak tersedia di device ini. Ketik manual saja."
                        }
                    },
                    label = { Text("Bicara") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(AssistChipDefaults.IconSize)
                        )
                    }
                )
                Text(
                    "atau ketik bebas",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            error?.let { msg ->
                Text(msg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            if (apiKey.isBlank()) {
                Text(
                    "Kunci API Gemini belum diisi. Buka Pengaturan → kolom \"Kunci API Gemini\".",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            sheetState.hide()
                            onDismiss()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Batal") }
                Button(
                    onClick = {
                        error = null
                        loading = true
                        onApply(text) { errMsg ->
                            loading = false
                            error = errMsg
                        }
                    },
                    enabled = !loading && text.isNotBlank() && apiKey.isNotBlank(),
                    modifier = Modifier.weight(1.4f)
                ) {
                    if (loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Memproses…")
                    } else {
                        Text("Generate")
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
