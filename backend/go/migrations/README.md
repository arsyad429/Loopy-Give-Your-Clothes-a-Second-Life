# Migrasi PostgreSQL Loopy

Berisi skrip DDL berurutan yang di-embed ke binary Go melalui `embed.go`:

1. `001_accounts_and_sessions.sql`: Tabel akun (`users`) dan sesi token (`auth_sessions`).
2. `002_pickup_and_donations.sql`: Tabel pemeriksaan pakaian (`clothing_checks`), pengajuan donasi (`donation_requests`), dan audit status (`donation_status_history`).
3. `003_vision_and_private_photos.sql`: Foto privat (`clothing_check_photos`), rekomendasi aksi AI, dan idempotensi pemeriksaan (`clothing_check_requests`).
4. `004_rewards_and_ledger.sql`: Katalog reward (`rewards`), penukaran (`redemptions`), dan ledger poin (`point_transactions`).
5. `005_donation_submit_idempotency.sql`: Idempotensi pengajuan pickup donatur (`donation_submit_requests`).
