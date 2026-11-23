# Backend Loopy

[API Go](go/README.md) menangani akun, sesi, dan peninjauan pickup oleh admin. [API Ktor](src/main/kotlin/id/ac/loopy/server/README.md) menangani analisis pakaian melalui Groq. Kedua API memakai skema PostgreSQL dan token sesi yang terpisah. [Panduan backend](../docs/backend.md)

| File atau folder | Kegunaan |
| --- | --- |
| [`go/`](go/README.md) | API Go, migrasi akun dan donasi, serta tes PostgreSQL. |
| [`src/`](src/README.md) | Kode Ktor, migrasi Flyway, dan tes API pemeriksaan. |
| [`build.gradle.kts`](build.gradle.kts) | Dependensi Ktor, Groq HTTP client, PostgreSQL, Flyway dan pengujian. |
| [`settings.gradle.kts`](settings.gradle.kts) | Nama proyek Gradle dan konfigurasi repositori dependensi. |
| [`gradle.properties`](gradle.properties) | Pengaturan JVM dan Gradle untuk build backend. |
| [`gradle/`](gradle/README.md) | Konfigurasi Gradle wrapper. |
| [`gradlew`](gradlew) dan [`gradlew.bat`](gradlew.bat) | Skrip wrapper untuk Linux/macOS dan Windows. |

## Jalankan API pemeriksaan

Dari root, isi `GROQ_API_KEY` di `.env` lokal, lalu jalankan:

```sh
docker compose -f compose.vision.yaml up --build -d --wait
curl -fsS http://127.0.0.1:8081/ready
```

Untuk `./gradlew run` dari folder ini, set `DATABASE_URL`, `DATABASE_USER`, `DATABASE_PASSWORD`, `GROQ_API_KEY`, dan `GROQ_MODEL` di environment. `GROQ_RESPONSE_MODE` menerima `json_schema` (default) atau `json_object`. Ktor mendengarkan pada port 8080 di dalam container.

## Uji Ktor

```sh
./gradlew test installDist
```

[Daftar tes pemeriksaan](src/test/kotlin/id/ac/loopy/server/checks/README.md). Dua tes PostgreSQL berjalan ketika ada database uji dan variabel `LOOPY_B2_TEST_JDBC_URL`, `LOOPY_B2_TEST_DB_USER`, serta `LOOPY_B2_TEST_DB_PASSWORD`. Gunakan database uji khusus karena tes menjalankan migrasi dan membuat akun sementara.

```sh
LOOPY_B3_LIVE=0 ./gradlew test \
  --tests '*ClothingCheckStoreIntegrationTest' \
  --tests '*ClothingCheckRoutesTest' --rerun-tasks
```

Mode live memerlukan kredensial Groq di environment dan `GROQ_SMOKE_IMAGE` berupa path absolut ke JPEG uji. `GROQ_SMOKE_CLOTHING_TYPE` opsional, default `TSHIRT`.

```sh
./gradlew test --tests '*GroqVisionSmokeTest' --rerun-tasks
LOOPY_B3_LIVE=1 ./gradlew test --tests '*ClothingCheckRoutesTest' --rerun-tasks
```

Jika variabel aktivasi database atau foto kosong, tes terkait dilewati. Laporan detail ada di `build/reports/tests/test/index.html`.
