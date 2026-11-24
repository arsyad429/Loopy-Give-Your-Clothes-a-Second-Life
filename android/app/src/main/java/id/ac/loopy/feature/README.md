# feature

Layar Compose, ViewModel dan state UI per fitur. Login, register dan pemeriksaan terhubung ke Go.

## Isi file dan perannya

| File atau folder | Kegunaan |
| --- | --- |
| [`admin/`](admin/README.md) | A02 dan A03 memakai data contoh untuk pratinjau antrean, detail, jadwal, dan penolakan. API admin Go masih perlu disambungkan ke layar. |
| [`auth/`](auth/README.md) | S02 dan S03 memvalidasi form dan menghubungi Go melalui LoginViewModel. Role sesi menentukan rute pengguna. |
| [`clothingcheck/`](clothingcheck/README.md) | S05 memeriksa kualitas foto sebelum upload. S06 menampilkan keputusan AI, penjelasan, dan foto privat melalui sesi Go. |
| [`donations/`](donations/README.md) | Rencana layar riwayat dan detail donasi S08–S09. Integrasi menunggu endpoint pickup pengguna. |
| [`home/`](home/README.md) | S04 menyapa pengguna yang sudah masuk dan menyediakan tombol Periksa pakaian untuk membuka S05. |
| [`landing/`](landing/README.md) | S01 adalah pembuka aplikasi. Pengguna memilih masuk atau mendaftar sebelum membuka pemeriksaan. |
| [`pickup/`](pickup/README.md) | Rute S07 menerima checkId dari hasil yang berpotensi layak. Form dan pengajuan pickup masih dalam rencana. |
| [`profile/`](profile/README.md) | Template profil pengguna S11 dan profil admin A04 untuk data akun dan tindakan terkait sesi. |
| [`rewards/`](rewards/README.md) | Template S10 untuk katalog hadiah dan riwayat penukaran. Integrasi menunggu API Go untuk poin dan rewards. |
| [`settings/`](settings/README.md) | Template S12 untuk preferensi pengguna. Tema yang aktif dapat diuji melalui katalog debug dan disimpan dengan DataStore. |
| [`verification/`](verification/README.md) | S13 merupakan pratinjau verifikasi email pada katalog debug. Penerbitan dan konfirmasi OTP masih dalam rencana. |
