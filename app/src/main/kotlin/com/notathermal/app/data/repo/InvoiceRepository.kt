package com.notathermal.app.data.repo

import com.notathermal.app.data.db.InvoiceDao
import com.notathermal.app.data.db.InvoiceEntity
import com.notathermal.app.data.db.InvoiceItemEntity
import com.notathermal.app.data.db.InvoiceType
import com.notathermal.app.data.db.InvoiceWithItems
import com.notathermal.app.data.db.PaymentMethod
import com.notathermal.app.util.Format
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class InvoiceRepository(private val dao: InvoiceDao) {

    fun observeAll(): Flow<List<InvoiceEntity>> = dao.observeAll()

    fun observeInvoice(id: Long): Flow<InvoiceWithItems?> = dao.observeWithItems(id)

    suspend fun getInvoice(id: Long): InvoiceWithItems? = dao.getWithItems(id)

    suspend fun deleteInvoice(id: Long) = dao.deleteById(id)

    suspend fun markAsPaid(id: Long, paymentMethod: String, paymentReceived: Double) {
        val current = dao.getWithItems(id) ?: return
        val total = current.invoice.total
        val received = paymentReceived.coerceAtLeast(total)
        val change = (received - total).coerceAtLeast(0.0)
        dao.updatePayment(id, paymentMethod, received, change)
    }

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
        val isHutang = paymentMethod == PaymentMethod.HUTANG
        val received = if (isHutang) 0.0 else paymentReceived
        val change = if (isHutang) 0.0 else (received - total).coerceAtLeast(0.0)
        val code = nextCode(now)
        val invoice = InvoiceEntity(
            code = code,
            invoiceType = InvoiceType.REGULAR,
            customerName = customerName?.takeIf { it.isNotBlank() },
            cashierName = cashierName?.takeIf { it.isNotBlank() },
            note = note?.takeIf { it.isNotBlank() },
            subtotal = subtotal,
            discount = discount,
            taxPercent = taxPercent,
            taxAmount = taxAmount,
            total = total,
            paymentMethod = paymentMethod,
            paymentReceived = received,
            change = change,
            createdAt = now
        )
        return dao.insertInvoiceWithItems(invoice, items)
    }

    suspend fun createPlnTokenInvoice(
        meterNo: String,
        customerName: String?,
        kwh: Double,
        tokenNumber: String,
        nominal: Double,
        adminFee: Double,
        paymentMethod: String,
        paymentReceived: Double,
        note: String?,
        now: Long = System.currentTimeMillis()
    ): Long {
        val total = (nominal + adminFee).coerceAtLeast(0.0)
        val isHutang = paymentMethod == PaymentMethod.HUTANG
        val received = if (isHutang) 0.0 else paymentReceived
        val change = if (isHutang) 0.0 else (received - total).coerceAtLeast(0.0)
        val code = nextCode(now)
        val invoice = InvoiceEntity(
            code = code,
            invoiceType = InvoiceType.PLN_TOKEN,
            customerName = customerName?.takeIf { it.isNotBlank() },
            cashierName = null,
            note = note?.takeIf { it.isNotBlank() },
            subtotal = nominal,
            discount = 0.0,
            taxPercent = 0.0,
            taxAmount = adminFee,
            total = total,
            paymentMethod = paymentMethod,
            paymentReceived = received,
            change = change,
            meterNo = meterNo,
            kwh = kwh,
            tokenNumber = tokenNumber,
            createdAt = now
        )
        val item = InvoiceItemEntity(
            invoiceId = 0,
            name = "Token Listrik PLN",
            quantity = 1.0,
            price = nominal,
            discount = 0.0,
            subtotal = nominal
        )
        return dao.insertInvoiceWithItems(invoice, listOf(item))
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
