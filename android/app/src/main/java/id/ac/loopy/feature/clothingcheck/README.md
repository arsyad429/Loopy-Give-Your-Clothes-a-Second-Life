# clothingcheck

Template fitur S05 AI Clothing Detector, S06 Clothing Check Result. UI bisnis belum diimplementasikan; kerjakan Screen/UiState/ViewModel terpisah.

## Isi file dan perannya

| File/folder | Peran dan isi yang diharapkan |
| --- | --- |
| `CheckResultScreen.kt` | Template S06: Foto, jenis, hasil, deskripsi; tiga kategori; eligible saja boleh pickup; konfirmasi pakaian bersih dan aman; hasil AI estimasi awal. |
| `CheckResultUiState.kt` | TEMPLATE S06 (F05) — belum ada implementasi bisnis. TODO: buat CheckResultUiState: data class immutable berisi nilai form/data, field errors, loading, error, serta event sukses tanpa menyimpan NavController. Kebutuhan: Foto, jenis, hasil, deskripsi; tiga kategori; eligible saja boleh pickup; konfirmasi pakaian bersih dan aman; hasil AI estimasi awal. Definisi selesai: acceptance criteria pada docs/acceptance-tests.md terkait fitur ini lulus. |
| `CheckResultViewModel.kt` | TEMPLATE S06 (F05) — belum ada implementasi bisnis. TODO: buat CheckResultViewModel: ViewModel dengan private MutableStateFlow, StateFlow immutable, viewModelScope, cancellation, loading/success/error dan cegah request berulang. Kebutuhan: Foto, jenis, hasil, deskripsi; tiga kategori; eligible saja boleh pickup; konfirmasi pakaian bersih dan aman; hasil AI estimasi awal. Definisi selesai: acceptance criteria pada docs/acceptance-tests.md terkait fitur ini lulus. |
| `ClothingDetectorScreen.kt` | Template S05: Photo Picker dan kamera Activity Result + FileProvider; validasi MIME/ukuran/kejelasan; upload ke backend AI; loading/error/retry; gagal tidak boleh lanjut. |
| `ClothingDetectorUiState.kt` | TEMPLATE S05 (F04) — belum ada implementasi bisnis. TODO: buat ClothingDetectorUiState: data class immutable berisi nilai form/data, field errors, loading, error, serta event sukses tanpa menyimpan NavController. Kebutuhan: Photo Picker dan kamera Activity Result + FileProvider; validasi MIME/ukuran/kejelasan; upload ke backend AI; loading/error/retry; gagal tidak boleh lanjut. Definisi selesai: acceptance criteria pada docs/acceptance-tests.md terkait fitur ini lulus. |
| `ClothingDetectorViewModel.kt` | TEMPLATE S05 (F04) — belum ada implementasi bisnis. TODO: buat ClothingDetectorViewModel: ViewModel dengan private MutableStateFlow, StateFlow immutable, viewModelScope, cancellation, loading/success/error dan cegah request berulang. Kebutuhan: Photo Picker dan kamera Activity Result + FileProvider; validasi MIME/ukuran/kejelasan; upload ke backend AI; loading/error/retry; gagal tidak boleh lanjut. Definisi selesai: acceptance criteria pada docs/acceptance-tests.md terkait fitur ini lulus. |

## TODO

- [ ] **S05 / F04**: Photo Picker dan kamera Activity Result + FileProvider; validasi MIME/ukuran/kejelasan; upload ke backend AI; loading/error/retry; gagal tidak boleh lanjut.
- [ ] **S06 / F05**: Foto, jenis, hasil, deskripsi; tiga kategori; eligible saja boleh pickup; konfirmasi pakaian bersih dan aman; hasil AI estimasi awal.
- [ ] Screen stateless menerima state/callback; UiState immutable; ViewModel via repository, StateFlow dan viewModelScope.
- [ ] Loading/error/empty, cancellation/retry dan anti double-submit, sesuai fitur.
- [ ] Cocokkan node di docs/screen-map.md dan lengkapi AC terkait sebelum menandai selesai.
