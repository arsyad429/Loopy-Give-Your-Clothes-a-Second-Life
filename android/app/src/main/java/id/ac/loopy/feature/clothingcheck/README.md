# clothingcheck

Fitur S05 AI Clothing Detector dan S06 Clothing Check Result. Kedua layar sudah terhubung ke ViewModel masing-masing. Integrasi backend AI masih menunggu implementasi repository dan penyambungannya ke aplikasi.

## Isi file dan perannya

| File/folder | Peran dan isi yang diharapkan |
| --- | --- |
| `CheckResultScreen.kt` | S06 menampilkan foto, jenis, deskripsi, dan saran dari hasil AI. Menangani kondisi memuat, gagal, dan tanpa hasil. Memerlukan konfirmasi bersih dan aman sebelum membuka penjemputan. |
| `CheckResultUiState.kt` | State S06 untuk hasil pemeriksaan, loading, error, dan konfirmasi bersih serta aman. Menentukan kapan pengguna bisa melanjutkan ke pickup. |
| `CheckResultViewModel.kt` | Memuat hasil berdasarkan checkId lewat repository, menyediakan coba ulang saat gagal, dan menerima konfirmasi untuk hasil yang berpotensi layak. Digunakan oleh rute S06 dengan checkId. |
| `ClothingDetectorScreen.kt` | UI S05 sesuai desain: route kamera/Photo Picker, pratinjau foto, tips, dan tombol pemeriksaan. `ClothingDetectorContent` menerima state/callback. TODO: hubungkan analisis melalui ViewModel/repository. |
| `ClothingDetectorUiState.kt` | State immutable untuk bitmap pratinjau, nama foto, loading, analisis, dan error. `canAnalyze` menentukan apakah tombol pemeriksaan aktif. TODO: sesuaikan dengan hasil integrasi backend. |
| `ClothingPhotoLoader.kt` | Membaca foto dari URI di `Dispatchers.IO`; memvalidasi JPEG/PNG dan batas 10 MiB, menyiapkan pratinjau maksimal 1600px, serta mengoreksi orientasi EXIF. Khusus S05, menjalankan pemeriksaan kualitas pada sampel hingga 128px. |
| `ClothingPhotoQuality.kt` | Memeriksa konten transparan, kegelapan, dan detail yang sangat rendah lewat perubahan luminans. Ambang awal ini perlu diuji dengan foto pakaian asli. |
| `ClothingDetectorViewModel.kt` | TEMPLATE S05 (F04) — belum ada implementasi bisnis. TODO: buat ClothingDetectorViewModel: ViewModel dengan private MutableStateFlow, StateFlow immutable, viewModelScope, cancellation, loading/success/error dan cegah request berulang. Kebutuhan: Photo Picker dan kamera Activity Result + FileProvider; validasi MIME/ukuran/kejelasan; upload ke backend AI; loading/error/retry; gagal tidak boleh lanjut. Definisi selesai: acceptance criteria pada docs/acceptance-tests.md terkait fitur ini lulus. |

## TODO

- [x] **S05 / F04 — input lokal**: UI, Photo Picker, kamera Activity Result + FileProvider, pratinjau, validasi MIME/ukuran, serta loading/error pembacaan foto.
- [x] **S05 / F04 — pemeriksaan kualitas awal**: blokir foto yang terlalu gelap atau tampak sangat buram, pertahankan pratinjau, dan minta pengguna mengganti foto.
- [ ] **S05 / F04 — integrasi AI**: verifikasi objek dan foto yang meragukan, upload melalui ViewModel/repository, loading/error/retry analisis; kegagalan tidak boleh membuka hasil sukses.
- [x] **S06 / F05 — UI**: Foto, jenis, hasil, deskripsi dan saran; tiga kategori; konfirmasi sebelum melanjutkan ke penjemputan.
- [ ] **S06 / F05 — integrasi**: Verifikasi pemuatan hasil dan signed URL dari backend aktual. S07 perlu mengambil `checkId` dari rute dan mengirim pengajuan sesuai aturan server.
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

