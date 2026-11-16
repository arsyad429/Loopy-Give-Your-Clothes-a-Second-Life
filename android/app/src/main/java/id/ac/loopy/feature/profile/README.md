# profile

Template fitur S11 Profil User, A04 Profil Admin. UI bisnis belum diimplementasikan; kerjakan Screen/UiState/ViewModel terpisah.

## Isi file dan perannya

| File/folder | Peran dan isi yang diharapkan |
| --- | --- |
| `AdminProfileScreen.kt` | Template A04: Informasi admin/logout; jangan menyediakan eskalasi role dari UI. |
| `AdminProfileUiState.kt` | TEMPLATE A04 (Design extension) — belum ada implementasi bisnis. TODO: buat AdminProfileUiState: data class immutable berisi nilai form/data, field errors, loading, error, serta event sukses tanpa menyimpan NavController. Kebutuhan: Informasi admin/logout; jangan menyediakan eskalasi role dari UI. Definisi selesai: acceptance criteria pada docs/acceptance-tests.md terkait fitur ini lulus. |
| `AdminProfileViewModel.kt` | TEMPLATE A04 (Design extension) — belum ada implementasi bisnis. TODO: buat AdminProfileViewModel: ViewModel dengan private MutableStateFlow, StateFlow immutable, viewModelScope, cancellation, loading/success/error dan cegah request berulang. Kebutuhan: Informasi admin/logout; jangan menyediakan eskalasi role dari UI. Definisi selesai: acceptance criteria pada docs/acceptance-tests.md terkait fitur ini lulus. |
| `UserProfileScreen.kt` | Template S11: Informasi akun/logout. Penyuntingan profil lengkap perlu kesepakatan tim. |
| `UserProfileUiState.kt` | TEMPLATE S11 (F03 extension) — belum ada implementasi bisnis. TODO: buat UserProfileUiState: data class immutable berisi nilai form/data, field errors, loading, error, serta event sukses tanpa menyimpan NavController. Kebutuhan: Informasi akun/logout. Penyuntingan profil lengkap perlu kesepakatan tim. Definisi selesai: acceptance criteria pada docs/acceptance-tests.md terkait fitur ini lulus. |
| `UserProfileViewModel.kt` | TEMPLATE S11 (F03 extension) — belum ada implementasi bisnis. TODO: buat UserProfileViewModel: ViewModel dengan private MutableStateFlow, StateFlow immutable, viewModelScope, cancellation, loading/success/error dan cegah request berulang. Kebutuhan: Informasi akun/logout. Penyuntingan profil lengkap perlu kesepakatan tim. Definisi selesai: acceptance criteria pada docs/acceptance-tests.md terkait fitur ini lulus. |

## TODO

- [ ] **S11 / F03 extension**: Informasi akun/logout. Penyuntingan profil lengkap perlu kesepakatan tim.
- [ ] **A04 / Design extension**: Informasi admin/logout; jangan menyediakan eskalasi role dari UI.
- [ ] Screen stateless menerima state/callback; UiState immutable; ViewModel via repository, StateFlow dan viewModelScope.
- [ ] Loading/error/empty, cancellation/retry dan anti double-submit, sesuai fitur.
- [ ] Cocokkan node di docs/screen-map.md dan lengkapi AC terkait sebelum menandai selesai.
