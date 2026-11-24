# di

Satu AppContainer per proses menyediakan Room, DataStore, serta Retrofit dan repository yang berbagi sesi Go.

## Isi file dan perannya

| File atau folder | Kegunaan |
| --- | --- |
| [`AppContainer.kt`](AppContainer.kt) | Menghubungkan Room dan DataStore serta berbagi satu Retrofit untuk health, autentikasi dan pemeriksaan. |
