# Backend Loopy

API Go menangani akun, pemeriksaan pakaian dengan Groq, dan peninjauan pickup. Login dan pemeriksaan berbagi satu server serta database PostgreSQL. [Implementasi Go](../backend/go/README.md)

## Cara kerja

| Bagian | Implementasi | Tanggung jawab |
| --- | --- | --- |
| Startup | [main.go](../backend/go/main.go), [store.go](../backend/go/store.go) | Membaca konfigurasi, membuka pool PostgreSQL, menjalankan migrasi, dan memulai server HTTP. |
| HTTP | [http.go](../backend/go/http.go) | Routing, autentikasi Bearer, pemeriksaan role, batas ukuran JSON, dan format error. |
| Autentikasi akun | [auth.go](../backend/go/auth.go), [password.go](../backend/go/password.go), [store.go](../backend/go/store.go) | Register, login, refresh, profil akun aktif, dan logout. Kata sandi di-hash dengan Argon2id. Database menyimpan hash token sesi. |
| Verifikasi donasi dan jadwal | [donation_http.go](../backend/go/donation_http.go), [donation.go](../backend/go/donation.go), [donation_store.go](../backend/go/donation_store.go) | Ringkasan, antrean, detail pengajuan, persetujuan dengan jadwal final, dan penolakan dengan alasan. |
| Pemeriksaan pakaian | [clothing_check.go](../backend/go/clothing_check.go), [clothing_photo.go](../backend/go/clothing_photo.go), [groq_vision.go](../backend/go/groq_vision.go), [clothing_check_store.go](../backend/go/clothing_check_store.go) | Validasi dan normalisasi foto, analisis Groq, penyimpanan atomik, dan pembacaan foto privat. |
| Penyimpanan | [schema.sql](../backend/go/schema.sql), [schema_pickup.sql](../backend/go/schema_pickup.sql), [schema_vision.sql](../backend/go/schema_vision.sql), [schema_rewards.sql](../backend/go/schema_rewards.sql) | Versi 1 akun/sesi, versi 2 pickup, versi 3 Vision, versi 4 poin dan reward. |
| Penyelesaian dan reward | [rewards_http.go](../backend/go/rewards_http.go), [rewards_store.go](../backend/go/rewards_store.go) | Transisi status, kredit unik, saldo, katalog, transaksi penukaran idempoten dan riwayat pengguna. |

Migrasi berjalan sekali per versi. Server mengambil advisory lock PostgreSQL untuk menyerialkan startup beberapa instance. Setiap startup menerapkan versi yang tertunda dan mencatatnya dalam transaksi. Migrasi mempertahankan akun, sesi, dan data pickup yang sudah tersimpan di database Go.

## Akun, sesi, dan hak akses

Register membuat akun ber-role `USER` dan membuat sesi. Login pengguna dan admin memakai endpoint yang sama. Buat admin melalui perintah `create-admin` dengan konfigurasi privat. Login mengembalikan access token dan refresh token. Aplikasi mengirim access token dalam header `Authorization: Bearer <accessToken>`. Refresh merotasi token. Logout mencabut sesi itu. Pada setiap request yang dilindungi, API membaca role akun dari database. [Alur, keamanan, kontrak, dan contoh autentikasi](auth-go.md)

Endpoint admin memerlukan role `ADMIN`. Antrean memuat ringkasan pengajuan, sedangkan kontak, alamat, dan penilaian AI tersedia di endpoint detail yang juga dilindungi. Keputusan admin mencatat ID akun dari sesi sebagai pelaku. [Kontrak verifikasi donasi dan penjadwalan](donation-review-go.md)

## Menjalankan dan memeriksa backend

Dari root repo, jalankan Docker Compose. Database memakai volume bernama sehingga data tersedia kembali saat service dijalankan ulang.

```bash
API_PORT=8082 docker compose -p loopy-go up --build -d --wait db backend
curl -fsS http://127.0.0.1:8082/health
curl -fsS http://127.0.0.1:8082/ready
```

- `GET /health` mengembalikan `{"status":"ok","mode":"go-auth"}` sebagai respons liveness yang dipakai klien sekarang.
- `GET /ready` memeriksa koneksi PostgreSQL dan mengembalikan `{"status":"ready"}`. Jika database tidak bisa diakses, responsnya `503 NOT_READY`.
- Untuk menjalankan Go langsung, set `DATABASE_URL`, masuk ke `backend/go`, lalu jalankan `go run .`. Alamat bawaan adalah `127.0.0.1:8080`; `HTTP_ADDR` dapat mengubahnya. Lihat [panduan deployment LXC](deploy-lxc.md) untuk jaringan dan deployment.

Dari `backend/go`, jalankan `go test ./...`, `go vet ./...`, dan `go build ./...`. Set `TEST_DATABASE_URL` ke PostgreSQL khusus pengujian untuk menjalankan tes integrasi. Tes membuat schema terpisah untuk setiap kasus dan menghapusnya setelah selesai. CI menjalankan tes integrasi dengan PostgreSQL 17 dan `-race`. [Bukti pemeriksaan](verification.md)

## Status fitur

Backend aktif menyediakan autentikasi, pemeriksaan/foto privat, verifikasi/jadwal/penolakan, transisi `PICKUP_SCHEDULED → PICKED_UP → COMPLETED` dengan 10 poin satu kali, pembacaan donasi pemilik, saldo, katalog, redeem, dan riwayat. Android menggunakan API untuk auth, pemeriksaan, pickup, donasi, admin dan reward. Saldo serta transaksi reward bersumber dari PostgreSQL; demo Room E–F telah dihapus. OTP dan fitur tambahan profil/settings memerlukan kontrak tersendiri. [Kontrak API](api-contract.md) · [Panduan E–F](completion-rewards.md)

## Pemeriksaan pakaian Go

Go menerima JPEG atau PNG dan jenis pakaian melalui multipart. Backend menormalkan foto menjadi JPEG, mengirimkannya ke Groq, lalu menyimpan hasil dan foto privat dalam satu transaksi PostgreSQL. Pemilik dan admin menggunakan access token Go untuk mengakses hasil dan foto. Pengulangan request dengan key dan payload yang sama mengembalikan hasil sebelumnya.

Isi `GROQ_API_KEY` di `.env` server untuk mengaktifkan analisis baru. Data dari skema backend lama memerlukan proses impor tersendiri. Android memakai access token Go yang sama untuk upload, detail, dan unduhan privat. Token diperbarui melalui refresh ketika server menolak access token dengan `401`. [Kontrak API](api-contract.md#kontrak-pemeriksaan-pakaian-go) · [Tes Go](../backend/go/README.md)
