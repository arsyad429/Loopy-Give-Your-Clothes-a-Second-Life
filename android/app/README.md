# app

Modul APK Loopy untuk Android API 24 ke atas. Build debug membuka katalog layar dan dapat menghubungkan login serta S05–S06 ke backend Go lokal.

## Isi file dan perannya

| File atau folder | Kegunaan |
| --- | --- |
| [`build.gradle.kts`](build.gradle.kts) | Plugin, dependency dan task build modul/project; jaga kompatibilitas versi. |
| [`proguard-rules.pro`](proguard-rules.pro) | Tempat aturan R8 tambahan setelah shrinking diuji nanti. |
| [`schemas/`](schemas/README.md) | KSP mengekspor skema Room ke folder ini agar perubahan database lokal dapat direview bersama kode. |
| [`src/`](src/README.md) | Source set Android memisahkan kode utama, konfigurasi debug, unit test JVM, dan tes instrumentasi. |
