# Backend Loopy

API Go menangani OTP pendaftaran, login, profil, pemeriksaan pakaian dengan Groq, pickup, keputusan admin, dan rewards. Satu PostgreSQL menyimpan data untuk seluruh alur. [Proyek Go](../backend/go/README.md) · [Kontrak endpoint](api-contract.md)

## Cara kerja

| Bagian | Titik masuk | Tanggung jawab |
| --- | --- | --- |
| Startup | [main.go](../backend/go/main.go) | Membaca konfigurasi, menyiapkan pool, menerapkan migrasi, memilih command, dan menjalankan HTTP dengan graceful shutdown. |
| Batas HTTP | [server.go](../backend/go/internal/httpapi/server.go) | Router, format error, Bearer, guard role, batas ukuran JSON, timeout, dan rate limit autentikasi. |
| Akun | [auth_handler.go](../backend/go/internal/httpapi/auth_handler.go) · [email_registration.go](../backend/go/internal/httpapi/email_registration.go) | Pendaftaran sementara, kirim ulang OTP, verifikasi, login, rotasi token, pembaruan profil dan logout. |
| Pemeriksaan | [clothing_check_handler.go](../backend/go/internal/httpapi/clothing_check_handler.go) · [vision](../backend/go/internal/vision/README.md) | Validasi multipart, reservasi key, pemrosesan foto, pemanggilan Groq, dan pembacaan foto privat. |
| Pickup dan admin | [donation_handler.go](../backend/go/internal/httpapi/donation_handler.go) · [donation_store.go](../backend/go/internal/store/donation_store.go) | Submit, ringkasan, antrean, detail, penjadwalan, dan penolakan. |
| Status dan rewards | [rewards_handler.go](../backend/go/internal/httpapi/rewards_handler.go) · [rewards_store.go](../backend/go/internal/store/rewards_store.go) | Donasi milik donatur, status dijemput atau selesai, saldo, katalog, redeem, dan riwayat. |
| Database | [postgres.go](../backend/go/internal/store/postgres.go) · [migrasi 001–007](../backend/go/migrations/README.md) | Pool pgx, migrasi dengan advisory lock, transaksi per alur, dan kompatibilitas database lama yang tercakup suite. |

Server memakai `serve` sebagai command utama. `create-admin` membuka database dan menjalankan migrasi sebelum membuat akun admin. `healthcheck` meminta `/ready` pada port 8080 di container. [Panduan akun admin](deploy-lxc.md#6-create-an-admin-account-if-needed)

## Akun, sesi, dan hak akses

Pendaftaran dengan konfigurasi normal mengembalikan `202` dan challenge. Kode yang benar membuat akun `USER` serta sesi, lalu mengembalikan `201`. Admin dibuat melalui CLI dan masuk dari endpoint login yang sama. PostgreSQL menyimpan hash Argon2id untuk password dan hash SHA-256 untuk token acak. Request terlindungi membawa access token. Refresh mengganti pasangan token, dan logout menghapus sesi aktif. [Autentikasi](auth-go.md)

Layanan memakai identitas dari sesi untuk memfilter resource milik pengguna. Akses antrean dan keputusan memerlukan `ADMIN`. Antrean mengembalikan ringkasan, sedangkan kontak dan alamat dimuat melalui detail. Foto privat diakses dengan token oleh pemilik atau admin. [Review admin](donation-review-go.md)

## Menjalankan dan memeriksa backend

Mulai dari root repository. Gunakan `.env.example` sebagai rujukan untuk database, SMTP, dan Groq. Gunakan nama proyek Compose yang sama jika ingin membuka volume PostgreSQL yang sudah ada.

```bash
API_PORT=8082 docker compose -p loopy-go up --build -d --wait db backend
curl -fsS http://127.0.0.1:8082/health
curl -fsS http://127.0.0.1:8082/ready
```

| Pemeriksaan | Arti hasil |
| --- | --- |
| `GET /health` | HTTP `200` dengan `{"status":"ok","mode":"go-auth"}` menunjukkan proses HTTP merespons. |
| `GET /ready` | `200` dengan `{"status":"ready"}` menunjukkan koneksi PostgreSQL tersedia. Kegagalan database memberi `503 NOT_READY`. |
| Daftar dan verifikasi OTP | Menguji SMTP, alamat pengirim, challenge, pengiriman ke inbox, dan pembuatan akun. |
| Unggah foto memakai akun uji | Menguji request multipart, izin, kredensial Groq, dan hasil analisis end-to-end. |

Untuk Go di host, siapkan PostgreSQL yang bisa diakses, set `DATABASE_URL`, lalu jalankan `go run .` dari `backend/go`. `HTTP_ADDR` mengatur bind dan defaultnya `127.0.0.1:8080`. Compose development dapat memublikasikan port host sesuai konfigurasinya. Compose LXC memakai `API_BIND_ADDRESS` dengan default loopback. [Koneksi perangkat](development.md#koneksi-android-ke-backend) · [Konfigurasi VM](deploy-lxc.md)

## Pemeriksaan pakaian Go

1. `POST /v1/checks` menerima satu file JPEG atau PNG hingga 10 MiB serta satu kode `clothingType`. Identitas berasal dari token `USER`.
2. Backend mengoreksi orientasi, menormalisasi gambar ke JPEG, dan mencatat reservasi key per akun. Panggilan Groq dilakukan dengan deadline dan jumlah pemeriksaan paralel yang dibatasi.
3. Hasil, foto, dan referensi key disimpan atomik. Request dengan key serta payload yang sama dapat memperoleh hasil sebelumnya. Jika payload berubah, gunakan key baru.
4. Pemilik atau admin membaca detail dan foto melalui endpoint terpisah. Hasil yang berpotensi layak dapat dipakai untuk mengajukan pickup.

Jika `GROQ_API_KEY` kosong, pemeriksaan baru mengembalikan `503 AI_UNAVAILABLE` meski `/ready` sehat. `502 AI_RESPONSE_FAILED` menunjukkan respons provider gagal dipakai. Cek model, mode respons, dan konektivitas pada konfigurasi yang efektif di container. [Aturan foto dan provider](../backend/go/internal/vision/README.md) · [Contoh request](api-contract.md#kontrak-pemeriksaan-pakaian-go)

## Verifikasi pengembangan

```bash
cd backend/go
go test -race -count=1 ./...
go vet ./...
go build ./...
```

Set `TEST_DATABASE_URL` ke PostgreSQL khusus tes untuk mencakup migrasi, rollback, kepemilikan, dan konkurensi. Suite membuka schema uji terisolasi. Tanpa variabel itu, tes yang memerlukan PostgreSQL dilewati. [Workflow CI](../.github/workflows/README.md) · [Hasil verifikasi](verification.md)
