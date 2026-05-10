package com.notathermal.app.ui.form

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notathermal.app.ai.AiInvoiceParser
import com.notathermal.app.ai.ParsedInvoice
import com.notathermal.app.data.db.InvoiceItemEntity
import com.notathermal.app.data.db.PaymentMethod
import com.notathermal.app.data.prefs.AppSettings
import com.notathermal.app.data.prefs.SettingsRepository
import com.notathermal.app.data.repo.InvoiceRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Per-item holder backed by Compose snapshot state. Each field is an independent
 * observable, so editing one item's field only recomposes composables that read
 * that exact field — siblings and the parent form don't pay the cost.
 */
@Stable
class ItemDraftHolder(val id: String = UUID.randomUUID().toString()) {
    var name by mutableStateOf("")
    var quantity by mutableStateOf("1")
    var price by mutableStateOf("")
    var discount by mutableStateOf("")

    val qtyValue: Double get() = quantity.replace(',', '.').toDoubleOrNull() ?: 0.0
    val priceValue: Double get() = price.replace(',', '.').toDoubleOrNull() ?: 0.0
    val discountValue: Double get() = discount.replace(',', '.').toDoubleOrNull() ?: 0.0
    val subtotal: Double get() = (qtyValue * priceValue - discountValue).coerceAtLeast(0.0)
}

/**
 * Top-level form state. Held in the ViewModel so it survives configuration
 * changes; exposed to Compose via snapshot state (mutable*Of).
 */
@Stable
class InvoiceFormStateHolder {
    val items: SnapshotStateList<ItemDraftHolder> =
        mutableStateListOf(ItemDraftHolder())

    var customerName by mutableStateOf("")
    var cashierName by mutableStateOf("")
    var note by mutableStateOf("")
    var discount by mutableStateOf("")
    var taxPercent by mutableStateOf("")
    var paymentMethod by mutableStateOf(PaymentMethod.TUNAI)
    var paymentReceived by mutableStateOf("")
    var saving by mutableStateOf(false)
    var error by mutableStateOf<String?>(null)

    private val subtotalState = derivedStateOf { items.sumOf { it.subtotal } }
    val subtotal: Double get() = subtotalState.value

    val discountValue: Double get() = discount.replace(',', '.').toDoubleOrNull() ?: 0.0
    val taxPercentValue: Double get() = taxPercent.replace(',', '.').toDoubleOrNull() ?: 0.0
    val taxBase: Double get() = (subtotal - discountValue).coerceAtLeast(0.0)
    val taxAmount: Double get() = taxBase * taxPercentValue / 100.0
    val total: Double get() = (taxBase + taxAmount).coerceAtLeast(0.0)
    val paymentReceivedValue: Double get() = paymentReceived.replace(',', '.').toDoubleOrNull() ?: 0.0
    val change: Double get() = (paymentReceivedValue - total).coerceAtLeast(0.0)

    private val canSaveState = derivedStateOf {
        !saving && items.any { it.name.isNotBlank() && it.qtyValue > 0 && it.priceValue > 0 }
    }
    val canSave: Boolean get() = canSaveState.value

    fun addItem() {
        items += ItemDraftHolder()
    }

    fun removeItem(id: String) {
        items.removeAll { it.id == id }
        if (items.isEmpty()) items += ItemDraftHolder()
    }
}

class InvoiceFormViewModel(
    private val invoiceRepository: InvoiceRepository,
    settingsRepository: SettingsRepository,
    private val aiInvoiceParser: AiInvoiceParser
) : ViewModel() {

    val holder = InvoiceFormStateHolder()

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings.Default)

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { s ->
                if (holder.taxPercent.isBlank() && s.taxPercentDefault > 0) {
                    holder.taxPercent = s.taxPercentDefault.toString()
                }
            }
        }
    }

    /**
     * Calls Gemini to parse [userText] and merges the result into the form
     * state. Existing items are replaced if the AI returns at least one item;
     * customer / cashier / tax are only filled when blank, so an AI run never
     * silently overwrites something the user already typed.
     */
    fun applyAi(
        apiKey: String,
        userText: String,
        onResult: (Result<ParsedInvoice>) -> Unit
    ) {
        viewModelScope.launch {
            val result = aiInvoiceParser.parse(apiKey, userText)
            result.onSuccess { parsed -> mergeFromAi(parsed) }
            onResult(result)
        }
    }

    private fun mergeFromAi(parsed: ParsedInvoice) {
        if (parsed.items.isNotEmpty()) {
            holder.items.clear()
            parsed.items.forEach { p ->
                holder.items += ItemDraftHolder().also {
                    it.name = p.name
                    it.quantity = formatNumber(p.quantity, integerIfWhole = true)
                    it.price = formatNumber(p.price, integerIfWhole = true)
                    it.discount = if (p.discount > 0) formatNumber(p.discount, integerIfWhole = true) else ""
                }
            }
            if (holder.items.isEmpty()) holder.items += ItemDraftHolder()
        }
        if (holder.customerName.isBlank() && !parsed.customerName.isNullOrBlank()) {
            holder.customerName = parsed.customerName
        }
        if (holder.cashierName.isBlank() && !parsed.cashierName.isNullOrBlank()) {
            holder.cashierName = parsed.cashierName
        }
        if (holder.taxPercent.isBlank() && parsed.taxPercent != null && parsed.taxPercent > 0) {
            holder.taxPercent = formatNumber(parsed.taxPercent, integerIfWhole = false)
        }
        if (holder.note.isBlank() && !parsed.notes.isNullOrBlank()) {
            holder.note = parsed.notes
        }
    }

    private fun formatNumber(value: Double, integerIfWhole: Boolean): String =
        if (integerIfWhole && value % 1.0 == 0.0) value.toLong().toString()
        else value.toString().trimEnd('0').trimEnd('.')

    fun save(onSaved: (Long) -> Unit) {
        if (!holder.canSave) return
        holder.saving = true
        holder.error = null
        viewModelScope.launch {
            try {
                val items = holder.items
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
                    customerName = holder.customerName,
                    cashierName = holder.cashierName,
                    note = holder.note,
                    discount = holder.discountValue,
                    taxPercent = holder.taxPercentValue,
                    paymentMethod = holder.paymentMethod,
                    paymentReceived = holder.paymentReceivedValue
                )
                holder.saving = false
                onSaved(id)
            } catch (t: Throwable) {
                holder.saving = false
                holder.error = t.message ?: "Gagal menyimpan invoice"
            }
        }
    }
}
