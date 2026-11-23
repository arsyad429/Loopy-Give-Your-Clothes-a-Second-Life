# src

Source set Android memisahkan kode utama, konfigurasi debug, unit test JVM, dan tes instrumentasi.

## Isi file dan perannya

| File atau folder | Kegunaan |
| --- | --- |
| [`androidTest/`](androidTest/README.md) | Tes instrumentasi Android dijalankan pada perangkat atau emulator dengan connectedDebugAndroidTest. |
| [`debug/`](debug/README.md) | Manifest khusus debug mengizinkan HTTP untuk API lokal yang diakses lewat ADB reverse. |
| [`main/`](main/README.md) | Source set utama memuat manifest, kode Kotlin aplikasi, dan resource UI. |
| [`test/`](test/README.md) | Unit test Android yang berjalan pada JVM melalui testDebugUnitTest. |
