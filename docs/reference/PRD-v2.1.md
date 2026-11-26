# Product Requirements Document (PRD)
# Loopy — Give Your Clothes a Second Life! ♻️

| Informasi | Keterangan |
|---|---|
| Product Name | Loopy |
| Document Type | Product Requirements Document |
| Version | 2.1 |
| Status | Draft — Pending Team Approval |
| Platform | Mobile Application |
| Category | Sustainability / Clothing Donation |
| Project Manager | Neva |
| Team Members | Neva, Rio, Alex, Arsyad |
| Reference Application | ReWear — Garuda Hacks 2025 |

---

# 1. Product Overview

## 1.1 Product Description

**Loopy** adalah aplikasi mobile berbasis AI yang dirancang untuk mempermudah masyarakat dalam mendonasikan pakaian bekas yang masih layak pakai.

Melalui Loopy, pengguna dapat memeriksa kondisi pakaian menggunakan AI, mengajukan layanan penjemputan pakaian (*Pickup Request*), memantau perkembangan donasi, serta memperoleh poin yang dapat ditukarkan dengan berbagai reward.

Loopy mengusung tagline **"Give Your Clothes a Second Life!"**, yaitu memberikan kesempatan kedua bagi pakaian yang sudah tidak digunakan agar dapat bermanfaat kembali.

Berbeda dengan sistem donasi konvensional yang mengharuskan donatur mengunjungi lokasi tertentu, Loopy menawarkan konsep penjemputan pakaian langsung dari alamat pengguna.

Aplikasi ini dikembangkan sebagai proyek akademik yang mengadaptasi konsep ReWear dari Garuda Hacks 2025.

## 1.2 Problem Statement

Permasalahan yang ingin diselesaikan Loopy:

1. Banyak masyarakat memiliki pakaian bekas yang masih layak pakai tetapi tidak digunakan.
2. Pengguna kesulitan menentukan apakah pakaian masih layak didonasikan.
3. Proses donasi konvensional sering mengharuskan pengguna mengantarkan pakaian sendiri.
4. Keterbatasan waktu dan mobilitas mengurangi keinginan untuk berdonasi.
5. Pengguna tidak selalu mengetahui perkembangan donasi yang telah diajukan.
6. Kurangnya insentif dapat membuat pengguna menunda kegiatan donasi.

## 1.3 Proposed Solution

Loopy menyediakan solusi melalui:

1. **AI Clothing Detector:** Pemeriksaan awal kondisi pakaian melalui foto.
2. **Pickup Service:** Pengajuan penjemputan pakaian dari rumah.
3. **Donation Tracking:** Pemantauan status donasi.
4. **Admin Management:** Pengelolaan pengajuan dan jadwal penjemputan.
5. **Loopy Rewards:** Sistem poin yang dapat ditukarkan dengan hadiah.
6. **Informative Landing Page:** Pengenalan aplikasi, manfaat, dan cara kerja Loopy bagi pengguna baru.

## 1.4 Product Goals

- Mempermudah proses donasi pakaian.
- Mengurangi hambatan donasi melalui sistem pickup.
- Membantu pemeriksaan awal kelayakan pakaian dengan AI.
- Memberikan transparansi perkembangan donasi.
- Memotivasi pengguna berdonasi melalui sistem reward.
- Mendukung pengurangan limbah tekstil.

## 1.5 Unique Value Proposition

**"Check Your Clothes, Schedule a Pickup, and Get Rewarded for Giving!"**

Loopy memungkinkan pengguna mengecek pakaian, meminta penjemputan, dan memperoleh reward dari donasi tanpa harus mencari atau mendatangi tempat donasi sendiri.

---

# 2. Target Users

## 2.1 User / Donatur

Pengguna yang memiliki pakaian bekas dan ingin mendonasikannya dengan mudah.

**Target pengguna:**
- Mahasiswa dan pelajar.
- Pekerja.
- Masyarakat umum.
- Individu yang peduli lingkungan.

**Kebutuhan user:**
- Memahami manfaat aplikasi sebelum mendaftar.
- Memeriksa kelayakan pakaian.
- Mengajukan penjemputan dari rumah.
- Memantau status donasi.
- Mengumpulkan dan menukarkan poin.

## 2.2 Admin / Pengelola

Admin bertanggung jawab mengelola pengajuan donasi dan proses penjemputan.

**Tugas admin:**
- Melihat pengajuan donasi.
- Memeriksa data dan hasil AI.
- Menerima atau menolak pengajuan.
- Menentukan jadwal penjemputan.
- Memperbarui status donasi.
- Melakukan monitoring melalui dashboard.

