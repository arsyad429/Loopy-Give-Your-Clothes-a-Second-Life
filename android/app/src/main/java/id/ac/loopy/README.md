# Package aplikasi id.ac.loopy

[LoopyApplication.kt](LoopyApplication.kt) menyediakan satu `AppContainer` sepanjang proses. [MainActivity.kt](MainActivity.kt) memasang tema serta `LoopyApp`, dan mencatat event lifecycle pada build debug.

Alur fitur dimulai dari [UI dan navigasi](ui/README.md). Tiap [fitur](feature/README.md) merender state dari ViewModel. [Data](data/README.md) menangani Retrofit dan Room, sedangkan [domain](domain/README.md) memberi model Kotlin dan interface repository. Susunan dependensi ada di [di](di/README.md).

## Subfolder

[core](core/README.md)
