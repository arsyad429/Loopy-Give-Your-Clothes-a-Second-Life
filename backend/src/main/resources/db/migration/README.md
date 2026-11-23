# Migrasi Flyway Ktor

Flyway mencatat versi migrasi yang berhasil di flyway_schema_history. Perubahan skema berikutnya masuk lewat file versi baru.

| File atau folder | Kegunaan |
| --- | --- |
| [`V001__initial_schema.sql`](V001__initial_schema.sql) | Membuat tabel users, user_sessions, clothing_checks, clothing_check_photos, dan clothing_check_requests beserta batasan dan indeksnya. |
