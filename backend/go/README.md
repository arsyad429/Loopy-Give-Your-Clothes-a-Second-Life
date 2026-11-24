# Implementasi backend Go

Server memakai `net/http`, PostgreSQL melalui pgx, password Argon2id, dan access token serta refresh token opaque. Pemeriksaan pakaian memakai sesi akun Go yang sama. [Panduan backend](../../docs/backend.md) · [Autentikasi](../../docs/auth-go.md) · [Peninjauan donasi](../../docs/donation-review-go.md)

## File dan tanggung jawab

| File | Kegunaan |
| --- | --- |
| [main.go](main.go) | Konfigurasi, startup database dan migrasi, server HTTP, perintah `healthcheck` dan `create-admin`. |
| [http.go](http.go) | Routing, autentikasi bearer, kontrol role, validasi JSON dan pembatasan request. |
| [auth.go](auth.go) | Validasi akun, penerbitan sesi, login, registrasi, dan rotasi token. |
| [password.go](password.go) | Hash dan verifikasi password dengan Argon2id. |
| [store.go](store.go) | Akses PostgreSQL, migrasi berurutan, penyimpanan akun dan sesi. |
| [schema.sql](schema.sql) | Migrasi versi 1 untuk akun dan sesi. |
| [schema_pickup.sql](schema_pickup.sql) | Migrasi versi 2 untuk pemeriksaan, pengajuan pickup, dan audit status. |
| [schema_vision.sql](schema_vision.sql) | Migrasi versi 3 untuk foto privat, rekomendasi AI, dan idempotensi pemeriksaan. |
| [clothing_check.go](clothing_check.go) | Route upload multipart, detail dan foto, batas pekerjaan paralel serta alur analisis. |
| [clothing_check_store.go](clothing_check_store.go) | Reservasi idempotensi dan transaksi hasil, JPEG privat, serta pengecekan pemilik. |
| [clothing_photo.go](clothing_photo.go) | Validasi JPEG dan PNG, orientasi EXIF, resize, dan normalisasi JPEG. |
| [groq_vision.go](groq_vision.go) | Request Groq Vision dan validasi respons kategori, penjelasan, serta rekomendasi. |
| [donation.go](donation.go) | Aturan dan model peninjauan pickup oleh admin. |
| [donation_http.go](donation_http.go) | Endpoint ringkasan, antrean, detail, jadwal dan penolakan admin. |
| [donation_store.go](donation_store.go) | Query PostgreSQL, perubahan status berdasarkan versi, dan audit peninjauan. |
| [pickup.go](pickup.go) | Template Go untuk pengajuan donatur dan penyelesaian pickup beserta rencana tes transaksi. |
| [rewards.go](rewards.go), [rewards_http.go](rewards_http.go), [rewards_store.go](rewards_store.go) | Penyelesaian donasi, kredit 10 poin satu kali, saldo, katalog, redeem atomik/idempoten, dan riwayat. |
| [schema_rewards.sql](schema_rewards.sql) | Migrasi versi 4 untuk katalog, ledger dan snapshot redemption. Vision tetap versi 3; catatan versi reward lama dinormalisasi tanpa menghapus data. |
| [merge_migration_test.go](merge_migration_test.go) | Upgrade dari kedua riwayat versi 3: akun/sesi, Vision, saldo, dan redemption tetap tersedia. |
| [auth_test.go](auth_test.go) | Unit test akun, sesi, role, batas input, dan pembatasan request. |
| [store_integration_test.go](store_integration_test.go) | Tes PostgreSQL untuk autentikasi, konkurensi, transaksi dan masa berlaku sesi. |
| [clothing_check_integration_test.go](clothing_check_integration_test.go) | Tes PostgreSQL untuk login dan akses pemeriksaan dengan sesi yang sama serta migrasi data yang sudah ada. |
| [clothing_photo_test.go](clothing_photo_test.go) | Tes MIME, normalisasi, rotasi EXIF, dan transparansi gambar. |
| [groq_vision_test.go](groq_vision_test.go) | Tes format request, skema respons, dan rate limit Groq dengan server uji. |
| [donation_test.go](donation_test.go) | Unit test filter antrean dan validasi jadwal pickup. |
| [donation_integration_test.go](donation_integration_test.go) | Tes postgres untuk migrasi, pembatasan akses, konflik, dan rollback keputusan admin. |
| [go.mod](go.mod) dan [go.sum](go.sum) | Versi Go, dependensi, dan checksumnya. |

## Jalan dan uji di host

Gunakan versi Go dari `go.mod`. Set `DATABASE_URL` ke URL PostgreSQL dan jalankan `go run .` dari folder ini. Alamat bawaan adalah `127.0.0.1:8080`. Compose memakai `:8080` di dalam container dan mengatur port host melalui `API_PORT`. [Contoh Compose](../README.md)

Isi `GROQ_API_KEY` di lingkungan server. `GROQ_MODEL` dan `GROQ_RESPONSE_MODE` tersedia di contoh `.env` repo. Mode `json_schema` memerlukan dukungan structured output pada model. Mode `json_object` tetap memvalidasi hasil di server. Tanpa key, analisis baru merespons `503 AI_UNAVAILABLE`. [Kontrak pemeriksaan](../../docs/api-contract.md#kontrak-pemeriksaan-pakaian-go)

```sh
go test ./...
go vet ./...
go build ./...
```

Set `TEST_DATABASE_URL` ke PostgreSQL khusus pengujian untuk menjalankan tes integrasi. Tiap tes membuat dan membersihkan schema terpisah. CI menjalankan `go test -race -count=1 ./...` dan `govulncheck`.

Admin dapat dibuat melalui `go run . create-admin` dengan `LOOPY_ADMIN_NAME`, `LOOPY_ADMIN_EMAIL`, dan `LOOPY_ADMIN_PASSWORD` di environment privat. [Panduan admin lokal](../../docs/auth-go.md)
