# Dokumentasi teknis Loopy

Mulai dengan [README utama](../README.md) untuk mencoba alur. Dokumen di sini memberi kontrak, rujukan, langkah deploy, serta catatan pengujian.

| Kebutuhan | Bahan yang dipakai |
| --- | --- |
| Mencari rute tampilan | [Peta layar](screen-map.md), [data pemetaan](screen-map.json), dan [rujukan PRD/Figma](reference/README.md). |
| Mengubah endpoint | [Kontrak API](api-contract.md) dan [backend Go](backend.md) menjelaskan rute, payload, dan dependensi. |
| Mengubah akun atau OTP | [Autentikasi Go](auth-go.md) berisi login, sesi, profil, OTP dan CLI admin. |
| Mengerjakan pickup | [Peninjauan dan jadwal](donation-review-go.md) memuat aturan peran, jadwal WIB, dan konflik perubahan status. |
| Mengerjakan poin atau rewards | [Penyelesaian dan redeem](completion-rewards.md) memuat transaksi, idempotensi, dan riwayat implementasi. |
| Mencari data dan arsitektur | [Model data](data-model.md) serta [catatan arsitektur](architecture.md). Schema yang berjalan tercatat di [migrasi Go](../backend/go/migrations/README.md). |
| Menjalankan VM atau APK | [Deploy LXC](deploy-lxc.md) mencakup SMTP, admin, TLS, upgrade dan backup. [Development lintas OS](development.md) mencakup Gradle dan koneksi perangkat. |
| Menyiapkan demo atau review | [Kriteria penerimaan](acceptance-tests.md), [hasil verifikasi](verification.md), dan [backlog](backlog.md). |
| Melacak materi perkuliahan | [Pemetaan PAPB](papb-mapping.md) dan [katalog referensi](reference/papb-catalog.json). |

## Jalur baca untuk tim

1. Pengembang Android dapat membuka [peta layar](screen-map.md), mencari rute serta ViewModel, lalu menelusuri request pada [kontrak API](api-contract.md) dan [arsitektur](architecture.md).
2. Pengembang backend dapat mulai dari [bootstrap dan handler](backend.md), lalu melihat field, constraint, dan transaksi dalam [model data](data-model.md).
3. Penanggung jawab VM dapat memakai [panduan deploy](deploy-lxc.md) untuk memasang konfigurasi, memastikan container membacanya, dan menjalankan smoke test dari perangkat.
4. Reviewer dapat memilih skenario di [acceptance tests](acceptance-tests.md), mencatat hasil sesuai commit dan perangkat, lalu memeriksa [riwayat verifikasi](verification.md) untuk bukti yang sudah tersedia.

## Saat mengubah fitur

| Perubahan | Dokumen yang perlu dicocokkan |
| --- | --- |
| Rute Compose atau state layar | Peta layar, arsitektur, dan skenario penerimaan terkait. |
| Path, field JSON, status HTTP, atau hak akses | Kontrak API dan panduan alur terkait. |
| Schema, status transaksi, atau idempotensi | Model data, README migrasi, kontrak dan tes rollback/retry. |
| Environment, port, atau prosedur update | Panduan deployment, setup perangkat, dan langkah diagnosis. |
| Hasil uji baru | Catat commit, metode, environment, perangkat, dan hasil di riwayat verifikasi. |

Rujukan PRD dan desain tetap tersedia di [reference](reference/README.md). Perubahan implementasi dapat dilacak melalui lampiran dan dokumen teknis tanpa mengubah kriteria produk yang sudah tertulis.
