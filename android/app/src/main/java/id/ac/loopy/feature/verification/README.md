# verification

Template fitur S13 Verifikasi Email. UI bisnis belum diimplementasikan; kerjakan Screen/UiState/ViewModel terpisah.

## Isi file dan perannya

| File/folder | Peran dan isi yang diharapkan |
| --- | --- |
| `EmailVerificationScreen.kt` | Template S13: OTP expiry/resend/rate limit/percobaan gagal; alur belum diwajibkan PRD 2.1, sepakati sebelum implementasi. |
| `EmailVerificationUiState.kt` | TEMPLATE S13 (Design extension) — belum ada implementasi bisnis. TODO: buat EmailVerificationUiState: data class immutable berisi nilai form/data, field errors, loading, error, serta event sukses tanpa menyimpan NavController. Kebutuhan: OTP expiry/resend/rate limit/percobaan gagal; alur belum diwajibkan PRD 2.1, sepakati sebelum implementasi. Definisi selesai: acceptance criteria pada docs/acceptance-tests.md terkait fitur ini lulus. |
| `EmailVerificationViewModel.kt` | TEMPLATE S13 (Design extension) — belum ada implementasi bisnis. TODO: buat EmailVerificationViewModel: ViewModel dengan private MutableStateFlow, StateFlow immutable, viewModelScope, cancellation, loading/success/error dan cegah request berulang. Kebutuhan: OTP expiry/resend/rate limit/percobaan gagal; alur belum diwajibkan PRD 2.1, sepakati sebelum implementasi. Definisi selesai: acceptance criteria pada docs/acceptance-tests.md terkait fitur ini lulus. |

## TODO

- [ ] **S13 / Design extension**: OTP expiry/resend/rate limit/percobaan gagal; alur belum diwajibkan PRD 2.1, sepakati sebelum implementasi.
- [ ] Screen stateless menerima state/callback; UiState immutable; ViewModel via repository, StateFlow dan viewModelScope.
- [ ] Loading/error/empty, cancellation/retry dan anti double-submit, sesuai fitur.
- [ ] Cocokkan node di docs/screen-map.md dan lengkapi AC terkait sebelum menandai selesai.
