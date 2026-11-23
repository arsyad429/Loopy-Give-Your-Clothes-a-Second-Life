# admin

Pratinjau A02 dan A03 mendukung antrean, detail, penjadwalan, dan penolakan dengan data lokal. Dashboard A01 serta ViewModel jaringan masih template.

## Isi file dan perannya

| File atau folder | Kegunaan |
| --- | --- |
| [`AdminDashboardScreen.kt`](AdminDashboardScreen.kt) | Template A01: Guard admin server; metrik total/pending/scheduled/completed/rejected; navigasi requests; logout. |
| [`AdminDashboardUiState.kt`](AdminDashboardUiState.kt) | TEMPLATE A01 (F07) — belum ada implementasi bisnis. TODO: buat AdminDashboardUiState: data class immutable berisi nilai form/data, field errors, loading, error, serta event sukses tanpa menyimpan NavController. Kebutuhan: Guard admin server; metrik total/pending/scheduled/completed/rejected; navigasi requests; logout. Definisi selesai: acceptance criteria pada docs/acceptance-tests.md terkait fitur ini lulus. |
| [`AdminDashboardViewModel.kt`](AdminDashboardViewModel.kt) | TEMPLATE A01 (F07) — belum ada implementasi bisnis. TODO: buat AdminDashboardViewModel: ViewModel dengan private MutableStateFlow, StateFlow immutable, viewModelScope, cancellation, loading/success/error dan cegah request berulang. Kebutuhan: Guard admin server; metrik total/pending/scheduled/completed/rejected; navigasi requests; logout. Definisi selesai: acceptance criteria pada docs/acceptance-tests.md terkait fitur ini lulus. |
| [`AdminDonationDetailScreen.kt`](AdminDonationDetailScreen.kt) | UI detail hasil, foto, data pickup, pemilihan tanggal dan waktu, serta callback keputusan. |
| [`AdminDonationDetailUiState.kt`](AdminDonationDetailUiState.kt) | Model pratinjau peninjauan donasi, beserta contoh status yang dipakai di katalog debug. |
| [`AdminDonationDetailViewModel.kt`](AdminDonationDetailViewModel.kt) | TEMPLATE A03 (F07/F08/F09) — belum ada implementasi bisnis. TODO: buat AdminDonationDetailViewModel: ViewModel dengan private MutableStateFlow, StateFlow immutable, viewModelScope, cancellation, loading/success/error dan cegah request berulang. Kebutuhan: Foto + hasil AI + alamat; accept wajib jadwal; reject wajib alasan; Pending → Scheduled → Picked Up → Completed; audit history; completed +10 sekali secara atomik di backend. Definisi selesai: acceptance criteria pada docs/acceptance-tests.md terkait fitur ini lulus. |
| [`DonationRequestsScreen.kt`](DonationRequestsScreen.kt) | Daftar dan filter pengajuan, status, serta callback pembuka detail berdasarkan ID. |
| [`DonationRequestsUiState.kt`](DonationRequestsUiState.kt) | TEMPLATE A02 (F07) — belum ada implementasi bisnis. TODO: buat DonationRequestsUiState: data class immutable berisi nilai form/data, field errors, loading, error, serta event sukses tanpa menyimpan NavController. Kebutuhan: Guard admin; daftar/filter/pencarian requests; empty/error/loading; buka detail dengan ID. Definisi selesai: acceptance criteria pada docs/acceptance-tests.md terkait fitur ini lulus. |
| [`DonationRequestsViewModel.kt`](DonationRequestsViewModel.kt) | TEMPLATE A02 (F07) — belum ada implementasi bisnis. TODO: buat DonationRequestsViewModel: ViewModel dengan private MutableStateFlow, StateFlow immutable, viewModelScope, cancellation, loading/success/error dan cegah request berulang. Kebutuhan: Guard admin; daftar/filter/pencarian requests; empty/error/loading; buka detail dengan ID. Definisi selesai: acceptance criteria pada docs/acceptance-tests.md terkait fitur ini lulus. |

## TODO

- [ ] **A01 / F07**: Guard admin server; metrik total/pending/scheduled/completed/rejected; navigasi requests; logout.
- [ ] **A02 / F07**: Guard admin; daftar/filter/pencarian requests; empty/error/loading; buka detail dengan ID.
- [ ] **A03 / F07/F08/F09**: Foto + hasil AI + alamat; accept wajib jadwal; reject wajib alasan; Pending → Scheduled → Picked Up → Completed; audit history; completed +10 sekali secara atomik di backend.
- [ ] Screen stateless menerima state/callback; UiState immutable; ViewModel via repository, StateFlow dan viewModelScope.
- [ ] Loading/error/empty, cancellation/retry dan anti double-submit, sesuai fitur.
- [ ] Cocokkan node di docs/screen-map.md dan lengkapi AC terkait sebelum menandai selesai.
