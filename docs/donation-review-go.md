# Verifikasi donasi dan penjadwalan pickup

Admin dapat melihat antrean pengajuan, memeriksa detail dan hasil AI, lalu menyetujui dengan jadwal final atau menolak dengan alasan. Pengajuan dimulai dari `PENDING_REVIEW` versi 1.

## Endpoint

Semua request memakai header `Authorization: Bearer <accessToken>` dari akun `ADMIN`. Request POST juga memakai `Content-Type: application/json`.

| Metode dan endpoint | Request | Respons sukses |
| --- | --- | --- |
| `GET /v1/admin/summary` | Tanpa body | `200` dengan `total`, `pending`, `scheduled`, `pickedUp`, `completed`, dan `rejected`. |
| `GET /v1/admin/donations` | Opsional `status`, `query`, `limit`, `cursor` | `200` dengan `items` dan `nextCursor`. |
| `GET /v1/admin/donations/{id}` | UUID pengajuan | `200` dengan detail dan riwayat. |
| `POST /v1/admin/donations/{id}/schedule` | `scheduledDate`, `scheduledTime`, `timezone`, `expectedVersion` | `200` dengan detail berstatus `PICKUP_SCHEDULED` dan riwayat terbaru. |
| `POST /v1/admin/donations/{id}/reject` | `rejectionReason`, `expectedVersion` | `200` dengan detail berstatus `REJECTED` dan riwayat terbaru. |

**Antrean.** Gunakan `?status=PENDING_REVIEW` untuk menampilkan pengajuan yang menunggu admin. Status yang didukung adalah `PENDING_REVIEW`, `PICKUP_SCHEDULED`, `PICKED_UP`, `COMPLETED`, dan `REJECTED`. Pencarian mencocokkan sebagian UUID, nama donatur, atau kota. `limit` bawaan 25, dengan rentang 1–100. Urutan mulai dari pengajuan terlama, dengan UUID sebagai pembeda bila waktu sama. Kirim `nextCursor` bersama filter yang sama untuk mengambil halaman berikutnya. `null` menandai halaman terakhir. Item berisi `donationId`, `donorName`, `city`, `quantity`, `status`, `version`, `createdAt`, dan `updatedAt`.

**Detail.** Selain field antrean, detail memuat `phoneNumber`, `pickupAddress`, `preferredDate`, `preferredTime`, `notes`, serta objek `clothingCheck` yang berisi `checkId`, `photoKey`, `clothingType`, `aiResult`, dan `aiDescription`. `photoKey` adalah referensi penyimpanan privat. Gunakan `clothingCheck.checkId` untuk mengambil foto melalui `GET /v1/checks/{id}/photo` dengan token admin. Detail juga memuat `scheduledAt`, `scheduledDate`, `scheduledTime`, `timezone`, dan `rejectionReason`. Field keputusan yang belum terisi bernilai `null`. Setiap entri `history` memiliki `version`, `previousStatus`, `newStatus`, `updatedBy`, dan `updatedAt`.

**Penjadwalan.** Kirim tanggal `YYYY-MM-DD`, waktu `HH:mm` dalam format 24 jam, dan zona `Asia/Jakarta`. API mewajibkan kombinasi tanggal dan jam yang akan datang. PostgreSQL menyimpan satu `TIMESTAMPTZ`. Respons memberi `scheduledAt` dalam UTC beserta tanggal dan jam lokal WIB. Contohnya pukul `09:30` WIB dikembalikan sebagai pukul `02:30Z` pada tanggal yang sama. Preferensi dari pengguna tetap tersedia di field tersendiri.

**Penolakan.** Alasan wajib sepanjang 1–500 karakter setelah spasi di awal dan akhir dibuang. Alasan yang berhasil disimpan dapat dimuat kembali melalui endpoint detail.

## Transaksi dan permintaan bersamaan

1. Klien membaca `version` dari detail terakhir dan mengirimnya sebagai `expectedVersion`.
2. Server memvalidasi token, role, dan input. PostgreSQL mengunci baris pengajuan dengan `SELECT ... FOR UPDATE`.
3. Server memeriksa versi dan status `PENDING_REVIEW`. Untuk penjadwalan, database memeriksa kembali batas waktu setelah kunci diperoleh.
4. Dalam transaksi yang sama, server mengubah status, menyimpan waktu pickup atau alasan penolakan, menaikkan versi, dan menulis riwayat keputusan. Kesalahan saat menulis riwayat membatalkan perubahan status dan jadwal.
5. Jika dua keputusan memakai versi yang sama, satu saja yang berhasil. Jika muncul konflik, baca kembali detail untuk menampilkan keputusan terbaru kepada admin.

## Error yang perlu ditangani oleh Android