**Catatan:** Pada MVP, tidak terdapat role kurir terpisah. Proses pickup disimulasikan melalui dashboard admin.

---

# 3. Product Scope

## 3.1 MVP Features

| ID | Feature | Priority |
|---|---|---|
| F01 | Landing Page | P0 |
| F02 | Login & Register | P0 |
| F03 | Home Dashboard | P0 |
| F04 | AI Clothing Detector | P0 |
| F05 | Clothing Check Result | P0 |
| F06 | Pickup Request Form | P0 |
| F07 | Admin Dashboard | P0 |
| F08 | Donation Tracking | P0 |
| F09 | Loopy Points | P0 |
| F10 | Rewards & Redeem | P0 |

**P0:** Wajib dikembangkan untuk MVP.

**P1:** Penyempurnaan setelah seluruh fitur wajib selesai.

**P2:** Pengembangan untuk versi berikutnya.

## 3.2 Out of Scope

Fitur berikut tidak dikembangkan pada MVP:

- Integrasi Gojek, Grab, atau jasa kurir eksternal.
- GPS tracking kurir secara real-time.
- Aplikasi khusus kurir.
- Pembayaran online.
- Push notification.
- Marketplace pakaian bekas.
- Optimasi rute pickup.
- Integrasi voucher dengan merchant atau brand sungguhan.
- Sistem pengiriman hadiah fisik.

---

# 4. Functional Requirements

## F01 — Landing Page

### Description

Landing Page merupakan halaman pertama yang dilihat pengguna ketika membuka Loopy dan belum login.

Halaman ini bertujuan memperkenalkan aplikasi, menjelaskan manfaatnya, serta menarik minat pengunjung untuk mulai menggunakan Loopy.

Landing Page dibuat dengan desain yang menarik, modern, dan sesuai tema sustainability.

### Main Components

**1. Hero Section**

Menampilkan:
- Logo dan nama Loopy.
- Tagline "Give Your Clothes a Second Life!"
- Ilustrasi pakaian atau karakter maskot Loopy.
- Deskripsi singkat aplikasi.
- Tombol **Get Started**.

**2. About Loopy**

Menjelaskan secara singkat bahwa Loopy membantu pengguna mendonasikan pakaian bekas melalui pemeriksaan AI dan layanan pickup.

**3. Why Loopy? (Benefits)**

Menampilkan tiga manfaat utama:

- **AI Clothing Check:** Periksa kondisi pakaian dengan bantuan AI.
- **Easy Pickup:** Ajukan penjemputan tanpa harus keluar rumah.
- **Earn Rewards:** Dapatkan poin dari donasi yang berhasil.

**4. How It Works**

Menampilkan empat langkah:

1. Scan Your Clothes.
2. Request a Pickup.
3. Track Your Donation.
4. Earn & Redeem Points.

**5. Call to Action (CTA)**

Tombol:
- **Get Started:** Menuju Register.
- **Login:** Menuju Login.

### Business Rules

- Landing Page dapat diakses tanpa login.
- Pengguna harus login atau register untuk menggunakan fitur donasi.
- Jika pengguna sudah login, aplikasi dapat langsung membuka Home Dashboard.
- Landing Page tidak meminta informasi pribadi.

**Priority:** P0

---

## F02 — Login & Register

### Description

Fitur autentikasi untuk membuat akun dan mengakses aplikasi berdasarkan role.

### Functional Requirements

**User:**
- Register menggunakan nama, email, dan password.
- Login menggunakan email dan password.
- Logout.
- Mendapat pesan validasi jika input tidak valid.

**Admin:**
- Login menggunakan akun admin yang sudah dibuat.
- Mengakses Admin Dashboard.
- Logout.

### Business Rules

- Email harus unik.
- User baru otomatis memiliki role `user`.
- Akun admin dibuat secara khusus, bukan melalui registrasi publik.
- User dan admin diarahkan ke halaman sesuai role.
- User biasa tidak dapat mengakses fungsi admin.

**Priority:** P0

---

## F03 — Home Dashboard

### Description

Home Dashboard merupakan halaman utama setelah user berhasil login.

Berbeda dengan Landing Page yang berfungsi memperkenalkan aplikasi, Home Dashboard berfokus pada aktivitas pengguna.

### Main Components

- Greeting atau sambutan pengguna.
- Tombol **Check Your Clothes**.
- Ringkasan status donasi terakhir.
- Total Loopy Points.
- Akses menuju My Donations.
- Akses menuju Rewards.
- Informasi singkat atau tips donasi.

### Navigation

Navigasi utama user:

- Home
- My Donations
- Rewards
- Profile / Logout

