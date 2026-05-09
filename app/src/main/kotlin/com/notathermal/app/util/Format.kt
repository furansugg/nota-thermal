package com.notathermal.app.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Format {
    private val moneyFormat: DecimalFormat = DecimalFormat(
        "#,##0.##",
        DecimalFormatSymbols(Locale("id", "ID")).apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }
    )

    private val dateFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID"))
    private val dateOnlyFmt = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
    private val invoiceNoFmt = SimpleDateFormat("yyyyMMdd", Locale.US)

    fun number(value: Double): String = moneyFormat.format(value)

    fun datetime(epochMillis: Long): String = dateFmt.format(Date(epochMillis))

    fun dateOnly(epochMillis: Long): String = dateOnlyFmt.format(Date(epochMillis))

    fun invoiceNumber(epochMillis: Long, sequence: Int): String {
        val dayPart = invoiceNoFmt.format(Date(epochMillis))
        return "INV-$dayPart-${sequence.toString().padStart(4, '0')}"
    }
}
