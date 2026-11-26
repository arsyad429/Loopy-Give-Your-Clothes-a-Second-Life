# Source set Android

| Folder | Dipakai untuk |
| --- | --- |
| [main](main/README.md) | Aplikasi, manifest, resource, dan akses backend. |
| [debug](debug/README.md) | Override manifest untuk koneksi HTTP pengembangan. |
| [test](test/README.md) | Unit test JVM untuk mapper, repository, dan state. |
| [androidTest](androidTest/README.md) | Tes Compose dan Room pada perangkat atau emulator. |

Build varian memilih manifest dan dependency yang sesuai dari [Gradle modul](../build.gradle.kts).
