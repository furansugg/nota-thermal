package com.notathermal.app.ui.printer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notathermal.app.data.prefs.AppSettings
import com.notathermal.app.data.prefs.SettingsRepository
import com.notathermal.app.print.BluetoothPrinterService
import com.notathermal.app.print.PrinterDevice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PrinterState(
    val devices: List<PrinterDevice> = emptyList(),
    val bluetoothEnabled: Boolean = false,
    val hasPermission: Boolean = false,
    val testing: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

class PrinterViewModel(
    private val printerService: BluetoothPrinterService,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings.Default)

    private val _state = MutableStateFlow(PrinterState())
    val state: StateFlow<PrinterState> = _state.asStateFlow()

    fun refresh() {
        _state.update {
            it.copy(
                bluetoothEnabled = printerService.isBluetoothEnabled(),
                hasPermission = printerService.hasConnectPermission(),
                devices = if (printerService.hasConnectPermission()) printerService.listPairedDevices() else emptyList(),
                error = null
            )
        }
    }

    fun selectDevice(device: PrinterDevice) {
        viewModelScope.launch {
            settingsRepository.setPrinter(device.address, device.name)
            _state.update { it.copy(message = "Printer ${device.name} dipilih") }
        }
    }

    fun clearDevice() {
        viewModelScope.launch {
            settingsRepository.setPrinter(null, null)
            _state.update { it.copy(message = "Printer dilepas") }
        }
    }

    fun testPrint() {
        val s = settings.value
        val address = s.printerAddress
        if (address.isNullOrBlank()) {
            _state.update { it.copy(error = "Pilih printer dulu") }
            return
        }
        if (!printerService.isBluetoothEnabled()) {
            _state.update { it.copy(error = "Bluetooth belum aktif") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(testing = true, message = null, error = null) }
            val result = printerService.testPrint(address, s.paperWidth)
            result.fold(
                onSuccess = { _state.update { it.copy(testing = false, message = "Test print terkirim") } },
                onFailure = { t -> _state.update { it.copy(testing = false, error = t.message ?: "Gagal test print") } }
            )
        }
    }

    fun clearMessages() {
        _state.update { it.copy(message = null, error = null) }
    }
}
