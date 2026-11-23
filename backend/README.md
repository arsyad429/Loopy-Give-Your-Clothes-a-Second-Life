# backend

Backend aktif berada di [`go/`](go/README.md): register/login/refresh/logout/me, PostgreSQL, Argon2id, dan sesi yang dapat dicabut. Baca [alur, kode, dan teori auth](../docs/auth-go.md) terlebih dahulu. Compose dari root membangun Go. Project Kotlin/Ktor di folder ini tetap scaffold referensi dan dapat dijalankan terpisah dengan `./gradlew run`; AI/donasi/admin/poin/redeem masih TODO.

## Isi file dan perannya

| File/folder | Peran dan isi yang diharapkan |
| --- | --- |
| `go/` | Backend autentikasi Go aktif, migrasi SQL, unit/integration test, dan panduan menjalankan. |
| `build.gradle.kts` | Plugin, dependency dan task build modul/project; jaga kompatibilitas versi. |
| `gradle/` | Baca README di subfolder gradle untuk peran file dan TODO. |
| `gradle.properties` | Pengaturan JVM/Gradle dan property project; bukan tempat secret. |
| `gradlew` | Bootstrap Gradle Linux/macOS resmi. Pakai sh ./gradlew bila executable bit hilang. |
| `gradlew.bat` | Bootstrap Gradle Windows untuk cmd/PowerShell. |
| `settings.gradle.kts` | Nama project, module include dan repository resolution. |
| `src/` | Baca README di subfolder src untuk peran file dan TODO. |

## TODO

- [ ] Hubungkan auth Android ke backend Go; lengkapi AI dan fitur bisnis sesuai docs/api-contract.md dan docs/data-model.md.
- [ ] Validasi role/ownership server dan transaksi poin/redeem; uji concurrency/rollback PostgreSQL.
- [ ] Jalankan tes dan perbarui README saat perilaku berubah.
