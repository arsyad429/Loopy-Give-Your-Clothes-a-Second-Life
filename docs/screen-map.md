# Pemetaan PRD/Figma/Kotlin

Metadata dibaca 16 November 2026. Detail design context terkena kuota Figma Starter; visual/token/aset belum diverifikasi. PRD memuat 13 layar wajib; desain terbaru menambah empat layar serta state/dialog. Screenshot hanya referensi.

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

Figma Register memuat username, PRD hanya nama/email/password. Profil lengkap/settings/OTP bukan kewajiban PRD2.1; jangan mewajibkannya tanpa perluasan scope. MVP cukup menu akun/logout.

TODO: ambil high-fidelity context per node+screenshot, download aset Figma resmi, typography/color/spacing ke theme, UI Compose responsif, verifikasi screenshot emulator. Tidak ada temporary asset URL di app.
