package com.notathermal.app.ui.printer

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notathermal.app.print.PrinterDevice
import com.notathermal.app.ui.common.appViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrinterScreen(onBack: () -> Unit) {
    val viewModel = appViewModel { container ->
        PrinterViewModel(container.bluetoothPrinterService, container.settingsRepository)
    }
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        viewModel.refresh()
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                )
            )
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
        }
        viewModel.refresh()
    }

    LaunchedEffect(state.message, state.error) {
        state.message?.let { snackbarHostState.showSnackbar(it) }
        state.error?.let { snackbarHostState.showSnackbar(it) }
        viewModel.clearMessages()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Printer") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali") }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                StatusCard(
                    bluetoothEnabled = state.bluetoothEnabled,
                    hasPermission = state.hasPermission,
                    onEnableClick = {
                        context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                    }
                )
            }
            if (settings.printerAddress != null) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Printer terpilih", style = MaterialTheme.typography.labelLarge)
                            Text(
                                settings.printerName ?: settings.printerAddress.orEmpty(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                settings.printerAddress.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.testPrint() },
                                    enabled = !state.testing
                                ) { Text(if (state.testing) "Mencetak…" else "Test Print") }
                                OutlinedButton(onClick = { viewModel.clearDevice() }) {
                                    Text("Lepas")
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    "Perangkat ter-pair",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
            if (state.devices.isEmpty()) {
                item {
                    Text(
                        "Belum ada printer ter-pair. Pair printer di Setelan Bluetooth Android dulu, lalu tekan Refresh.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                items(state.devices, key = { it.address }) { device ->
                    DeviceRow(
                        device = device,
                        selected = settings.printerAddress == device.address,
                        onClick = { viewModel.selectDevice(device) }
                    )
                }
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun StatusCard(
    bluetoothEnabled: Boolean,
    hasPermission: Boolean,
    onEnableClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (bluetoothEnabled) Icons.Default.Bluetooth else Icons.Default.BluetoothDisabled,
                    contentDescription = null,
                    tint = if (bluetoothEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(0.dp))
                Text(
                    "  Bluetooth: ${if (bluetoothEnabled) "aktif" else "nonaktif"}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                "Izin Bluetooth: ${if (hasPermission) "diberikan" else "belum"}",
                style = MaterialTheme.typography.bodyMedium
            )
            if (!bluetoothEnabled) {
                OutlinedButton(onClick = onEnableClick) {
                    Text("Buka pengaturan Bluetooth")
                }
            }
        }
    }
}

@Composable
private fun DeviceRow(
    device: PrinterDevice,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Bluetooth, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(0.dp))
            Column(modifier = Modifier.fillMaxWidth().weight(1f).padding(start = 12.dp)) {
                Text(device.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "Terpilih",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
