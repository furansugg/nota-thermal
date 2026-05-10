package com.notathermal.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PeopleAlt
import androidx.compose.foundation.clickable
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

private val PAPER_WIDTHS: List<PaperWidth> = PaperWidth.values().toList()
private val TEXT_ALIGNS: List<TextAlign> = TextAlign.values().toList()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenPlnCustomers: () -> Unit = {},
    onOpenPlnProducts: () -> Unit = {}
) {
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
        onOpenPlnCustomers = onOpenPlnCustomers,
        onOpenPlnProducts = onOpenPlnProducts,
        onSave = viewModel::save
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsForm(
    initial: AppSettings,
    saving: Boolean,
    onBack: () -> Unit,
    onOpenPlnCustomers: () -> Unit,
    onOpenPlnProducts: () -> Unit,
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
        geminiApiKey: String,
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
    var geminiApiKey by rememberSaveable { mutableStateOf(initial.geminiApiKey) }
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    val scrollState = rememberScrollState()

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
        // Plain vertical-scroll Column for a fixed-size form: avoids LazyColumn's
        // per-item compose/measure overhead which causes visible jank when
        // scrolling past unmeasured TextField items.
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Section("Data Toko")
            OutlinedTextField(
                value = storeName,
                onValueChange = { storeName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nama toko") },
                singleLine = true
            )
            OutlinedTextField(
                value = storeAddress,
                onValueChange = { storeAddress = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Alamat") },
                minLines = 2
            )
            OutlinedTextField(
                value = storePhone,
                onValueChange = { storePhone = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Telepon") },
                singleLine = true
            )
            OutlinedTextField(
                value = headerText,
                onValueChange = { headerText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Header tambahan (opsional)") },
                minLines = 2
            )
            OutlinedTextField(
                value = footerText,
                onValueChange = { footerText = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Footer (kalimat penutup)") },
                minLines = 2
            )

            Section("Cetak")
            Text("Ukuran kertas", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PAPER_WIDTHS.forEach { p ->
                    FilterChip(
                        selected = paperWidth == p,
                        onClick = { paperWidth = p },
                        label = { Text("${p.mm.toInt()}mm") }
                    )
                }
            }
            Text("Alignment header / footer", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TEXT_ALIGNS.forEach { a ->
                    FilterChip(
                        selected = titleAlign == a,
                        onClick = { titleAlign = a },
                        label = { Text(a.name) }
                    )
                }
            }
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
            OutlinedTextField(
                value = taxPercent,
                onValueChange = { taxPercent = it.replace(',', '.').filter { c -> c.isDigit() || c == '.' } },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Pajak default (%)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
            ToggleRow("Auto cut kertas", cutPaper) { cutPaper = it }
            ToggleRow("Tampilkan nama kasir di struk", showCashier) { showCashier = it }
            ToggleRow("Tampilkan nama pelanggan di struk", showCustomer) { showCustomer = it }

            Section("Token PLN")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenPlnProducts)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Inventory2, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Kelola produk PLN", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Atur daftar produk (nama + nominal) untuk dipilih cepat di form Token PLN.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onOpenPlnCustomers)
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.PeopleAlt, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Kelola pelanggan PLN", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Lihat & hapus daftar pelanggan yang tersimpan otomatis dari invoice Token PLN.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null
                )
            }

            Section("AI (Gemini)")
            Text(
                "Diperlukan untuk fitur \"Isi otomatis dengan AI\" di form invoice. " +
                    "Kunci tidak dikirim ke siapapun selain Google. Tier gratis: ~60 request/menit.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = geminiApiKey,
                onValueChange = { geminiApiKey = it.trim() },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Kunci API Gemini") },
                placeholder = { Text("AIza...") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Default.AutoAwesome, contentDescription = null) }
            )
            androidx.compose.material3.TextButton(
                onClick = { uriHandler.openUri("https://aistudio.google.com/apikey") }
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Dapatkan API key gratis")
            }

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
                        geminiApiKey,
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
