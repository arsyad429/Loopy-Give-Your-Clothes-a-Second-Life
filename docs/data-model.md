# Model data dan transaksi

Migrasi [versi 1](../backend/go/schema.sql) membuat tabel akun dan sesi. Migrasi [versi 2](../backend/go/schema_pickup.sql) menambahkan pemeriksaan pakaian, pengajuan donasi, dan riwayat status. API mengembalikan waktu audit dalam UTC ISO-8601. Jadwal final memakai `Asia/Jakarta`. Identitas dan role berasal dari akun dalam sesi.

| Tabel | Field/batasan PRD §8 dan kebutuhan teknis |
| --- | --- |
| users | user_id PK, name, normalized email UNIQUE, password_hash atau auth-provider ID, role USER/ADMIN, total_points CHECK >=0 |
| auth_sessions | session_id PK, user_id FK, access_hash/refresh_hash UNIQUE (SHA-256, bukan token mentah), access_expires_at,refresh_expires_at,created_at; rotasi atomik dan hapus saat logout |
| clothing_checks | check_id PK, user_id FK, private photo key, clothing_type, ai_result tiga kategori, ai_description, created_at |
| donation_requests | donation_id PK, user_id FK, pasangan check_id/user_id FK ke pemeriksaan milik donatur, phone_number, pickup_address, city, quantity CHECK>=1, preferred_date/time, scheduled_at TIMESTAMPTZ, notes, status, rejection_reason, created_at/updated_at, version |
| donation_status_history | PK (donation_id, version), donation_id FK, previous_status null pada versi awal, new_status, updated_by FK, updated_at |
| point_transactions | transaction_id PK, user_id FK, donation_id nullable atau redemption_id nullable, transaction_type EARN/REDEEM, signed points, created_at; partial UNIQUE donation_id WHERE type=EARN |
| rewards | reward_id PK, name, description, image, required_points>0, reward_type, is_active |
| redemptions | redemption_id PK, user_id/reward_id FK, points_used, reward_details snapshot, redeemed_at |
| idempotency_records (dukungan) | actor_id+operation+key UNIQUE, request_hash, response reference/status; retry sama mengembalikan hasil sama |

Versi 2 mengindeks antrean menurut waktu dan status, serta pengajuan menurut donatur. Jadwal wajib untuk status terjadwal, dijemput, dan selesai. Alasan wajib untuk status ditolak. API detail admin mengembalikan kunci penyimpanan foto. Pengambilan foto privat untuk owner dan admin masih direncanakan.

## Completed yang direncanakan

BEGIN → verifikasi admin → lock donation+wallet dalam urutan konsisten → validasi PICKED_UP → status COMPLETED+audit → insert ledger +10 dengan unique donation ID → saldo +10 → COMMIT. Kegagalan rollback seluruh langkah. Retry completed tanpa poin tambahan. Jumlah pakaian tidak mengalikan poin: satu pengajuan selesai +10.

## Redeem yang direncanakan

BEGIN → autentikasi user → cek idempotency key/request hash → lock wallet → reward aktif+cost dari server → cek saldo cukup → redemption+ledger negatif+debit bersyarat saldo>=cost → simpan hasil idempotency → COMMIT. Gagal rollback semuanya. Dua request bersamaan tidak membuat saldo negatif. Harga/poin dari APK tidak dipercaya.

API aktif memproses `PENDING_REVIEW → PICKUP_SCHEDULED` dan `PENDING_REVIEW → REJECTED`. Transaksi mengunci pengajuan, memeriksa versinya, lalu mengubah status dan menulis riwayat pada versi yang sama. Lihat [alur verifikasi dan penjadwalan](donation-review-go.md). Skema menyediakan status `PICKED_UP` dan `COMPLETED` untuk penanganan pickup lanjutan.

Fitur pengajuan pengguna perlu memasukkan pengajuan awal dan riwayat versi 1 dalam satu transaksi. Keputusan berikutnya memakai migrasi tambahan untuk database yang sudah berjalan. Lihat juga [model akun dan sesi](auth-go.md).

## Database pemeriksaan Ktor

Compose Vision memakai Flyway dan database terpisah dari Go. [Migrasi Ktor](../backend/src/main/resources/db/migration/V001__initial_schema.sql)

| Tabel | Kegunaan |
| --- | --- |
| `users` | Identitas, role, dan saldo akun untuk pemeriksaan. |
| `user_sessions` | Hash bearer token, pemilik, dan tenggat sesi. |
| `clothing_checks` | Jenis pakaian, kategori, penjelasan, rekomendasi, dan waktu hasil. |
| `clothing_check_photos` | Byte JPEG privat berdasarkan `check_id`. |
| `clothing_check_requests` | Reservasi dan replay berdasarkan pemilik, key, hash request, dan ID hasil. |

Ktor menulis hasil, foto, dan penyelesaian key idempotensi dalam satu transaksi PostgreSQL.
