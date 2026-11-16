# server

Area tes perilaku PRD. Tulis assertions bermakna untuk validasi/error/role/transaksi dan persistence.

## Isi file dan perannya

| File/folder | Peran dan isi yang diharapkan |
| --- | --- |
| `AuthorizationTest.kt` | TEMPLATE — TODO tes nyata: USER 403 semua /admin; U1 tidak mengakses check/donation/redemption U2. Belum ada test berjalan untuk AC ini. |
| `HealthTest.kt` | Ktor testApplication memeriksa HTTP200 dan mode template. |
| `PickupValidationTest.kt` | TEMPLATE — TODO tes nyata: quantity>=1, tanggal tidak lampau, eligible owner check+clean/safe, retry idempotent. Belum ada test berjalan untuk AC ini. |
| `PointTransactionTest.kt` | TEMPLATE — TODO tes nyata: PostgreSQL completed+10 sekali saat retry/concurrency, rollback status/history/ledger. Belum ada test berjalan untuk AC ini. |
| `RedemptionTransactionTest.kt` | TEMPLATE — TODO tes nyata: PostgreSQL insufficient/concurrent saldo, idempotency payload mismatch, rollback debit/redemption. Belum ada test berjalan untuk AC ini. |

## TODO

- [ ] Isi TEMPLATE sesuai docs/api-contract.md dan docs/data-model.md; DB/auth/AI masih TODO.
- [ ] Validasi role/ownership server dan transaksi poin/redeem; uji concurrency/rollback PostgreSQL.
- [ ] Jalankan tes dan perbarui README saat perilaku berubah.
