package com.notathermal.app.ui.form

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notathermal.app.data.db.InvoiceItemEntity
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

data class ItemDraft(
    val id: String,
    val name: String = "",
    val quantity: String = "1",
    val price: String = "",
    val discount: String = ""
) {
    val qtyValue: Double get() = quantity.replace(',', '.').toDoubleOrNull() ?: 0.0
    val priceValue: Double get() = price.replace(',', '.').toDoubleOrNull() ?: 0.0
    val discountValue: Double get() = discount.replace(',', '.').toDoubleOrNull() ?: 0.0
    val subtotal: Double get() = (qtyValue * priceValue - discountValue).coerceAtLeast(0.0)
}

data class FormState(
    val items: List<ItemDraft> = listOf(ItemDraft(id = newId())),
    val customerName: String = "",
    val cashierName: String = "",
    val note: String = "",
    val discount: String = "",
    val taxPercent: String = "",
    val paymentMethod: String = PaymentMethod.TUNAI,
    val paymentReceived: String = "",
    val saving: Boolean = false,
    val error: String? = null
) {
    val subtotal: Double get() = items.sumOf { it.subtotal }
    val discountValue: Double get() = discount.replace(',', '.').toDoubleOrNull() ?: 0.0
    val taxPercentValue: Double get() = taxPercent.replace(',', '.').toDoubleOrNull() ?: 0.0
    val taxBase: Double get() = (subtotal - discountValue).coerceAtLeast(0.0)
    val taxAmount: Double get() = taxBase * taxPercentValue / 100.0
    val total: Double get() = (taxBase + taxAmount).coerceAtLeast(0.0)
    val paymentReceivedValue: Double get() = paymentReceived.replace(',', '.').toDoubleOrNull() ?: 0.0
    val change: Double get() = (paymentReceivedValue - total).coerceAtLeast(0.0)

    val canSave: Boolean
        get() = !saving && items.any { it.name.isNotBlank() && it.qtyValue > 0 && it.priceValue > 0 }
}

private fun newId(): String = java.util.UUID.randomUUID().toString()

class InvoiceFormViewModel(
    private val invoiceRepository: InvoiceRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(FormState())
    val state: StateFlow<FormState> = _state.asStateFlow()

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings.Default)

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { s ->
                if (_state.value.taxPercent.isBlank() && s.taxPercentDefault > 0) {
                    _state.update { it.copy(taxPercent = s.taxPercentDefault.toString()) }
                }
            }
        }
    }

    fun updateItem(id: String, transform: (ItemDraft) -> ItemDraft) {
        _state.update { st ->
            st.copy(items = st.items.map { if (it.id == id) transform(it) else it })
        }
    }

    fun addItem() {
        _state.update { st -> st.copy(items = st.items + ItemDraft(id = newId())) }
    }

    fun removeItem(id: String) {
        _state.update { st ->
            val next = st.items.filterNot { it.id == id }
            st.copy(items = next.ifEmpty { listOf(ItemDraft(id = newId())) })
        }
    }

    fun updateField(transform: (FormState) -> FormState) {
        _state.update(transform)
    }

    fun save(onSaved: (Long) -> Unit) {
        val current = _state.value
        if (!current.canSave) return
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            try {
                val items = current.items
                    .filter { it.name.isNotBlank() && it.qtyValue > 0 && it.priceValue > 0 }
                    .map {
                        InvoiceItemEntity(
                            invoiceId = 0,
                            name = it.name.trim(),
                            quantity = it.qtyValue,
                            price = it.priceValue,
                            discount = it.discountValue,
                            subtotal = it.subtotal
                        )
                    }
                val id = invoiceRepository.createInvoice(
                    items = items,
                    customerName = current.customerName,
                    cashierName = current.cashierName,
                    note = current.note,
                    discount = current.discountValue,
                    taxPercent = current.taxPercentValue,
                    paymentMethod = current.paymentMethod,
                    paymentReceived = current.paymentReceivedValue
                )
                _state.update { it.copy(saving = false) }
                onSaved(id)
            } catch (t: Throwable) {
                _state.update { it.copy(saving = false, error = t.message ?: "Gagal menyimpan invoice") }
            }
        }
    }
}
