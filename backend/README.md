# backend

Backend aktif di [`go/`](go/README.md) melayani autentikasi dan keputusan admin untuk verifikasi serta jadwal pickup. Compose dari root menjalankan Go dan PostgreSQL. Project Kotlin/Ktor di folder ini merupakan scaffold referensi yang dapat dijalankan terpisah dengan `./gradlew run`. [Panduan backend Loopy](../docs/backend.md)

## Isi file dan perannya

| File/folder | Peran dan isi yang diharapkan |
| --- | --- |
| `go/` | Backend Go aktif, migrasi SQL akun dan donasi, serta tes unit dan integrasi. |
| `build.gradle.kts` | Plugin, dependency dan task build modul/project; jaga kompatibilitas versi. |
| `gradle/` | Baca README di subfolder gradle untuk peran file dan TODO. |
| `gradle.properties` | Pengaturan JVM/Gradle dan property project; bukan tempat secret. |
| `gradlew` | Bootstrap Gradle Linux/macOS resmi. Pakai sh ./gradlew bila executable bit hilang. |
| `gradlew.bat` | Bootstrap Gradle Windows untuk cmd/PowerShell. |
| `settings.gradle.kts` | Nama project, module include dan repository resolution. |
| `src/` | Baca README di subfolder src untuk peran file dan TODO. |

## TODO

- [ ] Hubungkan autentikasi dan layar admin Android ke API Go. Lengkapi pengajuan pengguna, AI, dan fitur lanjutan sesuai kontrak.
- [ ] Validasi role/ownership server dan transaksi poin/redeem; uji concurrency/rollback PostgreSQL.
- [ ] Jalankan tes dan perbarui README saat perilaku berubah.
