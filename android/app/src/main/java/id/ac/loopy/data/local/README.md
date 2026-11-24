# local

LoopyDatabase menyimpan draft pickup dengan Room. Preferensi tema memakai DataStore.

`CompletionDemoDatabase.kt` menyimpan donasi, ledger poin, dan redemption E–F pada database debug terpisah. Draft yang sudah ada dan saldo akun server tidak diubah oleh data demo.

## Isi file dan perannya

| File atau folder | Kegunaan |
| --- | --- |
| [`LoopyDatabase.kt`](LoopyDatabase.kt) | Room database versi1 draft; tambahkan cache/migration sesuai kebutuhan fitur. |
| [`dao/`](dao/README.md) | Query Room untuk draft pickup dan template query cache bisnis per akun. |
| [`entity/`](entity/README.md) | Entitas Room untuk draft pickup serta template entitas cache bisnis. |
| [`preferences/`](preferences/README.md) | Preferences DataStore menyimpan pilihan tema lintas sesi aplikasi. |
