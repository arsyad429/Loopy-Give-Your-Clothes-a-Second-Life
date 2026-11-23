# Acceptance criteria dan rencana uji

Semua AC produk masih **TODO**. Tes fondasi dan placeholder bukan bukti MVP selesai. Gunakan data uji tanpa PII.

| AC | Fitur | Target | Bukti uji | Status |
| --- | --- | --- | --- | --- |
| AC-01 | F01 | Landing berisi informasi, manfaat dan empat langkah | Buka tanpa sesi, cek hero/tagline, About, benefit dan how it works. | TODO |
| AC-02 | F01 | Get Started ke Register | Klik CTA, periksa tujuan dan back stack. | TODO |
| AC-03 | F02 | Register/login user | Input valid/invalid, email duplikat, password salah, logout/login; jangan simpan password. | TODO |
| AC-04 | F02/F07 | Admin ke dashboard | Login akun admin seed di layar yang sama; user menuju Home. | TODO |
| AC-05 | F04 | Upload foto | Galeri/kamera, cancel, file salah/terlalu besar dan rotasi external Activity Result. | TODO |
| AC-06 | F04/F05 | AI menampilkan hasil | Tiga kategori, foto buram, offline/timeout/retry; gagal tidak membuka pickup. | TODO |
| AC-07 | F05/F06 | Pickup hanya eligible | Check eligible milik sendiri+konfirmasi bersih; bypass route/check user lain ditolak server. | TODO |
| AC-08 | F06 | Pending Review tersimpan | Quantity0/tanggal lampau ditolak; request valid tersimpan+history; double tap/retry tidak duplikat. | TODO |
| AC-09 | F07 | Accept/reject admin | Accept wajib jadwal, reject wajib alasan; user biasa 403. | TODO |
| AC-10 | F07 | Jadwal final admin | ScheduledDate/time disimpan dan dibedakan dari preferensi user. | TODO |
| AC-11 | F07/F08 | Status sampai Completed | Scheduled→Picked Up→Completed; skip/reverse ditolak, audit actor benar. | TODO |
| AC-12 | F08 | Status terbaru user | Dua sesi admin/user; update lalu refresh/resume, list/detail konsisten, alasan reject terlihat. | TODO |
| AC-13 | F09 | +10 tepat sekali | Complete quantity>1 tetap+10; retry/concurrent complete menghasilkan satu ledger earn. | TODO |
| AC-14 | F09/F10 | Saldo/katalog | Saldo server sama Home/Rewards; badge10,wallpaper20,voucher demo30. | TODO |
| AC-15 | F10 | Redeem saldo cukup | Konfirmasi batal tanpa mutasi; confirm berhasil mendapat reward. | TODO |
| AC-16 | F10 | Debit sesuai cost | Saldo30 redeem10 dan20 jadi0; injeksi error transaksi rollback saldo/ledger/redemption. | TODO |
| AC-17 | F10 | History tersimpan | Restart/relogin, My Rewards tetap memuat detail redemption demo. | TODO |
| AC-18 | F10 | Saldo cukup wajib | UI disable; request manual/concurrent tidak negatif; retry key sama tidak debit dua kali. | TODO |
| AC-19 | F02/F08 | Isolasi user | U1 tidak membaca check/donation/redemption U2; cache tidak bocor setelah logout. | TODO |
| AC-20 | F02/F07 | Admin authorization | USER akses semua endpoint /admin→403; register tidak bisa roleADMIN; UI tidak eskalasi. | TODO |


Demo PRD: register Neva(0)→foto eligible→pickup pending→admin schedule→picked up→completed(+10)→redeem badge(0)→history. Uji pula rejected, needs recheck, AI gagal dan saldo tidak cukup.

Nonfungsional: rotasi/background/restore, state hoisting, cancellation, retry/error/loading/empty, debounce pencarian, tema DataStore/draft Room bertahan restart, klik berulang, TalkBack/font scaling dan release tanpa cleartext/AI key.

Unit Android di src/test, persistence/UI di src/androidTest; unit dan integration test server di `backend/go/*_test.go`. Concurrency/rollback diuji terhadap PostgreSQL, bukan wallet fake Android. Catat perangkat/tanggal/hasil setelah tiap AC sungguh lulus.
