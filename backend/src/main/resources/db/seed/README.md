# seed

Direktori seed mengelompokkan source/config Loopy. README subfolder menjelaskan implementasi spesifik.

## Isi file dan perannya

| File atau folder | Kegunaan |
| --- | --- |
| [`admin.sql`](admin.sql) | Template seed admin privat, password hash, tidak ada credential hardcode. |
| [`rewards.sql`](rewards.sql) | Template seed idempotent badge10, wallpaper20, Voucher Demo30. |

## TODO

- [ ] Isi TEMPLATE sesuai docs/api-contract.md dan docs/data-model.md; DB/auth/AI masih TODO.
- [ ] Validasi role/ownership server dan transaksi poin/redeem; uji concurrency/rollback PostgreSQL.
- [ ] Jalankan tes dan perbarui README saat perilaku berubah.
