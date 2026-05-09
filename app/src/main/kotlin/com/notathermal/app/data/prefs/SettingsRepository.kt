package com.notathermal.app.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.notathermal.app.domain.PaperWidth
import com.notathermal.app.domain.TextAlign
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "nota_settings")

data class AppSettings(
    val storeName: String,
    val storeAddress: String,
    val storePhone: String,
    val headerText: String,
    val footerText: String,
    val paperWidth: PaperWidth,
    val titleAlignment: TextAlign,
    val currencySymbol: String,
    val showCashier: Boolean,
    val showCustomer: Boolean,
    val cutPaper: Boolean,
    val copies: Int,
    val taxPercentDefault: Double,
    val printerAddress: String?,
    val printerName: String?
) {
    companion object {
        val Default = AppSettings(
            storeName = "Toko Saya",
            storeAddress = "",
            storePhone = "",
            headerText = "",
            footerText = "Terima kasih atas kunjungan Anda",
            paperWidth = PaperWidth.MM_58,
            titleAlignment = TextAlign.CENTER,
            currencySymbol = "Rp",
            showCashier = false,
            showCustomer = true,
            cutPaper = true,
            copies = 1,
            taxPercentDefault = 0.0,
            printerAddress = null,
            printerName = null
        )
    }
}

class SettingsRepository(private val context: Context) {

    private object Keys {
        val STORE_NAME = stringPreferencesKey("store_name")
        val STORE_ADDRESS = stringPreferencesKey("store_address")
        val STORE_PHONE = stringPreferencesKey("store_phone")
        val HEADER_TEXT = stringPreferencesKey("header_text")
        val FOOTER_TEXT = stringPreferencesKey("footer_text")
        val PAPER_WIDTH = stringPreferencesKey("paper_width")
        val TITLE_ALIGN = stringPreferencesKey("title_alignment")
        val CURRENCY = stringPreferencesKey("currency_symbol")
        val SHOW_CASHIER = booleanPreferencesKey("show_cashier")
        val SHOW_CUSTOMER = booleanPreferencesKey("show_customer")
        val CUT_PAPER = booleanPreferencesKey("cut_paper")
        val COPIES = intPreferencesKey("copies")
        val TAX_PERCENT = stringPreferencesKey("tax_percent_default")
        val PRINTER_ADDR = stringPreferencesKey("printer_address")
        val PRINTER_NAME = stringPreferencesKey("printer_name")
    }

    val settings: Flow<AppSettings> = context.dataStore.data.map { it.toSettings() }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        context.dataStore.edit { prefs ->
            val current = prefs.toSettings()
            val next = transform(current)
            prefs[Keys.STORE_NAME] = next.storeName
            prefs[Keys.STORE_ADDRESS] = next.storeAddress
            prefs[Keys.STORE_PHONE] = next.storePhone
            prefs[Keys.HEADER_TEXT] = next.headerText
            prefs[Keys.FOOTER_TEXT] = next.footerText
            prefs[Keys.PAPER_WIDTH] = next.paperWidth.name
            prefs[Keys.TITLE_ALIGN] = next.titleAlignment.name
            prefs[Keys.CURRENCY] = next.currencySymbol
            prefs[Keys.SHOW_CASHIER] = next.showCashier
            prefs[Keys.SHOW_CUSTOMER] = next.showCustomer
            prefs[Keys.CUT_PAPER] = next.cutPaper
            prefs[Keys.COPIES] = next.copies.coerceAtLeast(1)
            prefs[Keys.TAX_PERCENT] = next.taxPercentDefault.toString()
            next.printerAddress?.let { prefs[Keys.PRINTER_ADDR] = it } ?: prefs.remove(Keys.PRINTER_ADDR)
            next.printerName?.let { prefs[Keys.PRINTER_NAME] = it } ?: prefs.remove(Keys.PRINTER_NAME)
        }
    }

    suspend fun setPrinter(address: String?, name: String?) {
        context.dataStore.edit { prefs ->
            if (address.isNullOrBlank()) {
                prefs.remove(Keys.PRINTER_ADDR)
                prefs.remove(Keys.PRINTER_NAME)
            } else {
                prefs[Keys.PRINTER_ADDR] = address
                if (!name.isNullOrBlank()) prefs[Keys.PRINTER_NAME] = name
            }
        }
    }

    private fun Preferences.toSettings(): AppSettings {
        val d = AppSettings.Default
        return AppSettings(
            storeName = this[Keys.STORE_NAME] ?: d.storeName,
            storeAddress = this[Keys.STORE_ADDRESS] ?: d.storeAddress,
            storePhone = this[Keys.STORE_PHONE] ?: d.storePhone,
            headerText = this[Keys.HEADER_TEXT] ?: d.headerText,
            footerText = this[Keys.FOOTER_TEXT] ?: d.footerText,
            paperWidth = PaperWidth.fromName(this[Keys.PAPER_WIDTH]),
            titleAlignment = TextAlign.fromName(this[Keys.TITLE_ALIGN]),
            currencySymbol = this[Keys.CURRENCY] ?: d.currencySymbol,
            showCashier = this[Keys.SHOW_CASHIER] ?: d.showCashier,
            showCustomer = this[Keys.SHOW_CUSTOMER] ?: d.showCustomer,
            cutPaper = this[Keys.CUT_PAPER] ?: d.cutPaper,
            copies = this[Keys.COPIES] ?: d.copies,
            taxPercentDefault = this[Keys.TAX_PERCENT]?.toDoubleOrNull() ?: d.taxPercentDefault,
            printerAddress = this[Keys.PRINTER_ADDR],
            printerName = this[Keys.PRINTER_NAME]
        )
    }
}
