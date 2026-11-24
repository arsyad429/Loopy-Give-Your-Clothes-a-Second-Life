# auth

S02 dan S03 memvalidasi form dan menghubungi Go melalui LoginViewModel. Role sesi menentukan rute pengguna.

## Isi file dan perannya

| File atau folder | Kegunaan |
| --- | --- |
| [`LoginScreen.kt`](LoginScreen.kt) | Form email dan password, validasi, pesan feedback, serta callback login atau menuju registrasi. |
| [`LoginUiState.kt`](LoginUiState.kt) | TEMPLATE S02 (F02) — belum ada implementasi bisnis. TODO: buat LoginUiState: data class immutable berisi nilai form/data, field errors, loading, error, serta event sukses tanpa menyimpan NavController. Kebutuhan: Email/password; validasi; pesan gagal; loading; login user → Home, admin → Admin Dashboard; cegah double submit. Definisi selesai: acceptance criteria pada docs/acceptance-tests.md terkait fitur ini lulus. |
| [`LoginViewModel.kt`](LoginViewModel.kt) | Mengirim login/register lewat repository, mengamati sesi, dan mengelola loading serta error. |
| [`RegisterScreen.kt`](RegisterScreen.kt) | Form nama, email dan password beserta validasi dan callback registrasi. |
| [`RegisterUiState.kt`](RegisterUiState.kt) | TEMPLATE S03 (F02) — belum ada implementasi bisnis. TODO: buat RegisterUiState: data class immutable berisi nilai form/data, field errors, loading, error, serta event sukses tanpa menyimpan NavController. Kebutuhan: Nama/email/password; email unik; role user otomatis; saldo awal 0; admin tidak melalui registrasi publik. Definisi selesai: acceptance criteria pada docs/acceptance-tests.md terkait fitur ini lulus. |
| [`RegisterViewModel.kt`](RegisterViewModel.kt) | Template ViewModel terpisah. S03 menggunakan LoginViewModel saat ini. |

## TODO

- [x] Login S02 dengan validasi, feedback dan navigasi berdasarkan role.
- [x] Registrasi publik S03 melalui Go dan masuk langsung sebagai `USER`.
- [ ] Screen stateless menerima state/callback; UiState immutable; ViewModel via repository, StateFlow dan viewModelScope.
- [ ] Loading/error/empty, cancellation/retry dan anti double-submit, sesuai fitur.
- [ ] Cocokkan node di docs/screen-map.md dan lengkapi AC terkait sebelum menandai selesai.
