# Interface Retrofit

| Interface | Rute |
| --- | --- |
| [AuthApi.kt](AuthApi.kt) | Daftar, kirim ulang, verifikasi OTP, login, refresh, profil, dan logout. |
| [ClothingCheckApi.kt](ClothingCheckApi.kt) | Upload multipart, detail pemeriksaan, dan foto privat. |
| [DonationApi.kt](DonationApi.kt) | Buat pengajuan, daftar donatur, dan detail. |
| [AdminApi.kt](AdminApi.kt) | Ringkasan, antrean, detail, keputusan, serta transisi status. |
| [HomeApi.kt](HomeApi.kt) | Donasi terakhir dan saldo untuk Beranda. |
| [RewardApi.kt](RewardApi.kt) | Saldo, katalog, penukaran, dan riwayat. |

Path relatif memakai base URL dari `BuildConfig.API_BASE_URL`. Repository menambahkan header Bearer dan `Idempotency-Key` sesuai kontrak. [Kontrak API](../../../../../../../../../../../docs/api-contract.md)
