# Backlog implementasi

PRD belum membagi pemilik implementasi. Neva/Rio/Alex/Arsyad tetap mengikuti tanggung jawab dokumentasi §12; tim menetapkan pemilik tugas kode terpisah.

| Urutan | Pekerjaan | Tempat awal | Selesai bila |
| --- | --- | --- | --- |
| 1 | Backend Go/auth sudah dipilih; sepakati AI, wilayah/waktu pickup dan scope tambahan | API/data model/screen map | Kontrak tim disetujui |
| 2 | Auth Go + migrasi akun/sesi + CLI admin tersedia; lanjut integrasi Android dan ownership fitur bisnis | backend/go, docs/auth-go.md | Backend auth teruji; isolasi bisnis diverifikasi saat route tersedia |
| 3 | Landing/login/register+session graph | feature landing/auth, navigation | AC01–04,19–20 |
| 4 | Home ringkasan/status/saldo | feature home, repositories | Loading/empty/error dan data benar |
| 5 | Kamera/galeri+AI proof of concept | clothingcheck, PhotoValidator, backend checks/storage | AC05–07, tiga hasil/error |
| 6 | Pickup submit idempotent | pickup, backend donations | AC07–08, request/history atomik |
| 7 | Admin dashboard/list/detail/schedule/reject | admin Android/backend | AC09–11, role server |
| 8 | My Donations+tracking detail | donations | AC12,19 tanpa GPS |
| 9 | Completed+ledger+10 sekali | CompletionTransaction | AC13 retry/concurrency |
| 10 | Katalog/redeem/history | rewards Android/backend | AC14–18 rollback/concurrency |
| 11 | UI Figma final+integrasi/demo | components/theme/features | AC01–20+aksesibilitas |
| Setelah MVP | Profil/settings/OTP | profile/settings/verification | Kontrak/scope disepakati |

TODO visual: ambil high-fidelity Figma context, token/font Plus Jakarta Sans dan aset resmi ketika akses tersedia. Screenshot acuan tidak dipakai sebagai aset UI. UI placeholder bukan implementasi desain final.

Di luar MVP: kurir terpisah, GPS live, pembayaran, push notification, voucher merchant nyata, hadiah fisik, marketplace dan optimasi rute. Pickup/reward simulasi akademik.
