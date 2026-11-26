# Password dan pengiriman OTP

[password.go](password.go) menyimpan turunan Argon2id dengan salt acak dan membandingkannya secara konstan saat login. [verification_mail.go](verification_mail.go) mengirim kode melalui STARTTLS atau TLS implisit sesuai `AUTH_SMTP_*`. Challenge dan digest HMAC kode ditangani oleh [pendaftaran HTTP](../httpapi/email_registration.go).

[password_test.go](password_test.go) menguji verifikasi hash. [verification_mail_test.go](verification_mail_test.go) memakai relay SMTP TLS uji. Konfigurasi provider ada di [panduan deploy](../../../../docs/deploy-lxc.md#email-verification-for-registration).
