# local

Direktori local mengelompokkan source/config Loopy. README subfolder menjelaskan implementasi spesifik.

## Isi file dan perannya

| File/folder | Peran dan isi yang diharapkan |
| --- | --- |
| `LoopyDatabase.kt` | Room database versi1 draft; tambahkan cache/migration sesuai kebutuhan fitur. |
| `CompletionDemoDatabase.kt` | Database debug terpisah untuk donasi, ledger poin, dan redemption E–F; tidak mengubah draft atau saldo server. |
| `dao/` | Baca README di subfolder dao untuk peran file dan TODO. |
| `entity/` | Baca README di subfolder entity untuk peran file dan TODO. |
| `preferences/` | Baca README di subfolder preferences untuk peran file dan TODO. |

## TODO

- [ ] Lanjutkan file template sesuai docs/backlog.md, docs/api-contract.md dan docs/acceptance-tests.md.
- [ ] Pertahankan UI → ViewModel → Repository → Data Source dan konfigurasi yang dapat dibuild.
- [ ] Perbarui daftar file dan bukti cek setelah implementasi berubah.
