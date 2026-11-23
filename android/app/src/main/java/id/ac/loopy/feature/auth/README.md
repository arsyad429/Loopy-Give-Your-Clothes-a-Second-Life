# auth

UI login S02 dan registrasi S03 menyediakan form dan validasi input lokal. Repository Retrofit dan penyimpanan sesi Android masih perlu disambungkan ke API Go.

## Isi file dan perannya

| File atau folder | Kegunaan |
| --- | --- |
| [`LoginScreen.kt`](LoginScreen.kt) | Form email dan password, validasi, pesan feedback, serta callback login atau menuju registrasi. |
| [`LoginUiState.kt`](LoginUiState.kt) | TEMPLATE S02 (F02) — belum ada implementasi bisnis. TODO: buat LoginUiState: data class immutable berisi nilai form/data, field errors, loading, error, serta event sukses tanpa menyimpan NavController. Kebutuhan: Email/password; validasi; pesan gagal; loading; login user → Home, admin → Admin Dashboard; cegah double submit. Definisi selesai: acceptance criteria pada docs/acceptance-tests.md terkait fitur ini lulus. |
| [`LoginViewModel.kt`](LoginViewModel.kt) | TEMPLATE S02 (F02) — belum ada implementasi bisnis. TODO: buat LoginViewModel: ViewModel dengan private MutableStateFlow, StateFlow immutable, viewModelScope, cancellation, loading/success/error dan cegah request berulang. Kebutuhan: Email/password; validasi; pesan gagal; loading; login user → Home, admin → Admin Dashboard; cegah double submit. Definisi selesai: acceptance criteria pada docs/acceptance-tests.md terkait fitur ini lulus. |
| [`RegisterScreen.kt`](RegisterScreen.kt) | Form nama, email dan password beserta validasi dan callback registrasi. |
| [`RegisterUiState.kt`](RegisterUiState.kt) | TEMPLATE S03 (F02) — belum ada implementasi bisnis. TODO: buat RegisterUiState: data class immutable berisi nilai form/data, field errors, loading, error, serta event sukses tanpa menyimpan NavController. Kebutuhan: Nama/email/password; email unik; role user otomatis; saldo awal 0; admin tidak melalui registrasi publik. Definisi selesai: acceptance criteria pada docs/acceptance-tests.md terkait fitur ini lulus. |
| [`RegisterViewModel.kt`](RegisterViewModel.kt) | TEMPLATE S03 (F02) — belum ada implementasi bisnis. TODO: buat RegisterViewModel: ViewModel dengan private MutableStateFlow, StateFlow immutable, viewModelScope, cancellation, loading/success/error dan cegah request berulang. Kebutuhan: Nama/email/password; email unik; role user otomatis; saldo awal 0; admin tidak melalui registrasi publik. Definisi selesai: acceptance criteria pada docs/acceptance-tests.md terkait fitur ini lulus. |

## TODO

- [ ] **S02 / F02**: Email/password; validasi; pesan gagal; loading; login user → Home, admin → Admin Dashboard; cegah double submit.
- [ ] **S03 / F02**: Nama/email/password; email unik; role user otomatis; saldo awal 0; admin tidak melalui registrasi publik.
- [ ] Screen stateless menerima state/callback; UiState immutable; ViewModel via repository, StateFlow dan viewModelScope.
- [ ] Loading/error/empty, cancellation/retry dan anti double-submit, sesuai fitur.
- [ ] Cocokkan node di docs/screen-map.md dan lengkapi AC terkait sebelum menandai selesai.
