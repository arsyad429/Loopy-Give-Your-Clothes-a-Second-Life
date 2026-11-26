# Pemeriksaan foto pakaian

[ClothingDetectorScreen.kt](ClothingDetectorScreen.kt) membuka Photo Picker atau kamera lewat `FileProvider`. [ClothingPhotoLoader.kt](ClothingPhotoLoader.kt) membaca pratinjau, dan [ClothingPhotoQuality.kt](ClothingPhotoQuality.kt) menandai foto yang sulit dinilai. [ClothingDetectorViewModel.kt](ClothingDetectorViewModel.kt) mengirim JPEG/PNG serta jenis pakaian melalui repository.

[CheckResultViewModel.kt](CheckResultViewModel.kt) mengambil hasil S06 menurut `checkId`. [CheckResultScreen.kt](CheckResultScreen.kt) menampilkan foto privat dan kategori AI. Hasil `POTENTIALLY_ELIGIBLE` dengan konfirmasi pakaian bersih dan aman membuka [pickup](../pickup/README.md).

Batas upload adalah 10 MiB. Backend menormalisasi gambar sebelum mengirim ke Groq. Untuk memeriksa gagalnya proses, cek MIME dan ukuran foto, koneksi API, serta `GROQ_API_KEY` di server. [Repository](../../data/repository/ClothingCheckRepositoryImpl.kt) · [Alur Groq](../../../../../../../../../../backend/go/internal/vision/README.md) · [Tes layar](../../../../../../../androidTest/java/id/ac/loopy/feature/clothingcheck/README.md)
