# Backend Loopy

Docker menjalankan API Go di [backend/go](../backend/go/README.md) dengan PostgreSQL 17. API menyediakan autentikasi akun dan peninjauan pengajuan pickup oleh admin. [Backend Kotlin/Ktor](../backend/README.md) merupakan scaffold terpisah.

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

Backend aktif menyediakan autentikasi, verifikasi/jadwal/penolakan, transisi `PICKUP_SCHEDULED → PICKED_UP → COMPLETED` dengan 10 poin satu kali, pembacaan donasi milik pengguna, saldo, katalog, penukaran idempoten, dan riwayat reward. [Panduan penyelesaian dan reward](completion-rewards.md). Android untuk alur ini memakai demo Room lokal yang terpisah; integrasi akun/API dan deployment VM belum dikerjakan. Email OTP, pengajuan pickup dari pengguna, analisis AI, dan unduhan foto masih menunggu implementasi. [Peta endpoint](api-contract.md)
