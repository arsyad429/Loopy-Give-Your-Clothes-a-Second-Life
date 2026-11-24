# main

Source set utama memuat manifest, kode Kotlin aplikasi, dan resource UI.

## Isi file dan perannya

| File atau folder | Kegunaan |
| --- | --- |
| [`AndroidManifest.xml`](AndroidManifest.xml) | Activity/application/permission. Main menolak cleartext, overlay debug mengizinkan HTTP lokal. |
| [`java/`](java/README.md) | Akar source Kotlin untuk aplikasi Android id.ac.loopy. |
| [`res/`](res/README.md) | Resource Android untuk ikon, font, nilai aplikasi, dan konfigurasi FileProvider. |
