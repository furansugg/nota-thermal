package com.notathermal.app.ui.pln

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notathermal.app.data.db.PaymentMethod
import com.notathermal.app.data.prefs.AppSettings
import com.notathermal.app.data.prefs.SettingsRepository
import com.notathermal.app.data.repo.InvoiceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlnFormState(
    val customerName: String = "",
    val meterNo: String = "",
    val kwh: String = "",
    val tokenNumber: String = "",
    val nominal: String = "",
    val adminFee: String = "2500",
    val paymentMethod: String = PaymentMethod.TUNAI,
    val paymentReceived: String = "",
    val note: String = "",
    val saving: Boolean = false,
    val error: String? = null
) {
    val kwhValue: Double get() = kwh.replace(',', '.').toDoubleOrNull() ?: 0.0
    val nominalValue: Double get() = nominal.replace(',', '.').toDoubleOrNull() ?: 0.0
    val adminFeeValue: Double get() = adminFee.replace(',', '.').toDoubleOrNull() ?: 0.0
    val total: Double get() = (nominalValue + adminFeeValue).coerceAtLeast(0.0)
    val paymentReceivedValue: Double get() = paymentReceived.replace(',', '.').toDoubleOrNull() ?: 0.0
    val change: Double get() =
        if (paymentMethod == PaymentMethod.HUTANG) 0.0
        else (paymentReceivedValue - total).coerceAtLeast(0.0)

    val tokenDigits: String get() = tokenNumber.filter { it.isDigit() }

    val canSave: Boolean
        get() = !saving &&
            meterNo.isNotBlank() &&
            kwhValue > 0 &&
            tokenDigits.length >= 16 &&
            nominalValue > 0
}

class PlnTokenFormViewModel(
    private val invoiceRepository: InvoiceRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(PlnFormState())
    val state: StateFlow<PlnFormState> = _state.asStateFlow()

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings.Default)

    fun update(transform: (PlnFormState) -> PlnFormState) {
        _state.update(transform)
    }

    fun save(onSaved: (Long) -> Unit) {
        val current = _state.value
        if (!current.canSave) return
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                val id = invoiceRepository.createPlnTokenInvoice(
                    meterNo = current.meterNo.trim(),
                    customerName = current.customerName.trim(),
                    kwh = current.kwhValue,
                    tokenNumber = current.tokenDigits,
                    nominal = current.nominalValue,
                    adminFee = current.adminFeeValue,
                    paymentMethod = current.paymentMethod,
                    paymentReceived = current.paymentReceivedValue,
                    note = current.note.trim()
                )
                _state.update { it.copy(saving = false) }
                onSaved(id)
            } catch (t: Throwable) {
                _state.update {
                    it.copy(saving = false, error = t.message ?: "Gagal menyimpan struk")
                }
            }
        }
    }
}
