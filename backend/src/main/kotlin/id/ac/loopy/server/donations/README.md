# donations

Layer backend donations. Isi route/service sesuai kontrak PRD; /health saja yang aktif saat scaffold.

## Isi file dan perannya

| File atau folder | Kegunaan |
| --- | --- |
| [`DonationRepository.kt`](DonationRepository.kt) | TEMPLATE — TODO: query per owner; preferred vs scheduled; optimistic locking pada update; jangan percaya role/status/points dari request. Lihat docs/api-contract.md, docs/data-model.md dan docs/acceptance-tests.md. |
| [`DonationRoutes.kt`](DonationRoutes.kt) | TEMPLATE — TODO: POST/GET donations, GET detail; eligible check milik user, konfirmasi bersih, idempotency; status awal pending. Lihat docs/api-contract.md, docs/data-model.md dan docs/acceptance-tests.md. |
| [`DonationService.kt`](DonationService.kt) | TEMPLATE — TODO: quantity minimal 1, telepon/alamat/kota, tanggal valid zona waktu pickup; persist request + history satu transaksi. Lihat docs/api-contract.md, docs/data-model.md dan docs/acceptance-tests.md. |

## TODO

- [ ] Isi TEMPLATE sesuai docs/api-contract.md dan docs/data-model.md; DB/auth/AI masih TODO.
- [ ] Validasi role/ownership server dan transaksi poin/redeem; uji concurrency/rollback PostgreSQL.
- [ ] Jalankan tes dan perbarui README saat perilaku berubah.
