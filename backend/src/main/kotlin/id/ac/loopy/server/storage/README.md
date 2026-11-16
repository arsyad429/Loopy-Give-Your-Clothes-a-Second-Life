# storage

Layer backend storage. Isi route/service sesuai kontrak PRD; /health saja yang aktif saat scaffold.

## Isi file dan perannya

| File/folder | Peran dan isi yang diharapkan |
| --- | --- |
| `PhotoStorage.kt` | TEMPLATE — TODO: simpan foto privat, signed URL pendek, validasi owner, ukuran/MIME, lifecycle dan deletion. Lihat docs/api-contract.md, docs/data-model.md dan docs/acceptance-tests.md. |

## TODO

- [ ] Isi TEMPLATE sesuai docs/api-contract.md dan docs/data-model.md; DB/auth/AI masih TODO.
- [ ] Validasi role/ownership server dan transaksi poin/redeem; uji concurrency/rollback PostgreSQL.
- [ ] Jalankan tes dan perbarui README saat perilaku berubah.
