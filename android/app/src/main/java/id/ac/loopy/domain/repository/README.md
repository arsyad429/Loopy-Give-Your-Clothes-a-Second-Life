# Kontrak repository Android

[ClothingCheckRepository.kt](ClothingCheckRepository.kt), [DonationRepository.kt](DonationRepository.kt), dan [RewardRepository.kt](RewardRepository.kt) menghubungkan ViewModel dengan data pemeriksaan, pengajuan, dan penukaran. Kontrak suspend memungkinkan tes memakai implementasi tiruan.

`AdminRepository.kt` dan `AuthRepository.kt` memuat rancangan interface tambahan. Alur admin dan autentikasi saat ini terhubung lewat [implementasi repository](../../data/repository/README.md).