TODO: Hubungkan repository dan upload ke backend, bersihkan cache setelah pemeriksaan,
dan uji kamera/galeri pada perangkat.

### Pemeriksaan kualitas foto S05

Pemeriksaan dijalankan setelah dekode dan koreksi EXIF. Foto yang ditolak tetap tampil bersama pesan
foto ulang. Perkiraan ketajaman membandingkan perubahan luminans lokal pada dua jarak.
Foto dengan sedikit tekstur saja tetap bisa diteruskan ke AI jika kegelapan atau blur berat tidak
terdeteksi. Teknik gradien mengacu pada prinsip penilaian fokus dalam
[Pertuz dkk.](https://www.sciencedirect.com/science/article/pii/S0031320312004736), dengan ambang
awal khusus aplikasi yang diuji memakai citra sintetis.

## Implementasi S06

- Rute `s06/{checkId}` membuat `CheckResultViewModel` dari repository yang sama dengan S05.
- Foto dapat berasal dari URI lokal `content://` atau signed URL HTTPS, dibaca di IO dengan batas 10 MiB dan koreksi orientasi EXIF. Jika foto tidak dapat dimuat, hasil pemeriksaan tetap terlihat.
- Hasil Perlu foto ulang dan Tidak direkomendasikan dapat kembali ke S05. Hasil Berpotensi layak dapat membuka `s07/{checkId}` setelah dikonfirmasi. Formulir S07 masih template.
- `CheckResultScreenTest` mencakup tiga kategori, pembacaan foto dari FileProvider, konfirmasi dan ID pemeriksaan, serta percobaan ulang setelah gagal memuat hasil.

## Dropdown jenis pakaian (19 November 2026)

- Field Jenis pakaian ditempatkan setelah pratinjau dan sebelum tombol kamera/galeri.
- `domain/model/ClothingType.kt` mendefinisikan 20 label dan value backend yang disepakati.
  Pilihan awal Kaos (`TSHIRT`); gunakan `clothingType.backendValue` saat membangun request API.
- `ClothingDetectorUiState.clothingType` menyediakan pilihan untuk UI. Route menyimpan value
  memakai rememberSaveable, sehingga pilihan bertahan saat rotasi dan pemilihan foto ulang.
- Dropdown nonaktif ketika foto dimuat atau analisis berjalan; daftar dapat digulir.
- TODO: teruskan clothingType bersama foto melalui repository saat endpoint upload diimplementasikan.
- Screenshot `docs/reference/s05-android.png` berasal dari versi 16 November, sebelum dropdown ditambahkan.
- Pemeriksaan perubahan dropdown: `assembleDebug`, `testDebugUnitTest`, dan
  `assembleDebugAndroidTest` berhasil. Tes interaksi dropdown ditambahkan, tetapi belum
  dijalankan di perangkat/emulator pada perubahan ini.

## Pemeriksaan implementasi

- `assembleDebug`, `testDebugUnitTest`, `lintDebug`: berhasil; lint masih memberi warning
  saran migrasi platform ExifInterface ke AndroidX serta kompleksitas path ikon hasil ekspor.
- `assembleDebugAndroidTest`: berhasil. Espresso diperbarui ke 3.7.0 dan AndroidX JUnit ke 1.3.0
  untuk menghindari pemanggilan InputManager reflection yang tidak tersedia di emulator terbaru.
- Tiga tes ClothingDetectorScreenTest lulus di Pixel_8 (API 37).
- Screenshot state awal pada docs/reference/s05-android.png, diperiksa terhadap screenshot S05 pengguna.
- Pengujian otomatis ini mencakup layout/state/callback; belum menguji aktivitas kamera/galeri
  perangkat secara menyeluruh atau analisis AI.
