# home

S04 menyapa pengguna yang sudah masuk dan menyediakan tombol Periksa pakaian untuk membuka S05.

## Isi file dan perannya

| File atau folder | Kegunaan |
| --- | --- |
| [`HomeScreen.kt`](HomeScreen.kt) | Menyapa pengguna yang sudah masuk dan membuka S05 melalui callback. |
| [`HomeUiState.kt`](HomeUiState.kt) | TEMPLATE S04 (F03/F09) — belum ada implementasi bisnis. TODO: buat HomeUiState: data class immutable berisi nilai form/data, field errors, loading, error, serta event sukses tanpa menyimpan NavController. Kebutuhan: Greeting; Check Your Clothes; donasi terakhir; saldo server; My Donations; Rewards; akun dan logout. Definisi selesai: acceptance criteria pada docs/acceptance-tests.md terkait fitur ini lulus. |
| [`HomeViewModel.kt`](HomeViewModel.kt) | TEMPLATE S04 (F03/F09) — belum ada implementasi bisnis. TODO: buat HomeViewModel: ViewModel dengan private MutableStateFlow, StateFlow immutable, viewModelScope, cancellation, loading/success/error dan cegah request berulang. Kebutuhan: Greeting; Check Your Clothes; donasi terakhir; saldo server; My Donations; Rewards; akun dan logout. Definisi selesai: acceptance criteria pada docs/acceptance-tests.md terkait fitur ini lulus. |

## TODO

- [ ] **S04 / F03/F09**: Donasi terakhir, saldo server, My Donations, Rewards, akun dan logout.
- [ ] Screen stateless menerima state/callback; UiState immutable; ViewModel via repository, StateFlow dan viewModelScope.
- [ ] Loading/error/empty, cancellation/retry dan anti double-submit, sesuai fitur.
- [ ] Cocokkan node di docs/screen-map.md dan lengkapi AC terkait sebelum menandai selesai.
