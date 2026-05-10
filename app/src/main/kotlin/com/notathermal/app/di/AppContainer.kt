package com.notathermal.app.di

import android.content.Context
import androidx.room.Room
import com.notathermal.app.data.db.AppDatabase
import com.notathermal.app.data.prefs.SettingsRepository
import com.notathermal.app.data.repo.InvoiceRepository
import com.notathermal.app.ai.AiInvoiceParser
import com.notathermal.app.print.BluetoothPrinterService
import com.notathermal.app.print.ReceiptComposer

/** Manual DI container — created in [com.notathermal.app.NotaApp]. */
class AppContainer(context: Context) {
    private val appContext: Context = context.applicationContext

    private val db: AppDatabase = Room.databaseBuilder(
        appContext,
        AppDatabase::class.java,
        "nota-thermal.db"
    )
        .fallbackToDestructiveMigration()
        .build()

    val invoiceRepository: InvoiceRepository = InvoiceRepository(db.invoiceDao())
    val settingsRepository: SettingsRepository = SettingsRepository(appContext)
    val bluetoothPrinterService: BluetoothPrinterService = BluetoothPrinterService(appContext)
    val receiptComposer: ReceiptComposer = ReceiptComposer()
    val aiInvoiceParser: AiInvoiceParser = AiInvoiceParser()
}
