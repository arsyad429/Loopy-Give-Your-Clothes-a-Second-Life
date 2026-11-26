# Profil donatur dan admin

[UserProfileScreen.kt](UserProfileScreen.kt) dan [AdminProfileScreen.kt](AdminProfileScreen.kt) memakai `AccountProfileScreen` bersama. Avatar membuka halaman sesuai peran. Profil membaca nama, email, username, dan status verifikasi lewat `GET /v1/auth/me`. [AccountViewModel.kt](AccountViewModel.kt) memiliki state profil dan mutasi yang dipakai [Pengaturan](../settings/README.md).

Panah kembali menjalankan navigasi ke layar sebelumnya. Tombol keluar ada di konten profil dan hanya aktif ketika tujuan profil berada pada lifecycle `RESUMED`. Profil donatur menyediakan jalan ke Rewards dan profil admin ke Dashboard. [Graf navigasi](../../ui/LoopyApp.kt) · [Tes alur kembali](../../../../../../../androidTest/java/id/ac/loopy/feature/ProfileNavigationTest.kt)

File `UserProfileViewModel.kt`, `AdminProfileViewModel.kt`, dan pasangan UiState-nya menyimpan kerangka lama. Kedua profil menggunakan `AccountViewModel`.
