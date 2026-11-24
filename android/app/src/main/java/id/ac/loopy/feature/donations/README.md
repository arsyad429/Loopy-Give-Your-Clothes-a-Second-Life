# donations

S08–S09 menampilkan daftar/filter, detail per ID, dan timeline pada demo Room debug tanpa sesi login. API pembacaan donasi pemilik sudah tersedia; integrasi Android ke akun server serta POST pengajuan pickup masih menunggu implementasi.

## Isi file dan perannya

| File atau folder | Kegunaan |
| --- | --- |
| [`DonationDetailScreen.kt`](DonationDetailScreen.kt) | Template S09: Timeline lima status; jadwal preferensi dan jadwal final dibedakan; alasan rejected; foto/detail; pengguna tidak dapat mengubah status. |
| [`DonationDetailUiState.kt`](DonationDetailUiState.kt) | TEMPLATE S09 (F08) — belum ada implementasi bisnis. TODO: buat DonationDetailUiState: data class immutable berisi nilai form/data, field errors, loading, error, serta event sukses tanpa menyimpan NavController. Kebutuhan: Timeline lima status; jadwal preferensi dan jadwal final dibedakan; alasan rejected; foto/detail; pengguna tidak dapat mengubah status. Definisi selesai: acceptance criteria pada docs/acceptance-tests.md terkait fitur ini lulus. |
| [`DonationDetailViewModel.kt`](DonationDetailViewModel.kt) | TEMPLATE S09 (F08) — belum ada implementasi bisnis. TODO: buat DonationDetailViewModel: ViewModel dengan private MutableStateFlow, StateFlow immutable, viewModelScope, cancellation, loading/success/error dan cegah request berulang. Kebutuhan: Timeline lima status; jadwal preferensi dan jadwal final dibedakan; alasan rejected; foto/detail; pengguna tidak dapat mengubah status. Definisi selesai: acceptance criteria pada docs/acceptance-tests.md terkait fitur ini lulus. |
| [`MyDonationsScreen.kt`](MyDonationsScreen.kt) | Template S08: Daftar donasi user aktif saja; loading/empty/error; refresh status terbaru; klik ID ke detail; filter/pencarian reaktif Flow. |
| [`MyDonationsUiState.kt`](MyDonationsUiState.kt) | TEMPLATE S08 (F08) — belum ada implementasi bisnis. TODO: buat MyDonationsUiState: data class immutable berisi nilai form/data, field errors, loading, error, serta event sukses tanpa menyimpan NavController. Kebutuhan: Daftar donasi user aktif saja; loading/empty/error; refresh status terbaru; klik ID ke detail; filter/pencarian reaktif Flow. Definisi selesai: acceptance criteria pada docs/acceptance-tests.md terkait fitur ini lulus. |
| [`MyDonationsViewModel.kt`](MyDonationsViewModel.kt) | TEMPLATE S08 (F08) — belum ada implementasi bisnis. TODO: buat MyDonationsViewModel: ViewModel dengan private MutableStateFlow, StateFlow immutable, viewModelScope, cancellation, loading/success/error dan cegah request berulang. Kebutuhan: Daftar donasi user aktif saja; loading/empty/error; refresh status terbaru; klik ID ke detail; filter/pencarian reaktif Flow. Definisi selesai: acceptance criteria pada docs/acceptance-tests.md terkait fitur ini lulus. |

## TODO

- [ ] **S08 / F08**: Daftar donasi user aktif saja; loading/empty/error; refresh status terbaru; klik ID ke detail; filter/pencarian reaktif Flow.
- [ ] **S09 / F08**: Timeline lima status; jadwal preferensi dan jadwal final dibedakan; alasan rejected; foto/detail; pengguna tidak dapat mengubah status.
- [ ] Screen stateless menerima state/callback; UiState immutable; ViewModel via repository, StateFlow dan viewModelScope.
- [ ] Loading/error/empty, cancellation/retry dan anti double-submit, sesuai fitur.
- [ ] Cocokkan node di docs/screen-map.md dan lengkapi AC terkait sebelum menandai selesai.
