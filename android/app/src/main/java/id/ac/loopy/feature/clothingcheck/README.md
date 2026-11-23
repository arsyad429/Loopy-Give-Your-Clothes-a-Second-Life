# Pemeriksaan pakaian S05 dan S06

S05 menyiapkan foto dan jenis pakaian. S06 memuat hasil berdasarkan checkId dan meminta konfirmasi bersih serta aman sebelum melanjutkan ke S07. [Desain dan peta layar](../../../../../../../../../../docs/screen-map.md)

| File atau folder | Kegunaan |
| --- | --- |
| [`ClothingDetectorScreen.kt`](ClothingDetectorScreen.kt) | Activity Result untuk kamera dan Photo Picker, pratinjau, dropdown jenis pakaian, dan aksi pemeriksaan. |
| [`ClothingDetectorUiState.kt`](ClothingDetectorUiState.kt) | State foto, input jenis pakaian, error, serta syarat aktivasi tombol. |
| [`ClothingDetectorViewModel.kt`](ClothingDetectorViewModel.kt) | Memanggil kontrak repository, menerbitkan checkId, dan menangani loading, error, dan pembatalan. |
| [`ClothingPhotoLoader.kt`](ClothingPhotoLoader.kt) | Membaca JPEG atau PNG dari URI, membatasi ukuran dan preview, mengoreksi EXIF, serta memuat foto hasil bila diberikan URI lokal atau URL HTTPS. |
| [`ClothingPhotoQuality.kt`](ClothingPhotoQuality.kt) | Pemeriksaan awal piksel transparan, kegelapan, dan detail rendah pada sampel foto. |
| [`CheckResultScreen.kt`](CheckResultScreen.kt) | Menampilkan foto, kategori, saran tindakan, coba ulang, dan konfirmasi kelayakan. |
| [`CheckResultUiState.kt`](CheckResultUiState.kt) | Menampung hasil dan menentukan kapan penjemputan dapat diajukan. |
| [`CheckResultViewModel.kt`](CheckResultViewModel.kt) | Memuat hasil berdasarkan checkId, mencoba ulang saat gagal, dan menerima konfirmasi untuk hasil berpotensi layak. |

Tahap integrasi berikutnya mengisi repository Android, menyambungkan sesi, dan mengambil foto privat berheader bearer. API Ktor sudah tersedia terpisah. Pemeriksaan fokus S05 memakai heuristik gradien dan ambangnya diuji dengan citra sintetis. [Rujukan metode fokus](https://www.sciencedirect.com/science/article/pii/S0031320312004736)
