# pickup

Template fitur S07 Pickup Request Form. UI bisnis belum diimplementasikan; kerjakan Screen/UiState/ViewModel terpisah.

## Isi file dan perannya

| File/folder | Peran dan isi yang diharapkan |
| --- | --- |
| `PickupRequestScreen.kt` | Template S07: Nama akun; telepon; alamat; kota; foto/jenis dari check; quantity >= 1; tanggal tidak lampau; preferensi waktu; notes opsional; Pending Review; konfirmasi sukses. |
| `PickupRequestUiState.kt` | TEMPLATE S07 (F06) — belum ada implementasi bisnis. TODO: buat PickupRequestUiState: data class immutable berisi nilai form/data, field errors, loading, error, serta event sukses tanpa menyimpan NavController. Kebutuhan: Nama akun; telepon; alamat; kota; foto/jenis dari check; quantity >= 1; tanggal tidak lampau; preferensi waktu; notes opsional; Pending Review; konfirmasi sukses. Definisi selesai: acceptance criteria pada docs/acceptance-tests.md terkait fitur ini lulus. |
| `PickupRequestViewModel.kt` | TEMPLATE S07 (F06) — belum ada implementasi bisnis. TODO: buat PickupRequestViewModel: ViewModel dengan private MutableStateFlow, StateFlow immutable, viewModelScope, cancellation, loading/success/error dan cegah request berulang. Kebutuhan: Nama akun; telepon; alamat; kota; foto/jenis dari check; quantity >= 1; tanggal tidak lampau; preferensi waktu; notes opsional; Pending Review; konfirmasi sukses. Definisi selesai: acceptance criteria pada docs/acceptance-tests.md terkait fitur ini lulus. |

## TODO

- [ ] **S07 / F06**: Nama akun; telepon; alamat; kota; foto/jenis dari check; quantity >= 1; tanggal tidak lampau; preferensi waktu; notes opsional; Pending Review; konfirmasi sukses.
- [ ] Screen stateless menerima state/callback; UiState immutable; ViewModel via repository, StateFlow dan viewModelScope.
- [ ] Loading/error/empty, cancellation/retry dan anti double-submit, sesuai fitur.
- [ ] Cocokkan node di docs/screen-map.md dan lengkapi AC terkait sebelum menandai selesai.
