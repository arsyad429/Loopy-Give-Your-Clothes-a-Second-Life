# auth

Template fitur S02 Login, S03 Register. UI bisnis belum diimplementasikan; kerjakan Screen/UiState/ViewModel terpisah.

## Isi file dan perannya

| File/folder | Peran dan isi yang diharapkan |
| --- | --- |
| `LoginScreen.kt` | Template S02: Email/password; validasi; pesan gagal; loading; login user → Home, admin → Admin Dashboard; cegah double submit. |
| `LoginUiState.kt` | TEMPLATE S02 (F02) — belum ada implementasi bisnis. TODO: buat LoginUiState: data class immutable berisi nilai form/data, field errors, loading, error, serta event sukses tanpa menyimpan NavController. Kebutuhan: Email/password; validasi; pesan gagal; loading; login user → Home, admin → Admin Dashboard; cegah double submit. Definisi selesai: acceptance criteria pada docs/acceptance-tests.md terkait fitur ini lulus. |
| `LoginViewModel.kt` | TEMPLATE S02 (F02) — belum ada implementasi bisnis. TODO: buat LoginViewModel: ViewModel dengan private MutableStateFlow, StateFlow immutable, viewModelScope, cancellation, loading/success/error dan cegah request berulang. Kebutuhan: Email/password; validasi; pesan gagal; loading; login user → Home, admin → Admin Dashboard; cegah double submit. Definisi selesai: acceptance criteria pada docs/acceptance-tests.md terkait fitur ini lulus. |
| `RegisterScreen.kt` | Template S03: Nama/email/password; email unik; role user otomatis; saldo awal 0; admin tidak melalui registrasi publik. |
| `RegisterUiState.kt` | TEMPLATE S03 (F02) — belum ada implementasi bisnis. TODO: buat RegisterUiState: data class immutable berisi nilai form/data, field errors, loading, error, serta event sukses tanpa menyimpan NavController. Kebutuhan: Nama/email/password; email unik; role user otomatis; saldo awal 0; admin tidak melalui registrasi publik. Definisi selesai: acceptance criteria pada docs/acceptance-tests.md terkait fitur ini lulus. |
| `RegisterViewModel.kt` | TEMPLATE S03 (F02) — belum ada implementasi bisnis. TODO: buat RegisterViewModel: ViewModel dengan private MutableStateFlow, StateFlow immutable, viewModelScope, cancellation, loading/success/error dan cegah request berulang. Kebutuhan: Nama/email/password; email unik; role user otomatis; saldo awal 0; admin tidak melalui registrasi publik. Definisi selesai: acceptance criteria pada docs/acceptance-tests.md terkait fitur ini lulus. |

## TODO

- [ ] **S02 / F02**: Email/password; validasi; pesan gagal; loading; login user → Home, admin → Admin Dashboard; cegah double submit.
- [ ] **S03 / F02**: Nama/email/password; email unik; role user otomatis; saldo awal 0; admin tidak melalui registrasi publik.
- [ ] Screen stateless menerima state/callback; UiState immutable; ViewModel via repository, StateFlow dan viewModelScope.
- [ ] Loading/error/empty, cancellation/retry dan anti double-submit, sesuai fitur.
- [ ] Cocokkan node di docs/screen-map.md dan lengkapi AC terkait sebelum menandai selesai.
