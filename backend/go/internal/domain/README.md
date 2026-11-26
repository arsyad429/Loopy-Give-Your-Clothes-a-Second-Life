# Model dan aturan backend

Paket domain mendefinisikan data bisnis dan kontrak yang dipakai HTTP serta PostgreSQL.

- [auth.go](auth.go) memvalidasi email, kata sandi, profil, dan data sesi. [email_registration.go](email_registration.go) mendefinisikan pendaftaran sementara dan kontrak verifikasi.
- [clothing_check.go](clothing_check.go) memodelkan hasil pemeriksaan, foto, dan analyzer. [donation.go](donation.go) mengatur status, validasi pickup, serta filter dan cursor daftar.
- [rewards.go](rewards.go) mendefinisikan katalog, saldo, transaksi poin, serta penukaran.
- [repositories.go](repositories.go) berisi interface backend. [errors.go](errors.go) mendefinisikan error yang diterjemahkan handler ke HTTP. [domain_test.go](domain_test.go) menguji aturan murni.
