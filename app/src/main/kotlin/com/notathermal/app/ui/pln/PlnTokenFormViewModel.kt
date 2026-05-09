package com.notathermal.app.ui.pln

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notathermal.app.data.db.PaymentMethod
import com.notathermal.app.data.prefs.AppSettings
import com.notathermal.app.data.prefs.SettingsRepository
import com.notathermal.app.data.repo.InvoiceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Compose-snapshot-backed state holder. Each property is observable
 * independently, so editing a single field doesn't recompose the entire form.
 */
@Stable
class PlnFormStateHolder {
    var customerName by mutableStateOf("")
    var meterNo by mutableStateOf("")
    var kwh by mutableStateOf("")
    var tokenNumber by mutableStateOf("")
    var nominal by mutableStateOf("")
    var adminFee by mutableStateOf("2500")
    var paymentMethod by mutableStateOf(PaymentMethod.TUNAI)
    var paymentReceived by mutableStateOf("")
    var note by mutableStateOf("")
    var saving by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    val kwhValue: Double get() = kwh.replace(',', '.').toDoubleOrNull() ?: 0.0
    val nominalValue: Double get() = nominal.replace(',', '.').toDoubleOrNull() ?: 0.0
    val adminFeeValue: Double get() = adminFee.replace(',', '.').toDoubleOrNull() ?: 0.0
    val total: Double get() = (nominalValue + adminFeeValue).coerceAtLeast(0.0)
    val paymentReceivedValue: Double get() = paymentReceived.replace(',', '.').toDoubleOrNull() ?: 0.0
    val change: Double
        get() = if (paymentMethod == PaymentMethod.HUTANG) 0.0
        else (paymentReceivedValue - total).coerceAtLeast(0.0)

    val tokenDigits: String get() = tokenNumber.filter { it.isDigit() }

    private val canSaveState = derivedStateOf {
        !saving &&
            meterNo.isNotBlank() &&
            kwhValue > 0 &&
            tokenDigits.length >= 16 &&
            nominalValue > 0
    }
    val canSave: Boolean get() = canSaveState.value
}

class PlnTokenFormViewModel(
    private val invoiceRepository: InvoiceRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    val holder = PlnFormStateHolder()

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings.Default)

    fun save(onSaved: (Long) -> Unit) {
        if (!holder.canSave) return
        holder.saving = true
        holder.error = null
        viewModelScope.launch {
            try {
                val id = invoiceRepository.createPlnTokenInvoice(
                    meterNo = holder.meterNo.trim(),
                    customerName = holder.customerName.trim(),
                    kwh = holder.kwhValue,
                    tokenNumber = holder.tokenDigits,
                    nominal = holder.nominalValue,
                    adminFee = holder.adminFeeValue,
                    paymentMethod = holder.paymentMethod,
                    paymentReceived = holder.paymentReceivedValue,
                    note = holder.note.trim()
                )
                holder.saving = false
                onSaved(id)
            } catch (t: Throwable) {
                holder.saving = false
                holder.error = t.message ?: "Gagal menyimpan struk"
            }
        }
    }
}
