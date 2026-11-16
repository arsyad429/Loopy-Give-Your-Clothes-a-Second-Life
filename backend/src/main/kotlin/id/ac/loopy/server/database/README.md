# database

Layer backend database. Isi route/service sesuai kontrak PRD; /health saja yang aktif saat scaffold.

## Isi file dan perannya

| File/folder | Peran dan isi yang diharapkan |
| --- | --- |
| `DatabaseFactory.kt` | TEMPLATE — TODO: koneksi pool PostgreSQL dari environment; migration runner; readiness check; transaksi dan rollback. Lihat docs/api-contract.md, docs/data-model.md dan docs/acceptance-tests.md. |
| `Migrations.kt` | TEMPLATE — TODO: jalankan migrasi berurutan sebelum menerima traffic; jangan mengubah SQL yang telah diterapkan. Lihat docs/api-contract.md, docs/data-model.md dan docs/acceptance-tests.md. |
| `Tables.kt` | TEMPLATE — TODO: tujuh entitas PRD §8 dan idempotency records; FK, timestamps, owner indexes, unique earn, saldo nonnegatif. Lihat docs/api-contract.md, docs/data-model.md dan docs/acceptance-tests.md. |

## TODO

- [ ] Isi TEMPLATE sesuai docs/api-contract.md dan docs/data-model.md; DB/auth/AI masih TODO.
- [ ] Validasi role/ownership server dan transaksi poin/redeem; uji concurrency/rollback PostgreSQL.
- [ ] Jalankan tes dan perbarui README saat perilaku berubah.
