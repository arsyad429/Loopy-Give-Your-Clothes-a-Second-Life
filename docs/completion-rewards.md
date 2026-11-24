# Penyelesaian donasi dan penukaran reward

Implementasi PRD 6.5–6.6 dan flowchart E–F mencakup konfirmasi penjemputan, penyelesaian donasi dengan 10 poin satu kali, katalog reward, konfirmasi penukaran, debit poin, dan riwayat penukaran. Frontend Kotlin Compose berjalan dalam mode demo lokal dengan Room. Backend Go menyediakan API PostgreSQL yang terpisah dan siap diintegrasikan. Koneksi frontend ke API dan deployment VM belum dikerjakan.

## Mencoba frontend tanpa VM

1. Buka folder `android/` di Android Studio dan jalankan varian **debug**.
2. Menu pratinjau menampilkan **Demo E–F · Admin penyelesaian donasi & poin**. Buka pengajuan **LP021** yang sudah dijadwalkan.
3. Tekan **Konfirmasi sudah dijemput** dan konfirmasi dialog. Status menjadi Dijemput, saldo tetap 20 poin.
4. Tekan **Selesaikan donasi (+10 poin)** setelah penerimaan dikonfirmasi. Status menjadi Selesai dan saldo menjadi 30 poin. Pengajuan yang selesai tidak memiliki tombol pemberian poin ulang.
5. Kembali ke menu pratinjau dan buka **S10 · Rewards & My Rewards**. Pilih reward. Membatalkan dialog tidak mengubah saldo; konfirmasi mengurangi poin sesuai biaya dan membuka tab Reward-ku.
6. Buka Beranda dan Donasiku melalui navigasi bawah untuk melihat saldo serta status yang sama. Tutup dan buka ulang aplikasi untuk memeriksa persistensi.

Data awal merupakan ilustrasi: LP018 dan LP017 masing-masing memiliki satu transaksi perolehan 10 poin. LP021 disediakan sebagai pengajuan terjadwal untuk pengujian penyelesaian. LP024 masih menunggu verifikasi. Data ini tidak menciptakan akun atau saldo server. Database `loopy-completion-demo.db` terpisah dari database draft `loopy.db` dan hanya dibuka pada build debug. Mode ini tidak menyimulasikan seluruh proses autentikasi, pengajuan pickup, atau sinkronisasi.

Frontend mengikuti Figma S04 `2:87`, S08 `2:93`, S09 `2:94`, S10 `2:95`, dialog `6:674`, dan Reward-ku `6:734`. Font Plus Jakarta Sans menggunakan resource lokal, termasuk bobot ExtraBold 800. Ikon asli Figma disimpan di `assets/figma_rewards/` dan dirender ke PNG xxxhdpi. Tidak ada URL aset sementara saat runtime. Konten memakai layout responsif dan scroll, tanpa menyalin status bar sistem dari mockup.

## Kode frontend

| Lokasi | Peran |
| --- | --- |
| `data/local/CompletionDemoDatabase.kt` | Entitas, DAO, dan database Room demo: donasi, ledger poin, dan redemption. |
| `data/repository/CompletionDemoRepository.kt` | Transaksi lokal: validasi urutan status, kredit unik per donasi, validasi saldo ulang, dan idempotensi penukaran. |
| `feature/rewards/CompletionViewModel.kt` | StateFlow bersama, Coroutines, status loading/busy/error/sukses, dan pencegahan klik berulang. |
| `feature/rewards/RewardsScreen.kt` | Katalog, saldo, dialog konfirmasi, tombol saldo tidak cukup, dan Reward-ku. |
| `feature/rewards/CompletionAdminScreen.kt` | Daftar demo admin; detail memakai komponen verifikasi yang sudah ada. |
| `feature/home/`, `feature/donations/` | Saldo Home, daftar donasi, dan detail timeline untuk alur penyelesaian. |
| `ui/components/CompletionComponents.kt` | Kartu, ikon, saldo, feedback, dan navigasi bawah yang dipakai bersama. |

Semua lokasi pada tabel relatif terhadap `android/app/src/main/java/id/ac/loopy/`. Pada integrasi berikutnya, data akun aktif dan hasil API menggantikan data demo; jangan mengunggah ledger/saldo demo menjadi saldo server. Kredit poin dan keputusan admin tetap divalidasi server.

## Backend API

Jalankan backend lokal sesuai [panduan backend](backend.md). PostgreSQL diperlukan; VM tidak diperlukan. Setelah penggabungan dengan main, Vision tetap memakai migrasi versi 3, sedangkan katalog reward, snapshot redemption, dan ledger poin memakai versi 4. Jika database development pernah menjalankan versi 3 reward dari branch lama, migrasi mengenali tabelnya, memindahkan catatan versinya ke 4, lalu menerapkan Vision versi 3 tanpa menghapus saldo atau riwayat. Migrasi tidak memberikan poin retroaktif untuk donasi lama yang sudah selesai sebelum fitur ini tersedia.

