# Dokumentasi teknis Loopy

Mulai dengan [README utama](../README.md) untuk mencoba alur. Dokumen di sini memberi kontrak, rujukan, langkah deploy, serta catatan pengujian.

| Kebutuhan | Bahan yang dipakai |
| --- | --- |
| Mencari rute tampilan | [Peta layar](screen-map.md), [data pemetaan](screen-map.json), dan [rujukan PRD/Figma](reference/README.md). |
| Mengubah endpoint | [Kontrak API](api-contract.md) dan [backend Go](backend.md) menjelaskan rute, payload, dan dependensi. |
| Mengubah akun atau OTP | [Autentikasi Go](auth-go.md) berisi login, sesi, profil, OTP dan CLI admin. |
| Mengerjakan pickup | [Peninjauan dan jadwal](donation-review-go.md) memuat aturan peran, jadwal WIB, dan konflik perubahan status. |
| Mengerjakan poin atau rewards | [Penyelesaian dan redeem](completion-rewards.md) memuat transaksi, idempotensi, dan riwayat implementasi. |
| Mencari data dan arsitektur | [Model data](data-model.md) serta [catatan arsitektur](architecture.md). Schema yang berjalan tercatat di [migrasi Go](../backend/go/migrations/README.md). |
| Menjalankan VM atau APK | [Deploy LXC](deploy-lxc.md) mencakup SMTP, admin, TLS, upgrade dan backup. [Development lintas OS](development.md) mencakup Gradle dan koneksi perangkat. |
| Menyiapkan demo atau review | [Kriteria penerimaan](acceptance-tests.md), [hasil verifikasi](verification.md), dan [backlog](backlog.md). |
| Melacak materi perkuliahan | [Pemetaan PAPB](papb-mapping.md) dan [katalog referensi](reference/papb-catalog.json). |
