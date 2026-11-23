# Kontrak API usulan

**Auth sudah diimplementasikan di Go** (`backend/go/`, backend Docker aktif): register, login, refresh, logout, me. Kontrak JSON final, masa berlaku token, validasi dan error ada di [auth-go.md](auth-go.md). `GET /health` menghasilkan `{"status":"ok","mode":"go-auth"}`; `GET /ready` memeriksa koneksi PostgreSQL. Endpoint bisnis selain auth masih target pengembangan dan menghasilkan 404. Scaffold Ktor tetap tersedia terpisah.

| Metode/path | Akses | Request → respons target | Fitur |
| --- | --- | --- | --- |
| POST /v1/auth/register | Public | name,email,password → user USER/0 poin+session; email unik, role tidak diterima | F02 |
| POST /v1/auth/login | Public | email,password → access/refresh token, expiresAt,user | F02 |
| POST /v1/auth/refresh | Refresh token | refreshToken → token pair baru, expiresAt,refreshExpiresAt,user; pair lama tidak berlaku | F02 |
| POST /v1/auth/logout | Login | revoke session/refresh token →204 | F02 |
| GET /v1/auth/me | Login | userId,name,email,role,totalPoints | F02/F03 |
| POST /v1/checks | User | multipart image → checkId,photo,type,aiResult,aiDescription,createdAt | F04/F05 |
| GET /v1/checks/{id} | Owner/admin | check tersimpan | F05 |
| POST /v1/donations | User | Idempotency-Key; checkId,cleanAndSafeConfirmed,phoneNumber,pickupAddress,city,quantity,preferredDate,preferredTime,notes → donationId/PENDING_REVIEW | F06 |
| GET /v1/donations | User | status/query/cursor → items,nextCursor milik user token | F03/F08 |
| GET /v1/donations/{id} | Owner | detail+history+jadwal final+rejectionReason | F08 |
| GET /v1/admin/summary | Admin | total/pending/scheduled/completed/rejected counts | F07 |
| GET /v1/admin/donations | Admin | status/query/cursor → requests | F07 |
| GET /v1/admin/donations/{id} | Admin | detail+AI+alamat | F07 |
| POST /v1/admin/donations/{id}/schedule | Admin | scheduledDate,scheduledTime,timezone,expectedVersion →scheduled+history | F07 |
| POST /v1/admin/donations/{id}/reject | Admin | rejectionReason,expectedVersion →rejected+history | F07 |
| POST /v1/admin/donations/{id}/status | Admin | targetStatus PICKED_UP/COMPLETED,expectedVersion →status+history; completed+10 atomik | F07/F09 |
| GET /v1/points/balance | User | balance,updatedAt | F09 |
| GET /v1/rewards | User | katalog rewardId,name,description,image,requiredPoints,type,isActive | F10 |
| POST /v1/redemptions | User | Idempotency-Key+rewardId → redemption,remainingBalance | F10 |
| GET /v1/redemptions | User | history milik user, reward snapshot/details/pointsUsed/redeemedAt | F10 |

Nama akun/foto/jenis pakaian diambil server dari akun/check, bukan dipercaya dari klien. Konfirmasi cleanAndSafeConfirmed wajib. Check gagal/ineligible atau bukan milik user ditolak. Semua endpoint admin memverifikasi role server; ownership di query/service. User tidak mengubah status/poin.

Enum status: PENDING_REVIEW/PICKUP_SCHEDULED/PICKED_UP/COMPLETED/REJECTED. AI: POTENTIALLY_ELIGIBLE/NEEDS_RECHECK/NOT_RECOMMENDED. Error usulan `{"error":{"code":"INSUFFICIENT_POINTS","message":"Poin belum cukup","fieldErrors":{}}}`. Status: 400/422 input,401 sesi,403 akses,404 tidak ditemukan,409 concurrency/idempotency conflict,413 file besar,415 MIME,429 rate limit,502/503 AI. Jangan membocorkan stack trace/PII.

Idempotency key per aksi, dipertahankan saat retry; server scope actor+operation dan bandingkan payload hash. Payload berbeda key sama →409. UI disabled saat loading membantu UX, bukan pengganti transaksi server.

TODO: sepakati JSON bisnis, limit JPEG/PNG, timeout/retry dan zona/waktu layanan. Buat Retrofit DTO untuk auth yang sudah aktif, lalu route Go untuk fitur bisnis. AI dipanggil server saja dan hasilnya estimasi awal. OTP/settings/profile tambahan Figma perlu kontrak baru setelah scope disepakati.
