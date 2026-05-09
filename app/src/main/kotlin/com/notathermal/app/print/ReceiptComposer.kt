package com.notathermal.app.print

import com.notathermal.app.data.db.InvoiceEntity
import com.notathermal.app.data.db.InvoiceItemEntity
import com.notathermal.app.data.db.InvoiceType
import com.notathermal.app.data.db.PaymentMethod
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
    fun composePlain(input: ReceiptInput, settings: AppSettings): String =
        if (input.invoice.invoiceType == InvoiceType.PLN_TOKEN) {
            composePlainPln(input, settings)
        } else {
            composePlainRegular(input, settings)
        }

    /** Format compatible with the DantSu ESC/POS library (uses [L][C][R] tags). */
    fun composeEscPos(input: ReceiptInput, settings: AppSettings): String =
        if (input.invoice.invoiceType == InvoiceType.PLN_TOKEN) {
            composeEscPosPln(input, settings)
        } else {
            composeEscPosRegular(input, settings)
        }

    // ---------- Regular invoice ----------

    private fun composePlainRegular(input: ReceiptInput, settings: AppSettings): String {
        val w = width(settings)
        val sb = StringBuilder()
        val invoice = input.invoice

        appendStoreHeaderPlain(sb, settings, w)
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
        appendPaymentLinesPlain(sb, invoice, w)
        sb.appendLine("=".repeat(w))
        if (!invoice.note.isNullOrBlank()) {
            sb.appendLine("Catatan: ${invoice.note}")
            sb.appendLine("-".repeat(w))
        }
        appendFooterPlain(sb, settings, w)
        return sb.toString()
    }

    private fun composeEscPosRegular(input: ReceiptInput, settings: AppSettings): String {
        val align = alignTag(settings.titleAlignment)
        val w = width(settings)
        val invoice = input.invoice
        val sb = StringBuilder()

        appendStoreHeaderEscPos(sb, settings, align)
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
        appendPaymentLinesEscPos(sb, invoice)
        sb.append("[L]").append("=".repeat(w)).append('\n')
        if (!invoice.note.isNullOrBlank()) {
            sb.append("[L]Catatan: ").append(escape(invoice.note)).append('\n')
            sb.append("[L]").append("-".repeat(w)).append('\n')
        }
        appendFooterEscPos(sb, settings, align)
        return sb.toString()
    }

    // ---------- PLN token ----------

    private fun composePlainPln(input: ReceiptInput, settings: AppSettings): String {
        val w = width(settings)
        val invoice = input.invoice
        val sb = StringBuilder()

        appendStoreHeaderPlain(sb, settings, w)
        sb.appendLine("=".repeat(w))
        sb.appendLine(alignText("STRUK TOKEN LISTRIK PLN", w, TextAlign.CENTER))
        sb.appendLine("=".repeat(w))
        sb.appendLine("No   : ${invoice.code}")
        sb.appendLine("Tgl  : ${Format.datetime(invoice.createdAt)}")
        if (settings.showCustomer && !invoice.customerName.isNullOrBlank()) {
            sb.appendLine("Plg  : ${invoice.customerName}")
        }
        sb.appendLine("Meter: ${invoice.meterNo.orEmpty()}")
        invoice.kwh?.let { sb.appendLine("kWh  : ${formatQty(it)} kWh") }
        sb.appendLine("-".repeat(w))
        sb.appendLine(alignText("NOMOR TOKEN / STROOM", w, TextAlign.CENTER))
        formatTokenForDisplay(invoice.tokenNumber.orEmpty()).forEach { line ->
            sb.appendLine(alignText(line, w, TextAlign.CENTER))
        }
        sb.appendLine("-".repeat(w))
        sb.appendLine(twoColumn("Nominal", Format.number(invoice.subtotal), w))
        if (invoice.taxAmount > 0) {
            sb.appendLine(twoColumn("Admin", Format.number(invoice.taxAmount), w))
        }
        sb.appendLine("=".repeat(w))
        sb.appendLine(twoColumn("TOTAL", "${settings.currencySymbol} ${Format.number(invoice.total)}", w))
        appendPaymentLinesPlain(sb, invoice, w)
        sb.appendLine("=".repeat(w))
        if (!invoice.note.isNullOrBlank()) {
            sb.appendLine("Catatan: ${invoice.note}")
            sb.appendLine("-".repeat(w))
        }
        appendFooterPlain(sb, settings, w)
        return sb.toString()
    }

    private fun composeEscPosPln(input: ReceiptInput, settings: AppSettings): String {
        val align = alignTag(settings.titleAlignment)
        val w = width(settings)
        val invoice = input.invoice
        val sb = StringBuilder()

        appendStoreHeaderEscPos(sb, settings, align)
        sb.append("[L]").append("=".repeat(w)).append('\n')
        sb.append("[C]<b>STRUK TOKEN LISTRIK PLN</b>\n")
        sb.append("[L]").append("=".repeat(w)).append('\n')
        sb.append("[L]No   : ").append(escape(invoice.code)).append('\n')
        sb.append("[L]Tgl  : ").append(Format.datetime(invoice.createdAt)).append('\n')
        if (settings.showCustomer && !invoice.customerName.isNullOrBlank()) {
            sb.append("[L]Plg  : ").append(escape(invoice.customerName)).append('\n')
        }
        sb.append("[L]Meter: ").append(escape(invoice.meterNo.orEmpty())).append('\n')
        invoice.kwh?.let {
            sb.append("[L]kWh  : ").append(formatQty(it)).append(" kWh\n")
        }
        sb.append("[L]").append("-".repeat(w)).append('\n')
        sb.append("[C]NOMOR TOKEN / STROOM\n")
        formatTokenForDisplay(invoice.tokenNumber.orEmpty()).forEach { line ->
            // Big bold token, centered. DantSu supports <font size='big'> for double width+height.
            sb.append("[C]<font size='big'><b>").append(escape(line)).append("</b></font>\n")
        }
        sb.append("[L]").append("-".repeat(w)).append('\n')
        sb.append("[L]Nominal[R]").append(Format.number(invoice.subtotal)).append('\n')
        if (invoice.taxAmount > 0) {
            sb.append("[L]Admin[R]").append(Format.number(invoice.taxAmount)).append('\n')
        }
        sb.append("[L]").append("=".repeat(w)).append('\n')
        sb.append("[L]<b>TOTAL</b>[R]<b>")
            .append(escape(settings.currencySymbol)).append(' ')
            .append(Format.number(invoice.total)).append("</b>\n")
        appendPaymentLinesEscPos(sb, invoice)
        sb.append("[L]").append("=".repeat(w)).append('\n')
        if (!invoice.note.isNullOrBlank()) {
            sb.append("[L]Catatan: ").append(escape(invoice.note)).append('\n')
            sb.append("[L]").append("-".repeat(w)).append('\n')
        }
        appendFooterEscPos(sb, settings, align)
        return sb.toString()
    }

    // ---------- shared helpers ----------

    private fun appendStoreHeaderPlain(sb: StringBuilder, settings: AppSettings, w: Int) {
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
    }

    private fun appendStoreHeaderEscPos(sb: StringBuilder, settings: AppSettings, align: String) {
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
    }

    private fun appendPaymentLinesPlain(sb: StringBuilder, invoice: InvoiceEntity, w: Int) {
        if (invoice.paymentMethod == PaymentMethod.HUTANG) {
            sb.appendLine(alignText("** BELUM LUNAS / HUTANG **", w, TextAlign.CENTER))
        } else if (invoice.paymentReceived > 0) {
            sb.appendLine(twoColumn("Bayar (${invoice.paymentMethod})", Format.number(invoice.paymentReceived), w))
            if (invoice.change > 0) {
                sb.appendLine(twoColumn("Kembali", Format.number(invoice.change), w))
            }
        } else {
            sb.appendLine(twoColumn("Pembayaran", invoice.paymentMethod, w))
        }
    }

    private fun appendPaymentLinesEscPos(sb: StringBuilder, invoice: InvoiceEntity) {
        if (invoice.paymentMethod == PaymentMethod.HUTANG) {
            sb.append("[C]<b>** BELUM LUNAS / HUTANG **</b>\n")
        } else if (invoice.paymentReceived > 0) {
            sb.append("[L]Bayar (").append(escape(invoice.paymentMethod)).append(")[R]")
                .append(Format.number(invoice.paymentReceived)).append('\n')
            if (invoice.change > 0) {
                sb.append("[L]Kembali[R]").append(Format.number(invoice.change)).append('\n')
            }
        } else {
            sb.append("[L]Pembayaran[R]").append(escape(invoice.paymentMethod)).append('\n')
        }
    }

    private fun appendFooterPlain(sb: StringBuilder, settings: AppSettings, w: Int) {
        if (settings.footerText.isNotBlank()) {
            settings.footerText.split('\n').forEach {
                sb.appendLine(alignText(it.trim(), w, settings.titleAlignment))
            }
        }
    }

    private fun appendFooterEscPos(sb: StringBuilder, settings: AppSettings, align: String) {
        if (settings.footerText.isNotBlank()) {
            settings.footerText.split('\n').forEach {
                sb.append("[$align]").append(escape(it.trim())).append('\n')
            }
        }
    }

    private fun alignTag(align: TextAlign): String = when (align) {
        TextAlign.LEFT -> "L"
        TextAlign.CENTER -> "C"
        TextAlign.RIGHT -> "R"
    }

    private fun escape(text: String): String =
        text.replace("[", "(").replace("]", ")")

    private fun formatQty(qty: Double): String {
        return if (qty == qty.toLong().toDouble()) qty.toLong().toString()
        else Format.number(qty)
    }

    /**
     * Formats a token number into chunks for display. Splits into 4-digit groups
     * and breaks into multiple lines so it fits in the printer's "big text" mode.
     */
    private fun formatTokenForDisplay(token: String): List<String> {
        val digits = token.filter { !it.isWhitespace() && it != '-' }
        if (digits.isEmpty()) return listOf("(token kosong)")
        // Split into groups of 4
        val groups = digits.chunked(4)
        // Keep at most 3 groups per line to fit in big-mode width (~16 chars on 58mm)
        return groups.chunked(3).map { it.joinToString(" ") }
    }

    private fun twoColumn(left: String, right: String, width: Int): String {
        if (left.length + right.length + 1 > width) {
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
