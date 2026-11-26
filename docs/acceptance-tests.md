# Acceptance criteria dan rencana uji

Panduan ini dipakai saat review fitur dan demo. Hasil unit test, tes Go dengan PostgreSQL, tes instrumen Compose, dan uji HP ke VM memberi bukti untuk lapisan yang berbeda. Catat hasil penerimaan untuk commit, APK, dan environment yang sedang diuji. [Riwayat bukti](verification.md) · [PRD](reference/PRD-v2.1.md)

## Persiapan pengujian

- Siapkan satu admin melalui CLI serta dua donatur uji. Gunakan inbox yang dikuasai untuk pengujian OTP. Pilih nama, nomor kontak, dan alamat fiktif.
- Siapkan satu foto JPEG/PNG yang jelas, satu foto buram, dan satu file yang tidak memenuhi batas. Pastikan koneksi backend serta konfigurasi SMTP dan Groq sesuai environment.
- Mulai dari landing pada APK yang baru dibuka. Catat base URL, commit backend, perangkat, dan build APK. Untuk kasus konkurensi, pakai database uji yang dapat dibuang.
- Simpan bukti berupa kode HTTP, riwayat status, saldo, atau tangkapan layar yang menyamarkan PII. [Setup perangkat](development.md#koneksi-android-ke-backend) · [Setup admin dan VM](deploy-lxc.md)

## Matriks penerimaan

| AC | Fitur | Uji dan hasil yang diharapkan |
| --- | --- | --- |
| AC-01 | F01 Landing | Buka aplikasi tanpa sesi. Hero, informasi produk, manfaat, dan langkah donasi tampil dan dapat dibaca saat scroll. |
| AC-02 | F01 Navigasi publik | Tekan CTA daftar dan kembali ke landing. Tombol Masuk membuka form login. Panah kembali dan tombol sistem mengikuti stack yang sama. |
| AC-03 | F02 Akun donatur | Daftar dengan email baru, uji kode salah, kirim ulang setelah cooldown dan masukkan kode terbaru. Akun dibuat setelah verifikasi. Masuk kembali memakai email/password. Cek validasi input, email duplikat, dan password salah. |
| AC-04 | F02/F07 Admin | Login via layar yang sama dengan akun provisioned. Admin menuju dashboard dan donatur menuju Beranda. Avatar membuka profil sesuai role. |
| AC-05 | F04 Pemilihan foto | Coba kamera dan galeri, batalkan pemilih, ganti foto, uji gambar terlalu besar dan tipe salah. Periksa tampilan setelah kembali dari aplikasi kamera. |
| AC-06 | F04/F05 Hasil AI | Verifikasi masing-masing cabang hasil dari fixture atau respons uji. Coba error provider dan retry. Pickup tersedia hanya untuk hasil yang berpotensi layak dengan konfirmasi. |
| AC-07 | F05/F06 Gate pickup | Dari hasil sendiri yang layak, lanjutkan setelah konfirmasi. Uji backend dengan hasil akun lain dan hasil ineligible. Mutasi ditolak. |
| AC-08 | F06 Submit pengajuan | Jumlah 0 dan tanggal lampau ditolak. Submit valid membuat `PENDING_REVIEW` versi 1 dengan riwayat awal. Ulangi payload dengan key sama dan periksa ID pengajuan sama. |
| AC-09 | F07 Keputusan admin | Jadwalkan dengan tanggal dan jam mendatang. Tolak pengajuan lain dengan alasan. Uji alasan kosong, jadwal lampau, dan request dengan role donatur atau versi lama. |
| AC-10 | F07 Jadwal final | Bandingkan preferensi awal dan jadwal admin pada detail. 09.30 WIB ditampilkan kembali sebagai 09.30 dan disimpan sebagai 02.30 UTC untuk tanggal yang sama. |
| AC-11 | F07/F08 Progres | Ubah status `PICKUP_SCHEDULED → PICKED_UP → COMPLETED`. Uji lompatan dan arah sebaliknya. Timeline memuat versi, waktu, dan akun pelaku yang sesuai. |
| AC-12 | F08 Donasiku | Dengan dua sesi, ubah status di admin lalu refresh donatur. Bandingkan ringkasan, timeline, dan alasan penolakan. Pada akun dengan 26 pengajuan, catat keterbatasan daftar Android yang masih mengambil halaman pertama. |
| AC-13 | F09 Kredit | Selesaikan satu pengajuan dengan kuantitas lebih dari satu. Saldo bertambah 10 tepat sekali. Submit perubahan status ulang dan cek ledger tetap satu kredit. |
| AC-14 | F09/F10 Saldo dan katalog | Bandingkan saldo API, Beranda dan Rewards. Katalog awal menyediakan badge 10, wallpaper 20 dan voucher demo 30. |
| AC-15 | F10 Konfirmasi | Batalkan dialog redeem dan cek saldo tetap. Konfirmasi dengan saldo cukup, lalu cek reward muncul di riwayat. |
| AC-16 | F10 Konsistensi transaksi | Di database uji, saldo 30 setelah redeem biaya 10 dan 20 menjadi 0. Periksa dua debit dan dua snapshot. Tes rollback menyimulasikan kegagalan salah satu langkah. |
| AC-17 | F10 Riwayat | Tutup dan masuk lagi. Riwayat memuat reward yang sama dan biaya pada waktu ditukar. |
| AC-18 | F10 Saldo kurang | Uji tombol dengan saldo kurang, request API, dan konkurensi. Saldo tidak negatif dan replay tidak mendebit dua kali. |
| AC-19 | F02/F08 Isolasi | Donatur kedua mencoba membaca detail pengajuan serta foto donatur pertama. Pastikan akses ditolak. Berganti akun lalu cek saldo, foto, serta riwayat yang ditampilkan. |
| AC-20 | F02/F07 Hak admin | Gunakan Bearer dari donatur pada rute admin dan periksa `403`. Tambahkan field role pada request pendaftaran dan pastikan payload ditolak. |

## Urutan demo utama

1. Daftar donatur uji, masukkan OTP, lalu cek saldo 0.
2. Unggah foto yang layak, buka form pickup, submit, dan catat ID serta versi awal.
3. Dari sesi admin, buka pengajuan dan jadwalkan penjemputan. Pada sesi donatur, refresh dan bandingkan jadwal final dengan preferensi.
4. Dari admin, tandai sudah dijemput dan selesai. Pada donatur, cek status akhir, riwayat, dan tambahan 10 poin.
5. Redeem badge, cek saldo menjadi 0 dan riwayat menyimpan satu reward. Ulangi dengan key yang sama di pengujian API.
6. Ulangi cabang penolakan pada pengajuan lain. Sertakan kasus foto tak layak dan saldo kurang.

## Pemeriksaan lintas fitur

| Skenario | Bukti |
| --- | --- |
| Navigasi profil dan tab | Admin dapat kembali dari profil ke dashboard lalu ke Pengajuan. Donatur dapat kembali dari profil lalu ke Donasiku. Kedua jalur tetap memakai sesi aktif. Beranda terbuka dari Donasiku dan Rewards. |
| Restart dan rotasi | Tema serta draf admin yang sudah tersimpan dapat dibaca setelah restart. Proses baru memerlukan login. Saat rotasi atau kembali dari kamera, periksa state, back stack, dan alur request. |
| Jaringan gagal | Uji ketika backend lambat atau tidak tersedia. Loading berakhir, pesan error terlihat, dan retry aman untuk operasi yang menggunakan key idempoten. |
| Aksesibilitas | Uji TalkBack, perbesaran font, label ikon, serta akses tombol di layar kecil. Catat layar yang diperiksa. |
| Build release | Periksa URL HTTPS, signing, respons API melalui host release, serta alur autentikasi dan foto. |

## Format catatan hasil

| Commit dan build | Environment dan perangkat | AC atau skenario | Hasil dan bukti | Status |
| --- | --- | --- | --- | --- |
| SHA serta versi APK | Lokal atau VM, model, versi Android | Misalnya AC-10 | Detail WIB, request dan respons yang sudah disamarkan | Lulus, gagal, atau belum diuji |

Tes Android JVM berada di [src/test](../android/app/src/test/README.md), tes UI/Room di [src/androidTest](../android/app/src/androidTest/README.md), dan tes Go dengan PostgreSQL di [suite HTTP dan store](../backend/go/README.md#pengujian).
