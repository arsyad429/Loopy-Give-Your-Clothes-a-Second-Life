# backend

Project Kotlin/Ktor, /health aktif. Auth/AI/donasi/admin/poin/redeem/DB masih template. Jalankan ./gradlew run atau Compose dari root.

## Isi file dan perannya

| File/folder | Peran dan isi yang diharapkan |
| --- | --- |
| `build.gradle.kts` | Plugin, dependency dan task build modul/project; jaga kompatibilitas versi. |
| `gradle/` | Baca README di subfolder gradle untuk peran file dan TODO. |
| `gradle.properties` | Pengaturan JVM/Gradle dan property project; bukan tempat secret. |
| `gradlew` | Bootstrap Gradle Linux/macOS resmi. Pakai sh ./gradlew bila executable bit hilang. |
| `gradlew.bat` | Bootstrap Gradle Windows untuk cmd/PowerShell. |
| `settings.gradle.kts` | Nama project, module include dan repository resolution. |
| `src/` | Baca README di subfolder src untuk peran file dan TODO. |

## TODO

- [ ] Isi TEMPLATE sesuai docs/api-contract.md dan docs/data-model.md; DB/auth/AI masih TODO.
- [ ] Validasi role/ownership server dan transaksi poin/redeem; uji concurrency/rollback PostgreSQL.
- [ ] Jalankan tes dan perbarui README saat perilaku berubah.
