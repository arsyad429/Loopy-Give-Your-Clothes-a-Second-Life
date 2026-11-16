# server

Direktori server mengelompokkan source/config Loopy. README subfolder menjelaskan implementasi spesifik.

## Isi file dan perannya

| File/folder | Peran dan isi yang diharapkan |
| --- | --- |
| `Application.kt` | Ktor Netty port8080, GET /health JSON mode template. Koneksi DB belum aktif. |
| `admin/` | Baca README di subfolder admin untuk peran file dan TODO. |
| `auth/` | Baca README di subfolder auth untuk peran file dan TODO. |
| `checks/` | Baca README di subfolder checks untuk peran file dan TODO. |
| `database/` | Baca README di subfolder database untuk peran file dan TODO. |
| `donations/` | Baca README di subfolder donations untuk peran file dan TODO. |
| `rewards/` | Baca README di subfolder rewards untuk peran file dan TODO. |
| `storage/` | Baca README di subfolder storage untuk peran file dan TODO. |

## TODO

- [ ] Isi TEMPLATE sesuai docs/api-contract.md dan docs/data-model.md; DB/auth/AI masih TODO.
- [ ] Validasi role/ownership server dan transaksi poin/redeem; uji concurrency/rollback PostgreSQL.
- [ ] Jalankan tes dan perbarui README saat perilaku berubah.
