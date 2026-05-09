package com.notathermal.app.print

import com.notathermal.app.data.db.InvoiceEntity
import com.notathermal.app.data.db.InvoiceItemEntity
import com.notathermal.app.data.prefs.AppSettings
import com.notathermal.app.domain.TextAlign
import com.notathermal.app.util.Format

data class ReceiptInput(
    val invoice: InvoiceEntity,
    val items: List<InvoiceItemEntity>
)

class ReceiptComposer {

    private fun width(settings: AppSettings): Int = settings.paperWidth.charsNormal

    /** Plain text preview (used in the in-app preview pane). */
    fun composePlain(input: ReceiptInput, settings: AppSettings): String {
        val w = width(settings)
        val sb = StringBuilder()
        val invoice = input.invoice

        if (settings.storeName.isNotBlank()) sb.appendLine(alignText(settings.storeName, w, settings.titleAlignment))
        if (settings.storeAddress.isNotBlank()) {
            settings.storeAddress.split('\n').forEach {
                sb.appendLine(alignText(it.trim(), w, settings.titleAlignment))
            }
        }
        if (settings.storePhone.isNotBlank()) {
            sb.appendLine(alignText("Telp: ${settings.storePhone}", w, settings.titleAlignment))
        }
        if (settings.headerText.isNotBlank()) {
            settings.headerText.split('\n').forEach {
                sb.appendLine(alignText(it.trim(), w, settings.titleAlignment))
            }
        }
        sb.appendLine("=".repeat(w))
        sb.appendLine("No  : ${invoice.code}")
        sb.appendLine("Tgl : ${Format.datetime(invoice.createdAt)}")
        if (settings.showCashier && !invoice.cashierName.isNullOrBlank()) {
            sb.appendLine("Kasir: ${invoice.cashierName}")
        }
        if (settings.showCustomer && !invoice.customerName.isNullOrBlank()) {
            sb.appendLine("Plg  : ${invoice.customerName}")
        }
        sb.appendLine("-".repeat(w))
        input.items.forEach { item ->
            sb.appendLine(item.name)
            val qtyStr = formatQty(item.quantity)
            val qtyPrice = "  $qtyStr x ${Format.number(item.price)}"
            sb.appendLine(twoColumn(qtyPrice, Format.number(item.subtotal), w))
            if (item.discount > 0) {
                sb.appendLine(twoColumn("  Diskon", "-${Format.number(item.discount)}", w))
            }
        }
        sb.appendLine("-".repeat(w))
        sb.appendLine(twoColumn("Subtotal", Format.number(invoice.subtotal), w))
        if (invoice.discount > 0) {
            sb.appendLine(twoColumn("Diskon", "-${Format.number(invoice.discount)}", w))
        }
        if (invoice.taxPercent > 0) {
            sb.appendLine(
                twoColumn(
                    "Pajak ${Format.number(invoice.taxPercent)}%",
                    Format.number(invoice.taxAmount),
                    w
                )
            )
        }
        sb.appendLine("=".repeat(w))
        sb.appendLine(twoColumn("TOTAL", "${settings.currencySymbol} ${Format.number(invoice.total)}", w))
        if (invoice.paymentReceived > 0) {
            sb.appendLine(twoColumn("Bayar (${invoice.paymentMethod})", Format.number(invoice.paymentReceived), w))
            if (invoice.change > 0) {
                sb.appendLine(twoColumn("Kembali", Format.number(invoice.change), w))
            }
        }
        sb.appendLine("=".repeat(w))
        if (!invoice.note.isNullOrBlank()) {
            sb.appendLine("Catatan: ${invoice.note}")
            sb.appendLine("-".repeat(w))
        }
        if (settings.footerText.isNotBlank()) {
            settings.footerText.split('\n').forEach {
                sb.appendLine(alignText(it.trim(), w, settings.titleAlignment))
            }
        }
        return sb.toString()
    }

