# Backend Go Loopy

Proyek Go menggunakan `net/http`, `pgx/v5`, dan PostgreSQL. Versi Go dan dependensi dikunci oleh [go.mod](go.mod) dan [go.sum](go.sum).

## Dari request ke database

1. [main.go](main.go) membaca konfigurasi, membuat pool PostgreSQL, menjalankan migrasi, dan merangkai server. Command yang tersedia adalah `serve`, `create-admin`, dan `healthcheck`.
2. [Paket HTTP](internal/httpapi/README.md) memeriksa sesi dan peran, memvalidasi payload, lalu memanggil service atau repository.
3. [Domain](internal/domain/README.md) mendefinisikan aturan, model, serta kontrak. [Store PostgreSQL](internal/store/README.md) menjalankan transaksi dan mengembalikan hasilnya.
4. [Paket security](internal/security/README.md) menangani Argon2id dan SMTP TLS. [Paket vision](internal/vision/README.md) memvalidasi foto dan menghubungi Groq.

Backend menerapkan [migrasi 001–007](migrations/README.md) saat startup. `GET /ready` memeriksa PostgreSQL. Pendaftaran dan pemeriksaan foto perlu diuji tersendiri untuk memverifikasi provider.

## Pengujian

Jalankan dari folder ini dengan `TEST_DATABASE_URL` yang mengarah ke PostgreSQL pengujian. Tes integrasi membuat schema terpisah untuk setiap suite.

```sh
go test -race -count=1 ./...
go vet ./...
go build ./...
```

[Kontrak API](../../docs/api-contract.md) · [Autentikasi dan admin](../../docs/auth-go.md) · [Deploy LXC](../../docs/deploy-lxc.md)

## Subfolder

[internal](internal/README.md)
