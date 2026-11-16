# navigation

Direktori navigation mengelompokkan source/config Loopy. README subfolder menjelaskan implementasi spesifik.

## Isi file dan perannya

| File/folder | Peran dan isi yang diharapkan |
| --- | --- |
| `LoopyNavGraph.kt` | TEMPLATE — TODO: ganti harness dengan graph public/user/admin berdasarkan sesi backend. Route hanya checkId/donationId, ambil melalui SavedStateHandle; jangan kirim foto/alamat. Landing publik; login USER→Home, ADMIN→Dashboard; logout bersihkan back stack+cache. Guard UI tidak mengganti authorization backend; deep link admin user biasa wajib ditolak. |

## TODO

- [ ] Lanjutkan file template sesuai docs/backlog.md, docs/api-contract.md dan docs/acceptance-tests.md.
- [ ] Pertahankan UI → ViewModel → Repository → Data Source dan konfigurasi yang dapat dibuild.
- [ ] Perbarui daftar file dan bukti cek setelah implementasi berubah.