    /** Format compatible with the DantSu ESC/POS library (uses [L][C][R] tags). */
    fun composeEscPos(input: ReceiptInput, settings: AppSettings): String {
        val align = when (settings.titleAlignment) {
            TextAlign.LEFT -> "L"
            TextAlign.CENTER -> "C"
            TextAlign.RIGHT -> "R"
        }
        val w = width(settings)
        val invoice = input.invoice
        val sb = StringBuilder()

        if (settings.storeName.isNotBlank()) {
            sb.append("[$align]<b>").append(escape(settings.storeName)).append("</b>\n")
        }
        if (settings.storeAddress.isNotBlank()) {
            settings.storeAddress.split('\n').forEach {
                sb.append("[$align]").append(escape(it.trim())).append('\n')
            }
        }
        if (settings.storePhone.isNotBlank()) {
            sb.append("[$align]Telp: ").append(escape(settings.storePhone)).append('\n')
        }
        if (settings.headerText.isNotBlank()) {
            settings.headerText.split('\n').forEach {
                sb.append("[$align]").append(escape(it.trim())).append('\n')
            }
        }
        sb.append("[L]").append("=".repeat(w)).append('\n')
        sb.append("[L]No  : ").append(escape(invoice.code)).append('\n')
        sb.append("[L]Tgl : ").append(Format.datetime(invoice.createdAt)).append('\n')
        if (settings.showCashier && !invoice.cashierName.isNullOrBlank()) {
            sb.append("[L]Kasir: ").append(escape(invoice.cashierName)).append('\n')
        }
        if (settings.showCustomer && !invoice.customerName.isNullOrBlank()) {
            sb.append("[L]Plg  : ").append(escape(invoice.customerName)).append('\n')
        }
        sb.append("[L]").append("-".repeat(w)).append('\n')

        input.items.forEach { item ->
            sb.append("[L]").append(escape(item.name)).append('\n')
            val qtyStr = formatQty(item.quantity)
            sb.append("[L]  ").append(qtyStr).append(" x ").append(Format.number(item.price))
                .append("[R]").append(Format.number(item.subtotal)).append('\n')
            if (item.discount > 0) {
                sb.append("[L]  Diskon[R]-").append(Format.number(item.discount)).append('\n')
            }
        }
        sb.append("[L]").append("-".repeat(w)).append('\n')
        sb.append("[L]Subtotal[R]").append(Format.number(invoice.subtotal)).append('\n')
        if (invoice.discount > 0) {
            sb.append("[L]Diskon[R]-").append(Format.number(invoice.discount)).append('\n')
        }
        if (invoice.taxPercent > 0) {
            sb.append("[L]Pajak ").append(Format.number(invoice.taxPercent)).append("%[R]")
                .append(Format.number(invoice.taxAmount)).append('\n')
        }
        sb.append("[L]").append("=".repeat(w)).append('\n')
        sb.append("[L]<b>TOTAL</b>[R]<b>").append(escape(settings.currencySymbol)).append(' ')
            .append(Format.number(invoice.total)).append("</b>\n")
        if (invoice.paymentReceived > 0) {
            sb.append("[L]Bayar (").append(escape(invoice.paymentMethod)).append(")[R]")
                .append(Format.number(invoice.paymentReceived)).append('\n')
            if (invoice.change > 0) {
                sb.append("[L]Kembali[R]").append(Format.number(invoice.change)).append('\n')
            }
        }
        sb.append("[L]").append("=".repeat(w)).append('\n')
        if (!invoice.note.isNullOrBlank()) {
            sb.append("[L]Catatan: ").append(escape(invoice.note)).append('\n')
            sb.append("[L]").append("-".repeat(w)).append('\n')
        }
        if (settings.footerText.isNotBlank()) {
            settings.footerText.split('\n').forEach {
                sb.append("[$align]").append(escape(it.trim())).append('\n')
            }
        }
        return sb.toString()
    }

    private fun escape(text: String): String =
        text.replace("[", "(").replace("]", ")")

    private fun formatQty(qty: Double): String {
        return if (qty == qty.toLong().toDouble()) qty.toLong().toString()
        else Format.number(qty)
    }

    private fun twoColumn(left: String, right: String, width: Int): String {
        if (left.length + right.length + 1 > width) {
            // Wrap left to fit; put right on its own right-aligned line.
            val leftWrapped = if (left.length >= width) left.substring(0, width) else left
            val rightLine = right.padStart(width)
            return "$leftWrapped\n$rightLine"
        }
        val space = width - left.length - right.length
        return left + " ".repeat(space) + right
    }

    private fun alignText(text: String, width: Int, align: TextAlign): String {
        val truncated = if (text.length > width) text.substring(0, width) else text
        val pad = (width - truncated.length).coerceAtLeast(0)
        return when (align) {
            TextAlign.LEFT -> truncated + " ".repeat(pad)
            TextAlign.RIGHT -> " ".repeat(pad) + truncated
            TextAlign.CENTER -> {
                val leftPad = pad / 2
                val rightPad = pad - leftPad
                " ".repeat(leftPad) + truncated + " ".repeat(rightPad)
            }
        }
    }
}
