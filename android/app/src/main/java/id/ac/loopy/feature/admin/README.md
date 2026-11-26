# Peninjauan donasi oleh admin

| Rute | Alur dan kode |
| --- | --- |
| A01 Dashboard | [AdminDashboardViewModel.kt](AdminDashboardViewModel.kt) memuat ringkasan dan tiga pengajuan yang menunggu dari antrean. [AdminDashboardScreen.kt](AdminDashboardScreen.kt) membuka profil, antrean, dan detail. |
| A02 Pengajuan | [DonationRequestsViewModel.kt](DonationRequestsViewModel.kt) menerapkan debounce pencarian, filter status, refresh, dan cursor. [DonationRequestsScreen.kt](DonationRequestsScreen.kt) menampilkan hasil serta aksi memuat halaman berikutnya. |
| A03 Detail | [AdminDonationDetailViewModel.kt](AdminDonationDetailViewModel.kt) membaca detail, menyimpan draf tanggal/jam di Room, mengirim jadwal final atau alasan tolak, serta memajukan status. [AdminDonationDetailScreen.kt](AdminDonationDetailScreen.kt) menampilkan foto privat, alamat dan dialog keputusan. |

Jadwal final ditampilkan dalam WIB. Perubahan status mengirim versi pengajuan agar konflik bisa dideteksi. Backend mencatat transisi dijemput dan selesai serta memberi poin ketika selesai. [Repositori admin](../../data/repository/AdminRepositoryImpl.kt) · [API peninjauan](../../../../../../../../../../docs/donation-review-go.md) · [Draf Room](../../data/local/README.md)
