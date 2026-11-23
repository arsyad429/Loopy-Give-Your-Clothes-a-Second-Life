# gradle

Katalog versi plugin dan dependensi Android serta konfigurasi Gradle Wrapper.

## Isi file dan perannya

| File atau folder | Kegunaan |
| --- | --- |
| [`libs.versions.toml`](libs.versions.toml) | Version catalog Android terpusat: Kotlin, Compose, Room, DataStore, Retrofit, lifecycle, Navigation, tests. |
| [`wrapper/`](wrapper/README.md) | Bootstrap Gradle untuk menghasilkan build Android dengan versi distribusi yang sama di setiap mesin. |
