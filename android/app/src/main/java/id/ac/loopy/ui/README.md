# ui

NavHost Compose, komponen tampilan yang dipakai ulang, tema, dan state demo fondasi.

## Isi file dan perannya

| File atau folder | Kegunaan |
| --- | --- |
| [`LoopyApp.kt`](LoopyApp.kt) | NavHost untuk katalog debug, autentikasi Go, navigasi berdasarkan role, serta alur S05–S07 dengan checkId. |
| [`TemplateViewModel.kt`](TemplateViewModel.kt) | StateFlow combine/stateIn, coroutine scope, loading/error/cancellation untuk demo fondasi. |
| [`components/`](components/README.md) | Komponen Compose bersama untuk input, tombol, tata letak, dan feedback, beserta template komponen fitur. |
| [`navigation/`](navigation/README.md) | Rute aktif berada di LoopyApp.kt. S05 dan S06 memerlukan sesi USER dan hanya meneruskan checkId melalui navigasi. |
| [`theme/`](theme/README.md) | Warna, tipografi, bentuk, dan tema Material 3 untuk Compose. |
