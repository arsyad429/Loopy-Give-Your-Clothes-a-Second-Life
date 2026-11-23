# landing

Landing S01 memperkenalkan Loopy dan menyediakan aksi menuju login atau registrasi.

## Isi file dan perannya

| File atau folder | Kegunaan |
| --- | --- |
| [`LandingScreen.kt`](LandingScreen.kt) | Composable landing dengan pengenalan layanan, manfaat, langkah penggunaan, serta navigasi aksi. |
| [`LandingUiState.kt`](LandingUiState.kt) | TEMPLATE S01 (F01) — belum ada implementasi bisnis. TODO: buat LandingUiState: data class immutable berisi nilai form/data, field errors, loading, error, serta event sukses tanpa menyimpan NavController. Kebutuhan: Hero dan tagline; About Loopy; tiga benefit; empat langkah; Get Started → Register; Login → Login. Definisi selesai: acceptance criteria pada docs/acceptance-tests.md terkait fitur ini lulus. |
| [`LandingViewModel.kt`](LandingViewModel.kt) | TEMPLATE S01 (F01) — belum ada implementasi bisnis. TODO: buat LandingViewModel: ViewModel dengan private MutableStateFlow, StateFlow immutable, viewModelScope, cancellation, loading/success/error dan cegah request berulang. Kebutuhan: Hero dan tagline; About Loopy; tiga benefit; empat langkah; Get Started → Register; Login → Login. Definisi selesai: acceptance criteria pada docs/acceptance-tests.md terkait fitur ini lulus. |

## TODO

- [ ] **S01 / F01**: Hero dan tagline; About Loopy; tiga benefit; empat langkah; Get Started → Register; Login → Login.
- [ ] Screen stateless menerima state/callback; UiState immutable; ViewModel via repository, StateFlow dan viewModelScope.
- [ ] Loading/error/empty, cancellation/retry dan anti double-submit, sesuai fitur.
- [ ] Cocokkan node di docs/screen-map.md dan lengkapi AC terkait sebelum menandai selesai.
