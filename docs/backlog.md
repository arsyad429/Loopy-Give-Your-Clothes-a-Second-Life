# Backlog implementasi

PRD belum membagi pemilik implementasi. Neva/Rio/Alex/Arsyad tetap mengikuti tanggung jawab dokumentasi §12; tim menetapkan pemilik tugas kode terpisah.

| Urutan | Pekerjaan | Tempat awal | Selesai bila |
| --- | --- | --- | --- |
| 1 | Backend Go/auth sudah dipilih; sepakati AI, wilayah/waktu pickup dan scope tambahan | API/data model/screen map | Kontrak tim disetujui |
| 2 | Auth Go, CLI admin, login/register Android, dan refresh sesi di memori | backend/go, docs/auth-go.md | Backend auth teruji; isolasi bisnis diverifikasi saat route tersedia |
| 3 | Landing/login/register+session graph | feature landing/auth, navigation | AC01–04,19–20 |
| 4 | Home ringkasan/status/saldo | feature home, repositories | Loading/empty/error dan data benar |
| 5 | Sambungkan pemeriksaan Android ke API | clothingcheck, repository Android, backend checks/storage | Retrofit memakai access token Go untuk upload, detail, dan foto privat. Uji pada perangkat untuk memverifikasi kamera dan navigasi. |
| 6 | Pickup submit idempotent | pickup, backend donations | AC07–08, request/history atomik |
| 7 | API Go untuk ringkasan, antrean, detail, jadwal, dan penolakan sudah tersedia. Lanjut integrasi Android dan penyediaan foto privat. | admin Android, backend/go, docs/donation-review-go.md | AC09–11 teruji dengan data sungguhan |
| 8 | My Donations+tracking detail | donations | AC12,19 tanpa GPS |
| 9 | Transisi status admin dan kredit +10 melalui API terintegrasi Android | feature/admin, backend/go | Uji status dan idempotensi dengan PostgreSQL |
| 10 | Katalog, saldo, redeem dan riwayat terintegrasi Android ke API akun aktif | feature/rewards, RewardRepositoryImpl | Uji Retrofit, state dan UI; lanjut verifikasi VM tim |
| 11 | UI Figma final+integrasi/demo | components/theme/features | AC01–20+aksesibilitas |
| Tambahan Figma | Profil, Settings, username, dan OTP saat daftar sudah terhubung | profile/settings/verification, docs/auth-go.md | Uji pengiriman dari VM dengan kredensial SMTP dan APK terbaru |

S05 dan S06 menggunakan font Plus Jakarta Sans dan ikon dari desain. Layar lain mengikuti peta Figma saat diimplementasikan.

Di luar MVP: kurir terpisah, GPS live, pembayaran, push notification, voucher merchant nyata, hadiah fisik, marketplace dan optimasi rute. Pickup/reward simulasi akademik.
