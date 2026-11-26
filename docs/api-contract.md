# Kontrak API Loopy

Android memakai satu base URL untuk akun, donasi, rewards, dan foto privat. Gunakan `Authorization: Bearer <accessToken>` untuk rute terlindungi dan `Content-Type: application/json` untuk body JSON. Upload foto memakai multipart. Timestamp audit memakai UTC RFC 3339. Jadwal pickup juga menyediakan tanggal dan jam WIB. [Backend](backend.md) · [Definisi rute](../backend/go/internal/httpapi/README.md)

## Ketersediaan dan akun

| Metode dan rute | Akses | Request dan respons sukses |
| --- | --- | --- |
| `GET /health` | Publik | `200` dan status proses HTTP. |
| `GET /ready` | Publik | `200 {"status":"ready"}` ketika PostgreSQL terjangkau. |
| `POST /v1/auth/register` | Publik | `name`, `email`, `password`, `username` opsional. `202` dengan challenge OTP. Mode pengujian tanpa OTP memberi `201` dan sesi. |
| `POST /v1/auth/register/resend` | Publik | `challengeId`. `202` dengan data challenge dan kode baru dikirim ke inbox. |
| `POST /v1/auth/register/verify` | Publik | `challengeId` dan `code` enam digit sebagai string. `201` dengan akun `USER` terverifikasi dan sesi. |
| `POST /v1/auth/login` | Publik | `email`, `password`. `200` dengan sesi untuk donatur atau admin. |
| `POST /v1/auth/refresh` | Refresh token | `refreshToken`. `200` dengan pasangan token baru. |
| `GET /v1/auth/me` | Akun aktif | `200` dengan objek profil. |
| `PATCH /v1/auth/me` | Akun aktif | `name`, `username`, serta pasangan `currentPassword` dan `newPassword` bila mengubah password. `200` dengan profil terbaru. |
| `POST /v1/auth/logout` | Akun aktif | Tanpa body. `204` dan sesi aktif dicabut. |