Halaman profil lengkap dapat dikembangkan setelah fitur utama selesai. Untuk MVP, informasi akun dan logout dapat digabungkan dalam menu sederhana.

**Priority:** P0

---

## F04 — AI Clothing Detector

### Description

Fitur untuk memeriksa kondisi pakaian melalui foto menggunakan layanan AI yang mendukung analisis gambar.

### User Flow

1. User membuka menu Check Your Clothes.
2. User memilih foto dari galeri atau mengambil foto dengan kamera.
3. Sistem memvalidasi foto.
4. Foto dikirim ke layanan AI.
5. AI menganalisis kondisi visual pakaian.
6. Hasil ditampilkan pada Check Result.

### AI Analysis Criteria

- Jenis pakaian.
- Kondisi fisik yang terlihat.
- Adanya sobekan.
- Adanya noda.
- Indikasi kerusakan berat.
- Rekomendasi kelayakan awal.

### Limitations

AI tidak dapat memastikan kondisi yang tidak terlihat, seperti bau, kebersihan menyeluruh, dan kerusakan tersembunyi.

Karena itu, hasil AI hanya merupakan **estimasi awal kelayakan**, bukan keputusan akhir penerimaan donasi.

### Error Handling

- Foto tidak valid → tampilkan pesan kesalahan.
- Foto tidak jelas → minta user mengunggah ulang.
- AI gagal memproses → tampilkan tombol Retry.
- Analisis gagal → user tidak dapat melanjutkan pengajuan.

**Priority:** P0

---

## F05 — Clothing Check Result

### Description

Menampilkan hasil pemeriksaan pakaian berdasarkan respons AI.

### Result Categories

| Result | Description |
|---|---|
| Potentially Eligible | Pakaian berpotensi layak donasi |
| Needs Recheck | Memerlukan foto atau pemeriksaan ulang |
| Not Recommended | Tidak direkomendasikan untuk donasi |

### Information Displayed

- Foto pakaian.
- Jenis pakaian.
- Hasil analisis.
- Penjelasan singkat AI.
- Rekomendasi tindakan.

### Business Rules

- Hasil Potentially Eligible → Dapat melanjutkan ke Pickup Request Form.
- Hasil Needs Recheck → User diminta mengulang pemeriksaan.
- Hasil Not Recommended → User mendapat saran perbaikan atau daur ulang.
- User wajib mengonfirmasi pakaian bersih dan aman digunakan sebelum mengajukan pickup.
- Admin tetap dapat menolak pakaian yang tidak memenuhi standar penerimaan.

**Priority:** P0

---

## F06 — Pickup Request Form

### Description

Fitur yang memungkinkan user mengajukan penjemputan pakaian dari alamat yang ditentukan.

### Form Fields

| Field | Required |
|---|---|
| Full Name (dari akun) | Yes |
| Phone Number | Yes |
| Pickup Address | Yes |
| City / Area | Yes |
| Clothing Type (dari AI) | Yes |
| Clothing Photo (dari AI) | Yes |
| Quantity | Yes |
| Preferred Pickup Date | Yes |
| Preferred Pickup Time | Yes |
| Additional Notes | No |

### User Flow

1. User mendapatkan hasil Potentially Eligible.
2. User menekan Request Pickup.
3. User mengisi data kontak, alamat, dan preferensi jadwal.
4. User mengirim pengajuan.
5. Sistem menyimpan data ke database.
6. Status awal menjadi Pending Review.
7. Sistem menampilkan konfirmasi pengajuan berhasil.

### Business Rules

- User harus login.
- Pengajuan hanya dapat dilakukan dari hasil pemeriksaan yang berpotensi layak.
- Jumlah pakaian minimal 1.
- Tanggal tidak boleh berada di masa lalu.
- Setiap pengajuan memiliki ID unik.
- Jadwal pilihan user merupakan preferensi, bukan jadwal final.
- Jadwal final dikonfirmasi oleh admin.

**Priority:** P0

---

## F07 — Admin Dashboard

### Description

Halaman khusus admin untuk memantau dan mengelola pengajuan donasi.

### Dashboard Overview

Menampilkan:
- Total Requests.
- Pending Requests.
- Scheduled Pickups.
- Completed Donations.
- Rejected Requests.

### Donation Management

Admin dapat:
- Melihat daftar pengajuan.
- Membuka detail pengajuan.
- Melihat foto dan hasil AI.
- Melihat alamat pickup.
- Menerima atau menolak pengajuan.
- Menentukan jadwal penjemputan.
- Memperbarui status donasi.

### Business Rules

