# S04 Beranda donatur

[HomeViewModel.kt](HomeViewModel.kt) memuat saldo dan donasi terakhir melalui dua request terpisah. [HomeUiState.kt](HomeUiState.kt) melacak loading dan kegagalan masing-masing hasil. [HomeScreen.kt](HomeScreen.kt) menyegarkan data saat kembali aktif dan menyediakan tombol retry.

Tombol `Cek pakaian` membuka S05. Tab menuju Donasiku atau Rewards, kartu donasi membuka detail, dan avatar membuka Profil secara langsung. Pengaturan serta logout tersedia dari profil. [API Beranda](../../data/remote/api/HomeApi.kt) · [Tes Beranda](../../../../../../../androidTest/java/id/ac/loopy/feature/HomeScreenTest.kt)
