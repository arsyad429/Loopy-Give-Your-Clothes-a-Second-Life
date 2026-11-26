# Fitur dan layar Android

Mulai dari [graf navigasi](../ui/LoopyApp.kt) untuk mencari rute dan susunan dependency. Layar Compose menerima state dari ViewModel dan meneruskan aksi pengguna. Repository menghubungkan fitur ke API Go, sementara draf jadwal admin disimpan di Room.

| Alur | Folder |
| --- | --- |
| Landing, masuk, daftar, dan OTP | [Landing](landing/README.md) · [Auth](auth/README.md) · [Verifikasi email](verification/README.md) |
| Beranda, pemeriksaan, dan pengajuan pickup | [Beranda](home/README.md) · [Pemeriksaan](clothingcheck/README.md) · [Pickup](pickup/README.md) |
| Status pengajuan dan penukaran poin | [Donasiku](donations/README.md) · [Rewards](rewards/README.md) |
| Tinjau, jadwalkan, dan selesaikan donasi | [Admin](admin/README.md) |
| Profil dan pengaturan akun | [Profil](profile/README.md) · [Pengaturan](settings/README.md) |

[Pemetaan rute dan Figma](../../../../../../../../../docs/screen-map.md) · [Tes fitur](../../../../../../test/java/id/ac/loopy/feature/README.md)