- Hanya admin yang dapat memverifikasi pengajuan.
- Pengajuan yang disetujui harus memiliki jadwal pickup.
- Pengajuan yang ditolak harus memiliki alasan.
- Status Picked Up hanya diberikan setelah penjemputan.
- Status Completed diberikan setelah penerimaan donasi dikonfirmasi.
- Perubahan status tercatat dalam database.

**Priority:** P0

---

## F08 — Donation Tracking

### Description

Fitur untuk memantau perkembangan proses donasi berdasarkan status yang diperbarui oleh sistem dan admin.

Tracking tidak menggunakan GPS real-time.

### Donation Status

| Status | Description | Updated By |
|---|---|---|
| Pending Review | Menunggu verifikasi | System |
| Pickup Scheduled | Pengajuan disetujui dan dijadwalkan | Admin |
| Picked Up | Pakaian telah dijemput | Admin |
| Completed | Donasi telah selesai | Admin |
| Rejected | Pengajuan ditolak | Admin |

### Status Flow

**Successful Donation:**

Pending Review → Pickup Scheduled → Picked Up → Completed

**Rejected Donation:**

Pending Review → Rejected

### Functional Requirements

- User dapat melihat seluruh pengajuan miliknya.
- User dapat melihat status terbaru.
- User dapat melihat detail donasi.
- User dapat melihat jadwal pickup yang telah dikonfirmasi.
- User dapat melihat alasan penolakan.
- User tidak dapat mengubah status sendiri.

**Priority:** P0

---

## F09 — Loopy Points

### Description

Loopy Points merupakan sistem penghargaan bagi pengguna yang berhasil menyelesaikan donasi.

Poin dapat dikumpulkan dan ditukarkan melalui halaman Rewards.

### Point Mechanism

| Activity | Points |
|---|---|
| Register | 0 |
| AI Clothing Check | 0 |
| Submit Pickup Request | 0 |
| Pickup Scheduled | 0 |
| Picked Up | 0 |
| Completed Donation | +10 |
| Rejected Donation | 0 |

### Business Rules

- User mendapatkan 10 poin setiap pengajuan berstatus Completed.
- Poin ditambahkan otomatis oleh sistem.
- Satu donasi hanya dapat menghasilkan poin satu kali.
- User tidak dapat menambahkan poin sendiri.
- Total poin ditampilkan di Home Dashboard dan Rewards Page.
- Poin akan berkurang setelah ditukarkan dengan reward.

**Priority:** P0

---

## F10 — Rewards & Redeem

### Description

Fitur yang memungkinkan user menukarkan Loopy Points dengan hadiah yang tersedia pada Rewards Page.

Fitur ini bertujuan memberikan motivasi tambahan agar pengguna terus berpartisipasi dalam kegiatan donasi.

### Main Components

**1. Points Balance**

Menampilkan jumlah poin yang dimiliki pengguna.

**2. Rewards Catalog**

Menampilkan daftar hadiah yang dapat ditukarkan.

Informasi reward:
- Nama reward.
- Gambar atau ikon reward.
- Deskripsi singkat.
- Jumlah poin yang dibutuhkan.
- Tombol Redeem.

**3. Redeem Confirmation**

Dialog konfirmasi sebelum user menukarkan poin.

**4. My Rewards / Redemption History**

Menampilkan riwayat penukaran hadiah yang berhasil dilakukan.

### Example Rewards

Untuk MVP akademik, hadiah dapat berupa reward digital dan voucher simulasi.

| Reward | Required Points |
|---|---|
| Eco Supporter Badge | 10 Points |
| Loopy Digital Wallpaper | 20 Points |
| Voucher Demo | 30 Points |

**Catatan:** Voucher demo bukan voucher komersial sungguhan dan tidak berlaku di merchant. Integrasi voucher resmi membutuhkan kerja sama atau penyedia reward.

### User Flow

1. User membuka halaman Rewards.
2. Sistem menampilkan total poin.
3. User melihat katalog reward.
4. User memilih reward.
5. Sistem memeriksa kecukupan poin.
6. Jika poin cukup, user menekan Redeem.
7. Sistem meminta konfirmasi.
8. Sistem mengurangi poin dan mencatat penukaran.
9. User menerima konfirmasi berhasil.
10. Reward muncul pada My Rewards / Redemption History.

### Business Rules

- User harus login untuk menukarkan poin.
- User hanya dapat redeem jika saldo poin mencukupi.
- Jika poin tidak cukup, tombol Redeem dinonaktifkan.
- Setiap penukaran tercatat dalam database.
- Poin tidak boleh menjadi negatif.
- Pengurangan poin dan pencatatan reward harus berhasil sebagai satu transaksi.
- Jika penukaran gagal, poin tidak boleh berkurang.
- Poin tidak dikembalikan secara otomatis setelah penukaran berhasil.
- Katalog reward untuk MVP menggunakan data yang telah ditentukan, tanpa fitur pengelolaan reward oleh admin.

