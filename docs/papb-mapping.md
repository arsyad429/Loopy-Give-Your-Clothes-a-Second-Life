# Pemetaan seluruh materi PAPB

Dibaca dari 10 PDF di /home/arsyad/Data/PAPB. Nama/ukuran/checksum di reference/papb-catalog.json. Kolaborator tidak memerlukan path absolut tersebut; PDF tidak dipublikasikan. Versi handout dapat berbeda dari baseline build yang dipin di repo.

| Materi sumber | Konsep | Lokasi penerapan | Status/TODO |
| --- | --- | --- | --- |
| Handout Dasar Kotlin dan Android.pdf | Kotlin/null safety/function/class/collection, struktur Android/Activity | domain/model, MainActivity, manifest, Gradle | Isi data class tujuh entitas dan validasi. |
| Modul Praktikum-FILKOM UB-Mobile_1_Debug_SDK_LIBRARY_MAIN ACTIVITY.pdf | Studio, SDK, dependency, build/debug | wrapper/app Gradle/MainActivity | SDK36, breakpoint/Logcat/APK. |
| Modul 2- Konsep Dasar UI Jetpack Compose.pdf | Text/Image/Button, Column/Row/Box, Modifier, state | feature Screens/ui components | Implementasikan layout Figma dan Preview. |
| Modul Praktikum-FILKOM UB-Mobile_2_Activity LifeCycle_Composable_Modifier.pdf | Lifecycle/setContent/Preview/Modifier/external Intent | MainActivity, clothingcheck | Rotasi/kamera/galeri dan lifecycle Logcat. |
| Modul PAPB 3- Konsep State dan Rekomposisi.pdf | remember/mutableStateOf/hoisting/recomposition | feature UiState/Screens | Isi form/checkbox/dialog, rememberSaveable, callback hoisting. |
| Modul_Praktikum_05_Navigation_Compose_FILKOM_UB.pdf | NavHost/controller/route/args/back stack | ui/LoopyApp, ui/navigation | Harness aktif; isi graph sesi, ID args/SavedStateHandle. |
| Modul Praktikum 06 Arsitektur Android FILKOM UB.pdf | UI/ViewModel/Repository/Data Source | TemplateViewModel/FoundationRepository/AppContainer | Fondasi aktif; terapkan pada semua fitur. |
| Modul Praktikum 07 REST API Retrofit.pdf | REST/JSON/DTO/Retrofit/remote data, loading/error | HealthApi/HealthDto/data remote | Health aktif; endpoint/mapper bisnis TODO. Modul08 mengganti callback dengan suspend. |
| Modul Praktikum 08 Kotlin Coroutines Flow.pdf | suspend/viewModelScope/StateFlow/operators/lifecycle | TemplateViewModel, MainActivity, DonationSearch | combine/stateIn/suspend/collectAsStateWithLifecycle aktif; isi debounce/distinctUntilChanged/collectLatest pencarian. |
| Modul Praktikum 09 (Beranotasi).pdf | DataStore/Room/entity/DAO/Flow/KSP | UserPreferencesDataSource/PickupDraft/LoopyDatabase/schemas | Tema+draft aktif; tambah cache per user/migration. Ikuti koreksi anotasi KSP/wiring. |

Setiap materi memiliki tempat dan TODO; seluruh fitur bisnis tetap belum diimplementasikan. Demo fondasi untuk mempelajari aliran dan penyimpanan sebelum mengisi fitur.
