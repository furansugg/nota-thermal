package com.notathermal.app.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notathermal.app.data.db.InvoiceWithItems
import com.notathermal.app.data.prefs.AppSettings
import com.notathermal.app.data.prefs.SettingsRepository
import com.notathermal.app.data.repo.InvoiceRepository
import com.notathermal.app.print.BluetoothPrinterService
import com.notathermal.app.print.ReceiptComposer
import com.notathermal.app.print.ReceiptInput
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DetailState(
    val printing: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null
)

class InvoiceDetailViewModel(
    private val invoiceId: Long,
    private val invoiceRepository: InvoiceRepository,
    settingsRepository: SettingsRepository,
    private val printerService: BluetoothPrinterService,
    private val composer: ReceiptComposer
) : ViewModel() {

    val invoice: StateFlow<InvoiceWithItems?> = invoiceRepository.observeInvoice(invoiceId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings.Default)

    private val _state = MutableStateFlow(DetailState())
    val state: StateFlow<DetailState> = _state.asStateFlow()

    fun previewText(): String {
        val current = invoice.value ?: return ""
        return composer.composePlain(
            ReceiptInput(current.invoice, current.items),
            settings.value
        )
    }

    fun print() {
        val current = invoice.value ?: return
        val s = settings.value
        val address = s.printerAddress
        if (address.isNullOrBlank()) {
            _state.update { it.copy(errorMessage = "Belum ada printer terpilih. Buka menu Printer.") }
            return
        }
        if (!printerService.isBluetoothEnabled()) {
            _state.update { it.copy(errorMessage = "Bluetooth belum aktif.") }
            return
        }
        if (!printerService.hasConnectPermission()) {
            _state.update { it.copy(errorMessage = "Izin Bluetooth belum diberikan. Buka menu Printer.") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(printing = true, statusMessage = null, errorMessage = null) }
            val text = composer.composeEscPos(
                ReceiptInput(current.invoice, current.items),
                s
            )
            val result = printerService.print(
                address = address,
                formatted = text,
                paperWidth = s.paperWidth,
                copies = s.copies,
                cutPaper = s.cutPaper
            )
            result.fold(
                onSuccess = {
                    _state.update {
                        it.copy(
                            printing = false,
                            statusMessage = "Berhasil mencetak"
                        )
                    }
                },
                onFailure = { t ->
                    _state.update {
                        it.copy(
                            printing = false,
                            errorMessage = t.message ?: "Gagal mencetak"
                        )
                    }
                }
            )
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            invoiceRepository.deleteInvoice(invoiceId)
            onDeleted()
        }
    }

    fun markPaid(paymentMethod: String, paymentReceived: Double) {
        viewModelScope.launch {
            try {
                invoiceRepository.markAsPaid(invoiceId, paymentMethod, paymentReceived)
                _state.update { it.copy(statusMessage = "Invoice ditandai LUNAS") }
            } catch (t: Throwable) {
                _state.update { it.copy(errorMessage = t.message ?: "Gagal memperbarui status") }
            }
        }
    }

    fun clearMessages() {
        _state.update { it.copy(statusMessage = null, errorMessage = null) }
    }
}