### Example

User memiliki **30 Loopy Points**.

User menukarkan:
- Eco Supporter Badge: 10 Points.
- Loopy Digital Wallpaper: 20 Points.

Total poin digunakan: 30.

**Remaining Balance: 0 Points**

**Priority:** P0

---

# 5. Use Case Requirements

## 5.1 System Actors

Loopy memiliki dua aktor utama:

1. User (Donatur)
2. Admin (Pengelola)

Pengunjung yang belum login dapat mengakses Landing Page.

## 5.2 User Use Cases

| ID | Use Case | Description |
|---|---|---|
| UC-01 | View Landing Page | Melihat informasi aplikasi |
| UC-02 | Register | Membuat akun |
| UC-03 | Login | Masuk ke aplikasi |
| UC-04 | View Home | Melihat Home Dashboard |
| UC-05 | Upload Photo | Mengunggah foto pakaian |
| UC-06 | Check Eligibility | Memeriksa pakaian menggunakan AI |
| UC-07 | View Check Result | Melihat hasil analisis |
| UC-08 | Submit Pickup Request | Mengajukan penjemputan |
| UC-09 | View My Donations | Melihat riwayat donasi |
| UC-10 | Track Donation | Melihat status pengajuan |
| UC-11 | View Points | Melihat saldo poin |
| UC-12 | Browse Rewards | Melihat katalog reward |
| UC-13 | Redeem Reward | Menukarkan poin |
| UC-14 | View My Rewards | Melihat riwayat penukaran |
| UC-15 | Logout | Keluar dari akun |

## 5.3 Admin Use Cases

| ID | Use Case | Description |
|---|---|---|
| UC-16 | Login Admin | Masuk sebagai admin |
| UC-17 | View Admin Dashboard | Melihat ringkasan donasi |
| UC-18 | View Requests | Melihat pengajuan |
| UC-19 | View Details | Memeriksa data pengajuan |
| UC-20 | Accept Request | Menyetujui pengajuan |
| UC-21 | Reject Request | Menolak pengajuan |
| UC-22 | Schedule Pickup | Menentukan jadwal |
| UC-23 | Update Status | Memperbarui status |
| UC-24 | Monitor Donations | Memantau donasi |
| UC-25 | Logout | Keluar dari akun |

---

# 6. Main User Flow

## 6.1 Landing & Authentication Flow

1. User membuka Loopy.
2. Jika belum login, sistem menampilkan Landing Page.
3. User melihat informasi Loopy, manfaat, dan cara kerja.
4. User menekan Get Started atau Login.
5. User melakukan registrasi atau login.
6. Sistem memverifikasi akun.
7. User diarahkan ke Home Dashboard.
8. Jika akun merupakan admin, sistem membuka Admin Dashboard.

## 6.2 AI Clothing Check Flow

1. User membuka AI Clothing Detector.
2. User mengambil atau mengunggah foto.
3. AI memproses foto.
4. Sistem menampilkan Check Result.
5. Jika berpotensi layak, user dapat melanjutkan ke Pickup Request.
6. Jika perlu diperiksa ulang, user diminta mengambil foto kembali.
7. Jika tidak direkomendasikan, sistem menampilkan saran alternatif.

## 6.3 Pickup Request Flow

1. User membuka Pickup Request Form dari Check Result.
2. User mengisi alamat dan preferensi jadwal.
3. User mengirim pengajuan.
4. Sistem menyimpan pengajuan.
5. Status menjadi Pending Review.
6. User dapat melihat pengajuan di My Donations.

## 6.4 Admin Verification Flow

1. Admin membuka daftar pengajuan.
2. Admin memilih pengajuan.
3. Admin memeriksa detail.
4. Admin menerima atau menolak.
5. Jika diterima, admin menentukan jadwal pickup.
6. Status berubah menjadi Pickup Scheduled.
7. Jika ditolak, status menjadi Rejected disertai alasan.

## 6.5 Donation Completion Flow

1. Proses pickup dilakukan sesuai jadwal.
2. Admin mengubah status menjadi Picked Up.
3. Setelah penerimaan pakaian dikonfirmasi, admin mengubah status menjadi Completed.
4. Sistem menambahkan 10 Loopy Points.
5. User dapat melihat status selesai dan saldo poin terbaru.

## 6.6 Reward Redemption Flow

1. User membuka Rewards Page.
2. User memilih hadiah.
3. Sistem memeriksa saldo poin.
4. User mengonfirmasi penukaran.
5. Sistem mengurangi poin.
6. Sistem mencatat reward yang ditukarkan.
7. User melihat konfirmasi berhasil.
8. Reward muncul pada My Rewards / Redemption History.

