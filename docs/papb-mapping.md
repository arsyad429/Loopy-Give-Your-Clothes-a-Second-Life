# Pemetaan materi PAPB

Dokumen ini menautkan 10 sumber perkuliahan ke contoh yang bisa ditelusuri pada Loopy. Identitas PDF, ukuran, dan checksum ada di [katalog materi](reference/papb-catalog.json). Versi API pada handout dapat berbeda dari [baseline Gradle](../android/gradle/libs.versions.toml).

| Materi sumber | Konsep | Contoh Loopy | Cara mendemonstrasikan |
| --- | --- | --- | --- |
| Handout Dasar Kotlin dan Android.pdf | Kelas, null safety, fungsi, koleksi, Activity dan manifest | [Model Kotlin](../android/app/src/main/java/id/ac/loopy/domain/model/README.md) dan [entry point](../android/app/src/main/java/id/ac/loopy/README.md) | Bandingkan field nullable jadwal atau alasan dengan state pengajuan yang belum diputus. |
| Modul Praktikum-FILKOM UB-Mobile_1_Debug_SDK_LIBRARY_MAIN ACTIVITY.pdf | SDK, dependency, build dan debug | [Gradle modul](../android/app/build.gradle.kts) dan [katalog versi](../android/gradle/libs.versions.toml) | Bangun APK debug dengan JDK 17 dan SDK 36, lalu cek Logcat ketika Activity dibuat. |
| Modul 2- Konsep Dasar UI Jetpack Compose.pdf | Composable, layout, Modifier, tema dan aset | [LoopyScreen](../android/app/src/main/java/id/ac/loopy/ui/components/LoopyScreen.kt) dan [Beranda](../android/app/src/main/java/id/ac/loopy/feature/home/HomeScreen.kt) | Ikuti header, konten scroll, dan bottom bar. Uji mode terang dan gelap. |
| Modul Praktikum-FILKOM UB-Mobile_2_Activity LifeCycle_Composable_Modifier.pdf | Lifecycle, setContent, Activity Result dan URI | [MainActivity](../android/app/src/main/java/id/ac/loopy/MainActivity.kt) dan [S05](../android/app/src/main/java/id/ac/loopy/feature/clothingcheck/ClothingDetectorScreen.kt) | Buka kamera, kembali ke aplikasi, dan lihat event lifecycle di debug. |
| Modul PAPB 3- Konsep State dan Rekomposisi.pdf | `remember`, state hoisting, recomposition, dan callback | [Form pickup](../android/app/src/main/java/id/ac/loopy/feature/pickup/README.md) dan [state Rewards](../android/app/src/main/java/id/ac/loopy/feature/rewards/README.md) | Ubah kuantitas, amati validasi, lalu lihat loading dan hasil submit. |
| Modul_Praktikum_05_Navigation_Compose_FILKOM_UB.pdf | `NavHost`, tujuan, parameter dan back stack | [LoopyApp](../android/app/src/main/java/id/ac/loopy/ui/LoopyApp.kt) dan [tes navigasi](../android/app/src/androidTest/java/id/ac/loopy/feature/README.md) | Bandingkan rute awal, pengalihan menurut role, ID detail, serta tab Beranda yang kembali ke entri lama. |
| Modul Praktikum 06 Arsitektur Android FILKOM UB.pdf | UI, ViewModel, repository, dan dependency injection | [AppContainer](../android/app/src/main/java/id/ac/loopy/di/AppContainer.kt) dan [layer data](../android/app/src/main/java/id/ac/loopy/data/README.md) | Telusuri aksi refresh Beranda dari composable sampai dua request Retrofit. |
| Modul Praktikum 07 REST API Retrofit.pdf | HTTP, DTO, header, JSON dan pemetaan error | [Interface Retrofit](../android/app/src/main/java/id/ac/loopy/data/remote/api/README.md) dan [tes kontrak HTTP](../android/app/src/test/java/id/ac/loopy/data/repository/README.md) | Baca request pickup dan cocokkan DTO dengan body Go serta error per field. |
| Modul Praktikum 08 Kotlin Coroutines Flow.pdf | `suspend`, scope, `StateFlow`, debounce dan cancellation | [ViewModel Pengajuan](../android/app/src/main/java/id/ac/loopy/feature/admin/DonationRequestsViewModel.kt) dan [ViewModel profil](../android/app/src/main/java/id/ac/loopy/feature/profile/AccountViewModel.kt) | Ketik kueri cepat dan amati pemuatan setelah debounce. Buka kembali halaman profil dan lihat refresh saat resume. |
| Modul Praktikum 09 (Beranotasi).pdf | DataStore, Room, entity, DAO, schema dan KSP | [Penyimpanan lokal](../android/app/src/main/java/id/ac/loopy/data/local/README.md) dan [tes Room](../android/app/src/androidTest/java/id/ac/loopy/FoundationPersistenceTest.kt) | Uji draf jadwal per admin dan migrasi schema 1 ke 2 pada database perangkat uji. |

## Jalur demo konsep

1. Mulai dari `MainActivity` dan tunjukkan lifecycle saat membuka kamera. Ulangi pada build debug tanpa mencetak data foto atau akun.
2. Ikuti satu aksi dari UI menuju ViewModel, repository, interface Retrofit, handler Go, dan transaksi. Pickup cocok untuk membahas idempotensi. Profil cocok untuk membahas sesi dan lifecycle.
3. Demonstrasikan pemisahan state perangkat dan data server dengan draf admin di Room serta jadwal final di API. Tutup dengan unit test atau tes perangkat yang memeriksa konsep itu.

[Panduan setup](development.md) · [Arsitektur](architecture.md) · [Penerimaan produk](acceptance-tests.md)
