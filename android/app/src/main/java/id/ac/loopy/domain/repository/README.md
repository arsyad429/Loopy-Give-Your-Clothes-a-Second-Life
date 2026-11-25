# repository

Kontrak domain repository. ViewModel S05 dan S06 memakai ClothingCheckRepository untuk meminta analisis dan mengambil detail.

## Isi file dan perannya

| File atau folder | Kegunaan |
| --- | --- |
| [`AdminRepository.kt`](AdminRepository.kt) | TEMPLATE — TODO: interface AdminRepository dengan suspend one-shot dan Flow untuk observasi. Kontrak: dashboard; request list/detail; schedule/reject/status transition; server authorization; completed 10 poin sekali. |
| [`AuthRepository.kt`](AuthRepository.kt) | TEMPLATE — TODO: interface AuthRepository dengan suspend one-shot dan Flow untuk observasi. Kontrak: register/login/logout/me; backend memverifikasi role; lindungi token dengan Android Keystore; hapus cache per user saat logout. |
| [`ClothingCheckRepository.kt`](ClothingCheckRepository.kt) | Kontrak suspend untuk analisis foto dan pembacaan hasil, serta Flow observasi cache berdasarkan checkId. |
| [`DonationRepository.kt`](DonationRepository.kt) | TEMPLATE — TODO: interface DonationRepository dengan suspend one-shot dan Flow untuk observasi. Kontrak: submit eligible check + konfirmasi bersih; observe/refresh daftar milik user; detail; status/history; idempotency submit. |
| [`RewardRepository.kt`](RewardRepository.kt) | Kontrak sesi aktif, pembacaan saldo/katalog/riwayat dan penukaran dengan request key. |
