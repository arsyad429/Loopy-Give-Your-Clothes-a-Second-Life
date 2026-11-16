# Arsitektur dan keputusan scaffold

Aliran Android: **Composable → ViewModel → Repository → remote API / DataStore / Room**. State kembali melalui Flow/StateFlow dan collectAsStateWithLifecycle. Manual DI di AppContainer menjaga satu instance DB/DataStore per proses. Versi dipin untuk baseline build, bukan klaim dependency terbaru.

Fondasi aktif: MainActivity/lifecycle Logcat, TemplateViewModel, FoundationRepository, tema DataStore, draft Room, Retrofit suspend GET /health, Navigation Compose ke placeholder. State bisnis berikutnya berada di ViewModel; remember/rememberSaveable dipakai sesuai kebutuhan state lokal form/dialog. Draft contoh memakai ID tetap agar Simpan berulang tidak menggandakan data.

DonationStatus mengatur transisi untuk UI. Model/kontrak bisnis masih TODO; UI final wajib stateless, dengan preview, aksesibilitas, Column/Row/Box, Modifier, LazyColumn, hoisting dan recomposition sesuai PAPB.

Backend dipilih **Kotlin/Ktor + PostgreSQL** agar tetap Kotlin dan mendukung transaksi poin server. Ini keputusan scaffold karena PRD belum menetapkan backend. Saat ini hanya liveness aktif; env database belum digunakan aplikasi. Auth, SQL, migration runner dan route bisnis masih template.

DataStore untuk preferensi tema/onboarding/filter; Room untuk draft/cache. Backend sumber kebenaran role, status, poin dan redemption. Token membutuhkan penyimpanan berbasis Keystore; password/AI API key tidak boleh masuk APK/DataStore. Cache bisnis wajib per user dan dibersihkan saat logout. Kamera/Photo Picker masih TODO; tidak ada permission yang belum dipakai.

Debug membuka katalog termasuk admin untuk developer. Ini bukan kontrol akses produk. Ganti harness dengan graph sesi public/user/admin di ui/navigation/LoopyNavGraph.kt; backend wajib mengotorisasi setiap request. Release sekarang hanya landing placeholder dan API .invalid, belum untuk distribusi.

TODO: implementasikan DTO/repository/service, wiring AppContainer/factory, auth interceptor tanpa logging credential, SavedStateHandle untuk ID route, cancellation/timeout/retry, debounce pencarian, tombol disabled dan idempotency server. Jangan mengirim foto/alamat melalui route.

Referensi: [Compose compiler plugin](https://developer.android.com/jetpack/androidx/releases/compose-kotlin), [Room](https://developer.android.com/jetpack/androidx/releases/room), [DataStore](https://developer.android.com/jetpack/androidx/releases/datastore).

Dokumentasi resource: file XML/aset tetap diedit di app/src/main/res. Task prepareMainResources menyalinnya ke build/generated/loopyRes/main tanpa Markdown sebelum kompilasi, karena Android menolak README.md di res/values dan res/xml. Jangan edit salinan generated.
