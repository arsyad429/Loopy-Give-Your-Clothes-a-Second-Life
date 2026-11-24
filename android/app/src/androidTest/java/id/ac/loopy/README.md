# loopy

`CompletionPersistenceTest.kt` memeriksa transaksi, rollback dan persistensi Room E–F. `CompletionFlowTest.kt` mempertahankan alur admin → poin → batal/konfirmasi redeem → Reward-ku → Home → detail setelah navigasi login/AI dari main digabungkan.

Tes instrumentasi untuk persistensi lokal DataStore dan Room, serta UI pemeriksaan pakaian.

## Isi file dan perannya

| File atau folder | Kegunaan |
| --- | --- |
| [`FoundationPersistenceTest.kt`](FoundationPersistenceTest.kt) | Instrumented test draft Room bertahan setelah database ditutup/dibuka. |
| [`feature`](feature/README.md) | Tes fitur menurut layar yang diuji. |
