# Store Backend Loopy

Implementasi PostgreSQL (`PGStore`) menggunakan `pgxpool` dengan runner migrasi berurutan dan query atomik per domain:

- `postgres.go`: Koneksi pool, health ping, dan runner migrasi ber-lock.
- `auth_store.go`: Manajemen akun dan rotasi sesi token.
- `clothing_check_store.go`: Reservasi idempotensi pemeriksaan dan penyimpanan foto JPEG privat.
- `donation_store.go`: Pengajuan donatur, antrean admin, dan keputusan ber-version lock.
- `rewards_store.go`: Transisi selesai (+10 poin atomik), katalog, dan penukaran reward idempoten.
