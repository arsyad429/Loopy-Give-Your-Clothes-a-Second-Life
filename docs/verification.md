# Hasil verifikasi scaffold

## Backend auth Go — 17 November 2026

Diverifikasi pada Windows amd64 dengan Go 1.27.4 dan PostgreSQL 17 lokal sementara. Implementasi ada di `backend/go/`; alur, kode dan teori ada di [auth-go.md](auth-go.md). Ini bukti backend F02; acceptance criteria produk tetap memerlukan integrasi Android dan fitur bisnis.

| Pemeriksaan | Hasil |
| --- | --- |
| `go test ./...`, `go vet ./...` | Lulus |
| `TEST_DATABASE_URL=... go test -count=1 -cover ./...` | Lulus seluruh unit/integration test; cakupan 68,9% statement |
| PostgreSQL: register/login/me/refresh/logout | Lulus; token lama ditolak setelah rotasi/logout, sesi perangkat lain tetap aktif |
| PostgreSQL: concurrency/rollback/expiry | Lulus; satu pemenang email duplikat dan refresh bersamaan, insert user rollback saat session gagal, token kedaluwarsa ditolak |
| PostgreSQL: role dan identitas | Lulus; admin login mengembalikan ADMIN, guard menolak USER, perubahan role langsung berlaku, `/me` memakai akun pemilik token |
| `go build -trimpath` + HTTP server binary | Lulus pada localhost:18085; health/readiness, admin login, user register/me/refresh/logout/revocation |
| CLI `create-admin` | Lulus membuat admin baru, menolak email yang sudah ada |
| `govulncheck@v1.9.0 ./...` | Tidak ada vulnerability yang ditemukan pada toolchain/dependency final. Temuan awal pada Go 1.25.0 dan dependency lama diselesaikan dengan Go 1.27.4, pgx 5.13.0, crypto 0.59.0 dan dependency transitif yang diperbarui |
| Compose dan CI YAML | Berhasil diparse; Docker CLI tidak tersedia pada host ini sehingga image/Compose belum dijalankan ulang |
| Pemeriksaan struktur repository | Terhenti oleh README yang belum tersedia di folder lokal `.github/modernize/`; bukan kegagalan test auth |

Database dan server sementara dihentikan setelah pemeriksaan. CI Go menyediakan PostgreSQL dan menjalankan race detector; race detector belum dijalankan lokal karena toolchain C Windows belum dikonfigurasi. Android auth, endpoint bisnis, email verification, password recovery dan MFA belum diimplementasikan.

## Konfigurasi deployment LXC — 17 November 2026

Ditambahkan `compose.lxc.yaml`, `docker/lxc.env.example` dan [panduan LXC](deploy-lxc.md). Kedua file Compose lolos validasi schema resmi Compose; YAML CI, tautan lokal dan sintaks shell di panduan juga diperiksa. CI menambahkan pemeriksaan `docker compose -f compose.lxc.yaml config --quiet` dengan password khusus validasi. Belum ada koneksi atau deployment ke server LXC, dan Docker CLI tidak tersedia pada mesin kerja ini. Tidak ada perubahan kode bisnis/auth untuk penambahan deployment ini.

## Verifikasi scaffold sebelumnya — 16 November 2026

Diverifikasi pada Linux amd64, 16 November 2026. Hasil berikut adalah pengujian fondasi/template sebelum backend auth Go ditambahkan.

| Pemeriksaan | Hasil |
| --- | --- |
| Docker Android: assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest | Lulus, 88 task, 2 menit 33 detik pada source final |
| APK debug | artifacts/outputs/apk/debug/app-debug.apk, berhasil dikompilasi |
| Unit Android | 1 test DonationStatusTest, seluruh 25 pasangan status diperiksa, 0 gagal |
| Lint | 0 error; 21 warning, mayoritas pembaruan dependency baseline, ditambah launcher icon dan data extraction rules untuk tahap release |
| Instrumented test APK | Berhasil dikompilasi; belum dieksekusi pada emulator/perangkat |
| Backend: test installDist | Lulus; 1 HealthTest, 0 gagal |
| Docker backend dan PostgreSQL | Keduanya healthy; GET /health pada localhost:18080 menghasilkan status ok, mode template |
| Struktur | Lulus: 13 layar MVP + 4 perluasan desain, 10 materi PAPB, 20 AC, wrapper dan README setiap folder source |
| Compose dan shell | docker compose config --quiet dan sh -n scripts/android-container.sh lulus |
| Git diff | Tidak ada whitespace error; file build/local.properties/.env tidak dilacak |

SHA256 APK debug final: `8d8cd065e2a8691a857872e71a51bfd5a5e0f5b894b60c89e6c8f9ff7b4e3b10`.

## Kondisi lingkungan pengujian

Partisi sistem/Docker pada host penuh, sedangkan partisi workspace memiliki ruang. Pengujian Docker memakai bind mount cache Gradle, direktori sementara dan data PostgreSQL ke partisi workspace. JAVA_TOOL_OPTIONS mengarahkan user.home/tmp JVM pengujian ke mount sementara. Ini konfigurasi pengujian lokal di luar repo; tidak mengubah konfigurasi portable Compose. Port8080 dipakai layanan lain sehingga health check memakai API_PORT=18080. Layanan proyek lain tidak dihentikan.

Build awal sempat gagal karena disk host, cache Gradle yang dipakai bersamaan lintas container/host, dan README di resource Android. Source final menyaring Markdown dari resource melalui prepareMainResources dan script container memakai snapshot baru. Build final dijalankan dengan cache container terpisah dan lulus. Untuk development berikutnya, sediakan ruang Docker yang memadai; jangan prune data proyek lain.

## Yang belum dibuktikan/diimplementasikan

- Runtime UI, instrumented persistence test dan flow emulator/perangkat belum dijalankan.
- Visual final Figma belum diimplementasikan/diverifikasi; design context terkena kuota Starter, metadata dan referensi tersimpan.
- UI bisnis PRD, AI, transaksi poin/redeem dan isolasi data bisnis masih template; backend auth dan migrasi akun/sesi sudah tersedia di Go.
- macOS/ARM belum diuji langsung; Windows telah diuji untuk backend Go di atas. Panduan dan batas tools Linux x86_64 ada di development.md.
- Sebelum release: lengkapi ikon, kebijakan ekstraksi/backup data, HTTPS deployment, auth/Keystore, UI final serta semua AC.
