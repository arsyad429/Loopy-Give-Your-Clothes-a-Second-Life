# auth

Layer backend auth. Isi route/service sesuai kontrak PRD; /health saja yang aktif saat scaffold.

## Isi file dan perannya

| File/folder | Peran dan isi yang diharapkan |
| --- | --- |
| `AuthRoutes.kt` | TEMPLATE — TODO: POST register/login/logout, GET me, password hash, token expiry; register selalu USER, admin seed privat. Lihat docs/api-contract.md, docs/data-model.md dan docs/acceptance-tests.md. |
| `AuthService.kt` | TEMPLATE — TODO: validasi email unik, hash Argon2id/bcrypt, session/token verification; jangan hardcode akun admin. Lihat docs/api-contract.md, docs/data-model.md dan docs/acceptance-tests.md. |
| `Authorization.kt` | TEMPLATE — TODO: autentikasi bearer; requireRole ADMIN; ownership dari token, bukan userId kiriman klien. Lihat docs/api-contract.md, docs/data-model.md dan docs/acceptance-tests.md. |

## TODO

- [ ] Isi TEMPLATE sesuai docs/api-contract.md dan docs/data-model.md; DB/auth/AI masih TODO.
- [ ] Validasi role/ownership server dan transaksi poin/redeem; uji concurrency/rollback PostgreSQL.
- [ ] Jalankan tes dan perbarui README saat perilaku berubah.
