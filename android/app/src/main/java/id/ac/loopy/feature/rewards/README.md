# rewards

Template S10 untuk katalog hadiah dan riwayat penukaran. Integrasi menunggu API Go untuk poin dan rewards.

## Isi file dan perannya

| File atau folder | Kegunaan |
| --- | --- |
| [`RewardsScreen.kt`](RewardsScreen.kt) | Template S10: Saldo server; katalog 10/20/30 poin; tombol disable jika tidak cukup; dialog konfirmasi; idempotency; riwayat My Rewards; voucher berlabel demo. |
| [`RewardsUiState.kt`](RewardsUiState.kt) | TEMPLATE S10 (F09/F10) — belum ada implementasi bisnis. TODO: buat RewardsUiState: data class immutable berisi nilai form/data, field errors, loading, error, serta event sukses tanpa menyimpan NavController. Kebutuhan: Saldo server; katalog 10/20/30 poin; tombol disable jika tidak cukup; dialog konfirmasi; idempotency; riwayat My Rewards; voucher berlabel demo. Definisi selesai: acceptance criteria pada docs/acceptance-tests.md terkait fitur ini lulus. |
| [`RewardsViewModel.kt`](RewardsViewModel.kt) | TEMPLATE S10 (F09/F10) — belum ada implementasi bisnis. TODO: buat RewardsViewModel: ViewModel dengan private MutableStateFlow, StateFlow immutable, viewModelScope, cancellation, loading/success/error dan cegah request berulang. Kebutuhan: Saldo server; katalog 10/20/30 poin; tombol disable jika tidak cukup; dialog konfirmasi; idempotency; riwayat My Rewards; voucher berlabel demo. Definisi selesai: acceptance criteria pada docs/acceptance-tests.md terkait fitur ini lulus. |

## TODO

- [ ] **S10 / F09/F10**: Saldo server; katalog 10/20/30 poin; tombol disable jika tidak cukup; dialog konfirmasi; idempotency; riwayat My Rewards; voucher berlabel demo.
- [ ] Screen stateless menerima state/callback; UiState immutable; ViewModel via repository, StateFlow dan viewModelScope.
- [ ] Loading/error/empty, cancellation/retry dan anti double-submit, sesuai fitur.
- [ ] Cocokkan node di docs/screen-map.md dan lengkapi AC terkait sebelum menandai selesai.
