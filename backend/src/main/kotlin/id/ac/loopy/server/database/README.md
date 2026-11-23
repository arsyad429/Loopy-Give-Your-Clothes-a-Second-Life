# PostgreSQL untuk API Ktor

Database Ktor terpisah dari database API Go. Aplikasi menerapkan migrasi sebelum membuka listener HTTP.

| File atau folder | Kegunaan |
| --- | --- |
| [`DatabaseFactory.kt`](DatabaseFactory.kt) | Pool HikariCP dari environment dan helper JDBC di Dispatchers.IO dengan commit dan rollback. |
| [`Migrations.kt`](Migrations.kt) | Menjalankan Flyway dari resources/db/migration saat startup. |
| [`Tables.kt`](Tables.kt) | Template model tabel untuk fitur Ktor lanjutan. Pemeriksaan saat ini menjalankan SQL langsung melalui JDBC. |
