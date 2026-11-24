# Pemeriksaan pakaian S05 dan S06

S05 memeriksa kualitas foto sebelum upload. S06 menampilkan keputusan AI, penjelasan, dan foto privat melalui sesi Go. [Peta layar](../../../../../../../../../../docs/screen-map.md)

| File atau folder | Kegunaan |
| --- | --- |
| [`ClothingDetectorScreen.kt`](ClothingDetectorScreen.kt) | Activity Result untuk kamera dan Photo Picker, pratinjau, dropdown jenis pakaian, dan aksi pemeriksaan. |
| [`ClothingDetectorUiState.kt`](ClothingDetectorUiState.kt) | State foto, input jenis pakaian, error, serta syarat aktivasi tombol. |
| [`ClothingDetectorViewModel.kt`](ClothingDetectorViewModel.kt) | Memanggil kontrak repository, menerbitkan checkId, dan menangani loading, error, dan pembatalan. |
| [`ClothingPhotoLoader.kt`](ClothingPhotoLoader.kt) | Membaca JPEG atau PNG dari URI, membatasi ukuran dan preview, mengoreksi EXIF, serta memuat foto hasil bila diberikan URI lokal atau URL HTTPS. |
| [`ClothingPhotoQuality.kt`](ClothingPhotoQuality.kt) | Pemeriksaan awal piksel transparan, kegelapan, dan detail rendah pada sampel foto. |
| [`CheckResultScreen.kt`](CheckResultScreen.kt) | Menampilkan foto privat dari endpoint Go, kategori, saran tindakan, coba ulang, dan konfirmasi kelayakan. |
| [`CheckResultUiState.kt`](CheckResultUiState.kt) | Menampung hasil dan menentukan kapan penjemputan dapat diajukan. |
| [`CheckResultViewModel.kt`](CheckResultViewModel.kt) | Memuat hasil berdasarkan checkId, mencoba ulang saat gagal, dan menerima konfirmasi untuk hasil berpotensi layak. |

Android memakai base URL Go dan sesi login yang sama untuk upload, detail, dan foto privat. S05 mengirim `checkId` ke S06. Pemeriksaan fokus S05 memakai heuristik gradien dan ambangnya diuji dengan citra sintetis. [Rujukan metode fokus](https://www.sciencedirect.com/science/article/pii/S0031320312004736)
