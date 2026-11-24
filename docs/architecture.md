# Arsitektur Loopy

Aliran Android: **Composable → ViewModel → Repository → remote API / DataStore / Room**. State kembali melalui Flow/StateFlow dan collectAsStateWithLifecycle. Manual DI di AppContainer menjaga satu instance DB/DataStore per proses. Versi dipin untuk baseline build, bukan klaim dependency terbaru.

Fondasi aktif: MainActivity/lifecycle Logcat, TemplateViewModel, FoundationRepository, tema DataStore, draft Room, Retrofit suspend GET /health, Navigation Compose ke placeholder. State bisnis berikutnya berada di ViewModel; remember/rememberSaveable dipakai sesuai kebutuhan state lokal form/dialog. Draft contoh memakai ID tetap agar Simpan berulang tidak menggandakan data.

DonationStatus mengatur transisi untuk UI. Model/kontrak bisnis masih TODO; UI final wajib stateless, dengan preview, aksesibilitas, Column/Row/Box, Modifier, LazyColumn, hoisting dan recomposition sesuai PAPB.

Compose utama membangun **Go + PostgreSQL** untuk autentikasi akun, pemeriksaan pakaian, dan peninjauan donasi. Endpoint pemeriksaan memakai sesi login yang sama dan menyimpan foto JPEG privat serta key idempotensi di PostgreSQL. Pengajuan pickup pengguna, pelacakan, poin, dan rewards masih direncanakan. [Panduan backend Loopy](backend.md)

DataStore untuk preferensi tema/onboarding/filter; Room untuk draft/cache. Backend sumber kebenaran role, status, poin dan redemption. Sesi uji disimpan di memori dan refresh diserialkan. Penyimpanan sesi lintas proses memerlukan Keystore. Password dan API key Groq tidak disimpan oleh Android. Cache bisnis wajib per user dan dibersihkan saat logout. S05 mengambil gambar lewat Photo Picker dan Activity Result kamera dengan URI dari FileProvider. S06 membaca hasil lewat kontrak repository yang sama dengan S05.

Debug membuka katalog pratinjau. `LoopyApp.kt` mengarahkan pengguna sesuai role dari login dan mensyaratkan sesi `USER` untuk alur S04–S06. Backend memeriksa sesi dan hak akses pada setiap request. Release membuka landing dan membutuhkan konfigurasi URL HTTPS.

S05 meneruskan `checkId` ke S06. S06 memuat metadata dan foto lewat endpoint Go yang terpisah. Repository upload mempertahankan key idempotensi ketika payload yang sama dicoba ulang setelah gagal.

Referensi: [Compose compiler plugin](https://developer.android.com/jetpack/androidx/releases/compose-kotlin), [Room](https://developer.android.com/jetpack/androidx/releases/room), [DataStore](https://developer.android.com/jetpack/androidx/releases/datastore).

Dokumentasi resource: file XML/aset tetap diedit di app/src/main/res. Task prepareMainResources menyalinnya ke build/generated/loopyRes/main tanpa Markdown sebelum kompilasi, karena Android menolak README.md di res/values dan res/xml. Jangan edit salinan generated.
