# core

Tempat utilitas lintas fitur. Alur kamera dan sesi aktif berada di fitur clothingcheck dan repository auth.

## Isi file dan perannya

| File atau folder | Kegunaan |
| --- | --- |
| [`DonationSearch.kt`](DonationSearch.kt) | TEMPLATE — TODO: StateFlow query → debounce → distinctUntilChanged → collectLatest; cancellation saat ViewModel dihentikan. |
| [`PhotoCapture.kt`](PhotoCapture.kt) | TEMPLATE F04 — TODO: buat cacheDir/clothing_photos, URI FileProvider authority ${packageName}.files. Gunakan ActivityResultContracts.TakePicture dan PickVisualMedia; simpan URI lintas recreation. Batal tidak dianggap sukses. Validasi konten/ukuran sebelum upload; hapus foto sementara sesuai lifecycle. FileProvider sudah terdaftar, res/xml/file_paths.xml hanya mengekspos folder cache foto tersebut. |
| [`PhotoValidator.kt`](PhotoValidator.kt) | TEMPLATE — TODO: validasi MIME, ukuran, file nyata; resize aman di Dispatchers.IO; tangani content URI kamera/galeri. |
| [`SessionStore.kt`](SessionStore.kt) | TEMPLATE — TODO: penyimpanan token terenkripsi Keystore, expiry, refresh, logout; jangan simpan password/token plaintext di DataStore. |
