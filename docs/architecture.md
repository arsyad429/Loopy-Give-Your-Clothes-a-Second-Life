# Arsitektur Loopy

Aliran Android: **Composable → ViewModel → Repository → remote API / DataStore / Room**. State kembali melalui Flow/StateFlow dan collectAsStateWithLifecycle. Manual DI di AppContainer menjaga satu instance DB/DataStore per proses. Versi dipin untuk baseline build, bukan klaim dependency terbaru.

Fondasi aktif: MainActivity/lifecycle Logcat, TemplateViewModel, FoundationRepository, tema DataStore, draft Room, Retrofit suspend GET /health, Navigation Compose ke placeholder. State bisnis berikutnya berada di ViewModel; remember/rememberSaveable dipakai sesuai kebutuhan state lokal form/dialog. Draft contoh memakai ID tetap agar Simpan berulang tidak menggandakan data.

DonationStatus mengatur transisi untuk UI. Model/kontrak bisnis masih TODO; UI final wajib stateless, dengan preview, aksesibilitas, Column/Row/Box, Modifier, LazyColumn, hoisting dan recomposition sesuai PAPB.

Compose utama membangun **Go + PostgreSQL** untuk akun dan peninjauan donasi. Compose Vision membangun **Ktor + PostgreSQL** untuk pemeriksaan Groq dengan migrasi Flyway, foto JPEG privat, dan idempotensi. Database dan token kedua layanan terpisah. Pengajuan pickup pengguna, pelacakan, poin, dan rewards masih direncanakan. [Panduan backend Loopy](backend.md)

DataStore untuk preferensi tema/onboarding/filter; Room untuk draft/cache. Backend sumber kebenaran role, status, poin dan redemption. Token membutuhkan penyimpanan berbasis Keystore; password/AI API key tidak boleh masuk APK/DataStore. Cache bisnis wajib per user dan dibersihkan saat logout. S05 mengambil gambar lewat Photo Picker dan Activity Result kamera dengan URI dari FileProvider. S06 membaca hasil lewat kontrak repository yang sama dengan S05.

Debug membuka katalog termasuk admin untuk developer. Ini bukan kontrol akses produk. Ganti harness dengan graph sesi public/user/admin di ui/navigation/LoopyNavGraph.kt; backend wajib mengotorisasi setiap request. Release membuka landing; URL produksi dan wiring sesi Android masih perlu dikonfigurasi.

TODO: implementasikan DTO/repository/service, wiring AppContainer/factory, auth interceptor tanpa logging credential, SavedStateHandle untuk ID route, cancellation/timeout/retry, debounce pencarian, tombol disabled dan idempotency server. Jangan mengirim foto/alamat melalui route.

Referensi: [Compose compiler plugin](https://developer.android.com/jetpack/androidx/releases/compose-kotlin), [Room](https://developer.android.com/jetpack/androidx/releases/room), [DataStore](https://developer.android.com/jetpack/androidx/releases/datastore).

Dokumentasi resource: file XML/aset tetap diedit di app/src/main/res. Task prepareMainResources menyalinnya ke build/generated/loopyRes/main tanpa Markdown sebelum kompilasi, karena Android menolak README.md di res/values dan res/xml. Jangan edit salinan generated.
