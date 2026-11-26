# Donasiku dan detail status

[MyDonationsViewModel.kt](MyDonationsViewModel.kt) memuat pengajuan milik akun lalu mengurutkannya dari yang terbaru. [MyDonationsScreen.kt](MyDonationsScreen.kt) menyediakan tarik untuk menyegarkan, pencarian, filter lokal, dan tautan ke detail S09. Daftar Android saat ini memakai halaman pertama API, yang berisi hingga 25 item secara default.

[DonationDetailViewModel.kt](DonationDetailViewModel.kt) membaca detail berdasarkan ID. [DonationDetailScreen.kt](DonationDetailScreen.kt) menampilkan status, alasan penolakan, jadwal final, dan timeline dari server. Tab Beranda, Donasiku, dan Rewards ditangani oleh [graf Android](../../ui/LoopyApp.kt). [Kontrak donasi](../../../../../../../../../../docs/api-contract.md) · [Tes ViewModel](../../../../../../../test/java/id/ac/loopy/feature/donations/README.md)
