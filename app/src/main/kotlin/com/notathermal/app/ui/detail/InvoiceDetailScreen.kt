package com.notathermal.app.ui.detail

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notathermal.app.data.db.PaymentMethod
import com.notathermal.app.ui.common.appViewModel
import com.notathermal.app.util.Format

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceDetailScreen(
    invoiceId: Long,
    onBack: () -> Unit,
    onOpenPrinter: () -> Unit
) {
    val viewModel = appViewModel { container ->
        InvoiceDetailViewModel(
            invoiceId = invoiceId,
            invoiceRepository = container.invoiceRepository,
            settingsRepository = container.settingsRepository,
            printerService = container.bluetoothPrinterService,
            composer = container.receiptComposer
        )
    }
    val invoice by viewModel.invoice.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showMarkPaidDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.statusMessage, state.errorMessage) {
        state.statusMessage?.let { snackbarHostState.showSnackbar(it) }
        state.errorMessage?.let { snackbarHostState.showSnackbar(it) }
        viewModel.clearMessages()
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Hapus invoice ini?") },
            text = { Text("Invoice akan dihapus permanen dari riwayat.") },
            confirmButton = {
                Button(onClick = {
                    showDeleteConfirm = false
                    viewModel.delete(onDeleted = onBack)
                }) { Text("Hapus") }
            },
            dismissButton = {
                OutlinedButton(onClick = { showDeleteConfirm = false }) { Text("Batal") }
            }
        )
    }

    if (showMarkPaidDialog) {
        val total = invoice?.invoice?.total ?: 0.0
        val currency = settings.currencySymbol
        MarkPaidDialog(
            total = total,
            currency = currency,
            onDismiss = { showMarkPaidDialog = false },
            onConfirm = { received ->
                showMarkPaidDialog = false
                viewModel.markPaid(PaymentMethod.TUNAI, received)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(invoice?.invoice?.code ?: "Invoice") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali") }
                },
                actions = {
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Hapus")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        val current = invoice
        if (current == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }
        val isHutang = current.invoice.paymentMethod == PaymentMethod.HUTANG
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (isHutang) {
                item {
                    FilledTonalButton(
                        onClick = { showMarkPaidDialog = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(Modifier.padding(start = 8.dp))
                        Text(
                            " Tandai Sudah Lunas",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceContainerLowest, RoundedCornerShape(20.dp))
                            .padding(18.dp)
                    ) {
                        Text(
                            text = viewModel.previewText(),
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
            item {
                Button(
                    onClick = { viewModel.print() },
                    enabled = !state.printing,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = MaterialTheme.shapes.large,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Icon(Icons.Default.Print, contentDescription = null)
                    Spacer(Modifier.padding(start = 8.dp))
                    Text(
                        text = if (state.printing) " Mencetak…"
                        else " Cetak ke printer (${settings.paperWidth.mm.toInt()}mm)",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            if (settings.printerAddress.isNullOrBlank()) {
                item {
                    OutlinedButton(
                        onClick = onOpenPrinter,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text("Pilih printer Bluetooth")
                    }
                }
            } else {
                item {
                    Text(
                        text = "Printer: ${settings.printerName ?: settings.printerAddress}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun MarkPaidDialog(
    total: Double,
    currency: String,
    onDismiss: () -> Unit,
    onConfirm: (received: Double) -> Unit
) {
    // Seed with a clean machine-readable string (not the locale-formatted one)
    // so it parses back reliably.
    var received by remember {
        mutableStateOf(if (total > 0) total.toCleanString() else "")
    }
    val parsed = received.replace(',', '.').toDoubleOrNull() ?: 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Tandai Lunas") },
        text = {
            Column {
                Text("Total: $currency ${Format.number(total)}")
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = received,
                    onValueChange = { v -> received = sanitizeNumber(v) },
                    label = { Text("Jumlah diterima ($currency)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
                if (parsed > total) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Kembali: $currency ${Format.number(parsed - total)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(parsed) },
                enabled = parsed >= total
            ) { Text("Lunas") }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) { Text("Batal") }
        }
    )
}

private fun Double.toCleanString(): String =
    if (this == toLong().toDouble()) toLong().toString() else toString()

private fun sanitizeNumber(v: String): String {
    val cleaned = v.filter { it.isDigit() || it == '.' || it == ',' }
    val firstDot = cleaned.indexOfFirst { it == '.' || it == ',' }
    return if (firstDot < 0) cleaned else {
        val before = cleaned.substring(0, firstDot)
        val after = cleaned.substring(firstDot + 1).filter { it.isDigit() }
        if (after.isEmpty()) before + cleaned[firstDot] else "$before${cleaned[firstDot]}$after"
    }
}
