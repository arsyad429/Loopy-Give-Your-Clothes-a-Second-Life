# loopy

`RewardsScreenTest.kt` memeriksa saldo kurang, pembatalan tanpa transaksi, konfirmasi penukaran dan riwayat/saldo yang dikembalikan repository. Fixture hanya digunakan pada tes; aplikasi memakai API. Tes demo Room lama dihapus bersama implementasi demonya.

Tes instrumentasi untuk persistensi lokal DataStore dan Room, serta UI pemeriksaan pakaian.

## Isi file dan perannya

| File atau folder | Kegunaan |
| --- | --- |
| [`FoundationPersistenceTest.kt`](FoundationPersistenceTest.kt) | Instrumented test draft Room bertahan setelah database ditutup/dibuka. |
| [`feature`](feature/README.md) | Tes fitur menurut layar yang diuji. |
