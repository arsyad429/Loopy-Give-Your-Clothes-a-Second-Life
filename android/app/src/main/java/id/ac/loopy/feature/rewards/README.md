# Rewards

S10 membaca saldo, katalog dan riwayat akun aktif melalui RewardApi. Penukaran memakai POST /v1/redemptions, Bearer login dan Idempotency-Key. Saldo serta biaya tetap ditentukan server.

| File | Peran |
| --- | --- |
| [RewardsScreen.kt](RewardsScreen.kt) | UI katalog, konfirmasi, riwayat dan pemeriksaan penukaran tertunda. |
| [RewardsViewModel.kt](RewardsViewModel.kt) | StateFlow, refresh, isolasi sesi, retry idempoten dan hasil transaksi. |
| [RewardsUiState.kt](RewardsUiState.kt) | Saldo nullable serta state loading, error, pending dan sukses. |

Demo Room lama sudah dihapus. [Panduan menjalankan dan batasan](../../../../../../../../../../docs/completion-rewards.md).
