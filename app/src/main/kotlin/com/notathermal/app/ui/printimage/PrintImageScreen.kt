package com.notathermal.app.ui.printimage

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.notathermal.app.ui.common.appViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrintImageScreen(onBack: () -> Unit, onOpenPrinter: () -> Unit) {
    val viewModel = appViewModel { container ->
        PrintImageViewModel(
            app = container.appContext,
            printerService = container.bluetoothPrinterService,
            settingsRepository = container.settingsRepository
        )
    }
    val holder = viewModel.holder
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    val pickLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) viewModel.loadFromUri(uri)
    }

    var copies by remember { mutableStateOf("1") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cetak Gambar") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { padding ->
        val current = settings
        val printerName = current?.printerName.orEmpty()
        val printerAddress = current?.printerAddress
        val paperMm = current?.paperWidth?.mm ?: com.notathermal.app.domain.PaperWidth.MM_58.mm
        val cutPaper = current?.cutPaper ?: true

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "Cetak gambar langsung ke printer thermal — tidak disimpan ke history.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Printer", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(4.dp))
                    if (printerAddress.isNullOrBlank()) {
                        Text("Belum ada printer dipilih.", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Text(
                            printerName.ifBlank { printerAddress },
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(printerAddress, style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = onOpenPrinter, modifier = Modifier.fillMaxWidth()) {
                        Text(if (printerAddress.isNullOrBlank()) "Pilih printer" else "Ganti printer")
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    pickLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Image, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (holder.sourceBitmap == null) "Pilih gambar dari galeri" else "Ganti gambar")
            }

            val preview = holder.processedBitmap ?: holder.sourceBitmap
            if (preview != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Image(
                            bitmap = preview.asImageBitmap(),
                            contentDescription = "Preview",
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(preview.width.toFloat() / preview.height.toFloat().coerceAtLeast(1f))
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Fit
                        )
                    }
                }

                AdjustSlider(
                    label = "Kecerahan",
                    value = holder.brightness,
                    valueRange = -100f..100f,
                    onChange = viewModel::setBrightness
                )
                AdjustSlider(
                    label = "Kontras",
                    value = holder.contrast,
                    valueRange = -100f..100f,
                    onChange = viewModel::setContrast
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Dithering (rekomendasi untuk foto)")
                        Text(
                            "Mati = threshold hitam-putih sederhana (cocok untuk logo / teks).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(checked = holder.dither, onCheckedChange = viewModel::setDither)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Jumlah copy", modifier = Modifier.weight(1f))
                    listOf("1", "2", "3").forEach { value ->
                        FilterChip(
                            selected = copies == value,
                            onClick = { copies = value },
                            label = { Text(value) }
                        )
                    }
                }
            }

            holder.error?.let { msg ->
                Text(msg, color = MaterialTheme.colorScheme.error)
            }
            holder.info?.let { msg ->
                Text(msg, color = MaterialTheme.colorScheme.primary)
            }

            Button(
                onClick = {
                    viewModel.print(
                        address = printerAddress,
                        paperWidthMm = paperMm,
                        copies = copies.toIntOrNull() ?: 1,
                        cutPaper = cutPaper
                    )
                },
                enabled = holder.canPrint && !printerAddress.isNullOrBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Print, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (holder.sending) "Mencetak…" else "Cetak gambar")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AdjustSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = Modifier.weight(1f))
            Text(value.toInt().toString(), style = MaterialTheme.typography.labelMedium)
        }
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = valueRange
        )
    }
}

