# checks

Layer backend checks. Isi route/service sesuai kontrak PRD; /health saja yang aktif saat scaffold.

## Isi file dan perannya

| File/folder | Peran dan isi yang diharapkan |
| --- | --- |
| `AiVisionGateway.kt` | TEMPLATE — TODO: adapter provider AI Vision. API key hanya environment server; enum result tiga kategori dan validasi output AI. Lihat docs/api-contract.md, docs/data-model.md dan docs/acceptance-tests.md. |
| `ClothingCheckRoutes.kt` | TEMPLATE — TODO: POST multipart /checks, GET /checks/{id}; login + ownership, MIME/limit; hasil gagal tidak eligible. Lihat docs/api-contract.md, docs/data-model.md dan docs/acceptance-tests.md. |
| `ClothingCheckService.kt` | TEMPLATE — TODO: validasi upload, private object storage, persist check/result, timeout dan retry terkontrol. Lihat docs/api-contract.md, docs/data-model.md dan docs/acceptance-tests.md. |

## TODO

- [ ] Isi TEMPLATE sesuai docs/api-contract.md dan docs/data-model.md; DB/auth/AI masih TODO.
- [ ] Validasi role/ownership server dan transaksi poin/redeem; uji concurrency/rollback PostgreSQL.
- [ ] Jalankan tes dan perbarui README saat perilaku berubah.
