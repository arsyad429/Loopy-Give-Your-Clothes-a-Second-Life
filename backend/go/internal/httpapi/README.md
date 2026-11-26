# Routing dan handler HTTP

[server.go](server.go) memasang rute, autentikasi Bearer, pembatasan akses berdasarkan peran, batas request, dan respons error JSON. Rute terlindungi mengambil peran dan identitas dari sesi di database.

| File | Alur |
| --- | --- |
| [auth_handler.go](auth_handler.go) | Login, refresh token, profil, pengaturan akun, dan logout. |
| [email_registration.go](email_registration.go) | Challenge OTP, kirim ulang, validasi, dan pembuatan akun setelah kode benar. |
| [clothing_check_handler.go](clothing_check_handler.go) | Upload multipart, pemeriksaan dengan Groq, pemutaran ulang idempoten, serta akses hasil dan foto privat. |
| [donation_handler.go](donation_handler.go) | Submit pickup, donasi terakhir, dashboard admin, antrean, jadwal dan penolakan. |
| [rewards_handler.go](rewards_handler.go) | Donasi milik donatur, status dijemput/selesai, katalog, saldo dan penukaran. |

[httpapi_test.go](httpapi_test.go) menguji perilaku HTTP dasar. [integration_test.go](integration_test.go) dan [email_registration_test.go](email_registration_test.go) menjalankan alur memakai PostgreSQL uji. [Kontrak API](../../../../docs/api-contract.md)
