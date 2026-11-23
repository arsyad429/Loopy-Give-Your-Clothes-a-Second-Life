# Pemeriksaan pakaian

Rute pemeriksaan menerima JPEG atau PNG dan jenis pakaian yang dipilih pengguna. Groq menilai kondisi visual pakaian. Hasil, foto JPEG privat, dan penyelesaian idempotensi disimpan bersama dalam PostgreSQL.

| File atau folder | Kegunaan |
| --- | --- |
| [`AiVisionGateway.kt`](AiVisionGateway.kt) | Membentuk request Groq, membatasi waktu dan ukuran respons, serta memvalidasi tiga kategori dan rekomendasi. |
| [`ClothingCheckRoutes.kt`](ClothingCheckRoutes.kt) | POST dan GET pemeriksaan, route foto privat, batas multipart, dan pemetaan error HTTP. |
| [`ClothingCheckService.kt`](ClothingCheckService.kt) | Validasi jenis pakaian, reservasi key, normalisasi, pemanggilan Groq, retry terbatas, dan penyimpanan hasil. |
| [`ClothingCheckStore.kt`](ClothingCheckStore.kt) | Reservasi idempotensi dan transaksi penyimpanan hasil. Membaca hasil untuk pemilik atau admin. |
| [`ClothingPhotoInput.kt`](ClothingPhotoInput.kt) | Memvalidasi format dan dimensi, menerapkan orientasi EXIF JPEG, dan menghasilkan JPEG berukuran maksimal 2 MiB. |

Ketiga hasil adalah `POTENTIALLY_ELIGIBLE`, `NEEDS_RECHECK`, dan `NOT_RECOMMENDED`. Hasil AI memberi penilaian awal sebelum verifikasi admin.
