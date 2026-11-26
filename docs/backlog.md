# Backlog implementasi

Alur inti akun, foto, pickup, admin, status, poin, dan redeem sudah terhubung pada Android dan Go. Backlog ini mencatat pekerjaan yang tersisa atau perlu dibuktikan. Penanggung jawab tugas kode dapat ditetapkan tim per issue, mengikuti pembagian dokumentasi [PRD §12](reference/PRD-v2.1.md). [Riwayat verifikasi](verification.md)

## Kesiapan demo dan rilis

| Pekerjaan | Kondisi saat ini | Selesai bila |
| --- | --- | --- |
| Pagination Donasiku | API mengirim `nextCursor` dan admin sudah memuat halaman lanjutan. Repository Donasiku baru mengambil halaman pertama, sehingga akun dengan lebih dari 25 pengajuan dapat kehilangan hasil dalam daftar. | Akun dengan lebih dari 25 pengajuan dapat melihat, mencari, dan memfilter semua item yang relevan. Uji refresh, perpindahan filter, dan ID ganda. [Repository Android](../android/app/src/main/java/id/ac/loopy/data/repository/DonationRepositoryImpl.kt) |
| Ketahanan submit pickup | Key retry tersimpan di memori repository dan form donatur berada di state layar. Draf jadwal admin sudah memakai Room. | Pada koneksi putus setelah server commit, hasil dapat direkonsiliasi setelah proses aplikasi dimulai ulang. Draf tidak tercampur saat berganti akun. [Form pickup](../android/app/src/main/java/id/ac/loopy/feature/pickup/README.md) |
| Pemulihan OTP | Challenge aktif berada di `LoginViewModel` selama proses hidup. | Tim menguji force-stop atau jaringan terputus selama pendaftaran dan mencatat cara memperoleh kode baru beserta pesan yang terlihat. [OTP](auth-go.md) |
| Alur lengkap di VM | CI dan beberapa tes perangkat telah dijalankan. Uji regresi menyeluruh perlu dilakukan pada versi APK dan server yang sama. | Lakukan satu demo dua akun dari daftar dengan inbox, analisis foto, review, jadwal, selesai, hingga redeem. Catat bukti masing-masing tahap. [Acceptance](acceptance-tests.md) |
| Desain dan aksesibilitas | Screenshot dan metadata tersedia. Pemeriksaan visual menyeluruh per node masih perlu dicatat. | Tangkapan layar dibandingkan untuk state loading, empty, error, dan sukses. TalkBack dan font besar diperiksa. [Peta layar](screen-map.md) |
| Distribusi APK | Properti URL HTTPS dan build release tersedia. | APK ditandatangani tim, dibangun dengan URL HTTPS yang terjangkau, dan lulus smoke test di HP. [Release](deploy-lxc.md#7-connect-android-or-a-public-domain) |

## Ketika mencatat pekerjaan baru

Sertakan rute layar atau endpoint, langkah reproduksi, data uji, commit, serta hasil yang diharapkan. Untuk perubahan transaksi, tambahkan kondisi retry, konkurensi, dan bukti rollback pada database uji. Untuk perubahan UI, sertakan role, ukuran layar, dan kondisi data yang dipakai.

Keputusan produk yang perlu disepakati tim mencakup wilayah layanan, jam pickup, dan jika ada perluasan cakupan AI. Di luar MVP terdapat kurir terpisah, GPS live, pembayaran, push notification, voucher merchant nyata, hadiah fisik, marketplace, dan optimasi rute. Pickup serta reward demo digunakan dalam simulasi akademik.
