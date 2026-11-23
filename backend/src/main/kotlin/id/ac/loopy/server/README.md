# Server Kotlin/Ktor

Server pemeriksaan pakaian dengan Groq dan PostgreSQL. Startup menjalankan Flyway, membuka satu klien Groq, dan memasang autentikasi bearer sebelum rute pemeriksaan.

| File atau folder | Kegunaan |
| --- | --- |
| [`Application.kt`](Application.kt) | Entry point Netty, wiring gateway dan store, serta rute liveness dan readiness. |
| [`auth`](auth/README.md) | Verifikasi token pemeriksaan, sesi, dan template penerbitan sesi lewat login. |
| [`checks`](checks/README.md) | Upload multipart, normalisasi, analisis Groq, idempotensi, dan pembacaan hasil. |
| [`database`](database/README.md) | Pool JDBC, transaksi, dan migrasi Flyway. |
| [`storage`](storage/README.md) | Penyimpanan dan pembacaan foto privat. |
| [`admin`](admin/README.md) | Template peninjauan admin untuk Ktor. API aktif peninjauan pickup berada di Go. |
| [`donations`](donations/README.md) | Template pengajuan dan riwayat donasi Ktor. |
| [`rewards`](rewards/README.md) | Template katalog dan redemption Ktor. |
