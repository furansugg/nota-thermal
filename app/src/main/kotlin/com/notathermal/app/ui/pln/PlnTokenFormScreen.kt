package com.notathermal.app.ui.pln

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notathermal.app.data.db.PaymentMethod
import com.notathermal.app.data.db.PlnCustomerEntity
import com.notathermal.app.data.db.PlnProductEntity
import com.notathermal.app.ui.common.appViewModel
import com.notathermal.app.util.Format

private val PLN_PAYMENT_METHODS = listOf(PaymentMethod.TUNAI, PaymentMethod.HUTANG)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlnTokenFormScreen(
    onBack: () -> Unit,
    onSaved: (Long) -> Unit
) {
    val viewModel = appViewModel { container ->
        PlnTokenFormViewModel(
            container.invoiceRepository,
            container.settingsRepository,
            container.plnCustomerRepository,
            container.plnProductRepository
        )
    }
    val holder = viewModel.holder
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val savedCustomers by viewModel.savedCustomers.collectAsStateWithLifecycle()
    val products by viewModel.products.collectAsStateWithLifecycle()
    val currency = settings.currencySymbol
    val onSave = remember(viewModel, onSaved) { { viewModel.save(onSaved) } }
    var showCustomerPicker by remember { mutableStateOf(false) }
    var showProductPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Token Listrik PLN") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = onSave, enabled = holder.canSave) {
                        Icon(Icons.Default.Save, contentDescription = "Simpan")
                    }
                }
            )
        }
    ) { padding ->
        if (showCustomerPicker) {
            PlnCustomerPickerSheet(
                customers = savedCustomers,
                onPick = { customer ->
                    holder.applyCustomer(customer)
                    showCustomerPicker = false
                },
                onDismiss = { showCustomerPicker = false }
            )
        }
        if (showProductPicker) {
            PlnProductPickerSheet(
                products = products,
                currency = currency,
                onPick = { product ->
                    holder.applyProduct(product)
                    showProductPicker = false
                },
                onDismiss = { showProductPicker = false }
            )
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Header("Produk") }
            if (products.isNotEmpty()) {
                item {
                    androidx.compose.material3.OutlinedButton(
                        onClick = { showProductPicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Inventory2, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Pilih produk PLN (${products.size})")
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = holder.productName,
                    onValueChange = { holder.productName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nama produk") },
                    placeholder = { Text("Token Listrik PLN") },
                    singleLine = true
                )
            }
            item { Header("Data Pelanggan & Meter") }
            if (savedCustomers.isNotEmpty()) {
                item {
                    androidx.compose.material3.OutlinedButton(
                        onClick = { showCustomerPicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PersonSearch, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Pilih dari pelanggan tersimpan (${savedCustomers.size})")
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = holder.customerName,
                    onValueChange = { holder.customerName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nama pelanggan") },
                    singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    value = holder.meterNo,
                    onValueChange = { holder.meterNo = it.filter(Char::isDigit) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("No. meter (ID Pelanggan)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    value = holder.kwh,
                    onValueChange = { holder.kwh = sanitizeNumber(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Jumlah kWh") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }

            item { Header("Nomor Token / Stroom") }
            item {
                OutlinedTextField(
                    value = holder.tokenNumber,
                    onValueChange = { holder.tokenNumber = formatTokenInput(it) },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nomor Token (16-20 digit)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    placeholder = { Text("1234 5678 9012 3456 7890") }
                )
            }

            item { Header("Pembayaran") }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = holder.nominal,
                        onValueChange = { holder.nominal = sanitizeNumber(it) },
                        modifier = Modifier.weight(1.4f),
                        label = { Text("Nominal ($currency)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = holder.adminFee,
                        onValueChange = { holder.adminFee = sanitizeNumber(it) },
                        modifier = Modifier.weight(1f),
                        label = { Text("Admin") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true
                    )
                }
            }
            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PLN_PAYMENT_METHODS.forEach { method ->
                        FilterChip(
                            selected = holder.paymentMethod == method,
                            onClick = { holder.paymentMethod = method },
                            label = { Text(method) }
                        )
                    }
                }
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
            item {
                OutlinedTextField(
                    value = holder.note,
                    onValueChange = { holder.note = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Catatan (opsional)") },
                    minLines = 2
                )
            }

            item { TotalsCard(holder, currency) }

            holder.error?.let { msg ->
                item { Text(msg, color = MaterialTheme.colorScheme.error) }
            }
            item {
                Button(
                    onClick = onSave,
                    enabled = holder.canSave,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (holder.saving) "Menyimpan…" else "Simpan & Lihat")
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

private fun formatTokenInput(raw: String): String {
    val digits = raw.filter { it.isDigit() }.take(20)
    return digits.chunked(4).joinToString(" ")
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
private fun Header(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlnProductPickerSheet(
    products: List<PlnProductEntity>,
    currency: String,
    onPick: (PlnProductEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Text(
                "Pilih produk PLN",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            if (products.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Belum ada produk. Tambahkan dari Pengaturan → Kelola produk PLN.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                    items(products, key = { it.id }) { p ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(p) }
                                .padding(vertical = 12.dp)
                        ) {
                            Text(
                                p.name,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "$currency ${Format.number(p.nominal)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlnCustomerPickerSheet(
    customers: List<PlnCustomerEntity>,
    onPick: (PlnCustomerEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    val filtered = remember(customers, query) {
        val q = query.trim().lowercase()
        if (q.isEmpty()) customers
        else customers.filter {
            it.customerName.lowercase().contains(q) || it.meterNo.contains(q)
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
        ) {
            Text(
                "Pelanggan PLN tersimpan",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Cari nama atau no. meter") },
                singleLine = true
            )
            Spacer(Modifier.height(8.dp))
            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Tidak ada data cocok",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                    items(filtered, key = { it.id }) { c ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onPick(c) }
                                .padding(vertical = 12.dp)
                        ) {
                            Text(
                                c.customerName,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                "No. Meter: ${c.meterNo}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TotalsCard(holder: PlnFormStateHolder, currency: String) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Nominal", modifier = Modifier.weight(1f))
                Text("$currency ${Format.number(holder.nominalValue)}")
            }
            if (holder.adminFeeValue > 0) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("Admin", modifier = Modifier.weight(1f))
                    Text("$currency ${Format.number(holder.adminFeeValue)}")
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "TOTAL",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "$currency ${Format.number(holder.total)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            if (holder.paymentMethod == PaymentMethod.HUTANG) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "BELUM LUNAS / HUTANG",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            } else if (holder.paymentReceivedValue > 0) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text("Diterima", modifier = Modifier.weight(1f))
                    Text("$currency ${Format.number(holder.paymentReceivedValue)}")
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Kembali",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "$currency ${Format.number(holder.change)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
