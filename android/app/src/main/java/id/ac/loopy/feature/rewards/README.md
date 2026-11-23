# rewards

S10 Rewards & My Rewards sudah memakai layout Figma, katalog 10/20/30 poin, dialog konfirmasi, validasi saldo, serta riwayat Room dalam mode demo debug. `CompletionViewModel.kt` membagikan StateFlow untuk Home, Donasiku, Rewards, dan `CompletionAdminScreen.kt`. Aturan transaksi berada pada `CompletionDemoRepository`, bukan composable. [Panduan dan batas integrasi](../../../../../../../../../../docs/completion-rewards.md).

## Isi file dan perannya

| File/folder | Peran dan isi yang diharapkan |
| --- | --- |
| `RewardsScreen.kt` | S10 demo lokal: saldo bersama, katalog, konfirmasi/batal, keadaan saldo kurang, sukses, dan Reward-ku. |
| `CompletionViewModel.kt` | StateFlow/Coroutines untuk demo E–F, loading/busy/error/sukses dan anti double-submit. |
| `CompletionAdminScreen.kt` | Daftar admin demo dan pemetaan detail untuk konfirmasi Dijemput/Selesai. |
| `RewardsUiState.kt` | TEMPLATE S10 (F09/F10) — belum ada implementasi bisnis. TODO: buat RewardsUiState: data class immutable berisi nilai form/data, field errors, loading, error, serta event sukses tanpa menyimpan NavController. Kebutuhan: Saldo server; katalog 10/20/30 poin; tombol disable jika tidak cukup; dialog konfirmasi; idempotency; riwayat My Rewards; voucher berlabel demo. Definisi selesai: acceptance criteria pada docs/acceptance-tests.md terkait fitur ini lulus. |
| `RewardsViewModel.kt` | TEMPLATE S10 (F09/F10) — belum ada implementasi bisnis. TODO: buat RewardsViewModel: ViewModel dengan private MutableStateFlow, StateFlow immutable, viewModelScope, cancellation, loading/success/error dan cegah request berulang. Kebutuhan: Saldo server; katalog 10/20/30 poin; tombol disable jika tidak cukup; dialog konfirmasi; idempotency; riwayat My Rewards; voucher berlabel demo. Definisi selesai: acceptance criteria pada docs/acceptance-tests.md terkait fitur ini lulus. |

## TODO

- [ ] **S10 / F09/F10**: Saldo server; katalog 10/20/30 poin; tombol disable jika tidak cukup; dialog konfirmasi; idempotency; riwayat My Rewards; voucher berlabel demo.
- [ ] Screen stateless menerima state/callback; UiState immutable; ViewModel via repository, StateFlow dan viewModelScope.
- [ ] Loading/error/empty, cancellation/retry dan anti double-submit, sesuai fitur.
- [ ] Cocokkan node di docs/screen-map.md dan lengkapi AC terkait sebelum menandai selesai.
