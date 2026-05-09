# Nota Thermal — Android

Aplikasi Android sederhana untuk **membuat invoice / struk** dan **mencetak via Bluetooth thermal printer (ESC/POS)**.
Fokus minimalis: tanpa login, tanpa master produk / inventory, tanpa multi-user. Cocok untuk warung,
penjual jasa, atau usaha kecil yang hanya butuh cetak struk cepat.

## Fitur

- Buat invoice dengan multi-item (nama, qty, harga, diskon per item)
- Diskon, pajak (%), metode pembayaran (Tunai / QRIS / Debit / Kredit / Transfer)
- Nomor invoice otomatis: `INV-YYYYMMDD-NNNN`
- Riwayat invoice tersimpan lokal (Room database)
- Detail invoice + preview struk (monospace) sebelum cetak
- Cetak Bluetooth thermal printer ESC/POS:
  - Lebar kertas **58mm / 80mm**
  - Alignment header / footer (LEFT / CENTER / RIGHT)
  - Auto cut, jumlah copy, simbol mata uang custom
  - Toggle nama kasir / pelanggan di struk
- Pengaturan toko: nama, alamat, telepon, header / footer custom, pajak default

## Stack

| Lapisan        | Teknologi                                                                                       |
| -------------- | ----------------------------------------------------------------------------------------------- |
| UI             | Jetpack Compose + Material 3                                                                    |
| Arsitektur     | MVVM dengan Repository + manual DI (`AppContainer`)                                              |
| Database       | Room                                                                                            |
| Preferences    | DataStore                                                                                       |
| Printing       | [DantSu/ESCPOS-ThermalPrinter-Android](https://github.com/DantSu/ESCPOS-ThermalPrinter-Android) |

- Min SDK: **24** (Android 7.0)
- Target SDK: **34**

## Persyaratan untuk build

- **JDK 17**
- **Android SDK** dengan `platforms;android-34` dan `build-tools;34.0.0`
- Set `ANDROID_HOME` (mis. `~/android-sdk`) **atau** buat file `local.properties`:
  ```
  sdk.dir=/path/to/android-sdk
  ```

## Build dari command line

```sh
git clone https://github.com/furansugg/nota-thermal.git
cd nota-thermal

echo "sdk.dir=$ANDROID_HOME" > local.properties

# Build APK debug → app/build/outputs/apk/debug/app-debug.apk
./gradlew assembleDebug

# Lint (peringatan saja, tidak abort)
./gradlew lintDebug

# Install langsung ke device/emulator yang aktif
./gradlew installDebug
```

Build pertama akan men-download semua dependency dari Maven Central + JitPack
(~5–10 menit tergantung koneksi). Build berikutnya jauh lebih cepat.

## Cara pakai (pengguna akhir)

1. Buka aplikasi → menu **Pengaturan** (ikon gear, kanan atas):
   - Isi nama toko, alamat, telepon, footer.
   - Pilih lebar kertas printer (58mm / 80mm).
   - Atur jumlah copy, auto-cut, dll.
   - Tekan **Simpan**.
2. Pair printer thermal Bluetooth dari **Setelan Android → Bluetooth**
   (PIN umum: `0000` / `1234`).
3. Di aplikasi, tekan ikon **Bluetooth** (kanan atas):
   - Berikan izin Bluetooth saat diminta.
   - Pilih printer dari daftar **Perangkat ter-pair**.
   - Tekan **Test Print** untuk memastikan koneksi OK.
4. Kembali ke beranda → tekan **Invoice Baru**:
   - Isi item (nama, qty, harga), diskon, pajak, metode pembayaran.
   - Tekan **Simpan & Lihat**.
5. Di halaman detail invoice → tekan **Cetak**.

> **Permission**: Android 12+ butuh `BLUETOOTH_CONNECT` & `BLUETOOTH_SCAN`
> (runtime). Android ≤ 11 butuh `ACCESS_FINE_LOCATION` untuk discovery. App
> akan meminta otomatis saat membuka menu Printer.

## Struktur direktori

```
app/src/main/kotlin/com/notathermal/app/
├── data/
│   ├── db/                # Room entity / DAO / database
│   ├── prefs/             # DataStore settings
│   └── repo/              # Repository (InvoiceRepository)
├── di/                    # AppContainer (manual DI)
├── domain/                # Enum (PaperWidth, TextAlign)
├── print/                 # BluetoothPrinterService + ReceiptComposer (ESC/POS)
├── ui/
│   ├── common/            # appViewModel helper
│   ├── theme/             # Material 3 theme
│   ├── home/              # Daftar invoice + FAB
│   ├── form/              # Form buat invoice baru
│   ├── detail/            # Detail invoice + tombol cetak
│   ├── settings/          # Setelan toko & cetak
│   └── printer/           # Pilih / test printer Bluetooth
└── util/                  # Format (mata uang, tanggal, nomor invoice)
```

## Lisensi

MIT — bebas dipakai, dimodifikasi, dan didistribusikan.
