# Development lintas OS

Gunakan checkout Git yang bersih ketika ingin membandingkan hasil dengan CI. Dari laptop, pilih Android Studio untuk menjalankan aplikasi di perangkat, atau Compose profil `tools` untuk build terisolasi. Backend Go dan PostgreSQL bisa tetap berjalan lewat Compose pada kedua pilihan.

## Setup backend lokal

1. Siapkan Docker Engine dan Compose v2 di Linux atau Docker Desktop dengan Linux containers di Windows/macOS. Beri Docker akses ke folder repository.
2. Jika `.env` lokal belum tersedia, salin [`.env.example`](../.env.example). Di PowerShell gunakan `Copy-Item .env.example .env`.
3. Tentukan `GROQ_API_KEY` untuk pengujian foto. Gunakan SMTP dan kunci OTP jika ingin menguji pendaftaran sungguhan. Untuk database lokal terisolasi dengan fixture, `AUTH_EMAIL_VERIFICATION_REQUIRED=false` memungkinkan pendaftaran tanpa inbox.
4. Jalankan dari root repository.

```sh
API_PORT=8082 docker compose -p loopy-go up --build -d --wait db backend
curl -fsS http://127.0.0.1:8082/ready
```

Di PowerShell, atur `$env:API_PORT = '8082'` lalu jalankan Compose. Gunakan nama proyek dan port yang sama ketika memeriksa instance itu. `docker compose -p loopy-go ps` menampilkan container dan port. [Panduan backend](backend.md) · [Konfigurasi SMTP VM](deploy-lxc.md#email-verification-for-registration)

## Koneksi Android ke backend

| Target | Rute dari aplikasi | Persiapan |
| --- | --- | --- |
| Emulator Android Studio | `http://10.0.2.2:8082/` | Loopback emulator diteruskan ke host emulator melalui `10.0.2.2`. |
| HP fisik via USB | `http://127.0.0.1:8082/` | Izinkan USB debugging, jalankan `adb reverse tcp:8082 tcp:8082` dari laptop. |
| VM di loopback, HP via USB | `http://127.0.0.1:18088/` | Teruskan port VM via SSH ke laptop, lalu jalankan `adb reverse tcp:18088 tcp:18088`. |
| APK release | URL HTTPS API | Isi `LOOPY_RELEASE_API_BASE_URL` saat build dan gunakan konfigurasi signing tim. |

Contoh dari laptop ketika API VM terikat ke `127.0.0.1:8080`. Ganti `user@vm.example.test` dengan akun dan host yang disiapkan tim. Biarkan terminal tunnel terbuka.

```sh
ssh -N -o ExitOnForwardFailure=yes \
  -L 127.0.0.1:18088:127.0.0.1:8080 user@vm.example.test
```

Di terminal lain, pastikan tunnel dan perangkat siap, lalu build dan pasang APK dari folder `android/`.

```sh
curl -fsS http://127.0.0.1:18088/ready
adb devices
adb reverse tcp:18088 tcp:18088
./gradlew assembleDebug -PLOOPY_API_BASE_URL=http://127.0.0.1:18088/
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

URL debug harus diakhiri `/`. `LOOPY_API_BASE_URL` di Compose dibaca oleh profil build container. Untuk Android Studio atau Gradle host, pakai properti Gradle. Build release membutuhkan properti HTTPS terpisah. [Konfigurasi Gradle](../android/app/build.gradle.kts) · [URL release dan jaringan](deploy-lxc.md#7-connect-android-or-a-public-domain)

## Build host atau container

| Pilihan | Kebutuhan | Output utama |
| --- | --- | --- |
| Gradle host | JDK 17, Platform Android 36, Build Tools 36.0.0 serta Android SDK yang terdeteksi. | `android/app/build/outputs/` dan `android/app/build/reports/`. |
| Compose profil `tools` | Docker dan izin file sharing. `android-container.sh` mengambil snapshot serta memakai cache Gradle bernama. Pada host ARM, emulasi amd64 mungkin diperlukan jika image alat build memerlukannya. | `artifacts/outputs/`, `artifacts/reports/`, dan schema Room kembali ke folder source. |

```sh
docker compose --profile tools run --rm android-dev assembleDebug testDebugUnitTest lintDebug
adb install -r artifacts/outputs/apk/debug/app-debug.apk
```

Source tetap disunting di host. Container mengabaikan `local.properties` dan output build host. Pertahankan line ending LF untuk skrip shell. Windows bisa memakai `gradlew.bat` pada build host. [Skrip build](../scripts/README.md) · [Proyek Android](../android/README.md)

## Pemeriksaan

```sh
python3 scripts/check-scaffold.py
docker compose config --quiet
(cd android && ./gradlew assembleDebug testDebugUnitTest lintDebug)
```

Untuk tes Go dengan PostgreSQL, arahkan `TEST_DATABASE_URL` ke database khusus tes. Tes membuat schema terisolasi. Jalankan `go test -race -count=1 ./...`, `go vet ./...`, dan `go build ./...` dari `backend/go`. [Backend Go](../backend/go/README.md)

Untuk instrumentasi, gunakan emulator atau perangkat uji khusus. Bila tes perlu dijalankan di HP yang menyimpan data, pasang APK aplikasi dan tes dengan `adb install -r` sebelum menjalankan tes secara manual. Gradle `connectedDebugAndroidTest` dapat menghapus paket setelah tes. [Cara instrumentasi](../android/app/src/androidTest/README.md) · [Matriks penerimaan](acceptance-tests.md)

## Troubleshooting

| Gejala | Periksa |
| --- | --- |
| `JAVA_HOME` atau SDK tidak ditemukan | Pilih JDK 17, install SDK yang dibutuhkan, dan cek `ANDROID_HOME` atau `local.properties` pada build host. |
| `LOOPY_API_BASE_URL harus diakhiri /` | Tambahkan garis miring akhir lalu buat APK kembali. |
| Perangkat ADB berstatus `unauthorized` | Buka kunci HP, periksa prompt USB, dan pastikan `adb devices` menampilkan `device`. |
| `/ready` tidak terjangkau dari laptop | Cek listener host, target tunnel, serta port Compose. Konfirmasi alamat dari `docker compose port backend 8080` pada instance yang dipakai. |
| `/ready` sehat, daftar gagal | Periksa konfigurasi OTP/SMTP pada container efektif. Uji inbox akun uji dan dashboard log provider. |
| Login bisa, pemeriksaan foto gagal | Cek JPEG/PNG, ukuran file, `GROQ_API_KEY`, dan respons `AI_UNAVAILABLE` atau `AI_RESPONSE_FAILED`. |
| Data lama hilang setelah startup ulang | Cek nama proyek dan volume Compose. `stop` serta `down` biasa mempertahankan volume. `down -v` menghapusnya. |
| Build pertama lambat atau ruang Docker habis | Cek kapasitas lokasi penyimpanan Docker dan cache Gradle. Pada Linux, cek kepemilikan `artifacts` sebelum mengedit output. |
| Pemeriksaan struktur melaporkan folder IDE lokal | Jalankan pada checkout bersih. Skrip juga memeriksa direktori tambahan yang ada di filesystem. |

[Catatan verifikasi lintas lingkungan](verification.md)
