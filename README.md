# Loopy — Give Your Clothes a Second Life! ♻️

Template aplikasi donasi pakaian **Kotlin + Jetpack Compose** untuk **Android Studio**, berdasarkan [PRD 2.1](https://hackmd.io/@Nepa/H1x82bUsze) dan [desain Loopy](https://www.figma.com/design/ZDndr4gOItY7elDq9w7YA3/Loopy?node-id=0-1). Dokumentasi dan TODO berbahasa Indonesia.

**Status: fondasi Android dan backend Go tersedia, belum MVP selesai.** Backend PostgreSQL menyediakan autentikasi, peninjauan donasi, penyelesaian dengan 10 poin satu kali, katalog reward, serta penukaran dan riwayat. [Alur penyelesaian donasi dan reward](docs/completion-rewards.md) dapat dicoba di Android melalui demo lokal Room pada build debug. UI login dan integrasi frontend ke API bisnis belum terhubung; deployment VM ditunda. AI dan pengajuan pickup pengguna masih perlu dilengkapi. Hasil pemeriksaan aktual ada di [verification.md](docs/verification.md).

## Mulai pengembangan dengan Docker

Prasyarat: Git, Docker Engine + Compose v2 atau Docker Desktop dengan Linux containers. Android Studio dan emulator/perangkat Android dipasang di host. Build pertama memerlukan internet dan ruang disk untuk SDK, image, dan dependency.

Jalankan dari **root repository** yang berisi `compose.yaml`:

```sh
# Linux/macOS/Git Bash
cp .env.example .env
docker compose up --build -d --wait
docker compose --profile tools build android-dev
docker compose --profile tools run --rm android-dev assembleDebug testDebugUnitTest lintDebug
```

PowerShell: ganti perintah pertama dengan `Copy-Item .env.example .env`; perintah Docker sama. Ganti password lokal di `.env` bila perlu; file ini diabaikan Git. Default hanya untuk development lokal.

Backend Go melayani autentikasi dan keputusan admin untuk donasi. `/health` tersedia di `http://localhost:8080` dan `/ready` memeriksa PostgreSQL. Migrasi akun, sesi, pengajuan, dan riwayat berjalan saat startup. APK Docker tersedia di `artifacts/outputs/apk/debug/app-debug.apk` dan laporan di `artifacts/reports/`. Emulator Android Studio menggunakan `http://10.0.2.2:8080/`. [Panduan backend](docs/backend.md)

Untuk menjalankan API di server LXC, ikuti [panduan deployment LXC](docs/deploy-lxc.md) dan gunakan `compose.lxc.yaml` (API + PostgreSQL, tanpa build Android).

## Buka project Android Studio

1. Pilih **Open** → folder **`android/`**, tanpa membuat project baru.
2. Gunakan **Gradle JDK 17**, Android Studio yang mendukung AGP 8.13.2. SDK Manager: **Android SDK Platform 36** dan **Build Tools 36.0.0**. Wrapper memilih Gradle 8.13.
3. Tunggu Gradle Sync. Android Studio membuat `local.properties` berisi lokasi SDK mesin Anda; jangan commit file ini. CLI host dapat memakai `sdk.dir` atau `ANDROID_HOME`.
4. Pilih modul/konfigurasi **app**, varian **debug**, lalu **Run** pada emulator API 24+ atau perangkat Android.
5. Halaman debug menyediakan 13 layar wajib dan 4 perluasan desain, uji tema persisten, draft Room dan tombol Periksa backend. Demo E–F menambahkan penyelesaian donasi, saldo, dan penukaran lokal persisten. Sebagian layar lain masih berupa placeholder/TODO.

Docker menyiapkan JDK/SDK dan build konsisten di Linux, Windows, dan macOS. **APK tetap berjalan di Android**, melalui emulator atau perangkat; Docker tidak menggantikan emulator. Tools SDK Linux memakai x86_64; host ARM seperti Apple Silicon mungkin membutuhkan emulasi `linux/amd64`. Lihat [development.md](docs/development.md).

## Isi repository

| Folder/file | Peran |
| --- | --- |
| [`android/`](android/README.md) | Project Android Studio, modul app, wrapper, dependency catalog |
| [`backend/`](backend/README.md) | API Go untuk autentikasi dan verifikasi donasi, serta scaffold Kotlin/Ktor |
| [`docker/`](docker/README.md), Dockerfile, compose.yaml | Build Android, backend runtime dan PostgreSQL |
| [`docs/`](docs/README.md) | Arsitektur, pemetaan 10 materi PAPB, screen map, API/data model, backlog dan 20 AC |
| [`scripts/`](scripts/README.md) | Build container dan pemeriksaan struktur |
| [`UI design/`](UI%20design/README.md) | Berkas .fig asli |
| [`.github/`](.github/README.md) | CI dan template kontribusi |
| REAMDE.md | Pengarah ke README ini sesuai ejaan permintaan awal |

Setiap folder source berisi README dengan peran, daftar file dan TODO. File template Kotlin berisi package dan komentar TODO agar tetap valid saat build, tanpa `TODO()` yang crash. Fondasi diisi secukupnya agar project siap dikembangkan.

## Perintah host

```sh
cd android
./gradlew assembleDebug testDebugUnitTest lintDebug
# Windows PowerShell: .\gradlew.bat assembleDebug testDebugUnitTest lintDebug
```

Backend Go: `cd backend/go`, set `DATABASE_URL` ke PostgreSQL, lalu `go run .`. Tes: `go test ./...` dan `go vet ./...`; lihat [panduan auth](docs/auth-go.md) untuk tes integrasi dan provisioning admin. Scaffold Ktor dapat dijalankan terpisah dari `backend/` dengan Gradle. Hindari menjalankan beberapa backend pada port 8080 yang sama.

```sh
docker compose logs -f backend
docker compose stop
docker compose down
```

`down` mempertahankan volume; `down -v` menghapus database/cache, hanya gunakan untuk reset data lokal.

## Kerja tim

Baca [arsitektur](docs/architecture.md), [PAPB](docs/papb-mapping.md), [backlog](docs/backlog.md), [acceptance tests](docs/acceptance-tests.md), dan [CONTRIBUTING.md](CONTRIBUTING.md). Ambil tugas melalui branch fitur dan PR. PRD belum membagi tanggung jawab implementasi; tim menentukan pemiliknya. Tidak ada kurir nyata, GPS, pembayaran, push notification atau voucher komersial dalam MVP.

Referensi konsep: ReWear, Garuda Hacks 6.0 (2025), oleh Farella Kamala Budianto, Kenlyn Tesalonika Winata, Vincentia Belinda Sumartoyo, sesuai atribusi PRD. PDF kuliah tidak disalin ke repo; pemetaan dan checksum disertakan. Tim perlu menentukan lisensi source/desain sebelum distribusi lebih luas.
