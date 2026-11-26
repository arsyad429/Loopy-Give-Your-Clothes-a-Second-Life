# Backend Loopy

[Proyek Go](go/README.md) menyediakan akun dan sesi, pemeriksaan foto melalui Groq, pengajuan dan penjadwalan pickup, transaksi poin, serta rewards. PostgreSQL menyimpan akun, status, foto privat, dan riwayat perubahan.

Mulai dari [main.go](go/main.go) untuk konfigurasi proses. Buka [handler HTTP](go/internal/httpapi/README.md) untuk endpoint dan guard akses, atau [store](go/internal/store/README.md) untuk transaksi dan query. Deployment Docker ada di [panduan VM](../docs/deploy-lxc.md).
