# Implementasi repository Android

| Implementasi | Tanggung jawab |
| --- | --- |
| [AuthRepositoryImpl.kt](AuthRepositoryImpl.kt) | Menyimpan sesi di memori, melindungi rotasi token dengan mutex, dan mencoba ulang request setelah refresh. |
| [ClothingCheckRepositoryImpl.kt](ClothingCheckRepositoryImpl.kt) | Membaca URI JPEG/PNG, membuat multipart, menjaga key percobaan ulang, dan mengambil hasil atau foto. |
| [DonationRepositoryImpl.kt](DonationRepositoryImpl.kt) | Submit pickup dengan key idempoten dalam memori serta baca daftar dan detail. Daftar donatur saat ini hanya membaca halaman pertama. |
| [AdminRepositoryImpl.kt](AdminRepositoryImpl.kt) | Ringkasan, antrean dengan cursor, detail dan keputusan memakai versi pengajuan. |
| [HomeRepository.kt](HomeRepository.kt) | Membaca saldo dan donasi terakhir secara terpisah. |
| [RewardRepositoryImpl.kt](RewardRepositoryImpl.kt) | Katalog, saldo, redeem idempoten dan riwayat. |

[Perakitan repository](../../di/AppContainer.kt) · [Tes repository](../../../../../../../test/java/id/ac/loopy/data/repository/README.md)
