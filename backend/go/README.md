# Implementasi Backend Go Loopy

Server memakai `net/http`, PostgreSQL melalui `pgx/v5`, password Argon2id, dan access/refresh token opaque. [Panduan backend](../../docs/backend.md) · [Autentikasi](../../docs/auth-go.md) · [Peninjauan donasi](../../docs/donation-review-go.md)

## Struktur Paket (Clean Architecture)

| Lokasi | Tanggung jawab |
| --- | --- |
| [main.go](main.go) | Entrypoint aplikasi (`serve`, `create-admin`, `healthcheck`), pembacaan konfigurasi, pool PostgreSQL, dan graceful shutdown. |
| [`migrations/`](migrations/README.md) | Skrip DDL PostgreSQL berurutan (`001`–`005`) yang di-embed ke binary melalui `embed.go`. |
| [`internal/domain/`](internal/domain/README.md) | Model domain murni, konstanta, aturan validasi bisnis, sentinel errors, dan interface repository. |
| [`internal/security/`](internal/security/README.md) | Hashing dan verifikasi kata sandi menggunakan Argon2id. |
| [`internal/vision/`](internal/vision/README.md) | Integrasi Groq Vision AI serta validasi/normalisasi foto pakaian JPEG/PNG + orientasi EXIF. |
| [`internal/store/`](internal/store/README.md) | Implementasi PostgreSQL (`PGStore`), runner migrasi advisory-lock, dan transaksi atomik per domain. |
| [`internal/httpapi/`](internal/httpapi/README.md) | Server HTTP, middleware Bearer/role, rate limiter, validasi JSON, dan handler endpoint. |
| [go.mod](go.mod) dan [go.sum](go.sum) | Versi Go, dependensi, dan checksum. |

## Menjalankan dan Menguji di Host

Set `DATABASE_URL` ke PostgreSQL lalu jalankan dari folder ini:

```sh
go test ./...
go vet ./...
go build ./...
```

Set `TEST_DATABASE_URL` ke PostgreSQL khusus pengujian untuk menjalankan tes integrasi (`store` dan `httpapi`). Tiap tes membuat dan membersihkan schema terpisah.
