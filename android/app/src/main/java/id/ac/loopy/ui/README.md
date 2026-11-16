# ui

Direktori ui mengelompokkan source/config Loopy. README subfolder menjelaskan implementasi spesifik.

## Isi file dan perannya

| File/folder | Peran dan isi yang diharapkan |
| --- | --- |
| `LoopyApp.kt` | NavHost katalog debug 13+4 layar, tema/draft/health. Release hanya placeholder landing. |
| `TemplateViewModel.kt` | StateFlow combine/stateIn, coroutine scope, loading/error/cancellation untuk demo fondasi. |
| `components/` | Baca README di subfolder components untuk peran file dan TODO. |
| `navigation/` | Baca README di subfolder navigation untuk peran file dan TODO. |
| `theme/` | Baca README di subfolder theme untuk peran file dan TODO. |

## TODO

- [ ] Lanjutkan file template sesuai docs/backlog.md, docs/api-contract.md dan docs/acceptance-tests.md.
- [ ] Pertahankan UI → ViewModel → Repository → Data Source dan konfigurasi yang dapat dibuild.
- [ ] Perbarui daftar file dan bukti cek setelah implementasi berubah.
