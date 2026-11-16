# rewards

Layer backend rewards. Isi route/service sesuai kontrak PRD; /health saja yang aktif saat scaffold.

## Isi file dan perannya

| File/folder | Peran dan isi yang diharapkan |
| --- | --- |
| `RedemptionService.kt` | TEMPLATE — TODO: lock wallet, cek saldo >= cost, debit+ledger+redemption atomik; rollback saat gagal; idempotency key scoped user + payload. Lihat docs/api-contract.md, docs/data-model.md dan docs/acceptance-tests.md. |
| `RewardCatalog.kt` | TEMPLATE — TODO: seed katalog Eco Supporter Badge 10, wallpaper 20, Voucher Demo 30; tidak ada admin CRUD reward dalam MVP. Lihat docs/api-contract.md, docs/data-model.md dan docs/acceptance-tests.md. |
| `RewardRoutes.kt` | TEMPLATE — TODO: GET balance/catalog/history; POST redemptions dengan idempotency key; respons voucher demo. Lihat docs/api-contract.md, docs/data-model.md dan docs/acceptance-tests.md. |

## TODO

- [ ] Isi TEMPLATE sesuai docs/api-contract.md dan docs/data-model.md; DB/auth/AI masih TODO.
- [ ] Validasi role/ownership server dan transaksi poin/redeem; uji concurrency/rollback PostgreSQL.
- [ ] Jalankan tes dan perbarui README saat perilaku berubah.
