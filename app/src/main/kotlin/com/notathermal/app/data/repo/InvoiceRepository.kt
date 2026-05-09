package com.notathermal.app.data.repo

import com.notathermal.app.data.db.InvoiceDao
import com.notathermal.app.data.db.InvoiceEntity
import com.notathermal.app.data.db.InvoiceItemEntity
import com.notathermal.app.data.db.InvoiceWithItems
import com.notathermal.app.util.Format
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class InvoiceRepository(private val dao: InvoiceDao) {

    fun observeAll(): Flow<List<InvoiceEntity>> = dao.observeAll()

    fun observeInvoice(id: Long): Flow<InvoiceWithItems?> = dao.observeWithItems(id)

    suspend fun getInvoice(id: Long): InvoiceWithItems? = dao.getWithItems(id)

    suspend fun deleteInvoice(id: Long) = dao.deleteById(id)

    suspend fun createInvoice(
        items: List<InvoiceItemEntity>,
        customerName: String?,
        cashierName: String?,
        note: String?,
        discount: Double,
        taxPercent: Double,
        paymentMethod: String,
        paymentReceived: Double,
        now: Long = System.currentTimeMillis()
    ): Long {
        val subtotal = items.sumOf { it.subtotal }
        val taxBase = (subtotal - discount).coerceAtLeast(0.0)
        val taxAmount = taxBase * taxPercent / 100.0
        val total = (taxBase + taxAmount).coerceAtLeast(0.0)
        val change = (paymentReceived - total).coerceAtLeast(0.0)
        val code = nextCode(now)
        val invoice = InvoiceEntity(
            code = code,
            customerName = customerName?.takeIf { it.isNotBlank() },
            cashierName = cashierName?.takeIf { it.isNotBlank() },
            note = note?.takeIf { it.isNotBlank() },
            subtotal = subtotal,
            discount = discount,
            taxPercent = taxPercent,
            taxAmount = taxAmount,
            total = total,
            paymentMethod = paymentMethod,
            paymentReceived = paymentReceived,
            change = change,
            createdAt = now
        )
        return dao.insertInvoiceWithItems(invoice, items)
    }

    private suspend fun nextCode(now: Long): String {
        val cal = Calendar.getInstance().apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val end = cal.timeInMillis
        val seq = dao.countBetween(start, end) + 1
        return Format.invoiceNumber(now, seq)
    }
}
