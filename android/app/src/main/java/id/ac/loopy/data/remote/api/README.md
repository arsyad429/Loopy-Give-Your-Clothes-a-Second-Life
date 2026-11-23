# api

Retrofit suspend API dan DTO. Health aktif; endpoint bisnis mengikuti docs/api-contract.md.

## Isi file dan perannya

| File atau folder | Kegunaan |
| --- | --- |
| [`AdminApi.kt`](AdminApi.kt) | TEMPLATE — TODO: Retrofit suspend endpoints Admin, lihat docs/api-contract.md. dashboard; request list/detail; schedule/reject/status transition; server authorization; completed 10 poin sekali. |
| [`AuthApi.kt`](AuthApi.kt) | TEMPLATE — TODO: Retrofit suspend endpoints Auth, lihat docs/api-contract.md. register/login/logout/me; backend memverifikasi role; lindungi token dengan Android Keystore; hapus cache per user saat logout. |
| [`ClothingCheckApi.kt`](ClothingCheckApi.kt) | TEMPLATE — TODO: Retrofit suspend endpoints ClothingCheck, lihat docs/api-contract.md. validasi foto; upload multipart; analisis backend; ambil result dengan checkId milik user; jangan menaruh AI API key di APK. |
| [`DonationApi.kt`](DonationApi.kt) | TEMPLATE — TODO: Retrofit suspend endpoints Donation, lihat docs/api-contract.md. submit eligible check + konfirmasi bersih; observe/refresh daftar milik user; detail; status/history; idempotency submit. |
| [`HealthApi.kt`](HealthApi.kt) | Retrofit suspend GET /health. |
| [`RewardApi.kt`](RewardApi.kt) | TEMPLATE — TODO: Retrofit suspend endpoints Reward, lihat docs/api-contract.md. saldo + katalog; redeem transaction idempotent; history; jangan mutasi saldo lokal sebagai sumber kebenaran. |

## TODO

- [ ] Lanjutkan file template sesuai docs/backlog.md, docs/api-contract.md dan docs/acceptance-tests.md.
- [ ] Pertahankan UI → ViewModel → Repository → Data Source dan konfigurasi yang dapat dibuild.
- [ ] Perbarui daftar file dan bukti cek setelah implementasi berubah.
