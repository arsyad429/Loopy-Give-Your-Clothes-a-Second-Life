# repository

`CompletionDemoRepository.kt` menangani transaksi Room demo E–F: kredit unik per donasi, validasi saldo ulang, idempotensi, serta riwayat penukaran. Data lokal ini terpisah dari AuthRepository dan ClothingCheckRepository yang menggunakan API Go.

Repository untuk autentikasi, pemeriksaan pakaian, dan demo fondasi aplikasi, beserta template akses data fitur berikutnya.

## Isi file dan perannya

| File atau folder | Kegunaan |
| --- | --- |
| [`AdminRepositoryImpl.kt`](AdminRepositoryImpl.kt) | TEMPLATE — TODO: implementasi AdminRepository, mapper DTO/domain, remote source, cache sesuai kebutuhan. dashboard; request list/detail; schedule/reject/status transition; server authorization; completed 10 poin sekali. Pertahankan CancellationException; expose error terstruktur tanpa data pribadi. |
| [`AuthRepositoryImpl.kt`](AuthRepositoryImpl.kt) | Login/register, sesi dalam memori, dan refresh yang diserialkan untuk request terproteksi. |
| [`ClothingCheckRepositoryImpl.kt`](ClothingCheckRepositoryImpl.kt) | Membaca URI foto, membuat multipart dan key retry, mengambil detail dan foto privat. |
| [`DonationRepositoryImpl.kt`](DonationRepositoryImpl.kt) | TEMPLATE — TODO: implementasi DonationRepository, mapper DTO/domain, remote source, cache sesuai kebutuhan. submit eligible check + konfirmasi bersih; observe/refresh daftar milik user; detail; status/history; idempotency submit. Pertahankan CancellationException; expose error terstruktur tanpa data pribadi. |
| [`FoundationRepository.kt`](FoundationRepository.kt) | Repository demo DataStore+Room+Retrofit, pintu akses ViewModel. |
| [`RewardRepositoryImpl.kt`](RewardRepositoryImpl.kt) | TEMPLATE — TODO: implementasi RewardRepository, mapper DTO/domain, remote source, cache sesuai kebutuhan. saldo + katalog; redeem transaction idempotent; history; jangan mutasi saldo lokal sebagai sumber kebenaran. Pertahankan CancellationException; expose error terstruktur tanpa data pribadi. |
