# Pemetaan PRD/Figma/Kotlin

Metadata node diambil pada 16 November 2026. Peta memuat 13 layar MVP dan empat perluasan desain. Gunakan [arsip Figma](../UI%20design/README.md) sebagai rujukan visual dan [LoopyApp](../android/app/src/main/java/id/ac/loopy/ui/LoopyApp.kt) sebagai graf Android yang berjalan.

| ID | Layar | Scope | File Android | Node Figma |
| --- | --- | --- | --- | --- |
| S01 | Landing Page | MVP | feature/landing/LandingScreen.kt | [2:82](https://www.figma.com/design/ZDndr4gOItY7elDq9w7YA3/Loopy?node-id=2-82) |
| S02 | Login | MVP | feature/auth/LoginScreen.kt | [2:83](https://www.figma.com/design/ZDndr4gOItY7elDq9w7YA3/Loopy?node-id=2-83) |
| S03 | Register | MVP | feature/auth/RegisterScreen.kt | [2:84](https://www.figma.com/design/ZDndr4gOItY7elDq9w7YA3/Loopy?node-id=2-84) |
| S04 | Home Dashboard | MVP | feature/home/HomeScreen.kt | [2:87](https://www.figma.com/design/ZDndr4gOItY7elDq9w7YA3/Loopy?node-id=2-87) |
| S05 | AI Clothing Detector | MVP | feature/clothingcheck/ClothingDetectorScreen.kt | [2:88](https://www.figma.com/design/ZDndr4gOItY7elDq9w7YA3/Loopy?node-id=2-88) |
| S06 | Clothing Check Result | MVP | feature/clothingcheck/CheckResultScreen.kt | [2:89](https://www.figma.com/design/ZDndr4gOItY7elDq9w7YA3/Loopy?node-id=2-89) |
| S07 | Pickup Request Form | MVP | feature/pickup/PickupRequestScreen.kt | [2:90](https://www.figma.com/design/ZDndr4gOItY7elDq9w7YA3/Loopy?node-id=2-90) |
| S08 | My Donations | MVP | feature/donations/MyDonationsScreen.kt | [2:93](https://www.figma.com/design/ZDndr4gOItY7elDq9w7YA3/Loopy?node-id=2-93) |
| S09 | Donation Detail | MVP | feature/donations/DonationDetailScreen.kt | [2:94](https://www.figma.com/design/ZDndr4gOItY7elDq9w7YA3/Loopy?node-id=2-94) |
| S10 | Rewards & My Rewards | MVP | feature/rewards/RewardsScreen.kt | [2:95](https://www.figma.com/design/ZDndr4gOItY7elDq9w7YA3/Loopy?node-id=2-95) |
| A01 | Admin Dashboard | MVP | feature/admin/AdminDashboardScreen.kt | [2:98](https://www.figma.com/design/ZDndr4gOItY7elDq9w7YA3/Loopy?node-id=2-98) |
| A02 | Donation Requests | MVP | feature/admin/DonationRequestsScreen.kt | [2:99](https://www.figma.com/design/ZDndr4gOItY7elDq9w7YA3/Loopy?node-id=2-99) |
| A03 | Admin Donation Detail | MVP | feature/admin/AdminDonationDetailScreen.kt | [2:100](https://www.figma.com/design/ZDndr4gOItY7elDq9w7YA3/Loopy?node-id=2-100) |
| S11 | Profil User | design-extension | feature/profile/UserProfileScreen.kt | [19:472](https://www.figma.com/design/ZDndr4gOItY7elDq9w7YA3/Loopy?node-id=19-472) |
| S12 | Settings | design-extension | feature/settings/SettingsScreen.kt | [19:485](https://www.figma.com/design/ZDndr4gOItY7elDq9w7YA3/Loopy?node-id=19-485) |
| A04 | Profil Admin | design-extension | feature/profile/AdminProfileScreen.kt | [19:498](https://www.figma.com/design/ZDndr4gOItY7elDq9w7YA3/Loopy?node-id=19-498) |
| S13 | Verifikasi Email | design-extension | feature/verification/EmailVerificationScreen.kt | [19:511](https://www.figma.com/design/ZDndr4gOItY7elDq9w7YA3/Loopy?node-id=19-511) |


Dialog/state: PickupSuccess6:663, RedeemConfirm6:674, RejectRequest6:687, Schedule6:703, NeedsRecheck6:712, NotRecommended6:723, Reward-ku6:734. UI kit2:51. Tambahan settings admin19:524, tersimpan19:537, OTP/validasi19:683.

Desain Register menambahkan username opsional. Profil, Settings, dan OTP saat daftar terhubung sebagai perluasan dari baseline PRD 2.1. [Catatan implementasi](reference/PRD-v2.1.md#lampiran-rujukan-implementasi)

Untuk review visual, ambil tampilan node terkait dan screenshot APK pada perangkat. Catat perbedaan warna, tipografi, jarak, ikon, serta state. Arsip metadata awal belum menjadi bukti kesesuaian visual seluruh layar.

## Jalur rute yang berjalan

| Alur | Rute dan kondisi |
| --- | --- |
| Buka aplikasi | S01 membuka S02 atau S03. |
| Pendaftaran | S03 menerima challenge dan membuka S13. Setelah kode benar, sesi donatur membuka S04. |
| Masuk | S02 menggunakan role dari respons. `USER` menuju S04, `ADMIN` menuju A01. |
| Pemeriksaan dan pickup | S04 → S05 → `s06/{checkId}` → `s07/{checkId}`. Lanjut ke S07 hanya dari hasil layak dan konfirmasi. Submit berhasil dapat membuka S08. |
| Daftar dan reward | S04, S08 dan S10 terhubung melalui tab. S08 membuka `s09/{id}`. |
| Keputusan admin | A01 → A02 → `a03/{id}`. Detail membuka dialog jadwal, penolakan, atau konfirmasi status sesuai state. |
| Profil dan pengaturan | Avatar donatur membuka S11, avatar admin membuka A04. Keduanya dapat menuju S12. Panah kembali mem-pop rute dan aksi keluar hanya diproses ketika profil aktif. |

## Memeriksa tiap state desain

- Untuk S05 dan S06, ambil snapshot sebelum memilih foto, saat analisis, dan untuk ketiga kategori hasil. Bandingkan ikon dan aset yang dipakai.
- Untuk S08, S09, A02 dan A03, periksa keadaan kosong, gagal, loading, serta daftar/detail berisi data. Pastikan preferensi dan jadwal final terbaca sebagai field berbeda.
- Untuk S10, uji saldo kurang, dialog konfirmasi, dan riwayat setelah penukaran. Untuk S13, uji kode salah, cooldown dan kirim ulang.
- Gunakan [kriteria penerimaan](acceptance-tests.md) untuk memisahkan review visual dan verifikasi alur. Mulai dari [kode fitur](../android/app/src/main/java/id/ac/loopy/feature/README.md) untuk mencari ViewModel dan data layar.
