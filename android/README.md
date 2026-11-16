# android

Project Android Studio Kotlin/Compose. Open folder ini di Studio, Gradle JDK17, SDK36, BuildTools36.0.0; modul app dan wrapper siap dikembangkan.

## Isi file dan perannya

| File/folder | Peran dan isi yang diharapkan |
| --- | --- |
| `app/` | Baca README di subfolder app untuk peran file dan TODO. |
| `build.gradle.kts` | Plugin, dependency dan task build modul/project; jaga kompatibilitas versi. |
| `gradle/` | Baca README di subfolder gradle untuk peran file dan TODO. |
| `gradle.properties` | Pengaturan JVM/Gradle dan property project; bukan tempat secret. |
| `gradlew` | Bootstrap Gradle Linux/macOS resmi. Pakai sh ./gradlew bila executable bit hilang. |
| `gradlew.bat` | Bootstrap Gradle Windows untuk cmd/PowerShell. |
| `settings.gradle.kts` | Nama project, module include dan repository resolution. |

## TODO

- [ ] Lanjutkan file template sesuai docs/backlog.md, docs/api-contract.md dan docs/acceptance-tests.md.
- [ ] Pertahankan UI → ViewModel → Repository → Data Source dan konfigurasi yang dapat dibuild.
- [ ] Perbarui daftar file dan bukti cek setelah implementasi berubah.
