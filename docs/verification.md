# Hasil verifikasi Loopy

## Konsolidasi backend Go — 24 November 2026

Tes Go dengan PostgreSQL dan deteksi race lulus, begitu juga `go vet`, `go build`, pemeriksaan struktur, dan validasi Compose lokal serta LXC. Build APK debug dan instrumentasi, enam unit test Android, dan lint lulus pada commit konsolidasi. Android mempertahankan Gradle Wrapper untuk build aplikasi.

## Integrasi Android S05–AI–S06 — 24 November 2026

| Pemeriksaan | Hasil |
| --- | --- |
| Gradle `assembleDebug`, `testDebugUnitTest`, `lintDebug`, dan `assembleDebugAndroidTest` | APK debug dan APK tes berhasil dibangun. Unit test serta lint lulus. |
| Retrofit Android ke Go dan Groq | Uji JVM sementara dengan API Retrofit, repository sesi, fixture JPEG sintetis, dan database uji terpisah berhasil login, merotasi token melalui refresh, mengunggah foto, replay key, membaca detail, dan mengunduh JPEG privat. Token sebelum refresh ditolak dengan `401`. |
| Perangkat Android | Setelah memasang APK port 8082 dan mengaktifkan ADB reverse, tombol Periksa backend menampilkan `ok (go-auth)`. Request dari HP ke endpoint registrasi memperoleh respons validasi `422`. Pengguna kemudian mengonfirmasi alur aplikasi berjalan di HP. |

Base URL APK uji adalah `http://127.0.0.1:8082/` lewat ADB reverse. Sesi aplikasi uji disimpan di memori. [Panduan development](development.md#koneksi-android-ke-backend)

## Migrasi pemeriksaan pakaian ke Go — 24 November 2026

Diuji di Linux dengan Go 1.27.4, PostgreSQL 17.6, serta instance Compose dan database uji terpisah.

| Pemeriksaan | Hasil |
| --- | --- |
| `go test -race -count=1 ./...`, `go vet ./...`, dan `go build` | Lulus. Tes integrasi mengaktifkan PostgreSQL dan memverifikasi migrasi versi 2 ke versi 3, sesi lama, data pickup lama, akses pemilik/admin, serta retry setelah kegagalan provider. |
| Docker Compose lokal dan LXC | Kedua konfigurasi valid. Image Go berhasil dibangun dan layanan basis data serta API lokal lulus health check. |
| Login → upload → Groq langsung | Token dari `POST /v1/auth/login` diterima oleh `POST /v1/checks` (`201`) untuk ilustrasi JPEG kaos sintetis. Groq mengembalikan kategori, penjelasan, dan saran. |
| Replay dan foto privat | Upload yang diulang dengan key dan payload sama mendapat hasil lama (`200`). Token pemilik yang sama dapat membaca detail dan JPEG privat (`200`). |
| Privasi dan logout | Akun lain menerima `404` untuk detail dan foto. Tanpa token atau setelah logout, API mengembalikan `401`. Bytes yang tidak bisa didekode sebagai JPEG ditolak dengan `422 INVALID_PHOTO`. |

Uji HTTP nyata mencakup 16 pemeriksaan integrasi API dan akses sesi. Akurasi model belum dievaluasi. Hasil integrasi Android tercatat pada bagian tersendiri di atas.

## Riwayat penggabungan Go dan Ktor sebelum migrasi — 24 November 2026

Diuji di Linux dengan dua proyek Compose dan database terpisah.

| Pemeriksaan | Hasil |
| --- | --- |
| Android `assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest` | Lulus. Enam unit test lulus dan lint melaporkan 0 error. APK instrumentasi berhasil dibangun. |
| Ktor `test installDist` | Tujuh tes lulus, termasuk store dan route dengan PostgreSQL. Smoke test Groq opt-in dilewati pada run Gradle terkontrol. |
| Go `go test -race -count=1 ./...`, `go vet`, dan `go build` | Lulus dengan PostgreSQL khusus pengujian. |
| HTTP dari container aktual | 23 pemeriksaan sesuai harapan mencakup login Go, pencabutan sesi, akses admin, validasi upload, POST Groq, replay, dan privasi detail serta foto. |
| POST pemeriksaan + Groq | Sesi Ktor valid menerima `201`. Groq mengembalikan `NEEDS_RECHECK` untuk ilustrasi kaos sintetis. |
| Satu login untuk kedua API | Login Go berhasil dan token Go diterima di `/v1/auth/me`, tetapi GET dan POST pemeriksaan Ktor menjawab `401`. Sesi Ktor untuk tes HTTP dibuat sebagai fixture di database uji. |

Hasil ini mendokumentasikan baseline dua API sebelum migrasi Go dan integrasi Android.

## Verifikasi donasi dan jadwal pickup Go — 23 November 2026

Diverifikasi pada Windows amd64 dengan Go dan PostgreSQL 17 lokal yang dibuat khusus untuk pengujian. [Kontrak dan contoh pengujian manual](donation-review-go.md)

| Pemeriksaan | Hasil |
| --- | --- |
| `go test ./...`, `go vet ./...`, dan `go build -trimpath` | Lulus |
| `TEST_DATABASE_URL=... go test -count=1 -cover ./...` | Lulus dengan cakupan 75,7% statement, termasuk tes autentikasi yang sudah ada |
| Migrasi akun versi 1 ke versi 2 | Akun dan sesi yang sudah ada tetap bisa dipakai. Startup ulang tidak menerapkan ulang migrasi. |
| Akses, antrean, dan detail | Anonim mendapat 401, role USER mendapat 403, dan admin dapat membaca ringkasan, filter, pencarian, halaman lanjutan, serta detail. |
| Persetujuan dan penolakan | Jadwal WIB dikonversi ke UTC dengan benar. Tanggal lampau dan alasan kosong ditolak. Status, alasan, jadwal, dan pelaku tercatat. |
| Transaksi PostgreSQL | Dua keputusan bersamaan menghasilkan satu perubahan dan satu konflik. Kegagalan insert riwayat membatalkan perubahan pengajuan. |
| Binary HTTP dan contoh SQL | Health, login admin, fixture dokumentasi, antrean, penjadwalan, dan penolakan lulus. Jadwal serta alasan tetap tersedia setelah server dimulai ulang. |

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

Database dan server sementara dihentikan setelah pemeriksaan. CI Go menyediakan PostgreSQL dan menjalankan race detector. Pemeriksaan lokal ini memakai tes Go tanpa race detector karena toolchain C Windows belum dikonfigurasi. Lihat [status backend terkini](backend.md).

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

- Verifikasi produk lengkap, termasuk login serta pengiriman pemeriksaan dan foto dari Android ke backend, masih tertunda.
- Pickup dari donatur, penyelesaian donasi, poin, dan redemption masih dalam backlog.
- macOS/ARM belum diuji langsung; Windows telah diuji untuk backend Go di atas. Panduan dan batas tools Linux x86_64 ada di development.md.
- Sebelum release: lengkapi ikon, kebijakan ekstraksi/backup data, HTTPS deployment, auth/Keystore, UI final serta semua AC.
