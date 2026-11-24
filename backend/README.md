# Backend Loopy

Go menjalankan satu API untuk akun, sesi, pemeriksaan pakaian dengan Groq, dan peninjauan pickup oleh admin. PostgreSQL menyimpan akun, sesi, hasil pemeriksaan, foto privat, dan alur peninjauan. [Kontrak API](../docs/api-contract.md) · [Arsitektur backend](../docs/backend.md)

## Mulai dari root repository

Salin `.env.example` ke `.env` jika belum ada. Isi `GROQ_API_KEY` di `.env` untuk mengaktifkan analisis baru.

```sh
API_PORT=8082 docker compose -p loopy-go up --build -d --wait db backend
curl -fsS http://127.0.0.1:8082/ready
```

Jika database Go sudah berjalan, gunakan kembali nama proyek Compose dan volume yang sesuai. Migrasi Go diterapkan saat startup.

Buka [backend/go](go/README.md) untuk peta setiap file, cara menjalankan binary di host, dan tes PostgreSQL.
