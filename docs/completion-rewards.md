# Penyelesaian donasi dan penukaran reward

Alur PRD 6.5–6.6 dan flowchart E–F menggunakan Android Kotlin/Compose → API Go → PostgreSQL. Admin mengonfirmasi penjemputan dan penerimaan; server memberi 10 poin satu kali setelah donasi selesai. Pengguna membaca saldo, memilih katalog server, mengonfirmasi penukaran, dan melihat riwayat akun sendiri.

## Menjalankan

1. Jalankan backend dan PostgreSQL sesuai [panduan backend](backend.md), atau gunakan server tim yang sudah aktif.
2. Buka folder `android/` di Android Studio. Atur `LOOPY_API_BASE_URL` di Gradle properties ke base URL server, diakhiri `/`. Default debug `http://10.0.2.2:8080/` mengarah ke port 8080 komputer dari emulator. Contoh bila API Docker memakai port 8082: `LOOPY_API_BASE_URL=http://10.0.2.2:8082/`.
3. Build ulang dan jalankan aplikasi. Aplikasi membuka landing/login, lalu dashboard sesuai peran akun.
4. Login sebagai admin. Pada donasi yang sudah dijadwalkan, konfirmasikan `PICKED_UP`, lalu `COMPLETED` setelah penerimaan benar-benar dikonfirmasi.
5. Login sebagai pemilik donasi. Home dan Rewards mengambil saldo server. Pilih reward, batalkan atau konfirmasikan penukaran, lalu lihat tab **Reward-ku**.
6. Tombol **Muat ulang** mengambil saldo, katalog, dan riwayat terbaru. Home dan Rewards juga memperbarui data ketika layar kembali aktif.

Backend diperlukan. VM tidak wajib untuk pengembangan jika Go/PostgreSQL berjalan lokal. Kode menggunakan konfigurasi URL dan sesi login yang sama dengan fitur lain; tidak ada URL VM atau token yang ditanam dalam kode.

## Implementasi Android

Semua lokasi berikut relatif terhadap `android/app/src/main/java/id/ac/loopy/`.

| Lokasi | Peran |
| --- | --- |
| `data/remote/api/RewardApi.kt` | Retrofit untuk saldo, katalog, penukaran dan riwayat. |
| `data/remote/dto/RewardDto.kt`, `data/mapper/RewardMapper.kt` | Kontrak JSON dan pemetaan domain. |
| `data/repository/RewardRepositoryImpl.kt` | Bearer dari akun aktif dan refresh token lewat AuthRepository. |
| `feature/rewards/RewardsViewModel.kt` | Loading/error/sukses, pembatalan saat akun berubah, pencegahan klik berulang, dan penyimpanan kunci percobaan penukaran di SavedStateHandle. |
| `feature/rewards/RewardsScreen.kt` | Saldo, katalog server, konfirmasi/batal, saldo kurang, riwayat, dan pemeriksaan permintaan tertunda. |
| `feature/admin/` | Peninjauan, jadwal, dan perubahan status melalui API admin yang sudah terintegrasi. |
| `feature/home/`, `feature/donations/` | Ringkasan saldo serta daftar/detail donasi melalui API. |

Nama, biaya, status aktif, dan riwayat reward berasal dari server. Saldo tidak dikurangi secara optimistis di HP. Setelah POST berhasil, aplikasi membaca saldo terkini; saldo pada respons replay tidak dipakai sebagai saldo terbaru. Jika pembaruan gagal setelah POST berhasil, UI tetap menyatakan penukaran berhasil dan meminta muat ulang.

Respons jaringan yang hilang atau HTTP 5xx meninggalkan permintaan tertunda. Tombol pemeriksaan mengulangi reward dan Idempotency-Key yang sama; penukaran lain diblokir sampai hasilnya jelas. Kunci bertahan selama ViewModel/navigasi dan pemulihan SavedState Android tersedia. Ini bukan penyimpanan permanen lintas uninstall/force-stop. Riwayat transaksi tetap disimpan server.

Demo E–F, database Room demo, route admin contoh, dan overload layar berbasis data contoh telah dihapus. Database demo yang mungkin pernah dibuat di HP tidak dibaca atau diunggah. Room untuk draft penjemputan dan pengaturan lokal tetap terpisah dari transaksi server. Font dan ikon Figma lokal tetap digunakan.

## Endpoint

