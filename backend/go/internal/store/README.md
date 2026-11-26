# Penyimpanan PostgreSQL

[postgres.go](postgres.go) menyiapkan `PGStore` serta menjalankan migrasi tertanam dengan advisory lock. Handler memakai kontrak domain agar aturan akses dan error tetap konsisten.

- [auth_store.go](auth_store.go) mengelola akun, rotasi sesi, logout, dan pembaruan profil. [email_registration_store.go](email_registration_store.go) menuntaskan challenge dan membuat akun dalam transaksi.
- [clothing_check_store.go](clothing_check_store.go) menyimpan reservasi idempoten, hasil AI, dan JPEG privat. [donation_store.go](donation_store.go) memvalidasi kepemilikan, penjadwalan, riwayat, dan pagination cursor.
- [rewards_store.go](rewards_store.go) memakai transaksi untuk penyelesaian donasi, ledger poin, dan redeem. Versi pengajuan mencegah dua perubahan status saling menimpa.

[store_integration_test.go](store_integration_test.go) memerlukan `TEST_DATABASE_URL` untuk menguji transaksi dan migrasi.
