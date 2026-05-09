package com.notathermal.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notathermal.app.domain.PaperWidth
import com.notathermal.app.domain.TextAlign
import com.notathermal.app.ui.common.appViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val viewModel = appViewModel { container -> SettingsViewModel(container.settingsRepository) }
    val current by viewModel.settings.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()

    var initialized by rememberSaveable { mutableStateOf(false) }
    var storeName by rememberSaveable { mutableStateOf("") }
    var storeAddress by rememberSaveable { mutableStateOf("") }
    var storePhone by rememberSaveable { mutableStateOf("") }
    var headerText by rememberSaveable { mutableStateOf("") }
    var footerText by rememberSaveable { mutableStateOf("") }
    var paperWidth by rememberSaveable { mutableStateOf(PaperWidth.MM_58) }
    var titleAlign by rememberSaveable { mutableStateOf(TextAlign.CENTER) }
    var currency by rememberSaveable { mutableStateOf("Rp") }
    var showCashier by rememberSaveable { mutableStateOf(false) }
    var showCustomer by rememberSaveable { mutableStateOf(true) }
    var cutPaper by rememberSaveable { mutableStateOf(true) }
    var copies by rememberSaveable { mutableStateOf("1") }
    var taxPercent by rememberSaveable { mutableStateOf("0") }

    LaunchedEffect(current) {
        if (!initialized) {
            storeName = current.storeName
            storeAddress = current.storeAddress
            storePhone = current.storePhone
            headerText = current.headerText
            footerText = current.footerText
            paperWidth = current.paperWidth
            titleAlign = current.titleAlignment
            currency = current.currencySymbol
            showCashier = current.showCashier
            showCustomer = current.showCustomer
            cutPaper = current.cutPaper
            copies = current.copies.toString()
            taxPercent = current.taxPercentDefault.toString()
            initialized = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali") }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Section("Data Toko") }
            item {
                OutlinedTextField(
                    value = storeName,
                    onValueChange = { storeName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nama toko") },
                    singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    value = storeAddress,
                    onValueChange = { storeAddress = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Alamat") },
                    minLines = 2
                )
            }
            item {
                OutlinedTextField(
                    value = storePhone,
                    onValueChange = { storePhone = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Telepon") },
                    singleLine = true
                )
            }
            item {
                OutlinedTextField(
                    value = headerText,
                    onValueChange = { headerText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Header tambahan (opsional)") },
                    minLines = 2
                )
            }
            item {
                OutlinedTextField(
                    value = footerText,
                    onValueChange = { footerText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Footer (kalimat penutup)") },
                    minLines = 2
                )
            }

            item { Section("Cetak") }
            item {
                Text("Ukuran kertas", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PaperWidth.values().forEach { p ->
                        FilterChip(
                            selected = paperWidth == p,
                            onClick = { paperWidth = p },
                            label = { Text("${p.mm.toInt()}mm") }
                        )
                    }
                }
            }
            item {
                Text("Alignment header / footer", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextAlign.values().forEach { a ->
                        FilterChip(
                            selected = titleAlign == a,
                            onClick = { titleAlign = a },
                            label = { Text(a.name) }
                        )
                    }
                }
            }
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = currency,
                        onValueChange = { currency = it },
                        modifier = Modifier.weight(1f),
                        label = { Text("Mata uang") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = copies,
                        onValueChange = { copies = it.filter(Char::isDigit).take(2) },
                        modifier = Modifier.weight(1f),
                        label = { Text("Jumlah copy") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )
                }
            }
            item {
                OutlinedTextField(
                    value = taxPercent,
                    onValueChange = { taxPercent = it.replace(',', '.').filter { c -> c.isDigit() || c == '.' } },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Pajak default (%)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )
            }
            item {
                ToggleRow("Auto cut kertas", cutPaper) { cutPaper = it }
            }
            item {
                ToggleRow("Tampilkan nama kasir di struk", showCashier) { showCashier = it }
            }
            item {
                ToggleRow("Tampilkan nama pelanggan di struk", showCustomer) { showCustomer = it }
            }

            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        viewModel.save(
                            storeName = storeName,
                            storeAddress = storeAddress,
                            storePhone = storePhone,
                            headerText = headerText,
                            footerText = footerText,
                            paperWidth = paperWidth,
                            titleAlignment = titleAlign,
                            currencySymbol = currency.ifBlank { "Rp" },
                            showCashier = showCashier,
                            showCustomer = showCustomer,
                            cutPaper = cutPaper,
                            copies = copies.toIntOrNull() ?: 1,
                            taxPercentDefault = taxPercent.toDoubleOrNull() ?: 0.0,
                            onDone = onBack
                        )
                    },
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (saving) "Menyimpan…" else "Simpan")
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun Section(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun ToggleRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = value, onCheckedChange = onChange)
    }
}
