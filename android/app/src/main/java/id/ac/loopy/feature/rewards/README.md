# rewards

S10 memakai layout Figma untuk saldo, katalog 10/20/30 poin, konfirmasi/batal redeem, saldo kurang, serta Reward-ku. State demo debug disimpan di Room melalui `CompletionViewModel` dan `CompletionDemoRepository`. API Go poin/reward sudah tersedia; integrasi Android ke akun server masih ditunda. Demo hanya ditampilkan tanpa sesi login agar saldo ilustrasi tidak tercampur dengan akun aktif.

## Isi file dan perannya

| File atau folder | Kegunaan |
| --- | --- |
| [`RewardsScreen.kt`](RewardsScreen.kt) | UI demo katalog, saldo, konfirmasi redeem, dan riwayat lokal. |
| [`CompletionViewModel.kt`](CompletionViewModel.kt) | StateFlow bersama untuk demo Home, donasi, dan reward. |
| [`CompletionAdminScreen.kt`](CompletionAdminScreen.kt) | Daftar demo admin dan pemetaan ke detail penyelesaian. |
| [`RewardsUiState.kt`](RewardsUiState.kt) | TEMPLATE S10 (F09/F10) — belum ada implementasi bisnis. TODO: buat RewardsUiState: data class immutable berisi nilai form/data, field errors, loading, error, serta event sukses tanpa menyimpan NavController. Kebutuhan: Saldo server; katalog 10/20/30 poin; tombol disable jika tidak cukup; dialog konfirmasi; idempotency; riwayat My Rewards; voucher berlabel demo. Definisi selesai: acceptance criteria pada docs/acceptance-tests.md terkait fitur ini lulus. |
| [`RewardsViewModel.kt`](RewardsViewModel.kt) | TEMPLATE S10 (F09/F10) — belum ada implementasi bisnis. TODO: buat RewardsViewModel: ViewModel dengan private MutableStateFlow, StateFlow immutable, viewModelScope, cancellation, loading/success/error dan cegah request berulang. Kebutuhan: Saldo server; katalog 10/20/30 poin; tombol disable jika tidak cukup; dialog konfirmasi; idempotency; riwayat My Rewards; voucher berlabel demo. Definisi selesai: acceptance criteria pada docs/acceptance-tests.md terkait fitur ini lulus. |

## TODO

- [ ] **S10 / F09/F10**: Saldo server; katalog 10/20/30 poin; tombol disable jika tidak cukup; dialog konfirmasi; idempotency; riwayat My Rewards; voucher berlabel demo.
- [ ] Screen stateless menerima state/callback; UiState immutable; ViewModel via repository, StateFlow dan viewModelScope.
- [ ] Loading/error/empty, cancellation/retry dan anti double-submit, sesuai fitur.
- [ ] Cocokkan node di docs/screen-map.md dan lengkapi AC terkait sebelum menandai selesai.
