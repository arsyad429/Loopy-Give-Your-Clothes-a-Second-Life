# admin

Layer backend admin. Isi route/service sesuai kontrak PRD; /health saja yang aktif saat scaffold.

## Isi file dan perannya

| File atau folder | Kegunaan |
| --- | --- |
| [`AdminDonationService.kt`](AdminDonationService.kt) | TEMPLATE — TODO: schedule hanya pending dan wajib jadwal; reject alasan; scheduled→picked_up→completed; audit actor + previous/new status. Lihat docs/api-contract.md, docs/data-model.md dan docs/acceptance-tests.md. |
| [`AdminRoutes.kt`](AdminRoutes.kt) | TEMPLATE — TODO: GET summary/requests/detail; POST schedule/reject/status dengan requireRole ADMIN setiap endpoint. Lihat docs/api-contract.md, docs/data-model.md dan docs/acceptance-tests.md. |
| [`CompletionTransaction.kt`](CompletionTransaction.kt) | TEMPLATE — TODO: lock donation + wallet konsisten, update completed, +10 ledger unik donationId dan saldo dalam SATU transaksi; retry tidak menambah poin. Lihat docs/api-contract.md, docs/data-model.md dan docs/acceptance-tests.md. |

## TODO

- [ ] Isi TEMPLATE sesuai docs/api-contract.md dan docs/data-model.md; DB/auth/AI masih TODO.
- [ ] Validasi role/ownership server dan transaksi poin/redeem; uji concurrency/rollback PostgreSQL.
- [ ] Jalankan tes dan perbarui README saat perilaku berubah.