---

# 7. Screen Requirements

## 7.1 Public & User Screens

| ID | Screen | Main Function |
|---|---|---|
| S01 | Landing Page | Pengenalan, benefit, dan cara kerja Loopy |
| S02 | Login | Login akun |
| S03 | Register | Membuat akun |
| S04 | Home Dashboard | Halaman utama user |
| S05 | AI Clothing Detector | Upload foto pakaian |
| S06 | Check Result | Hasil pemeriksaan AI |
| S07 | Pickup Request Form | Pengajuan pickup |
| S08 | My Donations | Riwayat dan status donasi |
| S09 | Donation Detail | Detail dan tracking donasi |
| S10 | Rewards Page | Saldo poin dan katalog hadiah |

Riwayat penukaran (*My Rewards*) dapat menggunakan tab pada Rewards Page.

Konfirmasi pickup dan redeem dapat menggunakan dialog, sehingga tidak memerlukan halaman tambahan.

**Total: 10 user/public screens.**

## 7.2 Admin Screens

| ID | Screen | Main Function |
|---|---|---|
| A01 | Admin Dashboard | Monitoring donasi |
| A02 | Donation Requests | Daftar pengajuan |
| A03 | Donation Detail | Verifikasi dan update status |

Admin menggunakan halaman login yang sama dengan user.

**Total: 3 admin screens.**

**Total keseluruhan: 13 screen utama.**

---

# 8. Data Requirements

## 8.1 Users

- user_id
- name
- email
- role
- total_points (saldo poin, jika disimpan sebagai data turunan)

Password dan kredensial dikelola menggunakan sistem autentikasi yang aman.

## 8.2 Clothing Checks

- check_id
- user_id
- clothing_photo
- clothing_type
- ai_result
- ai_description
- created_at

## 8.3 Donation Requests

- donation_id
- user_id
- check_id
- phone_number
- pickup_address
- city
- quantity
- preferred_date
- preferred_time
- scheduled_date
- scheduled_time
- notes
- status
- rejection_reason
- created_at
- updated_at

## 8.4 Donation Status History

- history_id
- donation_id
- previous_status
- new_status
- updated_by
- updated_at

## 8.5 Point Transactions

- transaction_id
- user_id
- donation_id (opsional untuk transaksi redeem)
- redemption_id (opsional untuk transaksi redeem)
- transaction_type (earn / redeem)
- points
- created_at

Nilai points positif untuk perolehan dan negatif untuk penukaran. Sistem mencegah transaksi poin ganda untuk penyelesaian donasi yang sama.

## 8.6 Rewards

- reward_id
- reward_name
- description
- image
- required_points
- reward_type
- is_active

## 8.7 Redemptions

- redemption_id
- user_id
- reward_id
- points_used
- reward_details
- redeemed_at

---

# 9. Non-Functional Requirements

## 9.1 Usability

- UI sederhana, modern, dan konsisten.
- Landing Page mudah dipahami pengunjung baru.
- Navigasi fitur utama mudah ditemukan.
- Form memiliki validasi dan pesan yang jelas.
- Alur penukaran poin mudah dipahami.

## 9.2 Performance

- Menampilkan loading saat AI memproses foto.
- Menangani kegagalan koneksi.
- Mencegah pengajuan dan redeem ganda akibat klik berulang.
- Data tracking dan poin sesuai kondisi terbaru database.

## 9.3 Security

- Autentikasi untuk fitur yang mengakses data pribadi.
- Role user dan admin memiliki hak akses berbeda.
- User hanya dapat melihat donasinya sendiri.
- Foto, alamat, dan nomor kontak dilindungi.
- API key AI tidak disimpan secara terbuka dalam aplikasi.
- Redeem harus divalidasi di backend, bukan hanya di UI.

## 9.4 Reliability

- Data pengajuan tetap tersimpan.
- Status donasi berubah sesuai alur.
- Poin hanya diberikan satu kali per donasi selesai.
- Redeem tidak boleh membuat saldo negatif.
- Penukaran gagal tidak mengurangi saldo poin.

---

# 10. Acceptance Criteria