| Endpoint | Akses |
| --- | --- |
| `POST /v1/admin/donations/{id}/status` | ADMIN; `targetStatus` PICKED_UP/COMPLETED dan `expectedVersion`. |
| `GET /v1/donations`, `GET /v1/donations/{id}` | USER; hanya donasi pemilik token. |
| `GET /v1/points/balance` | USER; saldo ledger server. |
| `GET /v1/rewards` | USER; katalog aktif. |
| `POST /v1/redemptions` | USER; body `{ "rewardId": "..." }` dan header `Idempotency-Key`. |
| `GET /v1/redemptions` | USER; riwayat snapshot reward akun sendiri. |

Server memvalidasi peran, versi status, ketersediaan reward, harga dan saldo. Kredit donasi serta debit penukaran bersifat transaksional/idempoten. Lihat [kontrak API](api-contract.md). Migrasi Vision tetap versi 3 dan rewards versi 4; kompatibilitas database branch lama dipertahankan.

Katalog awal server masih mencakup badge 10 poin, wallpaper 20 poin dan voucher simulasi 30 poin sesuai kontrak produk. Integrasi merchant serta pengunduhan berkas wallpaper belum tersedia; koneksi API tidak mengubah batasan hadiah tersebut.

## Verifikasi

Dari `android/`, jalankan `./gradlew assembleDebug testDebugUnitTest lintDebug`. Gunakan emulator atau perangkat uji khusus untuk `connectedDebugAndroidTest`. [Instrumentasi Android](../android/app/src/androidTest/README.md)

- `RewardRepositoryHttpTest`: request Retrofit sungguhan ke server HTTP pengujian lokal; JSON, Bearer/refresh, path endpoint, body dan Idempotency-Key.
- `RewardsViewModelTest`: retry respons tidak pasti, pemulihan kunci, replay, saldo terbaru, saldo kurang, kegagalan refresh setelah sukses, dan isolasi pergantian akun.
- `RewardsScreenTest`: saldo kurang, batal tanpa POST, konfirmasi dan riwayat/saldo hasil repository. Fixture hanya ada di source test.
- Backend: `TEST_DATABASE_URL=... go test ./...` dengan PostgreSQL pengujian.

Hasil aktual dicatat pada [verification.md](verification.md). Pengujian lokal tidak membuktikan deployment atau keterjangkauan VM tim.

## Menelusuri kredit dan debit

| Tahap | Perubahan server | Yang dicek di aplikasi |
| --- | --- | --- |
| Admin menetapkan jadwal dan menandai dijemput | Versi dan riwayat meningkat. Saldo belum bertambah. | Detail donatur memperlihatkan jadwal final dan progres. |
| Admin menyelesaikan pickup | PostgreSQL menulis status akhir, versi, riwayat, satu kredit +10, dan saldo dalam transaksi. | Saldo Beranda dan Rewards meningkat 10 setelah refresh. |
| Donatur memilih dan membatalkan reward | Belum ada request mutasi. | Saldo serta riwayat sama. |
| Donatur mengonfirmasi reward aktif | PostgreSQL mengunci saldo pemilik, cek key dan biaya, lalu menulis snapshot, debit, dan saldo baru. | Satu item riwayat serta saldo terbaru. |
| Respons mutasi hilang setelah commit | Server bisa membaca snapshot transaksi berdasarkan key dan akun. | Retry menggunakan key yang sama. Riwayat tetap satu transaksi. |

## Contoh memeriksa redeem dengan API

Gunakan akun uji yang punya minimal 10 poin, `LOOPY_BASE`, dan `DONOR_ACCESS_TOKEN` dari sesi akun. Contoh mendebit badge satu kali pada akun tes.

```sh
REDEEM_KEY="$(openssl rand -hex 16)"
curl -sS -i "$LOOPY_BASE/v1/redemptions" \
  -H "Authorization: Bearer $DONOR_ACCESS_TOKEN" \
  -H "Idempotency-Key: $REDEEM_KEY" \
  -H 'Content-Type: application/json' \
  -d '{"rewardId":"eco-supporter-badge"}'
```

Request pertama yang berhasil memberi `201` dengan `replayed=false`. Jalankan ulang request dengan variabel `REDEEM_KEY` yang sama untuk memperoleh `200` dan `replayed=true`. `remainingBalance` pada replay adalah snapshot setelah transaksi tersimpan. Baca `/v1/points/balance` untuk saldo saat ini dan `/v1/redemptions` untuk riwayat. Key yang sama dengan reward lain memberi `409 IDEMPOTENCY_CONFLICT`. [Kode transaksi Go](../backend/go/internal/store/rewards_store.go) · [Model ledger](data-model.md#poin-dan-penukaran)
