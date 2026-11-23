# android

Proyek Android Kotlin dan Compose. Buka folder ini di Android Studio dengan JDK 17, SDK 36, dan Build Tools 36.0.0.

## Isi file dan perannya

| File atau folder | Kegunaan |
| --- | --- |
| [`app/`](app/README.md) | Baca README di subfolder app untuk peran file dan TODO. |
| [`build.gradle.kts`](build.gradle.kts) | Plugin, dependency dan task build modul/project; jaga kompatibilitas versi. |
| [`gradle/`](gradle/README.md) | Baca README di subfolder gradle untuk peran file dan TODO. |
| [`gradle.properties`](gradle.properties) | Pengaturan JVM/Gradle dan property project; bukan tempat secret. |
| [`gradlew`](gradlew) | Bootstrap Gradle Linux/macOS resmi. Pakai sh ./gradlew bila executable bit hilang. |
| [`gradlew.bat`](gradlew.bat) | Bootstrap Gradle Windows untuk cmd/PowerShell. |
| [`settings.gradle.kts`](settings.gradle.kts) | Nama project, module include dan repository resolution. |

## TODO

- [ ] Lanjutkan file template sesuai docs/backlog.md, docs/api-contract.md dan docs/acceptance-tests.md.
- [ ] Pertahankan UI → ViewModel → Repository → Data Source dan konfigurasi yang dapat dibuild.
- [ ] Perbarui daftar file dan bukti cek setelah implementasi berubah.
