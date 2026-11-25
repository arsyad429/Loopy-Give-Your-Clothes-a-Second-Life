# Paket Internal Backend Loopy

Berisi paket privat yang hanya digunakan oleh aplikasi backend `loopy/backend`:

- `domain/`: Entitas bisnis, aturan validasi, error terstruktur, dan kontrak interface repository.
- `security/`: Utilitas kriptografi untuk hashing kata sandi dengan Argon2id.
- `vision/`: Integrasi Groq Vision API serta validasi/normalisasi foto pakaian JPEG/PNG.
- `store/`: Implementasi persistensi PostgreSQL (`pgxpool`) dan runner migrasi atomik.
- `httpapi/`: Router HTTP, middleware autentikasi/role, rate limiter, dan handler per domain.
