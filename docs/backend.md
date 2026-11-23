# Backend Loopy

Compose utama menjalankan [API Go](../backend/go/README.md) untuk akun dan peninjauan pickup. [API pemeriksaan Ktor](../backend/README.md) dijalankan dengan `compose.vision.yaml` dan memakai database serta sesi terpisah.

## Cara kerja

| Bagian | Implementasi | Tanggung jawab |
| --- | --- | --- |
| Startup | [main.go](../backend/go/main.go), [store.go](../backend/go/store.go) | Membaca konfigurasi, membuka pool PostgreSQL, menjalankan migrasi, dan memulai server HTTP. |
| HTTP | [http.go](../backend/go/http.go) | Routing, autentikasi Bearer, pemeriksaan role, batas ukuran JSON, dan format error. |
| Autentikasi akun | [auth.go](../backend/go/auth.go), [password.go](../backend/go/password.go), [store.go](../backend/go/store.go) | Register, login, refresh, profil akun aktif, dan logout. Kata sandi di-hash dengan Argon2id. Database menyimpan hash token sesi. |
| Verifikasi donasi dan jadwal | [donation_http.go](../backend/go/donation_http.go), [donation.go](../backend/go/donation.go), [donation_store.go](../backend/go/donation_store.go) | Ringkasan, antrean, detail pengajuan, persetujuan dengan jadwal final, dan penolakan dengan alasan. |
| Penyimpanan | [schema.sql](../backend/go/schema.sql), [schema_pickup.sql](../backend/go/schema_pickup.sql) | Migrasi versi 1 untuk akun dan sesi, lalu versi 2 untuk pemeriksaan pakaian, pengajuan donasi, dan riwayat status. |

Migrasi berjalan sekali per versi. Server mengambil advisory lock PostgreSQL untuk menyerialkan startup beberapa instance. Setiap startup menerapkan versi yang tertunda dan mencatatnya dalam transaksi. Database versi 1 mempertahankan akun dan sesi saat versi 2 diterapkan.

## Akun, sesi, dan hak akses

Register membuat akun ber-role `USER` dan membuat sesi. Login pengguna dan admin memakai endpoint yang sama. Buat admin melalui perintah `create-admin` dengan konfigurasi privat. Login mengembalikan access token dan refresh token. Aplikasi mengirim access token dalam header `Authorization: Bearer <accessToken>`. Refresh merotasi token. Logout mencabut sesi itu. Pada setiap request yang dilindungi, API membaca role akun dari database. [Alur, keamanan, kontrak, dan contoh autentikasi](auth-go.md)

Endpoint admin memerlukan role `ADMIN`. Antrean memuat ringkasan pengajuan, sedangkan kontak, alamat, dan penilaian AI tersedia di endpoint detail yang juga dilindungi. Keputusan admin mencatat ID akun dari sesi sebagai pelaku. [Kontrak verifikasi donasi dan penjadwalan](donation-review-go.md)

## Menjalankan dan memeriksa backend

Dari root repo, jalankan Docker Compose. Database memakai volume bernama sehingga data tersedia kembali saat service dijalankan ulang.

```bash
docker compose up --build -d --wait
curl http://localhost:8080/health
curl http://localhost:8080/ready
```

- `GET /health` mengembalikan `{"status":"ok","mode":"go-auth"}` sebagai respons liveness yang dipakai klien sekarang.
- `GET /ready` memeriksa koneksi PostgreSQL dan mengembalikan `{"status":"ready"}`. Jika database tidak bisa diakses, responsnya `503 NOT_READY`.
- Untuk menjalankan Go langsung, set `DATABASE_URL`, masuk ke `backend/go`, lalu jalankan `go run .`. Alamat bawaan adalah `127.0.0.1:8080`; `HTTP_ADDR` dapat mengubahnya. Lihat [panduan deployment LXC](deploy-lxc.md) untuk jaringan dan deployment.

Dari `backend/go`, jalankan `go test ./...`, `go vet ./...`, dan `go build ./...`. Set `TEST_DATABASE_URL` ke PostgreSQL khusus pengujian untuk menjalankan tes integrasi. Tes membuat schema terpisah untuk setiap kasus dan menghapusnya setelah selesai. CI menjalankan tes integrasi dengan PostgreSQL 17 dan `-race`. [Bukti pemeriksaan](verification.md)

## Status fitur

Backend aktif menyediakan autentikasi dasar serta keputusan admin dari `PENDING_REVIEW` ke `PICKUP_SCHEDULED` atau `REJECTED`. Android masih memakai pratinjau lokal. Email OTP, pengajuan pickup dari pengguna, analisis dan unduhan foto, pelacakan pengguna, penyelesaian pickup, poin, dan rewards masih menunggu implementasi pada API Go. [Peta endpoint dan rencana lanjutan](api-contract.md)

## Pemeriksaan pakaian Ktor

Ktor menerima JPEG atau PNG dan jenis pakaian melalui multipart, menormalkan gambar menjadi JPEG sebelum analisis Groq, dan menyimpan hasil serta foto privat dalam transaksi PostgreSQL. Pemilik dan admin dapat mengakses hasil dan foto dengan bearer token Ktor. Key idempotensi dipertahankan saat request yang sama diulang.

Dari root repository, isi kredensial Groq di `.env` lokal, lalu jalankan:

```sh
docker compose -f compose.vision.yaml up --build -d --wait
curl -fsS http://127.0.0.1:8081/ready
```

Token hasil login Go saat ini ditolak oleh API Ktor karena token dan pengguna berasal dari database sesi yang berbeda. Penerbitan sesi Ktor sudah tersedia di server untuk pengujian; route login publik dan wiring Android masih diperlukan untuk alur pengguna. [Kontrak API](api-contract.md) · [Cara menjalankan tes Ktor](../backend/README.md#uji-ktor)
