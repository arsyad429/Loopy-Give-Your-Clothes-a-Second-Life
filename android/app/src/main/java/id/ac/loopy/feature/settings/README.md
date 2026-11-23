# settings

Template fitur S12 Settings. UI bisnis belum diimplementasikan; kerjakan Screen/UiState/ViewModel terpisah.

## Isi file dan perannya

| File atau folder | Kegunaan |
| --- | --- |
| [`SettingsScreen.kt`](SettingsScreen.kt) | Template S12: Preferensi non-sensitif DataStore; ganti password/username memerlukan kontrak backend dan persetujuan scope. |
| [`SettingsUiState.kt`](SettingsUiState.kt) | TEMPLATE S12 (Design extension) — belum ada implementasi bisnis. TODO: buat SettingsUiState: data class immutable berisi nilai form/data, field errors, loading, error, serta event sukses tanpa menyimpan NavController. Kebutuhan: Preferensi non-sensitif DataStore; ganti password/username memerlukan kontrak backend dan persetujuan scope. Definisi selesai: acceptance criteria pada docs/acceptance-tests.md terkait fitur ini lulus. |
| [`SettingsViewModel.kt`](SettingsViewModel.kt) | TEMPLATE S12 (Design extension) — belum ada implementasi bisnis. TODO: buat SettingsViewModel: ViewModel dengan private MutableStateFlow, StateFlow immutable, viewModelScope, cancellation, loading/success/error dan cegah request berulang. Kebutuhan: Preferensi non-sensitif DataStore; ganti password/username memerlukan kontrak backend dan persetujuan scope. Definisi selesai: acceptance criteria pada docs/acceptance-tests.md terkait fitur ini lulus. |

## TODO

- [ ] **S12 / Design extension**: Preferensi non-sensitif DataStore; ganti password/username memerlukan kontrak backend dan persetujuan scope.
- [ ] Screen stateless menerima state/callback; UiState immutable; ViewModel via repository, StateFlow dan viewModelScope.
- [ ] Loading/error/empty, cancellation/retry dan anti double-submit, sesuai fitur.
- [ ] Cocokkan node di docs/screen-map.md dan lengkapi AC terkait sebelum menandai selesai.
