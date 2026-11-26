# S07 Pengajuan penjemputan

Rute membawa `checkId` dari hasil pemeriksaan yang berpotensi layak. [PickupRequestScreen.kt](PickupRequestScreen.kt) mengumpulkan nomor kontak, alamat, kota, jumlah pakaian, preferensi tanggal/jam, dan catatan opsional. [PickupRequestUiState.kt](PickupRequestUiState.kt) memuat validasi dan tanggal dalam zona WIB.

[PickupRequestViewModel.kt](PickupRequestViewModel.kt) memuat detail pemeriksaan dan mengirim pengajuan ke API. Server menyimpan status awal `PENDING_REVIEW`, lalu Android menampilkan konfirmasi dan dapat membuka Donasiku. Percobaan ulang selama proses yang sama memakai key idempoten dari repository. Isian donatur saat ini disimpan di state layar. [Repo donasi](../../data/repository/DonationRepositoryImpl.kt) · [Draf admin](../admin/README.md)