| ID | Acceptance Criteria |
|---|---|
| AC-01 | Landing Page menampilkan informasi, manfaat, dan cara kerja Loopy |
| AC-02 | Tombol Get Started mengarahkan user ke registrasi |
| AC-03 | User dapat register dan login |
| AC-04 | Admin login diarahkan ke Admin Dashboard |
| AC-05 | User dapat mengunggah foto pakaian |
| AC-06 | Sistem memproses foto menggunakan AI dan menampilkan hasil |
| AC-07 | User dapat mengajukan pickup untuk pakaian yang berpotensi layak |
| AC-08 | Pengajuan tersimpan dengan status Pending Review |
| AC-09 | Admin dapat menerima atau menolak pengajuan |
| AC-10 | Admin dapat menentukan jadwal pickup |
| AC-11 | Admin dapat memperbarui status sampai Completed |
| AC-12 | User dapat melihat status terbaru donasinya |
| AC-13 | Sistem memberikan 10 poin satu kali setelah donasi Completed |
| AC-14 | User dapat melihat saldo dan katalog reward |
| AC-15 | User dapat menukarkan reward jika poin cukup |
| AC-16 | Saldo poin berkurang sesuai reward yang ditukarkan |
| AC-17 | Penukaran berhasil tersimpan di My Rewards |
| AC-18 | Sistem mencegah redeem jika poin tidak mencukupi |
| AC-19 | User tidak dapat mengakses data orang lain |
| AC-20 | User biasa tidak dapat mengakses fitur admin |

---

# 11. Development Roadmap

## Phase 1 — Planning & Design

- Finalisasi PRD.
- Use Case Diagram.
- Flowchart.
- UI/UX di Figma.
- Screen Flow.
- Rancangan database.

## Phase 2 — Foundation

- Landing Page.
- Login & Register.
- User Home Dashboard.
- Admin Dashboard.
- Navigasi aplikasi.

## Phase 3 — Core Donation Features

- AI Clothing Detector.
- Check Result.
- Pickup Request Form.
- Penyimpanan pengajuan.
- Admin Accept / Reject.
- Pickup Scheduling.

## Phase 4 — Tracking & Rewards

- Donation Tracking.
- Update status oleh admin.
- Loopy Points.
- Rewards Catalog.
- Redeem Points.
- Redemption History.

## Phase 5 — Finalization

- Integrasi antarfitur.
- Pengujian user dan admin.
- Pengujian AI.
- Pengujian sistem poin dan redeem.
- Bug fixing.
- Persiapan demo.

---

# 12. Team Responsibilities

Pembagian tugas dokumentasi BAB I dan BAB II:

| Member | Role | Responsibilities |
|---|---|---|
| Neva | Project Manager | 1.1 Latar Belakang, 1.2 Tujuan Proyek, 2.5 Pembagian Tugas, PRD, koordinasi |
| Rio | Team Member | 1.3 Deskripsi Aplikasi, 2.1 Use Case Diagram, 2.2 Flowchart, 2.4 Definisi Fungsi |
| Alex | Team Member | 2.3 UI/UX dan 2.6 Screen Flow sisi User |
| Arsyad | Team Member | 2.3 UI/UX dan 2.6 Screen Flow sisi Admin, koordinasi desain |

**Catatan:** Pembagian implementasi kode ditetapkan secara terpisah.

---

# 13. Risks & Mitigation

| Risk | Mitigation |
|---|---|
| AI tidak akurat | Hasil dianggap pemeriksaan awal dan tetap diverifikasi admin |
| Integrasi AI sulit | Lakukan proof of concept AI sejak awal |
| Terlalu banyak fitur | Kerjakan berdasarkan urutan dependensi dan hindari fitur tambahan |
| Pickup membutuhkan kurir nyata | Gunakan simulasi admin untuk tugas PAPB |
| Pengajuan dari luar area layanan | Admin dapat menolak dengan alasan |
| Status tidak konsisten | Gunakan aturan perubahan status yang jelas |
| Poin diberikan berulang | Batasi satu transaksi earn per donasi |
| Saldo salah saat redeem | Validasi saldo dan gunakan transaksi database yang aman |
| Tidak ada mitra reward | Gunakan reward digital dan voucher demo berlabel simulasi |
| Data pribadi terekspos | Terapkan autentikasi dan pembatasan akses |

---

# 14. MVP Simulation Assumptions

Karena Loopy merupakan proyek akademik:

1. Pickup disimulasikan melalui dashboard admin.
2. Tidak memerlukan kurir sungguhan saat demo.
3. Admin memperbarui status secara manual.
4. Jadwal pickup ditentukan secara manual.
5. Loopy Points merupakan poin virtual.
6. Reward menggunakan hadiah digital atau voucher simulasi.
7. Tidak ada transaksi uang sungguhan.
8. AI memberikan estimasi kelayakan awal, bukan keputusan final.
9. Data kontak dan alamat demonstrasi menggunakan data uji.

---

# 15. Product Success Criteria

Loopy dinyatakan berhasil apabila:

