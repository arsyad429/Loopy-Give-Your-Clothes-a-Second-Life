# clothingcheck

Fitur S05 AI Clothing Detector dan S06 Clothing Check Result. UI S05 dan input foto lokal sudah diimplementasikan; S06 serta integrasi backend AI masih template. Kerjakan Screen/UiState/ViewModel terpisah.

## Isi file dan perannya

| File/folder | Peran dan isi yang diharapkan |
| --- | --- |
| `CheckResultScreen.kt` | Template S06: Foto, jenis, hasil, deskripsi; tiga kategori; eligible saja boleh pickup; konfirmasi pakaian bersih dan aman; hasil AI estimasi awal. |
| `CheckResultUiState.kt` | TEMPLATE S06 (F05) — belum ada implementasi bisnis. TODO: buat CheckResultUiState: data class immutable berisi nilai form/data, field errors, loading, error, serta event sukses tanpa menyimpan NavController. Kebutuhan: Foto, jenis, hasil, deskripsi; tiga kategori; eligible saja boleh pickup; konfirmasi pakaian bersih dan aman; hasil AI estimasi awal. Definisi selesai: acceptance criteria pada docs/acceptance-tests.md terkait fitur ini lulus. |
| `CheckResultViewModel.kt` | TEMPLATE S06 (F05) — belum ada implementasi bisnis. TODO: buat CheckResultViewModel: ViewModel dengan private MutableStateFlow, StateFlow immutable, viewModelScope, cancellation, loading/success/error dan cegah request berulang. Kebutuhan: Foto, jenis, hasil, deskripsi; tiga kategori; eligible saja boleh pickup; konfirmasi pakaian bersih dan aman; hasil AI estimasi awal. Definisi selesai: acceptance criteria pada docs/acceptance-tests.md terkait fitur ini lulus. |
| `ClothingDetectorScreen.kt` | UI S05 sesuai desain: route kamera/Photo Picker, pratinjau foto, tips, dan tombol pemeriksaan. `ClothingDetectorContent` menerima state/callback. TODO: hubungkan analisis melalui ViewModel/repository. |
| `ClothingDetectorUiState.kt` | State immutable untuk bitmap pratinjau, nama foto, loading, analisis, dan error. `canAnalyze` menentukan apakah tombol pemeriksaan aktif. TODO: sesuaikan dengan hasil integrasi backend. |
| `ClothingPhotoLoader.kt` | Membaca foto dari URI di `Dispatchers.IO`; memvalidasi JPEG/PNG dan batas 10 MiB, menyiapkan pratinjau maksimal 1600px, serta mengoreksi orientasi EXIF. TODO: sepakati batas upload dengan backend dan uji variasi foto/provider pada perangkat. |
| `ClothingDetectorViewModel.kt` | TEMPLATE S05 (F04) — belum ada implementasi bisnis. TODO: buat ClothingDetectorViewModel: ViewModel dengan private MutableStateFlow, StateFlow immutable, viewModelScope, cancellation, loading/success/error dan cegah request berulang. Kebutuhan: Photo Picker dan kamera Activity Result + FileProvider; validasi MIME/ukuran/kejelasan; upload ke backend AI; loading/error/retry; gagal tidak boleh lanjut. Definisi selesai: acceptance criteria pada docs/acceptance-tests.md terkait fitur ini lulus. |

## TODO

- [x] **S05 / F04 — input lokal**: UI, Photo Picker, kamera Activity Result + FileProvider, pratinjau, validasi MIME/ukuran, serta loading/error pembacaan foto.
- [ ] **S05 / F04 — integrasi AI**: pemeriksaan kejelasan/objek foto, upload melalui ViewModel/repository, loading/error/retry analisis; kegagalan tidak boleh membuka hasil sukses.
- [ ] **S06 / F05**: Foto, jenis, hasil, deskripsi; tiga kategori; eligible saja boleh pickup; konfirmasi pakaian bersih dan aman; hasil AI estimasi awal.
- [ ] Screen stateless menerima state/callback; UiState immutable; ViewModel via repository, StateFlow dan viewModelScope.
- [ ] Loading/error/empty, cancellation/retry dan anti double-submit, sesuai fitur.
- [ ] Cocokkan node di docs/screen-map.md dan lengkapi AC terkait sebelum menandai selesai.

## Implementasi S05 (16 November 2026)

- ClothingDetectorScreen: route kamera/Photo Picker dan ClothingDetectorContent stateless.
  Layout, teks, warna, radius, ikon, tipografi dari .fig lokal 2:88 dan screenshot pengguna.
  Status bar memakai Android asli. Preview Compose tersedia untuk 390dp dan 320dp/font 130%.
- ClothingDetectorUiState: foto preview, nama, loading/analisis/error, syarat tombol aktif.
- ClothingPhotoLoader: Dispatchers.IO, validasi JPEG/PNG dan maksimal 10 MiB,
  preview maksimal 1600px, koreksi EXIF, cancellation saat URI berubah.
  Limit lokal perlu disepakati dengan backend; kejelasan/objek foto belum diperiksa AI.
- Kamera memakai FileProvider; galeri PickVisualMedia tanpa izin akses seluruh galeri.
  URI disimpan lintas recreation; pembatalan mempertahankan foto sebelumnya.
- Ilustrasi kaus bukan hasil upload; tombol analisis aktif setelah foto valid dipilih.
  Tombol menampilkan pesan layanan AI belum terhubung, tanpa hasil palsu atau navigasi sukses.

TODO: ViewModel/repository, upload, retry/cancellation analisis, navigasi S06 via checkId,
pembersihan cache setelah pemeriksaan, uji kamera/galeri pada perangkat.

## Pemeriksaan implementasi

- `assembleDebug`, `testDebugUnitTest`, `lintDebug`: berhasil; lint masih memberi warning
  saran migrasi platform ExifInterface ke AndroidX serta kompleksitas path ikon hasil ekspor.
- `assembleDebugAndroidTest`: berhasil. Espresso diperbarui ke 3.7.0 dan AndroidX JUnit ke 1.3.0
  untuk menghindari pemanggilan InputManager reflection yang tidak tersedia di emulator terbaru.
- Tiga tes ClothingDetectorScreenTest lulus di Pixel_8 (API 37).
- Screenshot state awal pada docs/reference/s05-android.png, diperiksa terhadap screenshot S05 pengguna.
- Pengujian otomatis ini mencakup layout/state/callback; belum menguji aktivitas kamera/galeri
  perangkat secara menyeluruh atau analisis AI.
