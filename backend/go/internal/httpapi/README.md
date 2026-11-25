# HTTP API Backend Loopy

Menangani routing `net/http`, middleware Bearer token & role guard, pembatasan request, validasi JSON ketat, serta handler per fitur:

- `server.go`: Router, middleware autentikasi/role, rate limiter, JSON helper, dan pemetaan error.
- `auth_handler.go`: Autentikasi akun (`register`, `login`, `refresh`, `me`, `logout`).
- `clothing_check_handler.go`: Upload multipart, detail pemeriksaan, dan foto privat.
- `donation_handler.go`: Pengajuan pickup dan donasi terbaru donatur, serta ringkasan, antrean, detail, jadwal, dan penolakan admin.
- `rewards_handler.go`: Transisi status selesai, saldo poin, katalog reward, dan redeem idempoten.
