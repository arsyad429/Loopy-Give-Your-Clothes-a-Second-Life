# loopy

`CompletionPersistenceTest.kt` menguji transaksi, rollback, idempotensi, saldo kurang, dan persistensi Room demo E–F. `CompletionFlowTest.kt` menguji alur Compose admin → completion → batal/konfirmasi redeem → Reward-ku → Home → detail dan menyimpan screenshot ke external files aplikasi.

Area tes perilaku PRD. Tulis assertions bermakna untuk validasi/error/role/transaksi dan persistence.

## Isi file dan perannya

| File/folder | Peran dan isi yang diharapkan |
| --- | --- |
| `FoundationPersistenceTest.kt` | Instrumented test draft Room bertahan setelah database ditutup/dibuka. |

## TODO

- [ ] Lanjutkan file template sesuai docs/backlog.md, docs/api-contract.md dan docs/acceptance-tests.md.
- [ ] Pertahankan UI → ViewModel → Repository → Data Source dan konfigurasi yang dapat dibuild.
- [ ] Perbarui daftar file dan bukti cek setelah implementasi berubah.
