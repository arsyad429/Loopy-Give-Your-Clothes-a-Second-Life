# Model data dan transaksi

Tabel **users dan auth_sessions sudah aktif** melalui migrasi `backend/go/schema.sql`; tabel bisnis berikut masih rancangan. UUID dari server; audit UTC ISO-8601; jadwal pickup memakai zona layanan yang disepakati (usulan Asia/Jakarta). Owner/role diambil server dari sesi dan akun database, bukan input klien.

| Tabel | Field/batasan PRD §8 dan kebutuhan teknis |
| --- | --- |
| users | user_id PK, name, normalized email UNIQUE, password_hash atau auth-provider ID, role USER/ADMIN, total_points CHECK >=0 |
| auth_sessions | session_id PK, user_id FK, access_hash/refresh_hash UNIQUE (SHA-256, bukan token mentah), access_expires_at,refresh_expires_at,created_at; rotasi atomik dan hapus saat logout |
| clothing_checks | check_id PK, user_id FK, private photo key, clothing_type, ai_result tiga kategori, ai_description, created_at |
| donation_requests | donation_id PK, user_id/check_id FK, phone_number, pickup_address, city, quantity CHECK>=1, preferred_date/time, scheduled_date/time nullable, notes, status, rejection_reason, created_at/updated_at, version |
| donation_status_history | history_id PK, donation_id FK, previous_status nullable awal, new_status, updated_by FK, updated_at |
| point_transactions | transaction_id PK, user_id FK, donation_id nullable atau redemption_id nullable, transaction_type EARN/REDEEM, signed points, created_at; partial UNIQUE donation_id WHERE type=EARN |
| rewards | reward_id PK, name, description, image, required_points>0, reward_type, is_active |
| redemptions | redemption_id PK, user_id/reward_id FK, points_used, reward_details snapshot, redeemed_at |
| idempotency_records (dukungan) | actor_id+operation+key UNIQUE, request_hash, response reference/status; retry sama mengembalikan hasil sama |

Tambah FK/index owner/status/time. Foto privat via signed URL singkat untuk owner/admin. Cache Android tidak perlu seluruh PII.

## Completed

BEGIN → verifikasi admin → lock donation+wallet dalam urutan konsisten → validasi PICKED_UP → status COMPLETED+audit → insert ledger +10 dengan unique donation ID → saldo +10 → COMMIT. Kegagalan rollback seluruh langkah. Retry completed tanpa poin tambahan. Jumlah pakaian tidak mengalikan poin: satu pengajuan selesai +10.

## Redeem

BEGIN → autentikasi user → cek idempotency key/request hash → lock wallet → reward aktif+cost dari server → cek saldo cukup → redemption+ledger negatif+debit bersyarat saldo>=cost → simpan hasil idempotency → COMMIT. Gagal rollback semuanya. Dua request bersamaan tidak membuat saldo negatif. Harga/poin dari APK tidak dipercaya.

Transisi: PENDING_REVIEW → PICKUP_SCHEDULED → PICKED_UP → COMPLETED, atau PENDING_REVIEW → REJECTED. Schedule wajib tanggal/waktu final, reject wajib alasan, semua transisi diaudit. Preferensi user berbeda dari jadwal admin.

Auth menggunakan pgx, migrasi versi 1 transaksional, dan tes concurrency/rollback PostgreSQL; lihat [auth-go.md](auth-go.md). TODO bisnis: migrasi berikutnya, reward seed, transaksi donasi/poin/redeem dan isolasi owner. Request awal+history juga atomik. Jangan mengubah migrasi versi 1 untuk database yang sudah berjalan; tambahkan versi migrasi berikutnya.
