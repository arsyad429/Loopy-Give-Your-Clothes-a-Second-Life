# Loopy — Give Your Clothes a Second Life! ♻️

Loopy adalah aplikasi donasi pakaian dengan Android Kotlin dan Jetpack Compose, berdasarkan [PRD 2.1](docs/reference/PRD-v2.1.md) dan [peta desain Figma](docs/screen-map.md).

| Bagian | Status |
| --- | --- |
| Android | Android menyediakan UI login, register, pratinjau admin, kamera/galeri S05, validasi kualitas foto, serta layar hasil S06. Klien login dan API pemeriksaan masih perlu dihubungkan. |
| API Go | Menyediakan register, login, refresh, logout, profil akun aktif, dan peninjauan serta penjadwalan pickup oleh admin. |
| API Ktor | Menyediakan analisis foto dengan Groq, penyimpanan hasil dan foto privat di PostgreSQL, serta pembatasan akses pemilik/admin. |

Go dan Ktor memakai database dan sesi yang terpisah. Tahap integrasi Android berikutnya menentukan alur akun dan pengambilan foto privat. [Panduan backend](docs/backend.md) · [Kontrak endpoint](docs/api-contract.md) · [Hasil verifikasi](docs/verification.md)

## Jalankan dengan Docker

Gunakan Docker Engine dengan Compose v2 atau Docker Desktop dengan Linux containers. Siapkan `.env` lokal dari [`.env.example`](.env.example) jika belum ada. Simpan API key Groq hanya di `.env` lokal. Jalankan dari root repository.

API Go dan PostgreSQL:

```sh
docker compose up --build -d --wait db backend
curl -fsS http://127.0.0.1:8080/ready
```

API pemeriksaan Ktor dan PostgreSQL tersendiri:

```sh
docker compose -f compose.vision.yaml up --build -d --wait
curl -fsS http://127.0.0.1:8081/ready
```

Jika port host terpakai, atur `API_PORT` untuk Go atau `VISION_API_PORT` untuk Ktor. Compose Vision menggunakan nama proyek `loopy-vision` dan volume PostgreSQL terpisah. Ikuti [panduan Docker](docker/README.md) jika pernah menjalankan Ktor menggunakan Compose utama sebelum penggabungan ini. Untuk deployment Go ke server, lihat [deployment LXC](docs/deploy-lxc.md).

## Buka Android Studio

1. Buka folder [`android/`](android/README.md) sebagai proyek yang sudah ada.
2. Gunakan JDK 17, Android SDK Platform 36, dan Build Tools 36.0.0. Android Studio dapat membuat `local.properties` untuk SDK host.
3. Jalankan varian `debug` pada emulator atau perangkat API 24+. Katalog debug menyediakan akses ke layar yang sedang dikembangkan, termasuk S05 dan S06.
4. Emulator memakai `http://10.0.2.2:8080/` untuk API Go secara default. Lihat [panduan development](docs/development.md) untuk perangkat USB dan pengaturan port.

```sh
cd android
./gradlew assembleDebug testDebugUnitTest lintDebug
```

Alternatif dari root adalah `docker compose --profile tools run --rm android-dev assembleDebug testDebugUnitTest lintDebug`. APK hasil container ada di `artifacts/outputs/apk/debug/app-debug.apk`.

## Isi repository

| Folder atau file | Kegunaan |
| --- | --- |
| [`android/`](android/README.md) | Modul Android, pengujian, dan Gradle wrapper. |
| [`backend/`](backend/README.md) | API Go dan Ktor, migrasi, serta cara menjalankan tes backend. |
| [`docker/`](docker/README.md) | Dockerfile API Go dan Ktor serta contoh konfigurasi server. |
| [`docs/`](docs/README.md) | Arsitektur, kontrak, referensi, backlog, dan hasil pengujian. |
| [`scripts/`](scripts/README.md) | Build Android terisolasi dan pemeriksaan struktur repo. |
| [`UI design/`](UI%20design/README.md) | File desain Figma asli untuk layar Loopy. |
| [`.github/`](.github/README.md) | Workflow CI dan template kontribusi. |
| [`Dockerfile`](Dockerfile) | Image alat build Android yang dipakai profil Compose `tools`. |
| [`compose.yaml`](compose.yaml) | API Go, PostgreSQL, dan profil alat build Android lokal. |
| [`compose.vision.yaml`](compose.vision.yaml) | API pemeriksaan Ktor dan PostgreSQL terisolasi. |
| [`compose.lxc.yaml`](compose.lxc.yaml) | API Go dan PostgreSQL untuk deployment server. |
| [`.env.example`](.env.example) | Contoh port dan variabel Compose tanpa kredensial nyata. |
| [`.dockerignore`](.dockerignore) | Mengecualikan kredensial, cache, build, dan aset tertentu dari konteks Docker. |
| [`.editorconfig`](.editorconfig) | Konvensi format dasar lintas editor. |
| [`.gitattributes`](.gitattributes) | Pengaturan Git untuk tipe file dan akhir baris. |
| [`.gitignore`](.gitignore) | Mengecualikan file konfigurasi lokal dan artefak build dari Git. |
| [`CONTRIBUTING.md`](CONTRIBUTING.md) | Alur kontribusi, review, dan verifikasi. |
| [`REAMDE.md`](REAMDE.md) | Pengarah ke README utama. |

Referensi konsep: ReWear, Garuda Hacks 6.0 (2025), oleh Farella Kamala Budianto, Kenlyn Tesalonika Winata, Vincentia Belinda Sumartoyo, sesuai atribusi PRD. PDF kuliah tidak disalin ke repo; pemetaan dan checksum disertakan. Tim perlu menentukan lisensi source/desain sebelum distribusi lebih luas.