1. Pengguna baru memahami aplikasi melalui Landing Page.
2. User dapat register dan login.
3. AI dapat membantu memeriksa pakaian.
4. User dapat mengajukan pickup.
5. Admin dapat memverifikasi pengajuan.
6. Admin dapat memperbarui status hingga selesai.
7. User dapat memantau status.
8. Poin diberikan otomatis setelah donasi selesai.
9. User dapat menukarkan poin melalui Rewards Page.
10. Seluruh alur dapat didemonstrasikan dari awal hingga akhir.

## End-to-End Success Scenario

Seorang pengguna bernama Neva membuka aplikasi Loopy.

Landing Page menampilkan informasi aplikasi, manfaat, dan langkah-langkah donasi.

Neva tertarik dan memilih Get Started, kemudian melakukan registrasi dan login.

Pada Home Dashboard, Neva memilih Check Your Clothes dan mengunggah foto pakaian bekas.

AI memberikan hasil Potentially Eligible.

Neva melanjutkan ke Pickup Request Form dan mengisi alamat, nomor telepon, jumlah pakaian, serta preferensi jadwal.

Setelah pengajuan dikirim, status menjadi Pending Review.

Admin memeriksa pengajuan, menerima permintaan, dan menentukan jadwal pickup.

Setelah proses penjemputan dilakukan, admin mengubah status menjadi Picked Up.

Ketika pakaian telah diterima, admin mengubah status menjadi Completed.

Sistem otomatis memberikan 10 Loopy Points.

Neva membuka Rewards Page dan melihat katalog hadiah.

Neva memilih Eco Supporter Badge seharga 10 poin.

Sistem memproses penukaran, mengurangi saldo menjadi 0 poin, dan mencatat reward ke riwayat penukaran.

Dengan demikian, Neva berhasil menyelesaikan seluruh alur Loopy, mulai dari mengenal aplikasi hingga melakukan donasi dan menukarkan poin.

---

# 16. Open Questions

Hal yang perlu disepakati sebelum implementasi:

- [ ] Teknologi mobile: Kotlin, Java, atau Flutter?
- [ ] Backend dan database yang digunakan?
- [ ] Layanan AI Vision yang dipilih?
- [ ] Kamera, galeri, atau keduanya?
- [ ] Wilayah simulasi pickup?
- [ ] Pilihan jadwal pickup?
- [ ] Desain dan identitas visual Loopy?
- [ ] Jenis reward digital yang akan dibuat?
- [ ] Pembagian tugas implementasi kode?

---

# 17. References & Attribution

**Reference Project:** ReWear

**Event:** Garuda Hacks 6.0 (2025)

**Original Creators:**
- Farella Kamala Budianto
- Kenlyn Tesalonika Winata
- Vincentia Belinda Sumartoyo

**Reference Link:**

https://devpost.com/software/rewear-90is7f

Loopy merupakan proyek akademik yang mengadaptasi konsep ReWear sebagai referensi. Rancangan fitur Loopy merupakan penyesuaian kebutuhan proyek dan tidak diklaim sebagai fitur asli ReWear.

---

**Document Version:** 2.1

**Document Status:** Draft — Pending Team Approval

**Prepared by:** Neva — Project Manager, Loopy

## Lampiran rujukan implementasi

Lampiran ini membantu tim menelusuri naskah PRD v2.1 ke kode dan pengujian. Cakupan produk dan kriteria penerimaan mengikuti bagian utama dokumen.

| Fitur | Implementasi dan cara memeriksa |
| --- | --- |
| F01–F03 Landing, akun, dan Beranda | [Peta layar](../screen-map.md), [arsitektur Android](../architecture.md), dan [kontrak autentikasi](../auth-go.md). |
| F04–F06 Pemeriksaan dan pengajuan | [Kontrak foto dan pickup](../api-contract.md), [alur Go](../backend.md#pemeriksaan-pakaian-go), dan [pemeriksaan penerimaan](../acceptance-tests.md). |
| F07–F08 Peninjauan, jadwal, dan pelacakan | [Alur admin](../donation-review-go.md), [model data](../data-model.md), dan [peta layar](../screen-map.md). |
| F09–F10 Poin dan rewards | [Penyelesaian dan redeem](../completion-rewards.md) serta [model ledger](../data-model.md#poin-dan-penukaran). |
| AC-01–AC-20 | [Matriks langkah dan bukti](../acceptance-tests.md) serta [riwayat verifikasi](../verification.md). |

Implementasi juga mencakup username opsional, profil, pengaturan, dan OTP pada pendaftaran sebagai perluasan layar. Login berikutnya memakai email dan kata sandi. [Peta perluasan desain](../screen-map.md) · [Backlog dan pekerjaan rilis](../backlog.md)
