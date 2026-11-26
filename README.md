# Loopy — Give Your Clothes a Second Life ♻️

Loopy membantu donatur memeriksa kondisi pakaian, mengajukan penjemputan, dan menukar poin. Aplikasi Android memakai Kotlin dan Jetpack Compose. Backend Go menyimpan data di PostgreSQL, mengirim OTP lewat SMTP, dan memakai Groq untuk pemeriksaan foto. [PRD v2.1](docs/reference/PRD-v2.1.md) · [Peta layar Figma](docs/screen-map.md)

## Alur aplikasi

1. Donatur mulai dari landing, mendaftar dengan verifikasi email, lalu masuk dengan email dan kata sandi untuk kunjungan berikutnya. Admin dibuat lewat CLI dan masuk dari halaman yang sama. [Autentikasi](docs/auth-go.md)
2. Donatur memilih foto JPEG/PNG dan jenis pakaian. Hasil yang berpotensi layak membuka formulir pickup berisi alamat, kontak, jumlah, serta preferensi jadwal. [Pemeriksaan pakaian](android/app/src/main/java/id/ac/loopy/feature/clothingcheck/README.md) · [Form pickup](android/app/src/main/java/id/ac/loopy/feature/pickup/README.md)
3. Admin meninjau pengajuan, menetapkan jadwal atau menolak dengan alasan, lalu menandai pengajuan dijemput dan selesai. Donatur melihat status, jadwal final, dan riwayat di Donasiku. [Alur admin](docs/donation-review-go.md)
4. Penyelesaian memberi 10 poin sekali per pengajuan. Donatur melihat saldo, katalog, dan riwayat penukaran dari API. [Poin dan rewards](docs/completion-rewards.md)

## Menjalankan untuk pengembangan

1. Siapkan Docker Compose, JDK 17, Android SDK Platform 36, dan Build Tools 36.0.0.
2. Siapkan `.env` lokal dari [contoh environment](.env.example). Isi `GROQ_API_KEY` untuk foto dan variabel SMTP untuk OTP. Pengujian lokal tanpa SMTP dapat memakai `AUTH_EMAIL_VERIFICATION_REQUIRED=false`.
3. Jalankan backend dari root repository.

```sh
API_PORT=8082 docker compose -p loopy-go up --build -d --wait db backend
curl -fsS http://127.0.0.1:8082/ready
```

4. Buka `android/` di Android Studio atau jalankan Gradle dari folder itu. Untuk emulator, sesuaikan URL dengan port backend. Untuk HP fisik, jalankan `adb reverse tcp:8082 tcp:8082` di laptop dan gunakan `http://127.0.0.1:8082/` saat build. Properti URL membutuhkan garis miring di akhir.

```sh
cd android
./gradlew assembleDebug testDebugUnitTest lintDebug \
  -PLOOPY_API_BASE_URL=http://10.0.2.2:8082/
```

APK debug ada di `android/app/build/outputs/apk/debug/app-debug.apk`. Untuk VM, ikuti panduan deploy termasuk konfigurasi SMTP, startup, akun admin, dan backup. [Pengembangan Android](android/README.md) · [Deploy LXC](docs/deploy-lxc.md)

## Peta repository

| Lokasi | Isi |
| --- | --- |
| [Android](android/README.md) | UI per fitur, navigasi berdasarkan sesi, Retrofit, Room, dan tes. |
| [Backend](backend/README.md) | API, aturan akses, transaksi, serta migrasi PostgreSQL. |
| [Docker](docker/README.md) | Build backend dan template environment VM. |
| [Dokumentasi](docs/README.md) | Kontrak API, alur deploy, rujukan Figma, dan hasil verifikasi. |
| [Skrip](scripts/README.md) | Build Android dalam container dan pemeriksaan struktur. |
| [Desain](UI%20design/README.md) | Sumber desain Loopy. |
| [GitHub](.github/README.md) | Workflow CI dan template kontribusi. |

[Compose development](compose.yaml) menjalankan API dan database serta profil opsional `tools`. [Compose LXC](compose.lxc.yaml) menjalankan backend dan database di VM. [Dockerfile Android](Dockerfile) membangun image untuk profil `tools`. Ikuti [panduan kontribusi](CONTRIBUTING.md) sebelum membuka PR.

Referensi konsep adalah ReWear, Garuda Hacks 6.0 (2025), oleh Farella Kamala Budianto, Kenlyn Tesalonika Winata, dan Vincentia Belinda Sumartoyo, sesuai atribusi PRD. Katalog dan checksum materi kuliah ada di [rujukan PAPB](docs/reference/README.md). Tim perlu menentukan lisensi source dan desain sebelum distribusi yang lebih luas.
