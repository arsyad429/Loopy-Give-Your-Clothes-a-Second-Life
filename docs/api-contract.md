# Kontrak API Loopy

Compose utama menjalankan Go untuk akun dan admin (port host 8080 secara default). Compose Vision menjalankan Ktor untuk pemeriksaan (port host 8081 secara default). Masing-masing layanan memakai PostgreSQL dan bearer token tersendiri. [Panduan backend](backend.md) · [Kontrak autentikasi Go](auth-go.md) · [Kontrak peninjauan donasi Go](donation-review-go.md)

**Endpoint yang tersedia** mencakup `GET /health`, `GET /ready`, lima endpoint auth dan lima endpoint admin di Go, serta tiga endpoint pemeriksaan di Ktor. Tabel ini juga mencakup rencana endpoint untuk fitur lanjutan.

| Metode/path | Akses | Request → respons target | Fitur |
| --- | --- | --- | --- |
| POST /v1/auth/register | Public | name,email,password → user USER/0 poin+session; email unik, role tidak diterima | F02 |
| POST /v1/auth/login | Public | email,password → access/refresh token, expiresAt,user | F02 |
| POST /v1/auth/refresh | Refresh token | refreshToken → token pair baru, expiresAt,refreshExpiresAt,user; pair lama tidak berlaku | F02 |
| POST /v1/auth/logout | Login | revoke session/refresh token →204 | F02 |
| GET /v1/auth/me | Login | userId,name,email,role,totalPoints | F02/F03 |
| POST /v1/checks | USER | Bearer + Idempotency-Key; multipart `image`, `clothingType` → objek pemeriksaan (201 baru, 200 replay) | F04/F05 |
| GET /v1/checks/{id} | Owner/admin | Bearer → objek pemeriksaan tersimpan | F05 |
| GET /v1/checks/{id}/photo | Owner/admin | Bearer → JPEG privat | F05 |
| POST /v1/donations | User | Idempotency-Key; checkId,cleanAndSafeConfirmed,phoneNumber,pickupAddress,city,quantity,preferredDate,preferredTime,notes → donationId/PENDING_REVIEW | F06 |
| GET /v1/donations | User | status/query/cursor → items,nextCursor milik user token | F03/F08 |
| GET /v1/donations/{id} | Owner | detail+history+jadwal final+rejectionReason | F08 |
| GET /v1/admin/summary | Admin | total, pending, scheduled, pickedUp, completed, rejected | F07 |
| GET /v1/admin/donations | Admin | status/query/limit/cursor → items,nextCursor | F07 |
| GET /v1/admin/donations/{id} | Admin | detail, hasil AI, referensi foto privat, alamat, dan riwayat | F07 |
| POST /v1/admin/donations/{id}/schedule | Admin | scheduledDate,scheduledTime,timezone=Asia/Jakarta,expectedVersion → detail terjadwal dan riwayat | F07 |
| POST /v1/admin/donations/{id}/reject | Admin | rejectionReason,expectedVersion → detail ditolak dan riwayat | F07 |
| POST /v1/admin/donations/{id}/status | Admin | targetStatus PICKED_UP/COMPLETED,expectedVersion →status+history; completed+10 atomik | F07/F09 |
| GET /v1/points/balance | User | balance,updatedAt | F09 |
| GET /v1/rewards | User | katalog rewardId,name,description,image,requiredPoints,type,isActive | F10 |
| POST /v1/redemptions | User | Idempotency-Key+rewardId → redemption,remainingBalance | F10 |
| GET /v1/redemptions | User | history milik user, reward snapshot/details/pointsUsed/redeemedAt | F10 |

Nama akun/foto/jenis pakaian diambil server dari akun/check, bukan dipercaya dari klien. Konfirmasi cleanAndSafeConfirmed wajib. Check gagal/ineligible atau bukan milik user ditolak. Semua endpoint admin memverifikasi role server; ownership di query/service. User tidak mengubah status/poin.

Enum status: PENDING_REVIEW/PICKUP_SCHEDULED/PICKED_UP/COMPLETED/REJECTED. AI: POTENTIALLY_ELIGIBLE/NEEDS_RECHECK/NOT_RECOMMENDED. Error usulan `{"error":{"code":"INSUFFICIENT_POINTS","message":"Poin belum cukup","fieldErrors":{}}}`. Status: 400/422 input,401 sesi,403 akses,404 tidak ditemukan,409 concurrency/idempotency conflict,413 file besar,415 MIME,429 rate limit,502/503 AI. Jangan membocorkan stack trace/PII.

Rencana idempotency untuk pengajuan dan redemption memakai actor, operation, key, dan hash payload. Pada peninjauan admin, `expectedVersion` mencegah keputusan yang sama diterapkan dua kali. Klien membaca detail terbaru setelah `409`.

Pekerjaan berikutnya mencakup DTO Retrofit untuk auth dan admin, endpoint pengajuan pengguna, integrasi Android dengan API Ktor dan foto privat, serta fitur lanjutan di tabel. AI dipanggil server dan hasilnya menjadi penilaian awal. OTP, settings, dan profil tambahan memerlukan kontrak tersendiri.

## Kontrak pakaian Ktor

- POST memerlukan role `USER`, bearer token Ktor, serta `Idempotency-Key`. Kirim tepat satu file `image` JPEG/PNG maksimal 10 MiB dan satu `clothingType`, misalnya `TSHIRT`.
- Objek hasil berisi `checkId`, `userId`, `clothingPhoto`, `clothingType`, `aiResult`, `aiDescription`, `actionRecommendation`, dan `createdAt`. `clothingPhoto` menunjuk ke route foto privat dan memakai bearer token Ktor yang sama.
- Key dan payload yang sama mendapat hasil lama setelah tersimpan. Key dengan isi berbeda atau request yang masih diproses menghasilkan `409`.
- Pemilik dan admin dapat membaca detail dan foto. Akun lain mendapat `404`. Request tanpa sesi Ktor valid mendapat `401`.
- Upload tidak valid menerima `400`, `413`, `415`, atau `422`. Gangguan Groq menghasilkan `502` atau `503` dan tidak menyimpan hasil pemeriksaan.
