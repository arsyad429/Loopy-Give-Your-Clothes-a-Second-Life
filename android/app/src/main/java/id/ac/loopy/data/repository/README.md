# repository

`CompletionDemoRepository.kt` menangani transaksi Room untuk PRD 6.5–6.6, seed data ilustrasi, kredit unik per donasi, validasi saldo ulang, dan idempotensi redemption. Hanya untuk demo debug; repository API produksi yang masih berupa TEMPLATE belum dihubungkan.

Implementasi akses data melalui repository. FoundationRepository aktif; repository fitur masih template.

## Isi file dan perannya

| File/folder | Peran dan isi yang diharapkan |
| --- | --- |
| `AdminRepositoryImpl.kt` | TEMPLATE — TODO: implementasi AdminRepository, mapper DTO/domain, remote source, cache sesuai kebutuhan. dashboard; request list/detail; schedule/reject/status transition; server authorization; completed 10 poin sekali. Pertahankan CancellationException; expose error terstruktur tanpa data pribadi. |
| `AuthRepositoryImpl.kt` | TEMPLATE — TODO: implementasi AuthRepository, mapper DTO/domain, remote source, cache sesuai kebutuhan. register/login/logout/me; backend memverifikasi role; lindungi token dengan Android Keystore; hapus cache per user saat logout. Pertahankan CancellationException; expose error terstruktur tanpa data pribadi. |
| `ClothingCheckRepositoryImpl.kt` | TEMPLATE — TODO: implementasi ClothingCheckRepository, mapper DTO/domain, remote source, cache sesuai kebutuhan. validasi foto; upload multipart; analisis backend; ambil result dengan checkId milik user; jangan menaruh AI API key di APK. Pertahankan CancellationException; expose error terstruktur tanpa data pribadi. |
| `DonationRepositoryImpl.kt` | TEMPLATE — TODO: implementasi DonationRepository, mapper DTO/domain, remote source, cache sesuai kebutuhan. submit eligible check + konfirmasi bersih; observe/refresh daftar milik user; detail; status/history; idempotency submit. Pertahankan CancellationException; expose error terstruktur tanpa data pribadi. |
| `FoundationRepository.kt` | Repository demo DataStore+Room+Retrofit, pintu akses ViewModel. |
| `RewardRepositoryImpl.kt` | TEMPLATE — TODO: implementasi RewardRepository, mapper DTO/domain, remote source, cache sesuai kebutuhan. saldo + katalog; redeem transaction idempotent; history; jangan mutasi saldo lokal sebagai sumber kebenaran. Pertahankan CancellationException; expose error terstruktur tanpa data pribadi. |

## TODO

- [ ] Lanjutkan file template sesuai docs/backlog.md, docs/api-contract.md dan docs/acceptance-tests.md.
- [ ] Pertahankan UI → ViewModel → Repository → Data Source dan konfigurasi yang dapat dibuild.
- [ ] Perbarui daftar file dan bukti cek setelah implementasi berubah.
