# Migrasi PostgreSQL

[embed.go](embed.go) menyertakan SQL dalam binary dan mendaftarkan urutannya. Server menjalankan migrasi saat startup. Tambahkan file bernomor berikutnya beserta entri di `embed.go` saat mengubah schema yang sudah dipakai.

| Migrasi | Perubahan |
| --- | --- |
| [001](001_accounts_and_sessions.sql) | Akun, peran, hash kata sandi, dan sesi. |
| [002](002_pickup_and_donations.sql) | Pemeriksaan, pengajuan pickup, serta riwayat status. |
| [003](003_vision_and_private_photos.sql) | Foto privat dan reservasi idempoten pemeriksaan. |
| [004](004_rewards_and_ledger.sql) | Katalog rewards, transaksi poin, dan riwayat redeem. |
| [005](005_donation_submit_idempotency.sql) | Pemetaan idempotensi submit pickup. |
| [006](006_account_profiles.sql) | Username unik opsional dan status verifikasi email. |
| [007](007_email_registration.sql) | Challenge pendaftaran sementara dan digest OTP. |

Jalankan tes integrasi terhadap schema baru dan data yang sudah memakai versi sebelumnya. [Runner migrasi](../internal/store/postgres.go) · [Backup dan update VM](../../../docs/deploy-lxc.md#8-updates-and-backups)
