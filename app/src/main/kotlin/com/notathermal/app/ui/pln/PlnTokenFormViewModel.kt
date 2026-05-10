package com.notathermal.app.ui.pln

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notathermal.app.data.db.PaymentMethod
import com.notathermal.app.data.db.PlnCustomerEntity
import com.notathermal.app.data.db.PlnProductEntity
import com.notathermal.app.data.prefs.AppSettings
import com.notathermal.app.data.prefs.SettingsRepository
import com.notathermal.app.data.repo.InvoiceRepository
import com.notathermal.app.data.repo.PlnCustomerRepository
import com.notathermal.app.data.repo.PlnProductRepository
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
    var productName by mutableStateOf("Token Listrik PLN")
    var customerName by mutableStateOf("")
    var meterNo by mutableStateOf("")
    var kwh by mutableStateOf("")
    var tokenNumber by mutableStateOf("")
    var nominal by mutableStateOf("")
    var adminFee by mutableStateOf("0")
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

    fun applyCustomer(customer: PlnCustomerEntity) {
        customerName = customer.customerName
        meterNo = customer.meterNo
    }

    fun applyProduct(product: PlnProductEntity) {
        productName = product.name
        nominal = if (product.nominal % 1.0 == 0.0) product.nominal.toLong().toString()
        else product.nominal.toString()
    }
}

class PlnTokenFormViewModel(
    private val invoiceRepository: InvoiceRepository,
    settingsRepository: SettingsRepository,
    private val plnCustomerRepository: PlnCustomerRepository,
    plnProductRepository: PlnProductRepository
) : ViewModel() {

    val holder = PlnFormStateHolder()

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings.Default)

    val savedCustomers: StateFlow<List<PlnCustomerEntity>> = plnCustomerRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val products: StateFlow<List<PlnProductEntity>> = plnProductRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun save(onSaved: (Long) -> Unit) {
        if (!holder.canSave) return
        holder.saving = true
        holder.error = null
        viewModelScope.launch {
            try {
                val cleanedMeter = holder.meterNo.trim()
                val cleanedCustomer = holder.customerName.trim()
                val id = invoiceRepository.createPlnTokenInvoice(
                    meterNo = cleanedMeter,
                    customerName = cleanedCustomer,
                    productName = holder.productName.trim(),
                    kwh = holder.kwhValue,
                    tokenNumber = holder.tokenDigits,
                    nominal = holder.nominalValue,
                    adminFee = holder.adminFeeValue,
                    paymentMethod = holder.paymentMethod,
                    paymentReceived = holder.paymentReceivedValue,
                    note = holder.note.trim()
                )
                // Remember customer for autofill on future PLN invoices. Skipped
                // silently if either field is blank (rememberCustomer enforces
                // that), so this never persists junk rows.
                plnCustomerRepository.rememberCustomer(cleanedMeter, cleanedCustomer)
                holder.saving = false
                onSaved(id)
            } catch (t: Throwable) {
                holder.saving = false
                holder.error = t.message ?: "Gagal menyimpan struk"
            }
        }
    }
}
