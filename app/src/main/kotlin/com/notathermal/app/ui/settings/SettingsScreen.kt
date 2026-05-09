package com.notathermal.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notathermal.app.data.prefs.AppSettings
import com.notathermal.app.domain.PaperWidth
import com.notathermal.app.domain.TextAlign
import com.notathermal.app.ui.common.appViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val viewModel = appViewModel { container -> SettingsViewModel(container.settingsRepository) }
    val loaded by viewModel.settings.collectAsStateWithLifecycle()
    val saving by viewModel.saving.collectAsStateWithLifecycle()

    val current = loaded
    if (current == null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Pengaturan") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                        }
                    }
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        return
    }

    SettingsForm(
        initial = current,
        saving = saving,
        onBack = onBack,
        onSave = viewModel::save
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsForm(
    initial: AppSettings,
    saving: Boolean,
    onBack: () -> Unit,
    onSave: (
        storeName: String,
        storeAddress: String,
        storePhone: String,
        headerText: String,
        footerText: String,
        paperWidth: PaperWidth,
        titleAlignment: TextAlign,
        currencySymbol: String,
        showCashier: Boolean,
        showCustomer: Boolean,
        cutPaper: Boolean,
        copies: Int,
        taxPercentDefault: Double,
        onDone: () -> Unit
    ) -> Unit
) {
    var storeName by rememberSaveable { mutableStateOf(initial.storeName) }
    var storeAddress by rememberSaveable { mutableStateOf(initial.storeAddress) }
    var storePhone by rememberSaveable { mutableStateOf(initial.storePhone) }
    var headerText by rememberSaveable { mutableStateOf(initial.headerText) }
    var footerText by rememberSaveable { mutableStateOf(initial.footerText) }
    var paperWidth by rememberSaveable { mutableStateOf(initial.paperWidth) }
    var titleAlign by rememberSaveable { mutableStateOf(initial.titleAlignment) }
    var currency by rememberSaveable { mutableStateOf(initial.currencySymbol) }
    var showCashier by rememberSaveable { mutableStateOf(initial.showCashier) }
    var showCustomer by rememberSaveable { mutableStateOf(initial.showCustomer) }
    var cutPaper by rememberSaveable { mutableStateOf(initial.cutPaper) }
    var copies by rememberSaveable { mutableStateOf(initial.copies.toString()) }
    var taxPercent by rememberSaveable { mutableStateOf(initial.taxPercentDefault.toString()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pengaturan") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
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
                Column {
                    Text("Ukuran kertas", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(4.dp))
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
            }
            item {
                Column {
                    Text("Alignment header / footer", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(4.dp))
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
            item { ToggleRow("Auto cut kertas", cutPaper) { cutPaper = it } }
            item { ToggleRow("Tampilkan nama kasir di struk", showCashier) { showCashier = it } }
            item { ToggleRow("Tampilkan nama pelanggan di struk", showCustomer) { showCustomer = it } }

            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        onSave(
                            storeName,
                            storeAddress,
                            storePhone,
                            headerText,
                            footerText,
                            paperWidth,
                            titleAlign,
                            currency.ifBlank { "Rp" },
                            showCashier,
                            showCustomer,
                            cutPaper,
                            copies.toIntOrNull()?.coerceAtLeast(1) ?: 1,
                            taxPercent.toDoubleOrNull() ?: 0.0,
                            onBack
                        )
                    },
                    enabled = !saving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (saving) "Menyimpan…" else "Simpan")
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun Section(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
