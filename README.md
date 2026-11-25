# Loopy — Give Your Clothes a Second Life! ♻️

Loopy adalah aplikasi donasi pakaian dengan Android Kotlin dan Jetpack Compose, berdasarkan [PRD 2.1](docs/reference/PRD-v2.1.md) dan [peta desain Figma](docs/screen-map.md).

| Bagian | Status |
| --- | --- |
| Android | Login/register, pemeriksaan pakaian, pickup, donasi, admin, saldo dan rewards terhubung ke API Go dengan sesi akun aktif. |
| API Go | Autentikasi, pemeriksaan Groq dan foto privat, peninjauan/jadwal pickup, penyelesaian donasi dengan kredit 10 poin satu kali, saldo, katalog, penukaran, serta riwayat reward. |

Pemeriksaan pakaian memakai access token dari login Go. Android menyimpan sesi di memori selama proses aplikasi berjalan. [Panduan backend](docs/backend.md) · [Kontrak endpoint](docs/api-contract.md) · [Hasil verifikasi](docs/verification.md)

Untuk mencoba alur E–F, jalankan backend/PostgreSQL, login sesuai peran, lalu gunakan penyelesaian donasi oleh admin dan Rewards milik pengguna. Demo E–F sudah dihapus. [Panduan penyelesaian dan reward](docs/completion-rewards.md).

## Jalankan dengan Docker

Gunakan Docker Engine dengan Compose v2 atau Docker Desktop dengan Linux containers. Siapkan `.env` lokal dari [`.env.example`](.env.example) jika belum ada. Simpan API key Groq hanya di `.env` lokal. Jalankan dari root repository.

Jalankan API Go dan PostgreSQL. Port 8082 dan nama proyek `loopy-go` memberi ruang bagi instalasi development lama.

```sh
API_PORT=8082 docker compose -p loopy-go up --build -d --wait db backend
curl -fsS http://127.0.0.1:8082/ready
```

Untuk database Go yang sudah berjalan, gunakan kembali nama proyek Compose dan volume Go yang sama agar migrasi berjalan pada data tersebut. Data dari instalasi backend lama dengan skema berbeda memerlukan impor tersendiri. [Panduan Docker](docker/README.md) · [Deployment LXC](docs/deploy-lxc.md)

## Buka Android Studio

1. Buka folder [`android/`](android/README.md) sebagai proyek yang sudah ada.
2. Gunakan JDK 17, Android SDK Platform 36, dan Build Tools 36.0.0. Android Studio dapat membuat `local.properties` untuk SDK host.
3. Jalankan varian `debug` pada emulator atau perangkat API 24+. Di katalog debug, buka S02, masuk sebagai pengguna, lalu pilih **Periksa pakaian** untuk membuka S05. S06 terbuka setelah analisis berhasil.
4. Untuk HP fisik, aktifkan `adb reverse tcp:8082 tcp:8082` dan build APK dengan `-PLOOPY_API_BASE_URL=http://127.0.0.1:8082/`. Emulator memakai alamat `10.0.2.2` dengan port Go. [Panduan development](docs/development.md#koneksi-android-ke-backend)

```sh
cd android
./gradlew assembleDebug testDebugUnitTest lintDebug
```

Alternatif dari root adalah `docker compose --profile tools run --rm android-dev assembleDebug testDebugUnitTest lintDebug`. APK hasil container ada di `artifacts/outputs/apk/debug/app-debug.apk`.

## Isi repository

| Folder atau file | Kegunaan |
| --- | --- |
| [`android/`](android/README.md) | Modul Android, pengujian, dan Gradle wrapper. |
| [`backend/`](backend/README.md) | Backend Go, migrasi PostgreSQL, tes, dan template fitur berikutnya. |
| [`docker/`](docker/README.md) | Dockerfile API Go dan contoh konfigurasi server. |
| [`docs/`](docs/README.md) | Arsitektur, kontrak, referensi, backlog, dan hasil pengujian. |
| [`scripts/`](scripts/README.md) | Build Android terisolasi dan pemeriksaan struktur repo. |
| [`UI design/`](UI%20design/README.md) | File desain Figma asli untuk layar Loopy. |
| [`.github/`](.github/README.md) | Workflow CI dan template kontribusi. |
| [`Dockerfile`](Dockerfile) | Image alat build Android yang dipakai profil Compose `tools`. |
| [`compose.yaml`](compose.yaml) | API Go, PostgreSQL, dan profil alat build Android lokal. |
| [`compose.lxc.yaml`](compose.lxc.yaml) | API Go dan PostgreSQL untuk deployment server. |
| [`.env.example`](.env.example) | Contoh port dan variabel Compose tanpa kredensial nyata. |
| [`.dockerignore`](.dockerignore) | Mengecualikan kredensial, cache, build, dan aset tertentu dari konteks Docker. |
| [`.editorconfig`](.editorconfig) | Konvensi format dasar lintas editor. |
| [`.gitattributes`](.gitattributes) | Pengaturan Git untuk tipe file dan akhir baris. |
| [`.gitignore`](.gitignore) | Mengecualikan file konfigurasi lokal dan artefak build dari Git. |
| [`CONTRIBUTING.md`](CONTRIBUTING.md) | Alur kontribusi, review, dan verifikasi. |
| [`REAMDE.md`](REAMDE.md) | Pengarah ke README utama. |

Referensi konsep: ReWear, Garuda Hacks 6.0 (2025), oleh Farella Kamala Budianto, Kenlyn Tesalonika Winata, Vincentia Belinda Sumartoyo, sesuai atribusi PRD. PDF kuliah tidak disalin ke repo; pemetaan dan checksum disertakan. Tim perlu menentukan lisensi source/desain sebelum distribusi lebih luas.
