# Loopy — Give Your Clothes a Second Life! ♻️

Repository template aplikasi donasi pakaian Kotlin dan Jetpack Compose untuk Android Studio,
berdasarkan PRD Loopy 2.1 dan desain Figma. Fondasi PAPB sudah dihubungkan; fitur bisnis
login, AI, pickup, admin, poin dan redeem masih berupa file template dengan TODO.

`android/` adalah project Android Studio; `backend/` adalah template Kotlin/Ktor;
`docker/` dan `compose.yaml` menyediakan lingkungan development; `docs/` berisi
pemetaan materi PAPB, PRD, desain, kontrak API, backlog dan acceptance criteria.
Setiap subfolder source memiliki README yang menjelaskan file dan pekerjaannya.

Mulai dari root repository dengan Docker Engine/Desktop dan Compose v2:

```sh
cp .env.example .env
docker compose up --build -d --wait
docker compose --profile tools build android-dev
docker compose --profile tools run --rm android-dev assembleDebug testDebugUnitTest lintDebug
```

PowerShell menggunakan `Copy-Item .env.example .env` untuk perintah pertama.
APK Docker berada di `artifacts/outputs/apk/debug/app-debug.apk`.
Docker menyediakan lingkungan build/backend; APK dijalankan pada emulator atau perangkat Android.

Di Android Studio pilih **Open → android/**, Gradle JDK 17, SDK Platform 36 dan
Build Tools 36.0.0. Tunggu Gradle Sync, pilih modul **app** dan varian **debug**, lalu Run.

Panduan lengkap, koneksi emulator, keterbatasan lintas OS, dan troubleshooting tersedia di
**[README.md](README.md)** dan **[docs/development.md](docs/development.md)**.
