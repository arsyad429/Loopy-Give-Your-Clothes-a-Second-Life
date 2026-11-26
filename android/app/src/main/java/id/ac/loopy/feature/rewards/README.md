# S10 Rewards dan riwayat

[RewardsScreen.kt](RewardsScreen.kt) menampilkan saldo, katalog, konfirmasi penukaran, dan riwayat. [RewardsViewModel.kt](RewardsViewModel.kt) memuat data untuk akun aktif dan menyimpan penukaran tertunda dengan `SavedStateHandle` agar bisa memakai key yang sama saat mencoba ulang. [RewardsUiState.kt](RewardsUiState.kt) menyatakan loading, hasil, dan error.

Backend memvalidasi saldo dan mencatat penukaran serta debit secara atomik. Setelah penukaran berhasil, UI memperbarui saldo dan riwayat. [Repo rewards](../../data/repository/RewardRepositoryImpl.kt) · [Aturan poin](../../../../../../../../../../docs/completion-rewards.md)
