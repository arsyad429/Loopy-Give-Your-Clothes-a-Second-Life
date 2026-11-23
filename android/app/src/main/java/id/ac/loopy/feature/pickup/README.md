# pickup

Rute S07 menerima checkId dari hasil yang berpotensi layak. Form dan pengajuan pickup masih dalam rencana.

## Isi file dan perannya

| File atau folder | Kegunaan |
| --- | --- |
| [`PickupRequestScreen.kt`](PickupRequestScreen.kt) | Template S07: Nama akun; telepon; alamat; kota; foto/jenis dari check; quantity >= 1; tanggal tidak lampau; preferensi waktu; notes opsional; Pending Review; konfirmasi sukses. |
| [`PickupRequestUiState.kt`](PickupRequestUiState.kt) | TEMPLATE S07 (F06) — belum ada implementasi bisnis. TODO: buat PickupRequestUiState: data class immutable berisi nilai form/data, field errors, loading, error, serta event sukses tanpa menyimpan NavController. Kebutuhan: Nama akun; telepon; alamat; kota; foto/jenis dari check; quantity >= 1; tanggal tidak lampau; preferensi waktu; notes opsional; Pending Review; konfirmasi sukses. Definisi selesai: acceptance criteria pada docs/acceptance-tests.md terkait fitur ini lulus. |
| [`PickupRequestViewModel.kt`](PickupRequestViewModel.kt) | TEMPLATE S07 (F06) — belum ada implementasi bisnis. TODO: buat PickupRequestViewModel: ViewModel dengan private MutableStateFlow, StateFlow immutable, viewModelScope, cancellation, loading/success/error dan cegah request berulang. Kebutuhan: Nama akun; telepon; alamat; kota; foto/jenis dari check; quantity >= 1; tanggal tidak lampau; preferensi waktu; notes opsional; Pending Review; konfirmasi sukses. Definisi selesai: acceptance criteria pada docs/acceptance-tests.md terkait fitur ini lulus. |

## TODO

- [ ] **S07 / F06**: Nama akun; telepon; alamat; kota; foto/jenis dari check; quantity >= 1; tanggal tidak lampau; preferensi waktu; notes opsional; Pending Review; konfirmasi sukses.
- [ ] Screen stateless menerima state/callback; UiState immutable; ViewModel via repository, StateFlow dan viewModelScope.
- [ ] Loading/error/empty, cancellation/retry dan anti double-submit, sesuai fitur.
- [ ] Cocokkan node di docs/screen-map.md dan lengkapi AC terkait sebelum menandai selesai.
