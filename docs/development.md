# Development lintas OS

Dari root, `API_PORT=8082 docker compose -p loopy-go up --build -d --wait db backend` memulai Go dan PostgreSQL. Isi `GROQ_API_KEY` di `.env` untuk mengaktifkan analisis pakaian di backend yang sama dengan login. Build Android dengan `docker compose --profile tools run --rm android-dev assembleDebug testDebugUnitTest lintDebug`. Profile tools mencegah build Android berjalan setiap up.

Script container menyalin project ke direktori sementara, mengabaikan local.properties/cache/build host, lalu menyalin APK/laporan/schema kembali. Lokasi SDK Windows/macOS/Linux tidak mengganggu SDK container. Source diedit di host; container mengambil snapshot saat mulai. Gradle dependency cache memakai named volume. Run Android Studio membuild project host sendiri.

Linux memakai Engine+Compose. Windows/macOS memakai Docker Desktop Linux containers dan izin file sharing untuk folder repo. PowerShell: `Copy-Item .env.example .env`; jangan ubah .sh menjadi CRLF. Host ARM dapat memerlukan `platform: linux/amd64` pada android-dev dan emulasi Docker/binfmt; lebih lambat. Backend/DB memakai base image multiarch. Pengujian aktual di Linux amd64; tim perlu smoke test Windows/macOS.

## Koneksi Android ke backend

| Target | URL dan langkah |
| --- | --- |
| Emulator Android Studio | `http://10.0.2.2:8082/` untuk Go lokal dari contoh. Nilai bawaan Gradle menggunakan port 8080. |
| Perangkat yang muncul di ADB | `adb reverse tcp:8082 tcp:8082`, lalu build dengan URL `http://127.0.0.1:8082/`. |
| Port host lain | Set API_PORT dan LOOPY_API_BASE_URL di .env, recreate backend lalu rebuild APK |

Docker membaca LOOPY_API_BASE_URL di .env; Android Studio membaca property Gradle di ~/.gradle/gradle.properties atau android/gradle.properties. Contoh host untuk APK HP adalah `./gradlew assembleDebug -PLOOPY_API_BASE_URL=http://127.0.0.1:8082/`. URL wajib diakhiri `/`. Compose environment tidak otomatis mengubah build Studio. Port backend dibatasi loopback host; perangkat USB menggunakan adb reverse.

Install APK Docker: `adb install -r artifacts/outputs/apk/debug/app-debug.apk`. HTTP hanya manifest debug; release wajib HTTPS. AI key di server saja.

## Pemeriksaan

```sh
python3 scripts/check-scaffold.py
docker compose config --quiet
docker compose --profile tools run --rm android-dev assembleDebug testDebugUnitTest lintDebug
docker compose --profile tools run --rm android-dev assembleDebugAndroidTest
```

Instrumented test memerlukan emulator/perangkat host: `cd android` lalu `./gradlew connectedDebugAndroidTest`. Container tidak menyediakan emulator. Cek tema dan draft tetap tersimpan setelah force-stop/reopen. Cek retry backend mati/aktif, rotasi, back stack, font besar dan TalkBack.

## Troubleshooting

- Gradlew permission: gunakan sh ./gradlew atau perbaiki executable bit; Windows gradlew.bat.
- SDK host tidak ditemukan: SDK Manager, Platform36+BuildTools36.0.0 dan sdk.dir/ANDROID_HOME.
- Port terpakai: ubah API_PORT dan URL Android; jangan menghentikan proses lain tanpa memeriksa pemiliknya.
- Linux output container milik root: sesuaikan ownership artifacts dengan UID lokal saat perlu; jangan membuka Studio sebagai root.
- Build pertama lambat: unduhan SDK/Gradle/Maven; cache named volume digunakan berikutnya.
- Database auth: Go menjalankan migrasi users/auth_sessions saat startup. `/ready` memeriksa koneksi database, `/health` hanya liveness. Migrasi Go juga menyiapkan peninjauan pickup, hasil pemeriksaan, foto privat, dan key idempotensi.
- Partisi sistem Docker penuh walau workspace lapang: cek docker info/df, pilih lokasi penyimpanan Docker di disk cukup melalui pengaturan host; jangan prune data/image proyek lain.
- down -v menghapus data/cache; stop/down biasa mempertahankan volume.

Backend auth Go: lihat [auth-go.md](auth-go.md) untuk host run, kontrak, tes PostgreSQL, dan command privat `create-admin`. Compose memakai Go; tidak perlu JDK untuk backend ini. Untuk tes pemeriksaan dengan PostgreSQL, gunakan `TEST_DATABASE_URL` seperti dijelaskan di [README Go](../backend/go/README.md). Untuk demo terpadu, buka S02 di aplikasi debug dan gunakan akun `USER`. Tekan **Periksa pakaian** di S04, unggah JPEG/PNG yang jelas di S05, lalu cek kategori dan foto di S06. Sesi tersimpan selama proses aplikasi hidup.

Pada mesin verifikasi, port8080 sudah dipakai Airflow sehingga pengujian memakai API_PORT=18080. Data DB/cache/tmp pengujian ditempatkan di partisi workspace lewat Compose override lokal karena partisi Docker penuh; override ini tidak mengubah konfigurasi portable repository.
