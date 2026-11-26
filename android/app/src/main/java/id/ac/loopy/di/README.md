# Perakitan dependensi Android

[AppContainer.kt](AppContainer.kt) membuat Room, OkHttp, Retrofit, DAO draf admin, dan repository. Ia mengambil base URL dari `BuildConfig`, menetapkan timeout serta aturan redirect, lalu membagikan satu `AuthRepositoryImpl` agar request memakai sesi yang sama. [LoopyApplication.kt](../LoopyApplication.kt) memiliki instance container.

Tambahkan implementasi data di sini ketika fitur membutuhkan dependency baru. [Layer data](../data/README.md) · [Navigasi dan ViewModel](../ui/README.md)
