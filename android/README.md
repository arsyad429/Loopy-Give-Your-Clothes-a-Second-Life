# android

Proyek Android Kotlin dan Jetpack Compose. Buka folder ini di Android Studio dengan JDK 17, SDK Platform 36, dan Build Tools 36.0.0.

## Isi file dan perannya

| File atau folder | Kegunaan |
| --- | --- |
| [`app/`](app/README.md) | Modul APK Loopy untuk Android API 24 ke atas. Build debug membuka katalog layar dan dapat menghubungkan login serta S05–S06 ke backend Go lokal. |
| [`build.gradle.kts`](build.gradle.kts) | Plugin, dependency dan task build modul/project; jaga kompatibilitas versi. |
| [`gradle/`](gradle/README.md) | Katalog versi bersama untuk plugin dan dependensi Android serta konfigurasi Gradle Wrapper. |
| [`gradle.properties`](gradle.properties) | Pengaturan JVM/Gradle dan property project; bukan tempat secret. |
| [`gradlew`](gradlew) | Bootstrap Gradle Linux/macOS resmi. Pakai sh ./gradlew bila executable bit hilang. |
| [`gradlew.bat`](gradlew.bat) | Bootstrap Gradle Windows untuk cmd/PowerShell. |
| [`settings.gradle.kts`](settings.gradle.kts) | Nama project, module include dan repository resolution. |
