# Kontrak repository Android

[ClothingCheckRepository.kt](ClothingCheckRepository.kt), [DonationRepository.kt](DonationRepository.kt), dan [RewardRepository.kt](RewardRepository.kt) menghubungkan ViewModel dengan data pemeriksaan, pengajuan, dan penukaran. Kontrak suspend memungkinkan tes memakai implementasi tiruan.

Alur admin dan autentikasi memakai [implementasi repository](../../data/repository/README.md) langsung dari AppContainer.
