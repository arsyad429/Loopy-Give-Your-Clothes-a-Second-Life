# S12 Pengaturan akun

[SettingsScreen.kt](SettingsScreen.kt) mengedit nama dan username, serta menerima kata sandi saat ini dan kata sandi baru bila pengguna ingin menggantinya. [AccountViewModel](../profile/AccountViewModel.kt) menampilkan hasil dan error per field dari `PATCH /v1/auth/me`.

Saat perubahan kata sandi berhasil, backend mencabut sesi akun yang lain. Pengaturan memakai model yang sama pada profil donatur dan admin. `SettingsViewModel.kt` dan `SettingsUiState.kt` mempertahankan kerangka awal. [Kontrak profil](../../../../../../../../../../docs/auth-go.md)