Respons challenge memiliki `challengeId`, `maskedEmail`, `expiresInSeconds`, dan `resendAfterSeconds`. Respons sesi memiliki `accessToken`, `refreshToken`, `tokenType`, `expiresAt`, `refreshExpiresAt`, dan objek `user`. Profil membawa `userId`, `name`, `email`, `username` jika tersedia, `emailVerified`, `role`, dan `totalPoints`. [Contoh JSON akun dan sesi](auth-go.md#2-exact-http-contract)

## Pemeriksaan, pickup, dan daftar donatur

| Metode dan rute | Akses | Request dan respons sukses |
| --- | --- | --- |
| `POST /v1/checks` | `USER` | `Idempotency-Key`, multipart `image` dan `clothingType`. `201` hasil baru atau `200` replay. |
| `GET /v1/checks/{id}` | Pemilik atau admin | `200` metadata pemeriksaan. |
| `GET /v1/checks/{id}/photo` | Pemilik atau admin | `200` JPEG privat. |
| `POST /v1/donations` | `USER` | `Idempotency-Key` dan field pickup. `201` detail baru atau `200` replay. |
| `GET /v1/donations` | `USER` | Filter `status`, `query`, `limit`, `cursor`. `200` dengan `items` dan `nextCursor` untuk akun aktif. |
| `GET /v1/donations/latest` | `USER` | `200` dengan `item` ringkasan terbaru atau `{"item":null}`. |
| `GET /v1/donations/{id}` | Donatur pemilik | `200` detail, jadwal jika terisi, dan riwayat status. |

### Field pengajuan pickup

| Field | Validasi Go |
| --- | --- |
| `checkId` | UUID hasil `POTENTIALLY_ELIGIBLE` milik donatur. |
| `cleanAndSafeConfirmed` | Wajib `true`. |
| `phoneNumber` | 8–20 karakter dan setidaknya 8 digit. Spasi dan tanda telepon yang diizinkan boleh digunakan. |
| `pickupAddress` dan `city` | Alamat 5–300 karakter, kota 2–100 karakter. |
| `quantity` | Bilangan bulat 1–100. |
| `preferredDate` dan `preferredTime` | Tanggal `YYYY-MM-DD` hari ini atau setelahnya di WIB. Preferensi waktu 1–50 karakter. |
| `notes` | Boleh kosong, maksimum 300 karakter. |

Contoh isi body berikut memakai UUID ilustrasi. Ganti `checkId` dengan hasil akun uji dan pilih tanggal yang masih berlaku saat menjalankan.

```json
{
  "checkId": "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
  "cleanAndSafeConfirmed": true,
  "phoneNumber": "081200000000",
  "pickupAddress": "Jl. Contoh No. 12",
  "city": "Malang",
  "quantity": 3,
  "preferredDate": "2026-12-20",
  "preferredTime": "Pagi (08.00–11.00)",
  "notes": "Tiga pakaian dalam satu kantong."
}
```

Server mengambil nama dan pemeriksaan dari data akun. Pengajuan baru berstatus `PENDING_REVIEW`, memiliki `version=1`, dan sudah memuat riwayat awal.

### Daftar dan pagination

Daftar donor dan admin menerima filter yang sama. `limit` default 25 dan dapat diatur antara 1–100. `query` mencocokkan sebagian ID, nama, atau kota dan dapat memakai awalan `#` untuk pencarian ID. Server mengurutkan mulai dari pengajuan terlama dengan ID sebagai pembeda saat timestamp sama. Balikkan `nextCursor` apa adanya dengan filter yang sama. Berhenti saat `nextCursor` bernilai `null`.

Item daftar memuat `donationId`, `donorName`, `city`, `quantity`, `status`, `version`, `createdAt`, dan `updatedAt`. Detail menambahkan field pickup, hasil pemeriksaan, jadwal final, alasan penolakan, serta timeline. Android Donasiku saat ini mengambil halaman pertama saja dan mengurutkan subset tersebut dari terbaru di layar. Admin mendukung memuat halaman berikutnya. [Model donasi](../backend/go/internal/domain/donation.go) · [Backlog pagination](backlog.md)

## Admin dan rewards

| Metode dan rute | Akses | Request dan respons sukses |
| --- | --- | --- |
| `GET /v1/admin/summary` | `ADMIN` | `200` dengan total serta jumlah per status. |
| `GET /v1/admin/donations` | `ADMIN` | `200` halaman antrean sesuai filter. |
| `GET /v1/admin/donations/{id}` | `ADMIN` | `200` detail, alamat, kontak, penilaian, referensi foto, dan riwayat. |
| `POST /v1/admin/donations/{id}/schedule` | `ADMIN` | `scheduledDate`, `scheduledTime`, `timezone=Asia/Jakarta`, dan `expectedVersion`. `200` detail `PICKUP_SCHEDULED`. |
| `POST /v1/admin/donations/{id}/reject` | `ADMIN` | `rejectionReason` dan `expectedVersion`. `200` detail `REJECTED`. |
| `POST /v1/admin/donations/{id}/status` | `ADMIN` | `targetStatus` dan `expectedVersion`. `200` detail `PICKED_UP` atau `COMPLETED`. Penyelesaian memberi +10 poin. |
| `GET /v1/points/balance` | `USER` | `200` dengan `balance` dan `updatedAt`. |
| `GET /v1/rewards` | `USER` | `200` dengan `items` katalog, termasuk `rewardId`, `name`, `description`, `icon`, `requiredPoints`, `type`, `isActive`. |
| `POST /v1/redemptions` | `USER` | `Idempotency-Key` dan `rewardId`. `201` transaksi baru atau `200` replay dengan `redemption`, `remainingBalance`, `replayed`. |
| `GET /v1/redemptions` | `USER` | `200` dengan `items` riwayat akun, berisi snapshot reward, `pointsUsed`, dan `redeemedAt`. |

Klien admin membaca `version` terbaru sebelum keputusan dan mengirimnya sebagai `expectedVersion`. Setelah status berubah, gunakan versi baru dari respons untuk tindakan berikutnya. [Review dan jadwal](donation-review-go.md) · [Transaksi dan contoh redeem](completion-rewards.md)

## Error, retry, dan idempotensi

Gunakan status HTTP dan `error.code` untuk menentukan aksi. `fieldErrors` hadir ketika server punya feedback per field.

```json
{
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Check the submitted fields.",
    "fieldErrors": {"quantity": "Enter a clothing quantity between 1 and 100."}
  }
}
```

| Status atau kode | Penanganan klien |
| --- | --- |
| `400` input terstruktur salah, `413` terlalu besar, `415` MIME salah, `422 VALIDATION_ERROR` | Perbaiki request atau isi form sesuai field yang dilaporkan. |
| `401 INVALID_SESSION` | Lakukan refresh terkoordinasi. Jika refresh juga ditolak dengan `401`, buka login. |
| `403 FORBIDDEN` | Role sesi tidak berhak melakukan aksi itu. |
| `404 CHECK_NOT_FOUND` atau `DONATION_NOT_FOUND` | ID sudah tidak ada atau resource tidak dapat dibaca oleh akun ini. |
| `409 VERSION_CONFLICT` atau `INVALID_TRANSITION` | Ambil ulang detail dan sesuaikan aksi dengan status serta versi terbaru. |
| `409 IDEMPOTENCY_CONFLICT` | Key lama terikat ke request lain. Periksa apakah ini operasi baru atau percobaan ulang. |
| `409 CHECK_IN_PROGRESS` atau `CHECK_RESERVATION_EXPIRED` | Coba pemeriksaan lagi dengan key dan foto yang sama. |
| `409 INSUFFICIENT_POINTS` atau `404 REWARD_NOT_FOUND` | Muat ulang saldo dan katalog sebelum menawarkan pilihan baru. |
| `429` | Tunggu cooldown, slot kerja, atau nilai `Retry-After` bila dikirim server. |
| `502 AI_RESPONSE_FAILED`, `503 AI_UNAVAILABLE` atau `CHECK_UNAVAILABLE` | Pertahankan input dan cek provider atau konfigurasi server. |
| `503 VERIFICATION_EMAIL_UNAVAILABLE` | Cek konfigurasi SMTP dan hasil pengiriman. `/ready` hanya mengecek database. |

Pickup dan redeem menerima key 8–128 huruf ASCII, angka, underscore, atau tanda hubung. Pemeriksaan menerima 1–128 karakter dari himpunan yang sama serta titik. Gunakan tepat satu header. Scope key mengikuti akun dan operasi. Untuk hasil mutasi yang belum pasti setelah timeout, kirim payload yang sama dengan key yang sama. [Definisi API error](../backend/go/internal/domain/errors.go)

## Kontrak pemeriksaan pakaian Go

Set `LOOPY_BASE` sesuai environment, dan `DONOR_ACCESS_TOKEN` dari sesi akun uji. Contoh berikut mengunggah JPEG dan memberi key untuk operasi baru.

```sh
CHECK_KEY="$(openssl rand -hex 16)"
curl -sS -i "$LOOPY_BASE/v1/checks" \
  -H "Authorization: Bearer $DONOR_ACCESS_TOKEN" \
  -H "Idempotency-Key: $CHECK_KEY" \
  -F 'clothingType=TSHIRT' \
  -F 'image=@fixture-shirt.jpg;type=image/jpeg'
```

Kirim tepat satu file `image` bertipe JPEG/PNG dengan batas 10 MiB dan satu `clothingType` yang didukung. Objek hasil membawa `checkId`, `userId`, `clothingPhoto`, `clothingType`, `aiResult`, `aiDescription`, `actionRecommendation`, dan `createdAt`. Tiga kategori aktif ialah `POTENTIALLY_ELIGIBLE`, `NEEDS_RECHECK`, dan `NOT_RECOMMENDED`.

Unduh JPEG melalui rute dari `clothingPhoto` pada host API yang sama. Sertakan Bearer. Retry identik dapat memperoleh hasil lama, termasuk setelah sesi access token diganti jika akun tetap sama. [Penanganan foto dan Groq](backend.md#pemeriksaan-pakaian-go) · [Kode tipe pakaian](../backend/go/internal/domain/clothing_check.go)
