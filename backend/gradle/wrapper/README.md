# wrapper

Direktori wrapper mengelompokkan source/config Loopy. README subfolder menjelaskan implementasi spesifik.

## Isi file dan perannya

| File/folder | Peran dan isi yang diharapkan |
| --- | --- |
| `gradle-wrapper.jar` | Binary bootstrap resmi Gradle8.13, jangan diedit manual. |
| `gradle-wrapper.properties` | Distribusi Gradle8.13 dan SHA256 untuk validasi unduhan. |

## TODO

- [ ] Isi TEMPLATE sesuai docs/api-contract.md dan docs/data-model.md; DB/auth/AI masih TODO.
- [ ] Validasi role/ownership server dan transaksi poin/redeem; uji concurrency/rollback PostgreSQL.
- [ ] Jalankan tes dan perbarui README saat perilaku berubah.
