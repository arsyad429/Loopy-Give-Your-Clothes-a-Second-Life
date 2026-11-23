# loopy

Package utama Android memuat Application, Activity, UI, domain, data, dan dependency injection.

## Isi file dan perannya

| File atau folder | Kegunaan |
| --- | --- |
| [`LoopyApplication.kt`](LoopyApplication.kt) | Application pemilik AppContainer sepanjang proses. |
| [`MainActivity.kt`](MainActivity.kt) | setContent/edge-to-edge, ViewModel factory, lifecycle-aware collection dan Logcat lifecycle debug. |
| [`core/`](core/README.md) | Tempat utilitas lintas fitur. Alur kamera dan sesi aktif berada di fitur clothingcheck dan repository auth. |
| [`data/`](data/README.md) | Implementasi data Android untuk Retrofit ke Go, preferensi DataStore, dan draft Room. |
| [`di/`](di/README.md) | Satu AppContainer per proses menyediakan Room, DataStore, serta Retrofit dan repository yang berbagi sesi Go. |
| [`domain/`](domain/README.md) | Model bisnis dan kontrak repository yang dipakai oleh ViewModel dan lapisan data. |
| [`feature/`](feature/README.md) | Layar Compose, ViewModel dan state UI per fitur. Login, register dan pemeriksaan terhubung ke Go. |
| [`ui/`](ui/README.md) | NavHost Compose, komponen tampilan yang dipakai ulang, tema, dan state demo fondasi. |
