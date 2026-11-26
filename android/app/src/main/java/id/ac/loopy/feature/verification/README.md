# S13 Verifikasi email pendaftaran

[EmailVerificationScreen.kt](EmailVerificationScreen.kt) menampilkan alamat tersamar, isian kode enam digit, dan waktu menunggu kirim ulang. [LoginViewModel](../auth/LoginViewModel.kt) menyimpan challenge aktif dan memanggil endpoint kirim ulang atau verifikasi. Kode benar membuat akun dan membuka sesi donatur.

Challenge disimpan selama proses aplikasi hidup. Jika proses ditutup, pendaftaran mungkin perlu dimulai lagi setelah cooldown. [Backend OTP](../../../../../../../../../../backend/go/internal/httpapi/email_registration.go) · [SMTP di VM](../../../../../../../../../../docs/deploy-lxc.md#email-verification-for-registration)
