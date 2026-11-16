# Hasil verifikasi scaffold

Diverifikasi pada Linux amd64, 16 November 2026. Ini pengujian fondasi/template; seluruh acceptance criteria produk F01–F10 masih TODO.

| Pemeriksaan | Hasil |
| --- | --- |
| Docker Android: assembleDebug testDebugUnitTest lintDebug assembleDebugAndroidTest | Lulus, 88 task, 2 menit 33 detik pada source final |
| APK debug | artifacts/outputs/apk/debug/app-debug.apk, berhasil dikompilasi |
| Unit Android | 1 test DonationStatusTest, seluruh 25 pasangan status diperiksa, 0 gagal |
| Lint | 0 error; 21 warning, mayoritas pembaruan dependency baseline, ditambah launcher icon dan data extraction rules untuk tahap release |
| Instrumented test APK | Berhasil dikompilasi; belum dieksekusi pada emulator/perangkat |
| Backend: test installDist | Lulus; 1 HealthTest, 0 gagal |
| Docker backend dan PostgreSQL | Keduanya healthy; GET /health pada localhost:18080 menghasilkan status ok, mode template |
| Struktur | Lulus: 13 layar MVP + 4 perluasan desain, 10 materi PAPB, 20 AC, wrapper dan README setiap folder source |
| Compose dan shell | docker compose config --quiet dan sh -n scripts/android-container.sh lulus |
| Git diff | Tidak ada whitespace error; file build/local.properties/.env tidak dilacak |

SHA256 APK debug final: `8d8cd065e2a8691a857872e71a51bfd5a5e0f5b894b60c89e6c8f9ff7b4e3b10`.

## Kondisi lingkungan pengujian

Partisi sistem/Docker pada host penuh, sedangkan partisi workspace memiliki ruang. Pengujian Docker memakai bind mount cache Gradle, direktori sementara dan data PostgreSQL ke partisi workspace. JAVA_TOOL_OPTIONS mengarahkan user.home/tmp JVM pengujian ke mount sementara. Ini konfigurasi pengujian lokal di luar repo; tidak mengubah konfigurasi portable Compose. Port8080 dipakai layanan lain sehingga health check memakai API_PORT=18080. Layanan proyek lain tidak dihentikan.

Build awal sempat gagal karena disk host, cache Gradle yang dipakai bersamaan lintas container/host, dan README di resource Android. Source final menyaring Markdown dari resource melalui prepareMainResources dan script container memakai snapshot baru. Build final dijalankan dengan cache container terpisah dan lulus. Untuk development berikutnya, sediakan ruang Docker yang memadai; jangan prune data proyek lain.

## Yang belum dibuktikan/diimplementasikan

- Runtime UI, instrumented persistence test dan flow emulator/perangkat belum dijalankan.
- Visual final Figma belum diimplementasikan/diverifikasi; design context terkena kuota Starter, metadata dan referensi tersimpan.
- Seluruh fitur bisnis PRD, auth/AI, koneksi/migrasi DB aplikasi, transaksi poin/redeem dan isolasi data masih template.
- Windows/macOS/ARM belum diuji langsung; panduan dan batas tools Linux x86_64 ada di development.md.
- Sebelum release: lengkapi ikon, kebijakan ekstraksi/backup data, HTTPS deployment, auth/Keystore, UI final serta semua AC.
