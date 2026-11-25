# S04 Beranda pengguna

S04 menyapa akun aktif, membuka pemeriksaan pakaian S05, dan menampilkan saldo Loopy Points serta donasi terakhir dari Go. Kartu donasi membuka detail S09. Navigasi bawah mengarah ke Donasiku dan Rewards. Menu akun menyediakan logout.

| File | Kegunaan |
| --- | --- |
| [HomeScreen.kt](HomeScreen.kt) | Tata letak S04, tombol pemeriksaan, kartu saldo dan donasi, navigasi, serta menu akun. |
| [HomeUiState.kt](HomeUiState.kt) | State saldo, donasi, loading, dan kegagalan untuk setiap bagian. |
| [HomeViewModel.kt](HomeViewModel.kt) | Memuat saldo dan donasi melalui HomeRepository, termasuk refresh saat beranda dibuka kembali. |

Home menampilkan saldo nol hanya jika Go mengirimkannya. Ketika refresh gagal, data yang sebelumnya berhasil dimuat tetap terlihat dengan keterangan. Mode demo debug tetap memakai Room. S10 untuk akun aktif masih berupa halaman sementara.