| HTTP | Kode dan arti |
| --- | --- |
| `400` | `INVALID_DONATION_ID`, `INVALID_QUERY`, atau `INVALID_JSON`. Cek format UUID, parameter, atau body. |
| `401` | `INVALID_SESSION`. Access token tidak tersedia, kedaluwarsa, atau dicabut. |
| `403` | `FORBIDDEN`. Akun tidak memiliki role admin. |
| `404` | `DONATION_NOT_FOUND`. UUID valid tetapi pengajuan tidak ditemukan. |
| `409` | `VERSION_CONFLICT` saat versi sudah berubah, atau `INVALID_TRANSITION` saat versi cocok tetapi pengajuan sudah diputuskan. |
| `413` atau `415` | Body melewati 16 KiB atau tipe konten bukan JSON. |
| `422` | `VALIDATION_ERROR`. Gunakan `error.fieldErrors` untuk menampilkan masalah di field yang sesuai. |

## Mencoba dari database pengembangan

Buat akun donatur melalui register dan akun admin melalui [CLI create-admin](auth-go.md). Alur lengkap memakai [upload foto dan POST pengajuan](api-contract.md). Untuk tes kondisi tertentu di database pengembangan, SQL berikut bisa menyediakan pemeriksaan layak tanpa memanggil Groq. Fixture ini membawa referensi foto ilustrasi. Jalankan `psql` dengan variabel `-v donor_id=<UUID akun pengguna>` dan simpan `donation_id` yang dikembalikan.

```sql
BEGIN;
SELECT gen_random_uuid() AS check_id, gen_random_uuid() AS donation_id \gset
INSERT INTO clothing_checks (check_id, user_id, photo_key, clothing_type, ai_result, ai_description)
VALUES (:'check_id', :'donor_id', 'private/demo-shirt.jpg', 'TSHIRT',
        'POTENTIALLY_ELIGIBLE', 'Tidak tampak sobekan besar pada foto.');
INSERT INTO donation_requests (donation_id, user_id, check_id, phone_number, pickup_address,
                               city, quantity, preferred_date, preferred_time, notes)
VALUES (:'donation_id', :'donor_id', :'check_id', '081200000000', 'Jl. Contoh No. 12',
        'Malang', 3, CURRENT_DATE + 2, 'Pagi', 'Sudah dicuci');
INSERT INTO donation_status_history (donation_id, version, new_status, updated_by, updated_at)
SELECT donation_id, version, status, user_id, created_at
FROM donation_requests WHERE donation_id = :'donation_id';
COMMIT;
SELECT :'donation_id' AS donation_id;
```

Contoh PowerShell. Isi `$adminAccessToken` dari respons login admin dan `$donationId` dari hasil SQL.

```powershell
$base = 'http://localhost:8080'
$headers = @{ Authorization = "Bearer $adminAccessToken" }
$review = "$base/v1/admin/donations/$donationId"

Invoke-RestMethod "$base/v1/admin/donations?status=PENDING_REVIEW&limit=25" -Headers $headers
$detail = Invoke-RestMethod $review -Headers $headers

$schedule = @{
    scheduledDate = [DateTimeOffset]::UtcNow.AddHours(7).AddDays(2).ToString('yyyy-MM-dd')
    scheduledTime = '09:30'
    timezone = 'Asia/Jakarta'
    expectedVersion = $detail.version
} | ConvertTo-Json
Invoke-RestMethod "$review/schedule" -Method Post -Headers $headers -ContentType 'application/json' -Body $schedule
```

Untuk mencoba penolakan, gunakan pengajuan lain yang masih `PENDING_REVIEW`, ambil versi terbarunya, dan kirim body `{"rejectionReason":"Foto perlu diperjelas.","expectedVersion":1}` ke endpoint `/reject`.

## Menelusuri alur dari Android

| Langkah | Layar dan state | Request |
| --- | --- | --- |
| Buka dashboard | A01 mengambil ringkasan dan maksimal tiga pengajuan menunggu yang paling lama. | `GET /v1/admin/summary` dan daftar `PENDING_REVIEW` dengan `limit=3`. |
| Buka Pengajuan | A02 memuat filter dan kata kunci. Meski kueri berubah, cursor hanya berlaku untuk filter yang menghasilkan cursor itu. | `GET /v1/admin/donations`. |
| Buka detail | A03 membaca kontak, alamat, hasil pemeriksaan, status, dan versi. | `GET /v1/admin/donations/{id}` dan foto privat. |
| Tutup dialog dan buka kembali | Pilihan tanggal/jam sementara disimpan via Room per admin dan pengajuan pada alur simpan draf. Jadwal resmi berasal dari API. | Konfirmasi mengirim perubahan ke `/schedule` dengan versi terbaru. |
| Teruskan pickup | Gunakan versi dari respons terakhir. | `/status` menerima `PICKED_UP` setelah jadwal dan `COMPLETED` setelah dijemput. |
| Cek dari donatur | Refresh daftar/detail dan bandingkan preferensi serta jadwal final. | Rute `/v1/donations` memakai sesi pemilik. |

Jika API menolak versi atau transisi, tampilkan alasan dan muat detail baru sebelum menawarkan tindakan lagi. Draf hanya relevan saat pengajuan masih menunggu. [Kode Android admin](../android/app/src/main/java/id/ac/loopy/feature/admin/README.md) · [Penyelesaian dan poin](completion-rewards.md)
