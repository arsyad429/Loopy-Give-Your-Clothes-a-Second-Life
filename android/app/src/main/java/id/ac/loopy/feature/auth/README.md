# Masuk dan daftar

[LoginScreen.kt](LoginScreen.kt) dan [RegisterScreen.kt](RegisterScreen.kt) mengumpulkan input. [LoginViewModel.kt](LoginViewModel.kt) menjalankan masuk, pendaftaran, verifikasi kode, kirim ulang, dan logout. Password minimal 8 karakter, dengan validasi ulang oleh Go.

Pendaftaran membuat challenge dan membuka [S13 Verifikasi email](../verification/README.md). Akun dan sesi tersedia setelah kode benar. Login setelah itu memakai email dan kata sandi. [LoopyApp.kt](../../ui/LoopyApp.kt) mengarahkan donatur ke S04 dan admin ke A01 dari satu layar masuk. Admin disiapkan melalui CLI. [Panduan admin dan OTP](../../../../../../../../../../docs/auth-go.md)

`RegisterViewModel.kt`, `RegisterUiState.kt` dan `LoginUiState.kt` menyimpan kerangka tambahan. Jalur aktif memakai state dan feedback dalam `LoginViewModel.kt`.
