package com.notathermal.app.print

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import androidx.core.app.ActivityCompat
import com.dantsu.escposprinter.EscPosPrinter
import com.dantsu.escposprinter.connection.bluetooth.BluetoothConnection
import com.dantsu.escposprinter.connection.bluetooth.BluetoothPrintersConnections
import com.dantsu.escposprinter.textparser.PrinterTextParserImg
import com.notathermal.app.domain.PaperWidth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class PrinterDevice(
    val name: String,
    val address: String,
    val isPaired: Boolean
)

class BluetoothPrinterService(private val context: Context) {

    fun isBluetoothSupported(): Boolean {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return manager?.adapter != null
    }

    fun isBluetoothEnabled(): Boolean {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        return manager?.adapter?.isEnabled == true
    }

    fun hasConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    @SuppressLint("MissingPermission")
    fun listPairedDevices(): List<PrinterDevice> {
        if (!hasConnectPermission()) return emptyList()
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        val adapter: BluetoothAdapter = manager?.adapter ?: return emptyList()
        return adapter.bondedDevices.orEmpty().mapNotNull { device ->
            try {
                PrinterDevice(
                    name = device.name ?: "(Tanpa nama)",
                    address = device.address,
                    isPaired = device.bondState == BluetoothDevice.BOND_BONDED
                )
            } catch (e: SecurityException) {
                null
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun findConnection(address: String): BluetoothConnection? {
        return BluetoothPrintersConnections().list?.firstOrNull { it.device.address == address }
    }

    suspend fun testPrint(address: String, paperWidth: PaperWidth): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = findConnection(address) ?: error("Printer tidak ditemukan / belum dipair")
            val printer = createPrinter(connection, paperWidth)
            printer.printFormattedTextAndCut(testReceipt(paperWidth), 10f)
            printer.disconnectPrinter()
            Unit
        }
    }

    suspend fun print(
        address: String,
        formatted: String,
        paperWidth: PaperWidth,
        copies: Int = 1,
        cutPaper: Boolean = true,
        mmFeedBeforeCut: Int = 10
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = findConnection(address) ?: error("Printer tidak ditemukan / belum dipair")
            val printer = createPrinter(connection, paperWidth)
            // Trim trailing whitespace, but keep one trailing newline so the
            // last printed line is not flush against the cut.
            val trimmed = formatted.trimEnd('\n', '\r', ' ', '\t')
            val text = "$trimmed\n[L]\n"
            val feed = mmFeedBeforeCut.coerceIn(0, 30).toFloat()
            repeat(copies.coerceAtLeast(1)) {
                if (cutPaper) printer.printFormattedTextAndCut(text, feed) else printer.printFormattedText(text, feed)
            }
            printer.disconnectPrinter()
            Unit
        }
    }

    suspend fun printImage(
        address: String,
        bitmap: Bitmap,
        paperWidth: PaperWidth,
        copies: Int = 1,
        cutPaper: Boolean = true,
        mmFeedBeforeCut: Int = 10
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val connection = findConnection(address) ?: error("Printer tidak ditemukan / belum dipair")
            val printer = createPrinter(connection, paperWidth)
            // The library reads bitmaps in horizontal slices of 256px tall.
            // Resizing wide images to printer width keeps the print sharp and
            // avoids rendering artifacts from auto-scaling.
            val targetWidth = printer.printerWidthPx.coerceAtLeast(1)
            val scaled = scaleToWidth(bitmap, targetWidth)
            val hex = PrinterTextParserImg.bitmapToHexadecimalString(printer, scaled)
            val text = "[C]<img>$hex</img>\n[L]\n"
            val feed = mmFeedBeforeCut.coerceIn(0, 30).toFloat()
            repeat(copies.coerceAtLeast(1)) {
                if (cutPaper) printer.printFormattedTextAndCut(text, feed) else printer.printFormattedText(text, feed)
            }
            printer.disconnectPrinter()
            if (scaled !== bitmap) scaled.recycle()
            Unit
        }
    }

    private fun scaleToWidth(bitmap: Bitmap, targetWidth: Int): Bitmap {
        if (bitmap.width == targetWidth) return bitmap
        val ratio = targetWidth.toFloat() / bitmap.width.toFloat()
        val targetHeight = (bitmap.height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    private fun createPrinter(connection: BluetoothConnection, paperWidth: PaperWidth): EscPosPrinter {
        val widthMm = paperWidth.mm
        val charsPerLine = paperWidth.charsNormal
        return EscPosPrinter(connection, 203, widthMm, charsPerLine)
    }

    private fun testReceipt(paperWidth: PaperWidth): String {
        val w = paperWidth.charsNormal
        return buildString {
            append("[C]<u><b>Nota Thermal</b></u>\n")
            append("[C]Test Print\n")
            append("[L]\n")
            append("[L]").append("-".repeat(w)).append('\n')
            append("[L]Jika Anda dapat membaca pesan\n")
            append("[L]ini, koneksi printer berhasil.\n")
            append("[L]").append("-".repeat(w)).append('\n')
            append("[C]Terima kasih\n")
        }
    }
}