| Endpoint | Akses dan perilaku |
| --- | --- |
| `POST /v1/admin/donations/{id}/status` | ADMIN; body `{"targetStatus":"PICKED_UP","expectedVersion":2}` atau `{"targetStatus":"COMPLETED","expectedVersion":3}`. |
| `GET /v1/donations` | USER; daftar hanya milik akun pada token; filter/pagination sama dengan antrean admin. |
| `GET /v1/donations/{id}` | USER; detail dan riwayat hanya milik akun pada token. Akun lain mendapat 404. |
| `GET /v1/points/balance` | USER; `balance` dan `updatedAt`. |
| `GET /v1/rewards` | USER; `items` katalog aktif dengan `rewardId`, `name`, `description`, `icon`, `requiredPoints`, `type`, `isActive`. |
| `POST /v1/redemptions` | USER; body `{"rewardId":"eco-supporter-badge"}` dan satu header `Idempotency-Key`. |
| `GET /v1/redemptions` | USER; `items` riwayat sendiri dengan snapshot nama, deskripsi, ikon, tipe, biaya, dan waktu penukaran. |

ID reward awal: `eco-supporter-badge` (10), `loopy-digital-wallpaper` (20), dan `voucher-demo` (30). Voucher berlabel simulasi; tidak ada integrasi merchant atau transaksi uang. Reward digital pada tahap ini dicatat dan ditampilkan dalam riwayat; pengunduhan berkas wallpaper belum disediakan.

Penukaran pertama mengembalikan 201 dengan `redemption`, `remainingBalance`, dan `replayed:false`. Pengulangan key yang sama untuk reward yang sama mengembalikan 200 dan redemption yang sama dengan `replayed:true`; `remainingBalance` pada replay adalah saldo setelah transaksi aslinya, sehingga klien perlu membaca saldo terkini bila ada transaksi lain. Key yang sama untuk reward berbeda menghasilkan 409 `IDEMPOTENCY_CONFLICT`. Gunakan 8–128 huruf ASCII, angka, underscore, atau tanda hubung; UUID cocok. Key dipertahankan untuk retry satu transaksi dan diganti untuk penukaran baru yang disengaja.

Urutan wajib adalah `PICKUP_SCHEDULED → PICKED_UP → COMPLETED`. Hanya transisi terakhir memberikan 10 poin per pengajuan, berapa pun jumlah pakaian. Backend mengambil pemilik donasi, harga reward, role, dan saldo dari database. `expectedVersion` yang basi menghasilkan 409 `VERSION_CONFLICT`; skip/reverse/final menghasilkan 409 `INVALID_TRANSITION`. Saldo kurang menghasilkan 409 `INSUFFICIENT_POINTS`, reward nonaktif/tidak ditemukan menghasilkan 404 `REWARD_NOT_FOUND`.

Penyelesaian mengunci pengajuan dan menyimpan status, riwayat, ledger, serta saldo dalam satu transaksi. Ledger memiliki `UNIQUE(donation_id)`. Penukaran mengunci akun, memeriksa idempotency dan harga aktif, lalu menyimpan snapshot redemption, debit ledger, dan saldo dalam satu transaksi. Kegagalan salah satu penulisan membatalkan semuanya. Dua key penukaran berbeda tetap tidak dapat menggunakan saldo yang sama secara bersamaan.

## Verifikasi

Backend: set `TEST_DATABASE_URL` ke PostgreSQL khusus pengujian, lalu jalankan `go test -count=1 -cover ./...`, `go vet ./...`, dan `go build ./...` dari `backend/go/`. `rewards_integration_test.go` memeriksa alur HTTP, role/ownership, urutan status, kredit sekali, replay/conflict, saldo kurang, konkurensi, dan rollback PostgreSQL.

Android: jalankan `./gradlew assembleDebug testDebugUnitTest lintDebug` serta `./gradlew connectedDebugAndroidTest`. Pada PowerShell gunakan `.\gradlew.bat`. `CompletionPersistenceTest` memeriksa kredit sekali, idempotensi, saldo kurang, rollback Room, dan tutup/buka database. `CompletionFlowTest` memeriksa navigasi admin → penyelesaian → batal/konfirmasi redeem → Reward-ku → saldo Home → detail donasi. Screenshot disimpan pada external files aplikasi dan disalin ke `/sdcard/Download/completion-*.png` pada emulator pengujian agar tetap tersedia setelah runner membersihkan aplikasi. Salinan lokal hasil QA berada di `artifacts/completion-rewards/` (diabaikan Git).

Status integrasi akun/API dan pembuktian hasil aktual dicatat pada [verification.md](verification.md). Pengujian lokal ini tidak membuktikan koneksi VM, HTTPS deployment, atau sinkronisasi cloud.
