package com.notathermal.app.ui.printimage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.exifinterface.media.ExifInterface
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.notathermal.app.data.prefs.AppSettings
import com.notathermal.app.data.prefs.SettingsRepository
import com.notathermal.app.print.BluetoothPrinterService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Stable
class PrintImageStateHolder {
    var sourceBitmap by mutableStateOf<Bitmap?>(null)
    var processedBitmap by mutableStateOf<Bitmap?>(null)
    var brightness by mutableStateOf(0f) // -100 .. 100
    var contrast by mutableStateOf(0f) // -100 .. 100
    var dither by mutableStateOf(true)
    var sending by mutableStateOf(false)
    var info by mutableStateOf<String?>(null)
    var error by mutableStateOf<String?>(null)

    private val canPrintState = derivedStateOf { sourceBitmap != null && !sending }
    val canPrint: Boolean get() = canPrintState.value
}

class PrintImageViewModel(
    private val app: Context,
    private val printerService: BluetoothPrinterService,
    settingsRepository: SettingsRepository
) : ViewModel() {

    val holder = PrintImageStateHolder()

    val settings: StateFlow<AppSettings?> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun loadFromUri(uri: Uri) {
        holder.error = null
        holder.info = null
        viewModelScope.launch {
            runCatching {
                withContext(Dispatchers.IO) { decodeBitmap(uri) }
            }.onSuccess { bmp ->
                holder.sourceBitmap?.recycle()
                holder.sourceBitmap = bmp
                refreshProcessed()
            }.onFailure { t ->
                holder.error = t.message ?: "Gagal membuka gambar"
            }
        }
    }

    fun setBrightness(value: Float) {
        holder.brightness = value
        refreshProcessed()
    }

    fun setContrast(value: Float) {
        holder.contrast = value
        refreshProcessed()
    }

    fun setDither(value: Boolean) {
        holder.dither = value
        refreshProcessed()
    }

    fun clear() {
        holder.sourceBitmap?.recycle()
        holder.processedBitmap?.recycle()
        holder.sourceBitmap = null
        holder.processedBitmap = null
        holder.error = null
        holder.info = null
    }

    private fun refreshProcessed() {
        val src = holder.sourceBitmap ?: return
        viewModelScope.launch {
            val out = withContext(Dispatchers.Default) {
                applyAdjustments(src, holder.brightness, holder.contrast, holder.dither)
            }
            val previous = holder.processedBitmap
            holder.processedBitmap = out
            previous?.recycle()
        }
    }

    fun print(address: String?, paperWidthMm: Float, copies: Int, cutPaper: Boolean) {
        val target = address
        if (target.isNullOrBlank()) {
            holder.error = "Pilih printer Bluetooth dulu di halaman Printer."
            return
        }
        val source = holder.sourceBitmap ?: run {
            holder.error = "Pilih gambar terlebih dahulu."
            return
        }
        val paperWidth = com.notathermal.app.domain.PaperWidth.values()
            .minByOrNull { kotlin.math.abs(it.mm - paperWidthMm) } ?: com.notathermal.app.domain.PaperWidth.MM_58
        holder.sending = true
        holder.error = null
        holder.info = null
        viewModelScope.launch {
            val toPrint = withContext(Dispatchers.Default) {
                applyAdjustments(source, holder.brightness, holder.contrast, holder.dither)
            }
            val result = printerService.printImage(
                address = target,
                bitmap = toPrint,
                paperWidth = paperWidth,
                copies = copies.coerceAtLeast(1),
                cutPaper = cutPaper
            )
            toPrint.recycle()
            holder.sending = false
            result.onSuccess {
                holder.info = "Gambar berhasil dikirim ke printer."
            }.onFailure { t ->
                holder.error = t.message ?: "Gagal mencetak"
            }
        }
    }

    override fun onCleared() {
        holder.sourceBitmap?.recycle()
        holder.processedBitmap?.recycle()
        super.onCleared()
    }

    private fun decodeBitmap(uri: Uri): Bitmap {
        val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        app.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        // Cap longest side at ~2000px to keep memory and processing reasonable.
        val maxSide = 2000
        var sample = 1
        while (opts.outWidth / sample > maxSide || opts.outHeight / sample > maxSide) {
            sample *= 2
        }
        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bitmap = app.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOpts)
        } ?: error("Tidak bisa membaca gambar")

        val rotation = readExifRotation(uri)
        return if (rotation != 0) rotate(bitmap, rotation.toFloat()).also { if (it !== bitmap) bitmap.recycle() } else bitmap
    }

    private fun readExifRotation(uri: Uri): Int = runCatching {
        app.contentResolver.openInputStream(uri)?.use { ExifInterface(it) }
            ?.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            ?.let { orientation ->
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
    }.getOrDefault(0)

    private fun rotate(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    @Suppress("UNUSED_PARAMETER")
    private fun applyAdjustments(src: Bitmap, brightness: Float, contrast: Float, dither: Boolean): Bitmap {
        val w = src.width
        val h = src.height
        val pixels = IntArray(w * h)
        src.getPixels(pixels, 0, w, 0, 0, w, h)

        // Pre-convert to grayscale and apply brightness + contrast.
        val b = brightness // -100..100, treated as offset
        val cFactor = (259.0 * (contrast + 255.0)) / (255.0 * (259.0 - contrast))
        val gray = IntArray(w * h)
        for (i in pixels.indices) {
            val px = pixels[i]
            val r = (px shr 16) and 0xFF
            val g = (px shr 8) and 0xFF
            val bl = px and 0xFF
            // Standard luma weights
            var y = (0.299 * r + 0.587 * g + 0.114 * bl)
            y = cFactor * (y - 128.0) + 128.0 + b
            gray[i] = y.coerceIn(0.0, 255.0).toInt()
        }

        return if (dither) {
            // Floyd-Steinberg dithering on the grayscale buffer.
            val out = IntArray(w * h)
            val buf = gray.map { it.toFloat() }.toFloatArray()
            for (y in 0 until h) {
                for (x in 0 until w) {
                    val idx = y * w + x
                    val old = buf[idx]
                    val newP = if (old < 128f) 0f else 255f
                    val err = old - newP
                    out[idx] = if (newP == 0f) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
                    if (x + 1 < w) buf[idx + 1] += err * 7f / 16f
                    if (y + 1 < h) {
                        if (x > 0) buf[idx + w - 1] += err * 3f / 16f
                        buf[idx + w] += err * 5f / 16f
                        if (x + 1 < w) buf[idx + w + 1] += err * 1f / 16f
                    }
                }
            }
            Bitmap.createBitmap(out, w, h, Bitmap.Config.ARGB_8888)
        } else {
            // Simple threshold.
            val out = IntArray(w * h)
            for (i in gray.indices) {
                out[i] = if (gray[i] < 128) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
            }
            Bitmap.createBitmap(out, w, h, Bitmap.Config.ARGB_8888)
        }
    }
}
