# api

Deklarasi endpoint Retrofit untuk health, sesi, dan pemeriksaan yang disediakan oleh Go.

## Isi file dan perannya

| File atau folder | Kegunaan |
| --- | --- |
| [`AdminApi.kt`](AdminApi.kt) | TEMPLATE — TODO: Retrofit suspend endpoints Admin, lihat docs/api-contract.md. dashboard; request list/detail; schedule/reject/status transition; server authorization; completed 10 poin sekali. |
| [`AuthApi.kt`](AuthApi.kt) | Login, register, dan refresh sesi pada Go. |
| [`ClothingCheckApi.kt`](ClothingCheckApi.kt) | Upload multipart, detail berdasarkan ID, dan unduhan JPEG privat dengan bearer token. |
| [`DonationApi.kt`](DonationApi.kt) | TEMPLATE — TODO: Retrofit suspend endpoints Donation, lihat docs/api-contract.md. submit eligible check + konfirmasi bersih; observe/refresh daftar milik user; detail; status/history; idempotency submit. |
| [`HealthApi.kt`](HealthApi.kt) | Retrofit suspend GET /health. |
| [`RewardApi.kt`](RewardApi.kt) | TEMPLATE — TODO: Retrofit suspend endpoints Reward, lihat docs/api-contract.md. saldo + katalog; redeem transaction idempotent; history; jangan mutasi saldo lokal sebagai sumber kebenaran. |
